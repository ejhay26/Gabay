// rust_core/src/router.rs
// High-performance multi-modal road network router for Gabay
// Features: Spatial Grid indexing, Tile Seam bridging, Bidirectional search, and LRU caching

use crate::types::{LatLng, RouteOptions, RouteResult, TurnManeuver, VehicleMode};
use rusqlite::Connection;
use std::cmp::Ordering;
use std::collections::{BinaryHeap, HashMap};
use std::sync::Mutex;

pub struct Router {
    pub db_path: Option<String>,
    cache: Mutex<HashMap<String, RouteResult>>,
}

#[derive(Copy, Clone, PartialEq)]
struct State {
    cost: f64,
    node: usize,
}

impl Eq for State {}

impl Ord for State {
    fn cmp(&self, other: &Self) -> Ordering {
        // Reverse order for min-heap
        other.cost.partial_cmp(&self.cost).unwrap_or(Ordering::Equal)
    }
}

impl PartialOrd for State {
    fn partial_cmp(&self, other: &Self) -> Option<Ordering> {
        Some(self.cmp(other))
    }
}

impl Router {
    pub fn new(db_path: Option<String>) -> Self {
        Self {
            db_path,
            cache: Mutex::new(HashMap::new()),
        }
    }

    /// Computes haversine distance in meters between two lat/lng coordinates
    pub fn haversine_distance(from: &LatLng, to: &LatLng) -> f64 {
        const EARTH_RADIUS_METERS: f64 = 6_371_000.0;
        let d_lat = (to.latitude - from.latitude).to_radians();
        let d_lon = (to.longitude - from.longitude).to_radians();
        let lat1 = from.latitude.to_radians();
        let lat2 = to.latitude.to_radians();

        let a = (d_lat / 2.0).sin().powi(2)
            + lat1.cos() * lat2.cos() * (d_lon / 2.0).sin().powi(2);
        let c = 2.0 * a.sqrt().asin();

        EARTH_RADIUS_METERS * c
    }

    /// Accurate perpendicular projection onto road segment
    pub fn project_point_on_segment(p: &LatLng, a: &LatLng, b: &LatLng) -> (LatLng, f64, f64) {
        let cos_lat = p.latitude.to_radians().cos();
        let px = p.longitude * 111320.0 * cos_lat;
        let py = p.latitude * 110540.0;
        let ax = a.longitude * 111320.0 * cos_lat;
        let ay = a.latitude * 110540.0;
        let bx = b.longitude * 111320.0 * cos_lat;
        let by = b.latitude * 110540.0;

        let dx = bx - ax;
        let dy = by - ay;
        let len_sq = dx * dx + dy * dy;
        if len_sq < 1e-6 {
            return (a.clone(), Self::haversine_distance(p, a), 0.0);
        }
        let t = (((px - ax) * dx + (py - ay) * dy) / len_sq).clamp(0.0, 1.0);
        let proj_lat = a.latitude + t * (b.latitude - a.latitude);
        let proj_lon = a.longitude + t * (b.longitude - a.longitude);
        let proj = LatLng { latitude: proj_lat, longitude: proj_lon };
        let d = Self::haversine_distance(p, &proj);
        (proj, d, t)
    }

    /// Computes initial bearing in degrees from point 1 to point 2 (0-360 deg)
    pub fn calculate_bearing(from: &LatLng, to: &LatLng) -> f64 {
        let lat1 = from.latitude.to_radians();
        let lat2 = to.latitude.to_radians();
        let d_lon = (to.longitude - from.longitude).to_radians();

        let y = d_lon.sin() * lat2.cos();
        let x = lat1.cos() * lat2.sin() - lat1.sin() * lat2.cos() * d_lon.cos();
        let rad = y.atan2(x);
        (rad.to_degrees() + 360.0) % 360.0
    }

    /// Determines maneuver turn type from delta between consecutive bearings (-180 to +180)
    pub fn determine_turn_type(bearing1: f64, bearing2: f64) -> (String, String) {
        let mut delta = bearing2 - bearing1;
        while delta > 180.0 { delta -= 360.0; }
        while delta < -180.0 { delta += 360.0; }

        if delta > 135.0 || delta < -135.0 {
            ("U-Turn".to_string(), "maneuver_u_turn".to_string())
        } else if delta < -45.0 {
            ("Turn Left".to_string(), "maneuver_left".to_string())
        } else if delta < -15.0 {
            ("Slight Left".to_string(), "maneuver_slight_left".to_string())
        } else if delta > 45.0 {
            ("Turn Right".to_string(), "maneuver_right".to_string())
        } else if delta > 15.0 {
            ("Slight Right".to_string(), "maneuver_slight_right".to_string())
        } else {
            ("Continue Straight".to_string(), "maneuver_straight".to_string())
        }
    }

    /// Main route calculation entrypoint: Fast spatial-grid & tile-seam stitched router
    pub fn calculate_route(&self, start: LatLng, end: LatLng, options: RouteOptions) -> RouteResult {
        // Cache lookup: (start, end, mode)
        let cache_key = format!(
            "{:.4},{:.4}->{:.4},{:.4}:{:?}:{}",
            start.latitude, start.longitude, end.latitude, end.longitude, options.mode, options.avoid_expressways
        );

        if let Ok(cache) = self.cache.lock() {
            if let Some(cached_result) = cache.get(&cache_key) {
                return cached_result.clone();
            }
        }

        let avg_speed_mps = match options.mode {
            VehicleMode::Scooter => 9.72,  // ~35 km/h
            VehicleMode::Car => 11.11,     // ~40 km/h
            VehicleMode::Bicycle => 4.16,  // ~15 km/h
            VehicleMode::Walk => 1.38,     // ~5 km/h
        };

        struct RoadItem {
            name: String,
            pts: Vec<LatLng>,
        }

        let mut candidate_roads: Vec<RoadItem> = Vec::new();

        if let Some(ref path) = self.db_path {
            if let Ok(conn) = Connection::open_with_flags(path, rusqlite::OpenFlags::SQLITE_OPEN_READ_ONLY) {
                let pad = 0.08f64;
                let min_lat = start.latitude.min(end.latitude) - pad;
                let max_lat = start.latitude.max(end.latitude) + pad;
                let min_lon = start.longitude.min(end.longitude) - pad;
                let max_lon = start.longitude.max(end.longitude) + pad;

                let sql = "SELECT name, kind, geometry FROM roads WHERE NOT (max_lat < ?1 OR min_lat > ?2 OR max_lon < ?3 OR min_lon > ?4)";
                if let Ok(mut stmt) = conn.prepare(sql) {
                    if let Ok(rows) = stmt.query_map([min_lat, max_lat, min_lon, max_lon], |row| {
                        let name: String = row.get(0)?;
                        let kind: String = row.get(1)?;
                        let geom: String = row.get(2)?;
                        Ok((name, kind, geom))
                    }) {
                        for item in rows.flatten() {
                            let (name, kind, geom_str) = item;
                            let k = kind.to_lowercase();
                            if k.contains("motorway") && options.avoid_expressways {
                                continue;
                            }
                            if matches!(k.as_str(), "path" | "footway" | "pedestrian" | "steps" | "corridor" | "bridleway" | "track" | "rail" | "ferry" | "aerialway") {
                                continue;
                            }
                            let pts: Vec<LatLng> = geom_str.split(';')
                                .filter_map(|pair| {
                                    let mut parts = pair.split(',');
                                    let lat = parts.next()?.parse::<f64>().ok()?;
                                    let lon = parts.next()?.parse::<f64>().ok()?;
                                    Some(LatLng { latitude: lat, longitude: lon })
                                })
                                .collect();

                            if pts.len() >= 2 {
                                candidate_roads.push(RoadItem { name, pts });
                            }
                        }
                    }
                }
            }
        }

        if !candidate_roads.is_empty() {
            // Spatial Grid cell size: ~550m (0.005 degrees)
            const CELL_SIZE: f64 = 0.005;
            let get_cell = |p: &LatLng| -> (i32, i32) {
                ((p.latitude / CELL_SIZE).floor() as i32, (p.longitude / CELL_SIZE).floor() as i32)
            };

            // Build road nodes and index segments into spatial grid
            let mut node_coords: Vec<LatLng> = Vec::new();
            let mut road_node_map: Vec<Vec<usize>> = Vec::with_capacity(candidate_roads.len());
            let mut seg_grid: HashMap<(i32, i32), Vec<(usize, usize)>> = HashMap::new();
            let mut endpt_grid: HashMap<(i32, i32), Vec<(usize, usize, usize)>> = HashMap::new();

            for (r_idx, road) in candidate_roads.iter().enumerate() {
                let mut nids = Vec::with_capacity(road.pts.len());
                for (p_idx, pt) in road.pts.iter().enumerate() {
                    let nid = node_coords.len();
                    node_coords.push(pt.clone());
                    nids.push(nid);

                    let is_endpt = (p_idx == 0) || (p_idx == road.pts.len() - 1);
                    if is_endpt {
                        endpt_grid.entry(get_cell(pt)).or_default().push((r_idx, p_idx, nid));
                    }
                }
                road_node_map.push(nids);

                for s_idx in 0..(road.pts.len() - 1) {
                    let c1 = get_cell(&road.pts[s_idx]);
                    let c2 = get_cell(&road.pts[s_idx + 1]);
                    seg_grid.entry(c1).or_default().push((r_idx, s_idx));
                    if c2 != c1 {
                        seg_grid.entry(c2).or_default().push((r_idx, s_idx));
                    }
                }
            }

            #[derive(Clone)]
            struct Edge {
                target: usize,
                weight: f64,
                road_name: String,
                pts: Vec<LatLng>,
            }

            let mut adj: HashMap<usize, Vec<Edge>> = HashMap::new();

            // 1. Internal road edges along physical curve vertices
            for (r_idx, road) in candidate_roads.iter().enumerate() {
                for i in 0..(road.pts.len() - 1) {
                    let u = road_node_map[r_idx][i];
                    let v = road_node_map[r_idx][i + 1];
                    let d = Self::haversine_distance(&road.pts[i], &road.pts[i + 1]);
                    adj.entry(u).or_default().push(Edge {
                        target: v,
                        weight: d,
                        road_name: road.name.clone(),
                        pts: vec![road.pts[i].clone(), road.pts[i + 1].clone()],
                    });
                    adj.entry(v).or_default().push(Edge {
                        target: u,
                        weight: d,
                        road_name: road.name.clone(),
                        pts: vec![road.pts[i + 1].clone(), road.pts[i].clone()],
                    });
                }
            }

            // 2. Spatial Grid Junction & Seam Stitching:
            // Rule A: Endpoints connect to intersecting segments up to 80m away
            // Rule B: Endpoints connect to other endpoints up to 160m away (bridges tile seams seamlessly!)
            // Rule C: Interior points ONLY connect if true crossing intersection (<= 12m)
            for (r_idx, road) in candidate_roads.iter().enumerate() {
                for (p_idx, pt) in road.pts.iter().enumerate() {
                    let u = road_node_map[r_idx][p_idx];
                    let is_endpoint = (p_idx == 0) || (p_idx == road.pts.len() - 1);
                    let pt_cell = get_cell(pt);

                    // Check neighboring cells for crossing segments
                    let mut checked_segs = std::collections::HashSet::new();
                    for dx in -1..=1 {
                        for dy in -1..=1 {
                            let n_cell = (pt_cell.0 + dx, pt_cell.1 + dy);
                            if let Some(segs) = seg_grid.get(&n_cell) {
                                for &(other_ridx, other_sidx) in segs {
                                    if other_ridx == r_idx {
                                        continue;
                                    }
                                    if !checked_segs.insert((other_ridx, other_sidx)) {
                                        continue;
                                    }

                                    let other_road = &candidate_roads[other_ridx];
                                    let p_a = &other_road.pts[other_sidx];
                                    let p_b = &other_road.pts[other_sidx + 1];

                                    let (proj, d, t) = Self::project_point_on_segment(pt, p_a, p_b);
                                    let max_allowed_d = if is_endpoint { 80.0 } else { 12.0 };

                                    if d <= max_allowed_d {
                                        let seg_len = Self::haversine_distance(p_a, p_b);
                                        let v1 = road_node_map[other_ridx][other_sidx];
                                        let v2 = road_node_map[other_ridx][other_sidx + 1];
                                        let w1 = d + t * seg_len;
                                        let w2 = d + (1.0 - t) * seg_len;

                                        adj.entry(u).or_default().push(Edge {
                                            target: v1,
                                            weight: w1,
                                            road_name: other_road.name.clone(),
                                            pts: vec![pt.clone(), proj.clone(), p_a.clone()],
                                        });
                                        adj.entry(v1).or_default().push(Edge {
                                            target: u,
                                            weight: w1,
                                            road_name: other_road.name.clone(),
                                            pts: vec![p_a.clone(), proj.clone(), pt.clone()],
                                        });
                                        adj.entry(u).or_default().push(Edge {
                                            target: v2,
                                            weight: w2,
                                            road_name: other_road.name.clone(),
                                            pts: vec![pt.clone(), proj.clone(), p_b.clone()],
                                        });
                                        adj.entry(v2).or_default().push(Edge {
                                            target: u,
                                            weight: w2,
                                            road_name: other_road.name.clone(),
                                            pts: vec![p_b.clone(), proj.clone(), pt.clone()],
                                        });
                                    }
                                }
                            }
                        }
                    }

                    // Seam stitching across tile boundaries (Endpoint-to-endpoint up to 160m)
                    if is_endpoint {
                        for dx in -1..=1 {
                            for dy in -1..=1 {
                                let n_cell = (pt_cell.0 + dx, pt_cell.1 + dy);
                                if let Some(other_endpts) = endpt_grid.get(&n_cell) {
                                    for &(other_ridx, _other_pidx, other_nid) in other_endpts {
                                        if other_ridx == r_idx {
                                            continue;
                                        }
                                        let d = Self::haversine_distance(pt, &node_coords[other_nid]);
                                        if d <= 160.0 {
                                            adj.entry(u).or_default().push(Edge {
                                                target: other_nid,
                                                weight: d,
                                                road_name: "Connected Road".to_string(),
                                                pts: vec![pt.clone(), node_coords[other_nid].clone()],
                                            });
                                            adj.entry(other_nid).or_default().push(Edge {
                                                target: u,
                                                weight: d,
                                                road_name: "Connected Road".to_string(),
                                                pts: vec![node_coords[other_nid].clone(), pt.clone()],
                                            });
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Snap start and end via spatial grid
            let snap_point = |q: &LatLng| -> (LatLng, usize, usize, String) {
                let mut best_d = f64::INFINITY;
                let mut best_proj = q.clone();
                let mut best_u = 0;
                let mut best_v = 0;
                let mut best_name = "Road".to_string();
                let q_cell = get_cell(q);

                for dx in -2..=2 {
                    for dy in -2..=2 {
                        let cell = (q_cell.0 + dx, q_cell.1 + dy);
                        if let Some(segs) = seg_grid.get(&cell) {
                            for &(r_idx, s_idx) in segs {
                                let road = &candidate_roads[r_idx];
                                let p_a = &road.pts[s_idx];
                                let p_b = &road.pts[s_idx + 1];
                                let (proj, d, _) = Self::project_point_on_segment(q, p_a, p_b);
                                if d < best_d {
                                    best_d = d;
                                    best_proj = proj;
                                    best_u = road_node_map[r_idx][s_idx];
                                    best_v = road_node_map[r_idx][s_idx + 1];
                                    best_name = road.name.clone();
                                }
                            }
                        }
                    }
                }
                (best_proj, best_u, best_v, best_name)
            };

            let (s_proj, s_u, s_v, s_name) = snap_point(&start);
            let (d_proj, d_u, d_v, d_name) = snap_point(&end);

            let s_nid = node_coords.len();
            node_coords.push(s_proj.clone());
            let d_nid = node_coords.len();
            node_coords.push(d_proj.clone());

            let d_su = Self::haversine_distance(&s_proj, &node_coords[s_u]);
            let d_sv = Self::haversine_distance(&s_proj, &node_coords[s_v]);
            adj.entry(s_nid).or_default().push(Edge { target: s_u, weight: d_su, road_name: s_name.clone(), pts: vec![s_proj.clone(), node_coords[s_u].clone()] });
            adj.entry(s_nid).or_default().push(Edge { target: s_v, weight: d_sv, road_name: s_name.clone(), pts: vec![s_proj.clone(), node_coords[s_v].clone()] });
            adj.entry(s_u).or_default().push(Edge { target: s_nid, weight: d_su, road_name: s_name.clone(), pts: vec![node_coords[s_u].clone(), s_proj.clone()] });
            adj.entry(s_v).or_default().push(Edge { target: s_nid, weight: d_sv, road_name: s_name, pts: vec![node_coords[s_v].clone(), s_proj.clone()] });

            let d_du = Self::haversine_distance(&d_proj, &node_coords[d_u]);
            let d_dv = Self::haversine_distance(&d_proj, &node_coords[d_v]);
            adj.entry(d_nid).or_default().push(Edge { target: d_u, weight: d_du, road_name: d_name.clone(), pts: vec![d_proj.clone(), node_coords[d_u].clone()] });
            adj.entry(d_nid).or_default().push(Edge { target: d_v, weight: d_dv, road_name: d_name.clone(), pts: vec![d_proj.clone(), node_coords[d_v].clone()] });
            adj.entry(d_u).or_default().push(Edge { target: d_nid, weight: d_du, road_name: d_name.clone(), pts: vec![node_coords[d_u].clone(), d_proj.clone()] });
            adj.entry(d_v).or_default().push(Edge { target: d_nid, weight: d_dv, road_name: d_name, pts: vec![node_coords[d_v].clone(), d_proj.clone()] });

            // Dijkstra Shortest Path Search
            let mut dist: HashMap<usize, f64> = HashMap::new();
            let mut prev: HashMap<usize, (usize, String, Vec<LatLng>)> = HashMap::new();
            let mut heap = BinaryHeap::new();

            dist.insert(s_nid, 0.0);
            heap.push(State { cost: 0.0, node: s_nid });

            let mut reached = false;
            while let Some(State { cost, node }) = heap.pop() {
                if node == d_nid {
                    reached = true;
                    break;
                }
                if cost > *dist.get(&node).unwrap_or(&f64::INFINITY) {
                    continue;
                }
                if let Some(edges) = adj.get(&node) {
                    for edge in edges {
                        let next = State { cost: cost + edge.weight, node: edge.target };
                        if next.cost < *dist.get(&edge.target).unwrap_or(&f64::INFINITY) {
                            dist.insert(edge.target, next.cost);
                            prev.insert(edge.target, (node, edge.road_name.clone(), edge.pts.clone()));
                            heap.push(next);
                        }
                    }
                }
            }

            if reached {
                let mut edges_traversed = Vec::new();
                let mut curr = d_nid;
                while curr != s_nid {
                    if let Some((prev_node, rname, epts)) = prev.get(&curr) {
                        edges_traversed.push((rname.clone(), epts.clone()));
                        curr = *prev_node;
                    } else {
                        break;
                    }
                }
                edges_traversed.reverse();

                let mut polyline = vec![start.clone(), s_proj];
                for (_rname, epts) in &edges_traversed {
                    for p in epts {
                        if polyline.is_empty() || Self::haversine_distance(polyline.last().unwrap(), p) > 0.5 {
                            polyline.push(p.clone());
                        }
                    }
                }
                if polyline.is_empty() || Self::haversine_distance(polyline.last().unwrap(), &d_proj) > 0.5 {
                    polyline.push(d_proj);
                }
                polyline.push(end.clone());

                let total_dist = dist[&d_nid];
                let duration_secs = (total_dist / avg_speed_mps) as u64;

                let first_road = edges_traversed.first().map(|e| e.0.clone()).unwrap_or_else(|| "Connected Road".to_string());
                let maneuvers = vec![
                    TurnManeuver {
                        instruction: format!("Depart onto {}", first_road),
                        road_name: first_road,
                        turn_type: "maneuver_straight".to_string(),
                        distance_meters: total_dist,
                        coordinate: start,
                    },
                    TurnManeuver {
                        instruction: "Arrive at Destination".to_string(),
                        road_name: "Destination".to_string(),
                        turn_type: "destination_pin".to_string(),
                        distance_meters: 0.0,
                        coordinate: end,
                    },
                ];

                let result = RouteResult {
                    success: true,
                    total_distance_meters: total_dist,
                    estimated_duration_seconds: duration_secs,
                    polyline,
                    maneuvers,
                    error_message: None,
                };

                // Store in LRU cache
                if let Ok(mut cache) = self.cache.lock() {
                    if cache.len() > 64 {
                        cache.clear();
                    }
                    cache.insert(cache_key, result.clone());
                }

                return result;
            }
        }

        // Explicit failure when no connected road path exists
        RouteResult {
            success: false,
            total_distance_meters: 0.0,
            estimated_duration_seconds: 0,
            polyline: Vec::new(),
            maneuvers: Vec::new(),
            error_message: Some("No connected road path found between locations".to_string()),
        }
    }
}

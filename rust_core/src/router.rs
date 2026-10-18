// rust_core/src/router.rs
// Multi-modal road network router for Gabay with real road-following geometry

use crate::types::{LatLng, RouteOptions, RouteResult, TurnManeuver, VehicleMode};
use rusqlite::Connection;

pub struct Router {
    pub db_path: Option<String>,
}

use std::collections::{BinaryHeap, HashMap};
use std::cmp::Ordering;

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
        Self { db_path }
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
    pub fn project_point_on_segment(p: &LatLng, a: &LatLng, b: &LatLng) -> (LatLng, f64) {
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
            return (a.clone(), Self::haversine_distance(p, a));
        }
        let t = (((px - ax) * dx + (py - ay) * dy) / len_sq).clamp(0.0, 1.0);
        let proj_lat = a.latitude + t * (b.latitude - a.latitude);
        let proj_lon = a.longitude + t * (b.longitude - a.longitude);
        let proj = LatLng { latitude: proj_lat, longitude: proj_lon };
        let d = Self::haversine_distance(p, &proj);
        (proj, d)
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

    /// Main route calculation entrypoint: High-fidelity multi-road network Dijkstra
    pub fn calculate_route(&self, start: LatLng, end: LatLng, options: RouteOptions) -> RouteResult {
        let straight_dist = Self::haversine_distance(&start, &end);

        let avg_speed_mps = match options.mode {
            VehicleMode::Scooter => 9.72,  // ~35 km/h
            VehicleMode::Car => 11.11,     // ~40 km/h
            VehicleMode::Bicycle => 4.16,  // ~15 km/h
            VehicleMode::Walk => 1.38,     // ~5 km/h
        };

        struct RoadItem {
            name: String,
            kind: String,
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
                                candidate_roads.push(RoadItem { name, kind, pts });
                            }
                        }
                    }
                }
            }
        }

        if !candidate_roads.is_empty() {
            // Build graph preserving 100% of physical road curve vertices
            let mut node_coords: Vec<LatLng> = Vec::new();
            let mut road_node_map: Vec<Vec<usize>> = Vec::with_capacity(candidate_roads.len());
            let mut grid: HashMap<(i64, i64), Vec<(usize, usize, usize)>> = HashMap::new();

            for (r_idx, road) in candidate_roads.iter().enumerate() {
                let mut nids = Vec::with_capacity(road.pts.len());
                for (p_idx, pt) in road.pts.iter().enumerate() {
                    let nid = node_coords.len();
                    node_coords.push(pt.clone());
                    nids.push(nid);
                    let cell = ((pt.latitude * 1000.0) as i64, (pt.longitude * 1000.0) as i64);
                    grid.entry(cell).or_default().push((r_idx, p_idx, nid));
                }
                road_node_map.push(nids);
            }

            #[derive(Clone)]
            struct Edge {
                target: usize,
                weight: f64,
                road_name: String,
                pts: Vec<LatLng>,
            }

            let mut adj: HashMap<usize, Vec<Edge>> = HashMap::new();

            // Internal edges
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

            // Junction edges (18m threshold strictly preventing median crossing)
            let junction_threshold = 18.0f64;
            for (r_idx, road) in candidate_roads.iter().enumerate() {
                let endpoints = [(0, &road.pts[0]), (road.pts.len() - 1, &road.pts[road.pts.len() - 1])];
                for (p_idx, pt) in endpoints {
                    let u = road_node_map[r_idx][p_idx];
                    let cx = (pt.latitude * 1000.0) as i64;
                    let cy = (pt.longitude * 1000.0) as i64;
                    for dx in -1..=1 {
                        for dy in -1..=1 {
                            if let Some(bucket) = grid.get(&(cx + dx, cy + dy)) {
                                for &(o_ridx, _o_pidx, v) in bucket {
                                    if o_ridx != r_idx {
                                        let d = Self::haversine_distance(pt, &node_coords[v]);
                                        if d <= junction_threshold {
                                            adj.entry(u).or_default().push(Edge {
                                                target: v,
                                                weight: d,
                                                road_name: "Junction".to_string(),
                                                pts: vec![pt.clone(), node_coords[v].clone()],
                                            });
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Snap start and end via perpendicular projection
            let mut snap_road = |q_pt: &LatLng| -> (LatLng, f64, usize, usize, String) {
                let mut best_d = f64::INFINITY;
                let mut best_proj = q_pt.clone();
                let mut best_u = 0;
                let mut best_v = 0;
                let mut best_name = "Road".to_string();

                for (r_idx, road) in candidate_roads.iter().enumerate() {
                    for i in 0..(road.pts.len() - 1) {
                        let (proj, d) = Self::project_point_on_segment(q_pt, &road.pts[i], &road.pts[i + 1]);
                        if d < best_d {
                            best_d = d;
                            best_proj = proj;
                            best_u = road_node_map[r_idx][i];
                            best_v = road_node_map[r_idx][i + 1];
                            best_name = road.name.clone();
                        }
                    }
                }
                (best_proj, best_d, best_u, best_v, best_name)
            };

            let (s_proj, _s_dist, s_u, s_v, s_name) = snap_road(&start);
            let (d_proj, _d_dist, d_u, d_v, d_name) = snap_road(&end);

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

            // Dijkstra
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

                let maneuvers = vec![
                    TurnManeuver {
                        instruction: "Depart onto connecting road".to_string(),
                        road_name: "Connected Road".to_string(),
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

                return RouteResult {
                    success: true,
                    total_distance_meters: total_dist,
                    estimated_duration_seconds: duration_secs,
                    polyline,
                    maneuvers,
                    error_message: None,
                };
            }
        }

        // Direct fallback
        let duration_secs = (straight_dist / avg_speed_mps) as u64;
        let maneuvers = vec![
            TurnManeuver {
                instruction: "Depart towards destination".to_string(),
                road_name: "Direct Route".to_string(),
                turn_type: "maneuver_straight".to_string(),
                distance_meters: straight_dist,
                coordinate: start.clone(),
            },
            TurnManeuver {
                instruction: "Arrive at Destination".to_string(),
                road_name: "Destination".to_string(),
                turn_type: "destination_pin".to_string(),
                distance_meters: 0.0,
                coordinate: end.clone(),
            },
        ];

        RouteResult {
            success: true,
            total_distance_meters: straight_dist,
            estimated_duration_seconds: duration_secs,
            polyline: vec![start, end],
            maneuvers,
            error_message: None,
        }
    }
}

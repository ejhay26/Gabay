// rust_core/src/router.rs
// High-performance OpenStreetMap Canonical Graph Router for Gabay
// Deterministic routing: Zero threshold heuristics, zero tile seam guessing.

use crate::types::{LatLng, RouteOptions, RouteResult, TurnManeuver, VehicleMode};
use serde::{Deserialize, Serialize};
use std::cmp::Ordering;
use std::collections::{BinaryHeap, HashMap, HashSet};
use std::fs::File;
use std::io::BufReader;
use std::path::{Path, PathBuf};
use std::sync::{Arc, Mutex, RwLock};

pub const FLAG_SCOOTER: u8 = 1 << 0;
pub const FLAG_CAR: u8 = 1 << 1;
pub const FLAG_BICYCLE: u8 = 1 << 2;
pub const FLAG_WALK: u8 = 1 << 3;
pub const FLAG_TOLL: u8 = 1 << 4;
pub const FLAG_EXPRESSWAY: u8 = 1 << 5;
pub const FLAG_FERRY: u8 = 1 << 6;

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct OsmNode {
    pub id: i64,
    pub lat: f64,
    pub lon: f64,
}

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct OsmEdge {
    pub target_idx: u32,
    pub distance_m: f32,
    pub speed_limit_kmh: u8,
    pub flags: u8,
    pub road_name: String,
}

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct OsmRoutingGraph {
    pub magic: [u8; 4], // b"GBAY"
    pub version: u32,
    pub nodes: Vec<OsmNode>,
    /// node_offset[i]..node_offset[i+1] is the slice of outgoing edges from node i
    pub node_edge_offsets: Vec<u32>,
    pub edges: Vec<OsmEdge>,
}

#[derive(Copy, Clone, PartialEq)]
struct AStarState {
    f_cost: f32, // g_cost + heuristic
    g_cost: f32,
    node: u32,
}

impl Eq for AStarState {}

impl Ord for AStarState {
    fn cmp(&self, other: &Self) -> Ordering {
        // Reverse order for min-heap by f_cost, tie-break by g_cost
        other.f_cost.partial_cmp(&self.f_cost).unwrap_or(Ordering::Equal)
    }
}

impl PartialOrd for AStarState {
    fn partial_cmp(&self, other: &Self) -> Option<Ordering> {
        Some(self.cmp(other))
    }
}

pub struct Router {
    pub graph_path: Option<String>,
    graph: RwLock<Option<Arc<OsmRoutingGraph>>>,
    spatial_grid: RwLock<Option<Arc<HashMap<(i32, i32), Vec<u32>>>>>,
    cache: Mutex<HashMap<String, RouteResult>>,
}

impl Router {
    pub fn new(graph_path: Option<String>) -> Self {
        let router = Self {
            graph_path: graph_path.clone(),
            graph: RwLock::new(None),
            spatial_grid: RwLock::new(None),
            cache: Mutex::new(HashMap::new()),
        };

        if let Some(ref path) = graph_path {
            let _ = router.try_load_graph(path);
        }

        router
    }

    /// Load graph directly from memory or test harness
    pub fn from_graph(graph: OsmRoutingGraph) -> Self {
        let grid = Self::build_spatial_grid(&graph.nodes);
        Self {
            graph_path: None,
            graph: RwLock::new(Some(Arc::new(graph))),
            spatial_grid: RwLock::new(Some(Arc::new(grid))),
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
            ("Make a U-Turn".to_string(), "maneuver_u_turn".to_string())
        } else if delta < -45.0 {
            ("Turn Left".to_string(), "maneuver_left".to_string())
        } else if delta < -28.0 {
            ("Bear Slight Left".to_string(), "maneuver_slight_left".to_string())
        } else if delta > 45.0 {
            ("Turn Right".to_string(), "maneuver_right".to_string())
        } else if delta > 28.0 {
            ("Bear Slight Right".to_string(), "maneuver_slight_right".to_string())
        } else {
            ("Continue Straight".to_string(), "maneuver_straight".to_string())
        }
    }

    /// Build ~550m spatial grid for fast coordinate snapping
    fn build_spatial_grid(nodes: &[OsmNode]) -> HashMap<(i32, i32), Vec<u32>> {
        const CELL_SIZE: f64 = 0.005;
        let mut grid: HashMap<(i32, i32), Vec<u32>> = HashMap::new();
        for (idx, node) in nodes.iter().enumerate() {
            let cx = (node.lat / CELL_SIZE).floor() as i32;
            let cy = (node.lon / CELL_SIZE).floor() as i32;
            grid.entry((cx, cy)).or_default().push(idx as u32);
        }
        grid
    }

    /// Tries loading binary graph from file
    fn try_load_graph(&self, path_str: &str) -> anyhow::Result<()> {
        let candidate_path = if Path::new(path_str).is_dir() {
            Path::new(path_str).join("ph.gabay")
        } else if path_str.ends_with(".sqlite") {
            Path::new(path_str).parent().unwrap_or(Path::new("")).join("ph.gabay")
        } else {
            PathBuf::from(path_str)
        };

        if !candidate_path.exists() {
            anyhow::bail!("Graph file does not exist at {:?}", candidate_path);
        }

        let file = File::open(&candidate_path)?;
        let reader = BufReader::new(file);
        let loaded_graph: OsmRoutingGraph = bincode::deserialize_from(reader)?;

        let grid = Self::build_spatial_grid(&loaded_graph.nodes);

        if let Ok(mut g_lock) = self.graph.write() {
            *g_lock = Some(Arc::new(loaded_graph));
        }
        if let Ok(mut grid_lock) = self.spatial_grid.write() {
            *grid_lock = Some(Arc::new(grid));
        }

        Ok(())
    }

    /// Checks if a directed edge is accessible for the requested vehicle mode and restrictions
    pub fn is_edge_allowed(flags: u8, options: &RouteOptions) -> bool {
        // Ro-Ro Ferries transport cars, scooters, bikes, and pedestrians
        if (flags & FLAG_FERRY) != 0 {
            return !options.avoid_ferries;
        }

        match options.mode {
            VehicleMode::Scooter => {
                // Philippine legal restriction: <400cc motorcycles strictly prohibited on expressways and tolls
                (flags & FLAG_EXPRESSWAY) == 0
                    && (flags & FLAG_TOLL) == 0
                    && (flags & FLAG_SCOOTER) != 0
            }
            VehicleMode::Car => {
                let mut ok = (flags & FLAG_CAR) != 0;
                if options.avoid_expressways && (flags & FLAG_EXPRESSWAY) != 0 {
                    ok = false;
                }
                if options.avoid_tolls && (flags & FLAG_TOLL) != 0 {
                    ok = false;
                }
                ok
            }
            VehicleMode::Bicycle => {
                (flags & FLAG_EXPRESSWAY) == 0
                    && (flags & FLAG_TOLL) == 0
                    && (flags & FLAG_BICYCLE) != 0
            }
            VehicleMode::Walk => {
                (flags & FLAG_EXPRESSWAY) == 0
                    && (flags & FLAG_TOLL) == 0
                    && (flags & FLAG_WALK) != 0
            }
        }
    }

    /// Verifies whether a candidate node contains active outgoing edges traversable for the given mode
    fn node_has_traversable_edge(node_idx: u32, graph: &OsmRoutingGraph, options: &RouteOptions) -> bool {
        let start_edge = graph.node_edge_offsets[node_idx as usize] as usize;
        let end_edge = graph.node_edge_offsets[node_idx as usize + 1] as usize;
        (start_edge..end_edge).any(|e_idx| Self::is_edge_allowed(graph.edges[e_idx].flags, options))
    }

    /// Snaps a GPS coordinate to the closest valid, traversable graph node
    fn snap_to_node(
        &self,
        coord: &LatLng,
        graph: &OsmRoutingGraph,
        grid: &HashMap<(i32, i32), Vec<u32>>,
        options: &RouteOptions,
    ) -> Option<u32> {
        const CELL_SIZE: f64 = 0.005;
        let cx = (coord.latitude / CELL_SIZE).floor() as i32;
        let cy = (coord.longitude / CELL_SIZE).floor() as i32;

        let mut best_idx = None;
        let mut min_dist = f64::INFINITY;

        // Step 1: Concentric ring search (up to 4 rings = 9x9 cells, ~5 km radius)
        // Strictly filters for nodes that have valid traversable outgoing edges for this vehicle mode
        for r in 0i32..=4i32 {
            for dx in -r..=r {
                for dy in -r..=r {
                    if r > 0 && dx.abs() != r && dy.abs() != r {
                        continue;
                    }
                    if let Some(nodes) = grid.get(&(cx + dx, cy + dy)) {
                        for &n_idx in nodes {
                            if Self::node_has_traversable_edge(n_idx, graph, options) {
                                let node = &graph.nodes[n_idx as usize];
                                let d = Self::haversine_distance(coord, &LatLng { latitude: node.lat, longitude: node.lon });
                                if d < min_dist {
                                    min_dist = d;
                                    best_idx = Some(n_idx);
                                }
                            }
                        }
                    }
                }
            }
            if best_idx.is_some() {
                break;
            }
        }

        // Step 2: Expanded grid search up to 10 cells (~5.5km) if in rural / unpaved terrain
        if best_idx.is_none() {
            for dx in -10..=10 {
                for dy in -10..=10 {
                    if let Some(nodes) = grid.get(&(cx + dx, cy + dy)) {
                        for &n_idx in nodes {
                            let node = &graph.nodes[n_idx as usize];
                            let d = Self::haversine_distance(coord, &LatLng { latitude: node.lat, longitude: node.lon });
                            if d < min_dist {
                                min_dist = d;
                                best_idx = Some(n_idx);
                            }
                        }
                    }
                }
            }
        }

        // Step 3: Global fallback (e.g. coordinates outside current regional grid bounds)
        if best_idx.is_none() {
            for (idx, node) in graph.nodes.iter().enumerate() {
                let d = Self::haversine_distance(coord, &LatLng { latitude: node.lat, longitude: node.lon });
                if d < min_dist {
                    min_dist = d;
                    best_idx = Some(idx as u32);
                }
            }
        }

        best_idx
    }

    /// Calculate route between start and destination
    pub fn calculate_route(&self, start: LatLng, end: LatLng, options: RouteOptions) -> RouteResult {
        // Cache lookup: (start, end, mode, exp, toll, alt)
        let cache_key = format!(
            "{:.5},{:.5}->{:.5},{:.5}:{:?}:exp={}:toll={}:alt={}",
            start.latitude, start.longitude, end.latitude, end.longitude,
            options.mode, options.avoid_expressways, options.avoid_tolls, options.avoid_ferries
        );

        if let Ok(cache) = self.cache.lock() {
            if let Some(cached) = cache.get(&cache_key) {
                return cached.clone();
            }
        }

        // Lazy load graph if not yet loaded
        if self.graph.read().map(|g| g.is_none()).unwrap_or(true) {
            if let Some(ref path) = self.graph_path {
                let _ = self.try_load_graph(path);
            }
        }

        let graph_arc = match self.graph.read() {
            Ok(lock) => match lock.as_ref() {
                Some(g) => Arc::clone(g),
                None => {
                    return RouteResult {
                        success: false,
                        total_distance_meters: 0.0,
                        estimated_duration_seconds: 0,
                        polyline: vec![],
                        maneuvers: vec![],
                        error_message: Some("Offline road graph not loaded".to_string()),
                    };
                }
            },
            Err(_) => {
                return RouteResult {
                    success: false,
                    total_distance_meters: 0.0,
                    estimated_duration_seconds: 0,
                    polyline: vec![],
                    maneuvers: vec![],
                    error_message: Some("Failed to acquire graph lock".to_string()),
                };
            }
        };

        let grid_arc = match self.spatial_grid.read() {
            Ok(lock) => match lock.as_ref() {
                Some(grid) => Arc::clone(grid),
                None => {
                    return RouteResult {
                        success: false,
                        total_distance_meters: 0.0,
                        estimated_duration_seconds: 0,
                        polyline: vec![],
                        maneuvers: vec![],
                        error_message: Some("Spatial index not initialized".to_string()),
                    };
                }
            },
            Err(_) => {
                return RouteResult {
                    success: false,
                    total_distance_meters: 0.0,
                    estimated_duration_seconds: 0,
                    polyline: vec![],
                    maneuvers: vec![],
                    error_message: Some("Failed to acquire spatial index lock".to_string()),
                };
            }
        };

        let start_node = match self.snap_to_node(&start, &graph_arc, &grid_arc, &options) {
            Some(idx) => idx,
            None => {
                return RouteResult {
                    success: false,
                    total_distance_meters: 0.0,
                    estimated_duration_seconds: 0,
                    polyline: vec![],
                    maneuvers: vec![],
                    error_message: Some("Unable to snap start coordinate to road network".to_string()),
                };
            }
        };

        let dest_node = match self.snap_to_node(&end, &graph_arc, &grid_arc, &options) {
            Some(idx) => idx,
            None => {
                return RouteResult {
                    success: false,
                    total_distance_meters: 0.0,
                    estimated_duration_seconds: 0,
                    polyline: vec![],
                    maneuvers: vec![],
                    error_message: Some("Unable to snap destination coordinate to road network".to_string()),
                };
            }
        };

        // Identical start & end point
        if start_node == dest_node {
            let pt = LatLng {
                latitude: graph_arc.nodes[start_node as usize].lat,
                longitude: graph_arc.nodes[start_node as usize].lon,
            };
            return RouteResult {
                success: true,
                total_distance_meters: 0.0,
                estimated_duration_seconds: 0,
                polyline: vec![pt.clone()],
                maneuvers: vec![TurnManeuver {
                    instruction: "You have arrived at your destination".to_string(),
                    road_name: "Destination".to_string(),
                    turn_type: "maneuver_arrive".to_string(),
                    distance_meters: 0.0,
                    coordinate: pt,
                }],
                error_message: None,
            };
        }

        // If avoid_ferries is true, this indicates an Alternative Route calculation request.
        // We find the primary route first and penalize its intermediate nodes.
        let primary_nodes_opt: Option<HashSet<u32>> = if options.avoid_ferries {
            let mut base_opts = options.clone();
            base_opts.avoid_ferries = false;
            let primary = self.calculate_route(start.clone(), end.clone(), base_opts);
            if primary.success && primary.polyline.len() >= 3 {
                let mut p_set = HashSet::new();
                for pt in &primary.polyline[1..primary.polyline.len().saturating_sub(1)] {
                    if let Some(n_idx) = self.snap_to_node(pt, &graph_arc, &grid_arc, &options) {
                        p_set.insert(n_idx);
                    }
                }
                Some(p_set)
            } else {
                None
            }
        } else {
            None
        };

        // A* Shortest Path Search directed towards destination
        let dest_lat = graph_arc.nodes[dest_node as usize].lat;
        let dest_lon = graph_arc.nodes[dest_node as usize].lon;
        let dest_coord = LatLng { latitude: dest_lat, longitude: dest_lon };

        let heuristic = |node_idx: u32| -> f32 {
            let n = &graph_arc.nodes[node_idx as usize];
            let pt = LatLng { latitude: n.lat, longitude: n.lon };
            Self::haversine_distance(&pt, &dest_coord) as f32
        };

        let mut dist: HashMap<u32, f32> = HashMap::new();
        let mut parent: HashMap<u32, (u32, u32)> = HashMap::new(); // target -> (from_node, global_edge_idx)
        let mut heap = BinaryHeap::new();

        let start_h = heuristic(start_node);
        dist.insert(start_node, 0.0);
        heap.push(AStarState { f_cost: start_h, g_cost: 0.0, node: start_node });

        let mut reached_dest = false;

        while let Some(AStarState { g_cost, node, .. }) = heap.pop() {
            if node == dest_node {
                reached_dest = true;
                break;
            }

            if let Some(&current_best) = dist.get(&node) {
                if g_cost > current_best {
                    continue;
                }
            }

            let start_edge = graph_arc.node_edge_offsets[node as usize] as usize;
            let end_edge = graph_arc.node_edge_offsets[node as usize + 1] as usize;

            for (edge_offset, edge) in graph_arc.edges[start_edge..end_edge].iter().enumerate() {
                let global_edge_idx = (start_edge + edge_offset) as u32;

                // Vehicle Mode Restrictions & Legal Philippine Road Access
                if !Self::is_edge_allowed(edge.flags, &options) {
                    continue;
                }

                // If calculating an alternative route, apply a 2.5x penalty to primary route nodes
                let penalty = if let Some(ref p_nodes) = primary_nodes_opt {
                    if p_nodes.contains(&edge.target_idx) { 2.5 } else { 1.0 }
                } else {
                    1.0
                };

                // Ro-Ro Ferries: maritime transit takes longer than highway driving,
                // so we apply a 1.3x cost weighting so bridges/highways are naturally preferred,
                // while water crossings are seamlessly used when island hopping.
                let ferry_factor = if (edge.flags & FLAG_FERRY) != 0 { 1.3 } else { 1.0 };
                let next_cost = g_cost + edge.distance_m * penalty * ferry_factor;
                let is_cheaper = match dist.get(&edge.target_idx) {
                    Some(&old_cost) => next_cost < old_cost,
                    None => true,
                };

                if is_cheaper {
                    dist.insert(edge.target_idx, next_cost);
                    parent.insert(edge.target_idx, (node, global_edge_idx));
                    let h = heuristic(edge.target_idx);
                    heap.push(AStarState { f_cost: next_cost + h, g_cost: next_cost, node: edge.target_idx });
                }
            }
        }

        // Explicit Failure: If destination is unreachable (e.g. disconnected graph component)
        if !reached_dest {
            return RouteResult {
                success: false,
                total_distance_meters: 0.0,
                estimated_duration_seconds: 0,
                polyline: vec![],
                maneuvers: vec![],
                error_message: Some("No road connection found between origin and destination".to_string()),
            };
        }

        // Backtrack path to reconstruct polyline & road names
        let mut curr = dest_node;
        let mut path_nodes: Vec<u32> = vec![curr];
        let mut road_segments: Vec<(String, f32, bool)> = Vec::new(); // (road_name, distance_m, is_ferry)

        while let Some(&(prev, global_edge_idx)) = parent.get(&curr) {
            path_nodes.push(prev);
            let edge = &graph_arc.edges[global_edge_idx as usize];
            let is_ferry = (edge.flags & FLAG_FERRY) != 0;
            road_segments.push((edge.road_name.clone(), edge.distance_m, is_ferry));
            curr = prev;
            if curr == start_node {
                break;
            }
        }

        path_nodes.reverse();
        road_segments.reverse();

        // If an alternative route was requested, verify that it is distinct from primary route
        if let Some(ref p_nodes) = primary_nodes_opt {
            let alt_unique_count = path_nodes.iter().filter(|n| !p_nodes.contains(n)).count();
            let total_nodes = path_nodes.len();
            if alt_unique_count < 3 || (alt_unique_count as f32 / total_nodes as f32) < 0.12 {
                return RouteResult {
                    success: false,
                    total_distance_meters: 0.0,
                    estimated_duration_seconds: 0,
                    polyline: vec![],
                    maneuvers: vec![],
                    error_message: Some("No distinct alternative route available".to_string()),
                };
            }
        }

        let mut polyline: Vec<LatLng> = path_nodes
            .iter()
            .map(|&idx| {
                let n = &graph_arc.nodes[idx as usize];
                LatLng { latitude: n.lat, longitude: n.lon }
            })
            .collect();

        // Continuous Road Connection: Project start and destination coordinates onto the nearest road segment
        // to completely eliminate straight-line "jumping" across buildings, parking lots, or dead-ends.
        if polyline.len() >= 2 {
            let (proj_start, d_to_seg, t_start) = Self::project_point_on_segment(&start, &polyline[0], &polyline[1]);
            if d_to_seg < 1500.0 {
                if t_start > 0.05 && t_start < 0.95 {
                    polyline[0] = proj_start;
                }
                let d_user_to_road = Self::haversine_distance(&start, &polyline[0]);
                if d_user_to_road > 12.0 && d_user_to_road < 1000.0 {
                    polyline.insert(0, start.clone());
                }
            }

            let last = polyline.len() - 1;
            let (proj_end, d_to_end_seg, t_end) = Self::project_point_on_segment(&end, &polyline[last - 1], &polyline[last]);
            if d_to_end_seg < 1500.0 {
                if t_end > 0.05 && t_end < 0.95 {
                    polyline[last] = proj_end;
                }
                let d_road_to_dest = Self::haversine_distance(&polyline[last], &end);
                if d_road_to_dest > 12.0 && d_road_to_dest < 1000.0 {
                    polyline.push(end.clone());
                }
            }
        }

        // Exact physical road distance (unpenalized sum of segment distances)
        let total_dist: f64 = road_segments.iter().map(|s| s.1 as f64).sum();

        let avg_speed_mps = match options.mode {
            VehicleMode::Scooter => 9.72,  // ~35 km/h
            VehicleMode::Car => 11.11,     // ~40 km/h
            VehicleMode::Bicycle => 4.16,  // ~15 km/h
            VehicleMode::Walk => 1.38,     // ~5 km/h
        };

        // Accurately compute duration: standard road uses vehicle speed, ferry uses 25 km/h (6.94 m/s)
        let mut duration_secs: u64 = 0;
        for seg in &road_segments {
            let seg_speed = if seg.2 { 6.94 } else { avg_speed_mps };
            duration_secs += (seg.1 as f64 / seg_speed) as u64;
        }

        // Generate Turn-by-Turn Maneuvers
        let mut maneuvers: Vec<TurnManeuver> = Vec::new();

        if !polyline.is_empty() {
            let first_road = road_segments.first().map(|s| s.0.clone()).unwrap_or_else(|| "Road".to_string());
            let first_is_ferry = road_segments.first().map(|s| s.2).unwrap_or(false);
            let first_instr = if first_is_ferry {
                format!("Board Ferry: {}", first_road)
            } else {
                format!("Depart onto {}", first_road)
            };

            maneuvers.push(TurnManeuver {
                instruction: first_instr,
                road_name: first_road,
                turn_type: "maneuver_straight".to_string(),
                distance_meters: total_dist,
                coordinate: polyline[0].clone(),
            });

            // Check bearing changes where road names change or significant angular turns occur
            let mut acc_dist = 0.0;
            for i in 1..(polyline.len() - 1) {
                let seg_dist = Self::haversine_distance(&polyline[i - 1], &polyline[i]);
                acc_dist += seg_dist;

                let b1 = Self::calculate_bearing(&polyline[i - 1], &polyline[i]);
                let b2 = Self::calculate_bearing(&polyline[i], &polyline[i + 1]);

                let (turn_text, turn_icon) = Self::determine_turn_type(b1, b2);

                let seg_idx = (i - 1).min(road_segments.len() - 1);
                let next_seg_idx = i.min(road_segments.len() - 1);

                let road_name = &road_segments[next_seg_idx].0;
                let is_ferry = road_segments[next_seg_idx].2;
                let road_changed = road_segments[seg_idx].0 != road_segments[next_seg_idx].0;

                if is_ferry && (!road_segments[seg_idx].2 || road_changed) {
                    maneuvers.push(TurnManeuver {
                        instruction: format!("Board Ferry: {}", road_name),
                        road_name: road_name.clone(),
                        turn_type: "maneuver_straight".to_string(),
                        distance_meters: (total_dist - acc_dist).max(0.0),
                        coordinate: polyline[i].clone(),
                    });
                    acc_dist = 0.0;
                } else if turn_icon != "maneuver_straight" || (road_changed && acc_dist > 50.0) {
                    maneuvers.push(TurnManeuver {
                        instruction: format!("{} onto {}", turn_text, road_name),
                        road_name: road_name.clone(),
                        turn_type: turn_icon,
                        distance_meters: (total_dist - acc_dist).max(0.0),
                        coordinate: polyline[i].clone(),
                    });
                    acc_dist = 0.0;
                }
            }

            // Arrival maneuver
            if let Some(last_pt) = polyline.last() {
                maneuvers.push(TurnManeuver {
                    instruction: "You have arrived at your destination".to_string(),
                    road_name: "Destination".to_string(),
                    turn_type: "maneuver_arrive".to_string(),
                    distance_meters: 0.0,
                    coordinate: last_pt.clone(),
                });
            }
        }

        let result = RouteResult {
            success: true,
            total_distance_meters: total_dist,
            estimated_duration_seconds: duration_secs,
            polyline,
            maneuvers,
            error_message: None,
        };

        // Cache result in LRU
        if let Ok(mut cache) = self.cache.lock() {
            if cache.len() > 64 {
                cache.clear();
            }
            cache.insert(cache_key, result.clone());
        }

        result
    }
}

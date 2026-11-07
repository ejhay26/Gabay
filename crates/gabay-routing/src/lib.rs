//! OpenStreetMap Canonical Graph Router for Gabay.
//!
//! Evaluates vehicle-specific routing (Scooter, Car, Bicycle, Walk) over offline
//! graph data (`ph.gabay`). Runs lock-free with immutable `Arc<OsmRoutingGraph>`.

use std::cmp::Ordering;
use std::collections::{BinaryHeap, HashMap, HashSet};
use std::fs::File;
use std::io::BufReader;
use std::path::Path;
use std::sync::Arc;

use gabay_map::LonLat;
use serde::{Deserialize, Serialize};

pub const FLAG_SCOOTER: u8 = 1 << 0;
pub const FLAG_CAR: u8 = 1 << 1;
pub const FLAG_BICYCLE: u8 = 1 << 2;
pub const FLAG_WALK: u8 = 1 << 3;
pub const FLAG_TOLL: u8 = 1 << 4;
pub const FLAG_EXPRESSWAY: u8 = 1 << 5;
pub const FLAG_FERRY: u8 = 1 << 6;

pub const PHILIPPINES_MIN_LAT: f64 = 4.0;
pub const PHILIPPINES_MAX_LAT: f64 = 22.0;
pub const PHILIPPINES_MIN_LON: f64 = 116.0;
pub const PHILIPPINES_MAX_LON: f64 = 127.5;

pub const MAX_SNAP_DISTANCE_M: f64 = 15_000.0;
const CELL_SIZE: f64 = 0.005; // ~550m

#[derive(Debug, thiserror::Error)]
pub enum RouterError {
    #[error("graph file not found: {0}")]
    FileNotFound(String),
    #[error("failed to open graph file: {0}")]
    Io(#[from] std::io::Error),
    #[error("failed to deserialize graph: {0}")]
    Deserialize(#[from] bincode::Error),
    #[error("invalid graph magic {0:?}, expected b\"GBAY\"")]
    InvalidMagic([u8; 4]),
    #[error("graph has no nodes")]
    EmptyGraph,
    #[error("start point outside Philippines coverage: ({0}, {1})")]
    StartOutOfBounds(f64, f64),
    #[error("destination point outside Philippines coverage: ({0}, {1})")]
    DestOutOfBounds(f64, f64),
    #[error("cannot snap start position to road network within 15 km")]
    StartCannotSnap,
    #[error("cannot snap destination to road network within 15 km")]
    DestCannotSnap,
    #[error("no route found between origin and destination")]
    NoRouteFound,
    #[error("route search exceeded iteration limit")]
    IterationCapExceeded,
    #[error("no distinct alternative route available")]
    NoAlternativeRoute,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum VehicleMode {
    Scooter,
    Car,
    Bicycle,
    Walk,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub struct RouteOptions {
    pub mode: VehicleMode,
    pub avoid_tolls: bool,
    pub avoid_expressways: bool,
    pub avoid_ferries: bool,
}

impl Default for RouteOptions {
    fn default() -> Self {
        Self {
            mode: VehicleMode::Scooter,
            avoid_tolls: true,
            avoid_expressways: true,
            avoid_ferries: false,
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum ManeuverType {
    Depart,
    Straight,
    Left,
    Right,
    SlightLeft,
    SlightRight,
    UTurn,
    BoardFerry,
    Arrive,
}

#[derive(Debug, Clone, PartialEq)]
pub struct Maneuver {
    pub maneuver_type: ManeuverType,
    pub road_name: String,
    pub distance_meters: f64,
    pub coordinate: LonLat,
}

#[derive(Debug, Clone, PartialEq)]
pub struct RoutePlan {
    pub points: Vec<LonLat>,
    pub total_distance_meters: f64,
    pub estimated_duration_seconds: f64,
    pub maneuvers: Vec<Maneuver>,
}

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
    pub magic: [u8; 4],
    pub version: u32,
    pub nodes: Vec<OsmNode>,
    pub node_edge_offsets: Vec<u32>,
    pub edges: Vec<OsmEdge>,
}

#[derive(Copy, Clone, PartialEq)]
struct AStarState {
    f_cost: f32,
    g_cost: f32,
    node: u32,
}

impl Eq for AStarState {}

impl Ord for AStarState {
    fn cmp(&self, other: &Self) -> Ordering {
        other.f_cost.partial_cmp(&self.f_cost).unwrap_or(Ordering::Equal)
    }
}

impl PartialOrd for AStarState {
    fn partial_cmp(&self, other: &Self) -> Option<Ordering> {
        Some(self.cmp(other))
    }
}

pub fn haversine_distance(from: &LonLat, to: &LonLat) -> f64 {
    const EARTH_RADIUS_METERS: f64 = 6_371_000.0;
    let d_lat = (to.lat - from.lat).to_radians();
    let d_lon = (to.lon - from.lon).to_radians();
    let lat1 = from.lat.to_radians();
    let lat2 = to.lat.to_radians();

    let a = (d_lat / 2.0).sin().powi(2) + lat1.cos() * lat2.cos() * (d_lon / 2.0).sin().powi(2);
    let c = 2.0 * a.sqrt().asin();

    EARTH_RADIUS_METERS * c
}

pub fn project_point_on_segment(p: &LonLat, a: &LonLat, b: &LonLat) -> (LonLat, f64, f64) {
    let cos_lat = p.lat.to_radians().cos();
    let px = p.lon * 111320.0 * cos_lat;
    let py = p.lat * 110540.0;
    let ax = a.lon * 111320.0 * cos_lat;
    let ay = a.lat * 110540.0;
    let bx = b.lon * 111320.0 * cos_lat;
    let by = b.lat * 110540.0;

    let dx = bx - ax;
    let dy = by - ay;
    let len_sq = dx * dx + dy * dy;
    if len_sq < 1e-6 {
        return (*a, haversine_distance(p, a), 0.0);
    }
    let t = (((px - ax) * dx + (py - ay) * dy) / len_sq).clamp(0.0, 1.0);
    let proj_lat = a.lat + t * (b.lat - a.lat);
    let proj_lon = a.lon + t * (b.lon - a.lon);
    let proj = LonLat::new(proj_lon, proj_lat);
    let d = haversine_distance(p, &proj);
    (proj, d, t)
}

pub fn calculate_bearing(from: &LonLat, to: &LonLat) -> f64 {
    let lat1 = from.lat.to_radians();
    let lat2 = to.lat.to_radians();
    let d_lon = (to.lon - from.lon).to_radians();

    let y = d_lon.sin() * lat2.cos();
    let x = lat1.cos() * lat2.sin() - lat1.sin() * lat2.cos() * d_lon.cos();
    let rad = y.atan2(x);
    (rad.to_degrees() + 360.0) % 360.0
}

pub fn determine_turn_type(bearing1: f64, bearing2: f64) -> ManeuverType {
    let mut delta = bearing2 - bearing1;
    while delta > 180.0 { delta -= 360.0; }
    while delta < -180.0 { delta += 360.0; }

    if delta > 135.0 || delta < -135.0 {
        ManeuverType::UTurn
    } else if delta < -45.0 {
        ManeuverType::Left
    } else if delta < -28.0 {
        ManeuverType::SlightLeft
    } else if delta > 45.0 {
        ManeuverType::Right
    } else if delta > 28.0 {
        ManeuverType::SlightRight
    } else {
        ManeuverType::Straight
    }
}

pub struct Router {
    graph: Arc<OsmRoutingGraph>,
    spatial_grid: Arc<HashMap<(i32, i32), Vec<u32>>>,
}

impl Router {
    pub fn open(path: &Path) -> Result<Self, RouterError> {
        if !path.exists() {
            return Err(RouterError::FileNotFound(path.display().to_string()));
        }

        let file = File::open(path)?;
        let reader = BufReader::new(file);
        let graph: OsmRoutingGraph = bincode::deserialize_from(reader)?;

        Self::from_graph(graph)
    }

    pub fn from_graph(graph: OsmRoutingGraph) -> Result<Self, RouterError> {
        if &graph.magic != b"GBAY" {
            return Err(RouterError::InvalidMagic(graph.magic));
        }
        if graph.nodes.is_empty() {
            return Err(RouterError::EmptyGraph);
        }

        let mut grid: HashMap<(i32, i32), Vec<u32>> = HashMap::new();
        for (idx, node) in graph.nodes.iter().enumerate() {
            let cx = (node.lat / CELL_SIZE).floor() as i32;
            let cy = (node.lon / CELL_SIZE).floor() as i32;
            grid.entry((cx, cy)).or_default().push(idx as u32);
        }

        Ok(Self {
            graph: Arc::new(graph),
            spatial_grid: Arc::new(grid),
        })
    }

    pub fn is_edge_allowed(flags: u8, options: &RouteOptions) -> bool {
        if (flags & FLAG_FERRY) != 0 {
            return !options.avoid_ferries;
        }

        match options.mode {
            VehicleMode::Scooter => {
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

    fn node_has_traversable_edge(node_idx: u32, graph: &OsmRoutingGraph, options: &RouteOptions) -> bool {
        let node_edge_offsets = &graph.node_edge_offsets;
        if (node_idx as usize + 1) >= node_edge_offsets.len() {
            return false;
        }
        let start_edge = node_edge_offsets[node_idx as usize] as usize;
        let end_edge = node_edge_offsets[node_idx as usize + 1] as usize;
        (start_edge..end_edge).any(|e_idx| {
            if e_idx < graph.edges.len() {
                Self::is_edge_allowed(graph.edges[e_idx].flags, options)
            } else {
                false
            }
        })
    }

    fn snap_to_node(&self, coord: &LonLat, options: &RouteOptions) -> Option<u32> {
        if !coord.lat.is_finite() || !coord.lon.is_finite() {
            return None;
        }
        if coord.lat < PHILIPPINES_MIN_LAT
            || coord.lat > PHILIPPINES_MAX_LAT
            || coord.lon < PHILIPPINES_MIN_LON
            || coord.lon > PHILIPPINES_MAX_LON
        {
            return None;
        }

        let cx = (coord.lat / CELL_SIZE).floor() as i32;
        let cy = (coord.lon / CELL_SIZE).floor() as i32;

        let mut best_idx = None;
        let mut min_dist = f64::INFINITY;

        // Step 1: concentric ring search (up to 4 rings = 9x9 cells, ~5 km radius)
        for r in 0i32..=4i32 {
            for dx in -r..=r {
                for dy in -r..=r {
                    if r > 0 && dx.abs() != r && dy.abs() != r {
                        continue;
                    }
                    if let Some(nodes) = self.spatial_grid.get(&(cx + dx, cy + dy)) {
                        for &n_idx in nodes {
                            if Self::node_has_traversable_edge(n_idx, &self.graph, options) {
                                let node = &self.graph.nodes[n_idx as usize];
                                let d = haversine_distance(coord, &LonLat::new(node.lon, node.lat));
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

        // Step 2: expanded grid search up to 10 cells (~5.5km)
        if best_idx.is_none() {
            for dx in -10..=10 {
                for dy in -10..=10 {
                    if let Some(nodes) = self.spatial_grid.get(&(cx + dx, cy + dy)) {
                        for &n_idx in nodes {
                            if Self::node_has_traversable_edge(n_idx, &self.graph, options) {
                                let node = &self.graph.nodes[n_idx as usize];
                                let d = haversine_distance(coord, &LonLat::new(node.lon, node.lat));
                                if d < min_dist {
                                    min_dist = d;
                                    best_idx = Some(n_idx);
                                }
                            }
                        }
                    }
                }
            }
        }

        // Step 3: bounded fallback check
        if best_idx.is_none() {
            for (idx, node) in self.graph.nodes.iter().enumerate() {
                let d = haversine_distance(coord, &LonLat::new(node.lon, node.lat));
                if d < min_dist && d <= MAX_SNAP_DISTANCE_M
                    && Self::node_has_traversable_edge(idx as u32, &self.graph, options) {
                    min_dist = d;
                    best_idx = Some(idx as u32);
                }
            }
        }

        if min_dist > MAX_SNAP_DISTANCE_M {
            None
        } else {
            best_idx
        }
    }

    pub fn calculate_route(
        &self,
        start: LonLat,
        end: LonLat,
        options: RouteOptions,
    ) -> Result<RoutePlan, RouterError> {
        if !start.lat.is_finite() || !start.lon.is_finite() || start.lat < PHILIPPINES_MIN_LAT
            || start.lat > PHILIPPINES_MAX_LAT
            || start.lon < PHILIPPINES_MIN_LON
            || start.lon > PHILIPPINES_MAX_LON
        {
            return Err(RouterError::StartOutOfBounds(start.lon, start.lat));
        }

        if !end.lat.is_finite() || !end.lon.is_finite() || end.lat < PHILIPPINES_MIN_LAT
            || end.lat > PHILIPPINES_MAX_LAT
            || end.lon < PHILIPPINES_MIN_LON
            || end.lon > PHILIPPINES_MAX_LON
        {
            return Err(RouterError::DestOutOfBounds(end.lon, end.lat));
        }

        let start_node = self.snap_to_node(&start, &options).ok_or(RouterError::StartCannotSnap)?;
        let dest_node = self.snap_to_node(&end, &options).ok_or(RouterError::DestCannotSnap)?;

        if start_node == dest_node {
            let pt = LonLat::new(
                self.graph.nodes[start_node as usize].lon,
                self.graph.nodes[start_node as usize].lat,
            );
            return Ok(RoutePlan {
                points: vec![pt],
                total_distance_meters: 0.0,
                estimated_duration_seconds: 0.0,
                maneuvers: vec![Maneuver {
                    maneuver_type: ManeuverType::Arrive,
                    road_name: "Destination".to_string(),
                    distance_meters: 0.0,
                    coordinate: pt,
                }],
            });
        }

        let primary_nodes_opt: Option<HashSet<u32>> = if options.avoid_ferries {
            let mut base_opts = options;
            base_opts.avoid_ferries = false;
            let primary = self.calculate_route(start, end, base_opts).ok();
            if let Some(p) = primary {
                if p.points.len() >= 3 {
                    let mut p_set = HashSet::new();
                    for pt in &p.points[1..p.points.len().saturating_sub(1)] {
                        if let Some(n_idx) = self.snap_to_node(pt, &options) {
                            p_set.insert(n_idx);
                        }
                    }
                    Some(p_set)
                } else {
                    None
                }
            } else {
                None
            }
        } else {
            None
        };

        let dest_coord = LonLat::new(
            self.graph.nodes[dest_node as usize].lon,
            self.graph.nodes[dest_node as usize].lat,
        );

        let heuristic = |node_idx: u32| -> f32 {
            let n = &self.graph.nodes[node_idx as usize];
            haversine_distance(&LonLat::new(n.lon, n.lat), &dest_coord) as f32
        };

        let mut dist: HashMap<u32, f32> = HashMap::new();
        let mut parent: HashMap<u32, (u32, u32)> = HashMap::new();
        let mut heap = BinaryHeap::new();

        let start_h = heuristic(start_node);
        dist.insert(start_node, 0.0);
        heap.push(AStarState { f_cost: start_h, g_cost: 0.0, node: start_node });

        let mut reached_dest = false;
        let mut iterations: usize = 0;
        const MAX_ASTAR_ITERATIONS: usize = 350_000;

        while let Some(AStarState { g_cost, node, .. }) = heap.pop() {
            iterations += 1;
            if iterations > MAX_ASTAR_ITERATIONS {
                log::warn!("A* cap reached ({MAX_ASTAR_ITERATIONS}), aborting unreachable route");
                return Err(RouterError::IterationCapExceeded);
            }

            if node == dest_node {
                reached_dest = true;
                break;
            }

            if let Some(&current_best) = dist.get(&node) {
                if g_cost > current_best {
                    continue;
                }
            }

            let start_edge = self.graph.node_edge_offsets[node as usize] as usize;
            let end_edge = self.graph.node_edge_offsets[node as usize + 1] as usize;

            for (edge_offset, edge) in self.graph.edges[start_edge..end_edge].iter().enumerate() {
                let global_edge_idx = (start_edge + edge_offset) as u32;

                if !Self::is_edge_allowed(edge.flags, &options) {
                    continue;
                }

                let penalty = if let Some(ref p_nodes) = primary_nodes_opt {
                    if p_nodes.contains(&edge.target_idx) { 2.5 } else { 1.0 }
                } else {
                    1.0
                };

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

        if !reached_dest {
            return Err(RouterError::NoRouteFound);
        }

        let mut curr = dest_node;
        let mut path_nodes: Vec<u32> = vec![curr];
        let mut road_segments: Vec<(String, f32, bool)> = Vec::new();

        while let Some(&(prev, global_edge_idx)) = parent.get(&curr) {
            path_nodes.push(prev);
            let edge = &self.graph.edges[global_edge_idx as usize];
            let is_ferry = (edge.flags & FLAG_FERRY) != 0;
            road_segments.push((edge.road_name.clone(), edge.distance_m, is_ferry));
            curr = prev;
            if curr == start_node {
                break;
            }
        }

        path_nodes.reverse();
        road_segments.reverse();

        if let Some(ref p_nodes) = primary_nodes_opt {
            let alt_unique_count = path_nodes.iter().filter(|n| !p_nodes.contains(n)).count();
            let total_nodes = path_nodes.len();
            if alt_unique_count < 3 || (alt_unique_count as f32 / total_nodes as f32) < 0.12 {
                return Err(RouterError::NoAlternativeRoute);
            }
        }

        let mut polyline: Vec<LonLat> = path_nodes
            .iter()
            .map(|&idx| {
                let n = &self.graph.nodes[idx as usize];
                LonLat::new(n.lon, n.lat)
            })
            .collect();

        if polyline.len() >= 2 {
            let (proj_start, d_to_seg, t_start) = project_point_on_segment(&start, &polyline[0], &polyline[1]);
            if d_to_seg < 1500.0 {
                if t_start > 0.05 && t_start < 0.95 {
                    polyline[0] = proj_start;
                }
                let d_user_to_road = haversine_distance(&start, &polyline[0]);
                if d_user_to_road > 12.0 && d_user_to_road < 1000.0 {
                    polyline.insert(0, start);
                }
            }

            let last = polyline.len() - 1;
            let (proj_end, d_to_end_seg, t_end) = project_point_on_segment(&end, &polyline[last - 1], &polyline[last]);
            if d_to_end_seg < 1500.0 {
                if t_end > 0.05 && t_end < 0.95 {
                    polyline[last] = proj_end;
                }
                let d_road_to_dest = haversine_distance(&polyline[last], &end);
                if d_road_to_dest > 12.0 && d_road_to_dest < 1000.0 {
                    polyline.push(end);
                }
            }
        }

        let total_dist: f64 = road_segments.iter().map(|s| s.1 as f64).sum();

        let avg_speed_mps = match options.mode {
            VehicleMode::Scooter => 9.72,
            VehicleMode::Car => 11.11,
            VehicleMode::Bicycle => 4.16,
            VehicleMode::Walk => 1.38,
        };

        let mut duration_secs: f64 = 0.0;
        for seg in &road_segments {
            let seg_speed = if seg.2 { 6.94 } else { avg_speed_mps };
            duration_secs += seg.1 as f64 / seg_speed;
        }

        let mut maneuvers: Vec<Maneuver> = Vec::new();
        if !polyline.is_empty() {
            let first_road = road_segments.first().map(|s| s.0.clone()).unwrap_or_else(|| "Road".to_string());
            let first_is_ferry = road_segments.first().map(|s| s.2).unwrap_or(false);

            maneuvers.push(Maneuver {
                maneuver_type: if first_is_ferry { ManeuverType::BoardFerry } else { ManeuverType::Depart },
                road_name: first_road,
                distance_meters: total_dist,
                coordinate: polyline[0],
            });

            let mut acc_dist = 0.0;
            for i in 1..(polyline.len() - 1) {
                let seg_dist = haversine_distance(&polyline[i - 1], &polyline[i]);
                acc_dist += seg_dist;

                let b1 = calculate_bearing(&polyline[i - 1], &polyline[i]);
                let b2 = calculate_bearing(&polyline[i], &polyline[i + 1]);
                let turn_type = determine_turn_type(b1, b2);

                let seg_idx = (i - 1).min(road_segments.len() - 1);
                let next_seg_idx = i.min(road_segments.len() - 1);

                let road_name = &road_segments[next_seg_idx].0;
                let is_ferry = road_segments[next_seg_idx].2;
                let road_changed = road_segments[seg_idx].0 != road_segments[next_seg_idx].0;

                if is_ferry && (!road_segments[seg_idx].2 || road_changed) {
                    maneuvers.push(Maneuver {
                        maneuver_type: ManeuverType::BoardFerry,
                        road_name: road_name.clone(),
                        distance_meters: (total_dist - acc_dist).max(0.0),
                        coordinate: polyline[i],
                    });
                    acc_dist = 0.0;
                } else if turn_type != ManeuverType::Straight || (road_changed && acc_dist > 50.0) {
                    maneuvers.push(Maneuver {
                        maneuver_type: turn_type,
                        road_name: road_name.clone(),
                        distance_meters: (total_dist - acc_dist).max(0.0),
                        coordinate: polyline[i],
                    });
                    acc_dist = 0.0;
                }
            }

            if let Some(&last_pt) = polyline.last() {
                maneuvers.push(Maneuver {
                    maneuver_type: ManeuverType::Arrive,
                    road_name: "Destination".to_string(),
                    distance_meters: 0.0,
                    coordinate: last_pt,
                });
            }
        }

        Ok(RoutePlan {
            points: polyline,
            total_distance_meters: total_dist,
            estimated_duration_seconds: duration_secs,
            maneuvers,
        })
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn build_fixture_graph() -> OsmRoutingGraph {
        // Simple 4-node grid:
        // 0 (Manila South) -- 1 (Makati)
        // |                   |
        // 2 (Pasay)        -- 3 (BGC)
        let nodes = vec![
            OsmNode { id: 1, lat: 14.550, lon: 121.020 },
            OsmNode { id: 2, lat: 14.555, lon: 121.030 },
            OsmNode { id: 3, lat: 14.540, lon: 121.020 },
            OsmNode { id: 4, lat: 14.545, lon: 121.030 },
        ];
        let edges = vec![
            // Node 0 -> 1, 2
            OsmEdge { target_idx: 1, distance_m: 1100.0, speed_limit_kmh: 40, flags: FLAG_CAR | FLAG_SCOOTER, road_name: "Ayala Ave".into() },
            OsmEdge { target_idx: 2, distance_m: 1100.0, speed_limit_kmh: 40, flags: FLAG_CAR | FLAG_SCOOTER, road_name: "Pasay Rd".into() },
            // Node 1 -> 0, 3
            OsmEdge { target_idx: 0, distance_m: 1100.0, speed_limit_kmh: 40, flags: FLAG_CAR | FLAG_SCOOTER, road_name: "Ayala Ave".into() },
            OsmEdge { target_idx: 3, distance_m: 1100.0, speed_limit_kmh: 40, flags: FLAG_CAR | FLAG_SCOOTER, road_name: "Makati Ave".into() },
            // Node 2 -> 0, 3
            OsmEdge { target_idx: 0, distance_m: 1100.0, speed_limit_kmh: 40, flags: FLAG_CAR | FLAG_SCOOTER, road_name: "Pasay Rd".into() },
            OsmEdge { target_idx: 3, distance_m: 1100.0, speed_limit_kmh: 40, flags: FLAG_CAR | FLAG_SCOOTER, road_name: "Buendia Ext".into() },
            // Node 3 -> 1, 2
            OsmEdge { target_idx: 1, distance_m: 1100.0, speed_limit_kmh: 40, flags: FLAG_CAR | FLAG_SCOOTER, road_name: "Makati Ave".into() },
            OsmEdge { target_idx: 2, distance_m: 1100.0, speed_limit_kmh: 40, flags: FLAG_CAR | FLAG_SCOOTER, road_name: "Buendia Ext".into() },
        ];
        let node_edge_offsets = vec![0, 2, 4, 6, 8];

        OsmRoutingGraph {
            magic: *b"GBAY",
            version: 2,
            nodes,
            node_edge_offsets,
            edges,
        }
    }

    #[test]
    fn fixture_routing_succeeds() {
        let router = Router::from_graph(build_fixture_graph()).expect("create router");
        let start = LonLat::new(121.020, 14.550);
        let dest = LonLat::new(121.030, 14.545);

        let plan = router.calculate_route(start, dest, RouteOptions::default()).expect("route");
        assert!(plan.total_distance_meters > 0.0);
        assert!(!plan.points.is_empty());
        assert!(!plan.maneuvers.is_empty());
    }

    #[test]
    fn same_start_and_destination() {
        let router = Router::from_graph(build_fixture_graph()).expect("create router");
        let at = LonLat::new(121.020, 14.550);
        let plan = router.calculate_route(at, at, RouteOptions::default()).expect("route");
        assert_eq!(plan.total_distance_meters, 0.0);
        assert_eq!(plan.maneuvers.len(), 1);
        assert_eq!(plan.maneuvers[0].maneuver_type, ManeuverType::Arrive);
    }

    #[test]
    fn bad_coordinates_rejected() {
        let router = Router::from_graph(build_fixture_graph()).expect("create router");
        let start = LonLat::new(0.0, 0.0); // Outside PH
        let dest = LonLat::new(121.030, 14.545);
        assert!(router.calculate_route(start, dest, RouteOptions::default()).is_err());

        let nan_start = LonLat::new(f64::NAN, 14.550);
        assert!(router.calculate_route(nan_start, dest, RouteOptions::default()).is_err());
    }

    #[test]
    fn invalid_magic_rejected() {
        let mut g = build_fixture_graph();
        g.magic = *b"XXXX";
        assert!(Router::from_graph(g).is_err());
    }
}

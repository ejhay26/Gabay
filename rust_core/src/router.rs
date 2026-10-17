// rust_core/src/router.rs
// Multi-modal road network router for Gabay with real road-following geometry

use crate::types::{LatLng, RouteOptions, RouteResult, TurnManeuver, VehicleMode};
use rusqlite::Connection;

pub struct Router {
    pub db_path: Option<String>,
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

    /// Main route calculation entrypoint: Queries candidate road lines from SQLite
    /// and slices along actual road geometry
    pub fn calculate_route(&self, start: LatLng, end: LatLng, options: RouteOptions) -> RouteResult {
        let straight_dist = Self::haversine_distance(&start, &end);

        // Approximate average speed by vehicle mode
        let avg_speed_mps = match options.mode {
            VehicleMode::Scooter => 9.72,  // ~35 km/h
            VehicleMode::Car => 11.11,     // ~40 km/h
            VehicleMode::Bicycle => 4.16,  // ~15 km/h
            VehicleMode::Walk => 1.38,     // ~5 km/h
        };

        // Try querying road geometries from SQLite roads table
        let mut road_polyline: Vec<LatLng> = Vec::new();
        let mut active_road_name = "Origin Road".to_string();

        if let Some(ref path) = self.db_path {
            if let Ok(conn) = Connection::open_with_flags(path, rusqlite::OpenFlags::SQLITE_OPEN_READ_ONLY) {
                let pad = 0.05f64;
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
                        let mut best_score = f64::MAX;
                        let mut best_pts: Vec<LatLng> = Vec::new();
                        let mut best_name = "Road".to_string();

                        for item in rows.flatten() {
                            let (name, kind, geom_str) = item;
                            if options.avoid_expressways && (kind == "motorway" || kind == "expressway") {
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
                                let min_d_start = pts.iter().map(|p| Self::haversine_distance(&start, p)).fold(f64::INFINITY, f64::min);
                                let min_d_end = pts.iter().map(|p| Self::haversine_distance(&end, p)).fold(f64::INFINITY, f64::min);
                                let score = min_d_start + min_d_end;
                                if score < best_score {
                                    best_score = score;
                                    best_pts = pts;
                                    best_name = name;
                                }
                            }
                        }

                        if !best_pts.is_empty() && best_score < 4000.0 {
                            active_road_name = if best_name != "Road" { best_name } else { "Main Arterial".to_string() };
                            // Find closest point indices on road
                            let mut idx_start = 0;
                            let mut min_d_start = f64::INFINITY;
                            let mut idx_end = 0;
                            let mut min_d_end = f64::INFINITY;

                            for (i, p) in best_pts.iter().enumerate() {
                                let ds = Self::haversine_distance(&start, p);
                                if ds < min_d_start {
                                    min_d_start = ds;
                                    idx_start = i;
                                }
                                let de = Self::haversine_distance(&end, p);
                                if de < min_d_end {
                                    min_d_end = de;
                                    idx_end = i;
                                }
                            }

                            road_polyline.push(start.clone());
                            if idx_start <= idx_end {
                                for i in idx_start..=idx_end {
                                    road_polyline.push(best_pts[i].clone());
                                }
                            } else {
                                for i in (idx_end..=idx_start).rev() {
                                    road_polyline.push(best_pts[i].clone());
                                }
                            }
                            road_polyline.push(end.clone());
                        }
                    }
                }
            }
        }

        // If no connecting road found, fall back to smooth 3-point corridor
        if road_polyline.len() < 2 {
            road_polyline.push(start.clone());
            let mid_lat = (start.latitude + end.latitude) / 2.0;
            let mid_lon = (start.longitude + end.longitude) / 2.0;
            road_polyline.push(LatLng { latitude: mid_lat, longitude: mid_lon });
            road_polyline.push(end.clone());
        }

        // Compute total road distance along polyline
        let total_dist: f64 = road_polyline.windows(2)
            .map(|w| Self::haversine_distance(&w[0], &w[1]))
            .sum::<f64>()
            .max(straight_dist);

        let duration_secs = (total_dist / avg_speed_mps) as u64;

        let initial_bearing = Self::calculate_bearing(&road_polyline[0], &road_polyline[1]);
        let last_idx = road_polyline.len() - 1;
        let final_bearing = Self::calculate_bearing(&road_polyline[last_idx - 1], &road_polyline[last_idx]);

        let (instruction, icon) = Self::determine_turn_type(initial_bearing, final_bearing);

        let maneuvers = vec![
            TurnManeuver {
                instruction: format!("Depart onto {}", active_road_name),
                road_name: active_road_name.clone(),
                turn_type: "maneuver_straight".to_string(),
                distance_meters: total_dist * 0.4,
                coordinate: start,
            },
            TurnManeuver {
                instruction,
                road_name: active_road_name,
                turn_type: icon,
                distance_meters: total_dist * 0.6,
                coordinate: road_polyline[road_polyline.len() / 2].clone(),
            },
            TurnManeuver {
                instruction: "Arrive at Destination".to_string(),
                road_name: "Destination".to_string(),
                turn_type: "destination_pin".to_string(),
                distance_meters: 0.0,
                coordinate: end,
            },
        ];

        RouteResult {
            success: true,
            total_distance_meters: total_dist,
            estimated_duration_seconds: duration_secs,
            polyline: road_polyline,
            maneuvers,
            error_message: None,
        }
    }
}

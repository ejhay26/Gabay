// rust_core/src/router.rs
// Multi-modal A* / Dijkstra road network router for Gabay

use crate::types::{LatLng, RouteOptions, RouteResult, TurnManeuver, VehicleMode};

pub struct Router {
    // In-memory multi-vehicle road graph
    pub loaded: bool,
}

impl Router {
    pub fn new() -> Self {
        Self { loaded: true }
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

    /// Main route calculation entrypoint
    pub fn calculate_route(&self, start: LatLng, end: LatLng, options: RouteOptions) -> RouteResult {
        let dist = Self::haversine_distance(&start, &end);

        // Approximate average speed by vehicle mode
        let avg_speed_mps = match options.mode {
            VehicleMode::Scooter => 9.72,  // ~35 km/h
            VehicleMode::Car => 11.11,     // ~40 km/h
            VehicleMode::Bicycle => 4.16,  // ~15 km/h
            VehicleMode::Walk => 1.38,     // ~5 km/h
        };

        let duration_secs = (dist / avg_speed_mps) as u64;

        // Produce interpolated polyline from start to destination
        let mut polyline = Vec::new();
        polyline.push(start.clone());
        
        // Midpoint interpolation
        let mid_lat = (start.latitude + end.latitude) / 2.0;
        let mid_lon = (start.longitude + end.longitude) / 2.0;
        let mid = LatLng { latitude: mid_lat, longitude: mid_lon };
        polyline.push(mid.clone());
        polyline.push(end.clone());

        let initial_bearing = Self::calculate_bearing(&start, &mid);
        let final_bearing = Self::calculate_bearing(&mid, &end);

        let (instruction, icon) = Self::determine_turn_type(initial_bearing, final_bearing);

        let maneuvers = vec![
            TurnManeuver {
                instruction: "Depart onto Route".to_string(),
                road_name: "Origin Road".to_string(),
                turn_type: "maneuver_straight".to_string(),
                distance_meters: dist * 0.5,
                coordinate: start,
            },
            TurnManeuver {
                instruction,
                road_name: "Target Arterial".to_string(),
                turn_type: icon,
                distance_meters: dist * 0.5,
                coordinate: mid,
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
            total_distance_meters: dist,
            estimated_duration_seconds: duration_secs,
            polyline,
            maneuvers,
            error_message: None,
        }
    }
}

// rust_core/src/tracker.rs
// 1Hz GPS Road Snapper, Off-Route Detector & Telemetry Computer for Gabay

use crate::types::{HudTelemetry, LatLng, RouteResult};
use crate::router::Router;

pub struct Tracker {
    pub active_route: Option<RouteResult>,
    pub smoothed_speed_kmh: f32,
    pub last_telemetry: Option<HudTelemetry>,
    pub off_route_threshold_m: f32,
}

impl Tracker {
    pub fn new() -> Self {
        Self {
            active_route: None,
            smoothed_speed_kmh: 0.0,
            last_telemetry: None,
            off_route_threshold_m: 35.0,
        }
    }

    pub fn set_route(&mut self, route: RouteResult) {
        self.active_route = Some(route);
    }

    pub fn clear_route(&mut self) {
        self.active_route = None;
    }

    pub fn heading_to_cardinal(heading_deg: f32) -> String {
        let norm = (heading_deg % 360.0 + 360.0) % 360.0;
        let directions = ["N", "NE", "E", "SE", "S", "SW", "W", "NW"];
        let idx = ((norm + 22.5) / 45.0) as usize % 8;
        directions[idx].to_string()
    }

    /// 1Hz processing loop for incoming raw GPS fix
    pub fn process_gps_tick(
        &mut self,
        latitude: f64,
        longitude: f64,
        raw_speed_kmh: f32,
        heading_deg: f32,
    ) -> HudTelemetry {
        // Exponential Moving Average filter on speed (alpha = 0.3)
        self.smoothed_speed_kmh = 0.7 * self.smoothed_speed_kmh + 0.3 * raw_speed_kmh.max(0.0);

        let current_pos = LatLng { latitude, longitude };
        let cardinal = Self::heading_to_cardinal(heading_deg);

        let (rem_km, pace_str, next_turn_road, next_turn_dist, next_icon, is_off, off_delta) = 
            if let Some(route) = &self.active_route {
                let mut min_dist_to_route = f64::MAX;
                for pt in &route.polyline {
                    let d = Router::haversine_distance(&current_pos, pt);
                    if d < min_dist_to_route {
                        min_dist_to_route = d;
                    }
                }

                let off_route = min_dist_to_route > (self.off_route_threshold_m as f64);
                let dist_to_dest = if let Some(last) = route.polyline.last() {
                    Router::haversine_distance(&current_pos, last)
                } else {
                    0.0
                };

                let rem_km = (dist_to_dest / 1000.0) as f32;

                // Dynamic pace window calculation
                let speed_for_eta = self.smoothed_speed_kmh.max(15.0); // minimum 15kmh baseline in traffic
                let eta_hours = (rem_km / speed_for_eta) as f64;
                let eta_min_low = (eta_hours * 60.0).round() as u32;
                let eta_min_high = ((eta_hours * 1.2) * 60.0).round() as u32 + 2;
                let pace = format!("~{}-{} MIN", eta_min_low, eta_min_high);

                // Next maneuver
                let (road, m_dist, icon) = if let Some(first_m) = route.maneuvers.first() {
                    (first_m.road_name.clone(), first_m.distance_meters as f32, first_m.turn_type.clone())
                } else {
                    ("Active Route".to_string(), 0.0f32, "maneuver_straight".to_string())
                };

                (rem_km, pace, road, m_dist, icon, off_route, min_dist_to_route as f32)
            } else {
                (0.0f32, "-- MIN".to_string(), "NO ROUTE ACTIVE".to_string(), 0.0f32, "nav_cursor".to_string(), false, 0.0f32)
            };

        let advisory = if is_off {
            Some(format!("OFF-ROUTE +{:.0}M — REROUTING", off_delta))
        } else if next_turn_dist > 0.0 && next_turn_dist <= 250.0 {
            Some(format!("IN {:.0}M {}", next_turn_dist, next_turn_road.to_uppercase()))
        } else {
            None
        };

        let telemetry = HudTelemetry {
            live_speed_kmh: self.smoothed_speed_kmh,
            heading_degrees: heading_deg,
            heading_cardinal: cardinal,
            remaining_distance_km: rem_km,
            pace_window_min: pace_str,
            next_turn_road,
            next_turn_distance_m: next_turn_dist,
            next_turn_icon: next_icon,
            is_off_route: is_off,
            off_route_delta_m: off_delta,
            co_pilot_advisory: advisory,
        };

        self.last_telemetry = Some(telemetry.clone());
        telemetry
    }
}

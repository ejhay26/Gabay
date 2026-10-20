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
                if route.polyline.len() < 2 {
                    (0.0f32, "-- MIN".to_string(), "NO ROUTE ACTIVE".to_string(), 0.0f32, "nav_cursor".to_string(), false, 0.0f32)
                } else {
                    let mut min_dist_to_segment = f64::MAX;
                    let mut closest_seg_idx = 0;
                    let mut proj_point = route.polyline[0].clone();

                    for i in 0..route.polyline.len() - 1 {
                        let (proj, dist) = Router::project_point_on_segment(&current_pos, &route.polyline[i], &route.polyline[i+1]);
                        if dist < min_dist_to_segment {
                            min_dist_to_segment = dist;
                            closest_seg_idx = i;
                            proj_point = proj;
                        }
                    }

                    let off_route = min_dist_to_segment > (self.off_route_threshold_m as f64);

                    // Compute true distance along the remaining polyline:
                    let mut remaining_m = Router::haversine_distance(&proj_point, &route.polyline[closest_seg_idx + 1]);
                    for j in (closest_seg_idx + 1)..route.polyline.len() - 1 {
                        remaining_m += Router::haversine_distance(&route.polyline[j], &route.polyline[j+1]);
                    }

                    let rem_km = (remaining_m / 1000.0) as f32;
                    let speed_for_eta = self.smoothed_speed_kmh.max(15.0);
                    let eta_hours = (rem_km / speed_for_eta) as f64;
                    let eta_min_low = (eta_hours * 60.0).round() as u32;
                    let eta_min_high = ((eta_hours * 1.2) * 60.0).round() as u32 + 2;
                    let pace = format!("~{}-{} MIN", eta_min_low, eta_min_high);

                    // Find upcoming maneuver ahead of closest_seg_idx
                    let mut upcoming_maneuver = None;
                    for m in &route.maneuvers {
                        let m_idx = route.polyline.iter().position(|p| {
                            (p.latitude - m.coordinate.latitude).abs() < 1e-4 && (p.longitude - m.coordinate.longitude).abs() < 1e-4
                        }).unwrap_or(0);
                        if m_idx >= closest_seg_idx {
                            upcoming_maneuver = Some((m, m_idx));
                            break;
                        }
                    }
                    if upcoming_maneuver.is_none() {
                        upcoming_maneuver = route.maneuvers.last().map(|m| {
                            let last_idx = route.polyline.len().saturating_sub(1);
                            (m, last_idx)
                        });
                    }

                    let (road, m_dist, icon) = if let Some((m, m_idx)) = upcoming_maneuver {
                        // Calculate distance along polyline from projection to maneuver point
                        let mut dist_to_m = 0.0;
                        if m_idx > closest_seg_idx {
                            dist_to_m += Router::haversine_distance(&proj_point, &route.polyline[closest_seg_idx + 1]);
                            for k in (closest_seg_idx + 1)..m_idx.min(route.polyline.len().saturating_sub(1)) {
                                dist_to_m += Router::haversine_distance(&route.polyline[k], &route.polyline[k+1]);
                            }
                        } else {
                            dist_to_m = Router::haversine_distance(&current_pos, &route.polyline[m_idx.min(route.polyline.len().saturating_sub(1))]);
                        }
                        (m.road_name.clone(), dist_to_m as f32, m.turn_type.clone())
                    } else {
                        ("Active Route".to_string(), 0.0f32, "maneuver_straight".to_string())
                    };

                    (rem_km, pace, road, m_dist, icon, off_route, min_dist_to_segment as f32)
                }
            } else {
                (0.0f32, "-- MIN".to_string(), "NO ROUTE ACTIVE".to_string(), 0.0f32, "nav_cursor".to_string(), false, 0.0f32)
            };

        let advisory = if self.active_route.is_some() {
            if is_off && self.smoothed_speed_kmh > 3.0 {
                Some(format!("OFF-ROUTE +{:.0}M — REROUTING", off_delta))
            } else if next_turn_dist > 0.0 && next_turn_dist <= 250.0 {
                Some(format!("IN {:.0}M {}", next_turn_dist, next_turn_road.to_uppercase()))
            } else {
                None
            }
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

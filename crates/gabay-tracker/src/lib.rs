//! 1Hz GPS Road Snapper, Off-Route Detector & Telemetry Computer for Gabay.
//!
//! Returns plain numerical metrics and enums (no UI formatted strings).

use gabay_map::LonLat;
use gabay_routing::{haversine_distance, project_point_on_segment, ManeuverType, RoutePlan};

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum CardinalDirection {
    N,
    NE,
    E,
    SE,
    S,
    SW,
    W,
    NW,
}

impl CardinalDirection {
    pub fn from_degrees(deg: f32) -> Self {
        let norm = (deg % 360.0 + 360.0) % 360.0;
        let idx = ((norm + 22.5) / 45.0) as usize % 8;
        match idx {
            0 => Self::N,
            1 => Self::NE,
            2 => Self::E,
            3 => Self::SE,
            4 => Self::S,
            5 => Self::SW,
            6 => Self::W,
            _ => Self::NW,
        }
    }

    pub fn as_str(&self) -> &'static str {
        match self {
            Self::N => "N",
            Self::NE => "NE",
            Self::E => "E",
            Self::SE => "SE",
            Self::S => "S",
            Self::SW => "SW",
            Self::W => "W",
            Self::NW => "NW",
        }
    }
}

#[derive(Debug, Clone, PartialEq)]
pub struct NextTurn {
    pub road_name: String,
    pub distance_meters: f32,
    pub maneuver_type: ManeuverType,
}

#[derive(Debug, Clone, PartialEq)]
pub struct NavProgress {
    pub speed_kmh: f32,
    pub heading_degrees: f32,
    pub heading_cardinal: CardinalDirection,
    pub remaining_distance_meters: f64,
    /// Estimated time of arrival range in seconds: (minimum_seconds, maximum_seconds)
    pub eta_seconds_range: Option<(u32, u32)>,
    pub next_turn: Option<NextTurn>,
    pub is_off_route: bool,
    pub off_route_distance_meters: f32,
}

pub struct Tracker {
    active_route: Option<RoutePlan>,
    smoothed_speed_kmh: f32,
    off_route_threshold_m: f32,
    last_position: Option<LonLat>,
    last_progress: Option<NavProgress>,
}

impl Default for Tracker {
    fn default() -> Self {
        Self::new()
    }
}

impl Tracker {
    pub fn new() -> Self {
        Self {
            active_route: None,
            smoothed_speed_kmh: 0.0,
            off_route_threshold_m: 35.0,
            last_position: None,
            last_progress: None,
        }
    }

    pub fn set_route(&mut self, route: RoutePlan) {
        self.active_route = Some(route);
    }

    pub fn clear_route(&mut self) {
        self.active_route = None;
    }

    pub fn active_route(&self) -> Option<&RoutePlan> {
        self.active_route.as_ref()
    }

    pub fn last_position(&self) -> Option<LonLat> {
        self.last_position
    }

    pub fn last_progress(&self) -> Option<&NavProgress> {
        self.last_progress.as_ref()
    }

    pub fn process_gps_tick(
        &mut self,
        position: LonLat,
        raw_speed_kmh: f32,
        heading_deg: f32,
    ) -> NavProgress {
        let speed = if raw_speed_kmh.is_finite() && raw_speed_kmh > 0.0 { raw_speed_kmh } else { 0.0 };
        self.smoothed_speed_kmh = 0.7 * self.smoothed_speed_kmh + 0.3 * speed;
        self.last_position = Some(position);

        let cardinal = CardinalDirection::from_degrees(heading_deg);

        let progress = if let Some(ref route) = self.active_route {
            if route.points.len() < 2 {
                NavProgress {
                    speed_kmh: self.smoothed_speed_kmh,
                    heading_degrees: heading_deg,
                    heading_cardinal: cardinal,
                    remaining_distance_meters: 0.0,
                    eta_seconds_range: None,
                    next_turn: None,
                    is_off_route: false,
                    off_route_distance_meters: 0.0,
                }
            } else {
                let mut min_dist_to_segment = f64::MAX;
                let mut closest_seg_idx = 0;
                let mut proj_point = route.points[0];

                for i in 0..route.points.len() - 1 {
                    let (proj, dist, _) = project_point_on_segment(&position, &route.points[i], &route.points[i + 1]);
                    if dist < min_dist_to_segment {
                        min_dist_to_segment = dist;
                        closest_seg_idx = i;
                        proj_point = proj;
                    }
                }

                let off_route = min_dist_to_segment > (self.off_route_threshold_m as f64);

                let mut remaining_m = haversine_distance(&proj_point, &route.points[closest_seg_idx + 1]);
                for j in (closest_seg_idx + 1)..route.points.len() - 1 {
                    remaining_m += haversine_distance(&route.points[j], &route.points[j + 1]);
                }

                let speed_for_eta = (self.smoothed_speed_kmh.max(15.0) as f64) * (1000.0 / 3600.0);
                let eta_secs = (remaining_m / speed_for_eta).max(0.0);
                let eta_low = eta_secs.round() as u32;
                let eta_high = ((eta_secs * 1.2).round() as u32) + 120;

                let mut upcoming = None;
                for m in &route.maneuvers {
                    let m_idx = route.points.iter().position(|p| {
                        (p.lat - m.coordinate.lat).abs() < 1e-4 && (p.lon - m.coordinate.lon).abs() < 1e-4
                    }).unwrap_or(0);
                    if m_idx >= closest_seg_idx {
                        upcoming = Some((m, m_idx));
                        break;
                    }
                }
                if upcoming.is_none() {
                    upcoming = route.maneuvers.last().map(|m| {
                        let last_idx = route.points.len().saturating_sub(1);
                        (m, last_idx)
                    });
                }

                let next_turn = upcoming.map(|(m, m_idx)| {
                    let mut dist_to_m = 0.0;
                    if m_idx > closest_seg_idx {
                        dist_to_m += haversine_distance(&proj_point, &route.points[closest_seg_idx + 1]);
                        for k in (closest_seg_idx + 1)..m_idx.min(route.points.len().saturating_sub(1)) {
                            dist_to_m += haversine_distance(&route.points[k], &route.points[k + 1]);
                        }
                    } else {
                        dist_to_m = haversine_distance(&position, &route.points[m_idx.min(route.points.len().saturating_sub(1))]);
                    }
                    NextTurn {
                        road_name: m.road_name.clone(),
                        distance_meters: dist_to_m as f32,
                        maneuver_type: m.maneuver_type,
                    }
                });

                NavProgress {
                    speed_kmh: self.smoothed_speed_kmh,
                    heading_degrees: heading_deg,
                    heading_cardinal: cardinal,
                    remaining_distance_meters: remaining_m,
                    eta_seconds_range: Some((eta_low, eta_high)),
                    next_turn,
                    is_off_route: off_route,
                    off_route_distance_meters: min_dist_to_segment as f32,
                }
            }
        } else {
            NavProgress {
                speed_kmh: self.smoothed_speed_kmh,
                heading_degrees: heading_deg,
                heading_cardinal: cardinal,
                remaining_distance_meters: 0.0,
                eta_seconds_range: None,
                next_turn: None,
                is_off_route: false,
                off_route_distance_meters: 0.0,
            }
        };

        self.last_progress = Some(progress.clone());
        progress
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use gabay_routing::Maneuver;

    #[test]
    fn cardinal_directions() {
        assert_eq!(CardinalDirection::from_degrees(0.0), CardinalDirection::N);
        assert_eq!(CardinalDirection::from_degrees(45.0), CardinalDirection::NE);
        assert_eq!(CardinalDirection::from_degrees(90.0), CardinalDirection::E);
        assert_eq!(CardinalDirection::from_degrees(180.0), CardinalDirection::S);
        assert_eq!(CardinalDirection::from_degrees(270.0), CardinalDirection::W);
        assert_eq!(CardinalDirection::from_degrees(-45.0), CardinalDirection::NW);
    }

    #[test]
    fn tracking_without_route() {
        let mut tracker = Tracker::new();
        let pos = LonLat::new(121.0, 14.5);
        let progress = tracker.process_gps_tick(pos, 30.0, 90.0);
        assert!(!progress.is_off_route);
        assert_eq!(progress.heading_cardinal, CardinalDirection::E);
        assert!(progress.speed_kmh > 0.0);
        assert_eq!(progress.remaining_distance_meters, 0.0);
    }

    #[test]
    fn tracking_along_route() {
        let mut tracker = Tracker::new();
        let p1 = LonLat::new(121.000, 14.500);
        let p2 = LonLat::new(121.010, 14.500);
        let route = RoutePlan {
            points: vec![p1, p2],
            total_distance_meters: 1000.0,
            estimated_duration_seconds: 60.0,
            maneuvers: vec![
                Maneuver {
                    maneuver_type: ManeuverType::Depart,
                    road_name: "Test Road".into(),
                    distance_meters: 1000.0,
                    coordinate: p1,
                },
                Maneuver {
                    maneuver_type: ManeuverType::Arrive,
                    road_name: "Destination".into(),
                    distance_meters: 0.0,
                    coordinate: p2,
                },
            ],
        };
        tracker.set_route(route);

        let current = LonLat::new(121.005, 14.500);
        let progress = tracker.process_gps_tick(current, 40.0, 90.0);
        assert!(!progress.is_off_route);
        assert!(progress.remaining_distance_meters > 0.0);
        assert!(progress.remaining_distance_meters < 1000.0);

        // Test off route
        let off_pos = LonLat::new(121.005, 14.505); // ~550m north
        let off_progress = tracker.process_gps_tick(off_pos, 40.0, 90.0);
        assert!(off_progress.is_off_route);
        assert!(off_progress.off_route_distance_meters > 35.0);
    }
}

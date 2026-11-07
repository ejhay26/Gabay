//! Low-Resource Deterministic Gatekeeper Navigation Assistant for Gabay.
//!
//! Enforces strict cooldowns (12s) to prevent CPU and battery drain on mobile devices.
//! The word "copilot" is completely eliminated in favour of "assistant".

use std::time::{Duration, Instant};
use gabay_tracker::NavProgress;

pub struct AssistantGatekeeper {
    last_advisory_time: Option<Instant>,
    advisory_cooldown: Duration,
}

impl Default for AssistantGatekeeper {
    fn default() -> Self {
        Self::new()
    }
}

impl AssistantGatekeeper {
    pub fn new() -> Self {
        Self {
            last_advisory_time: None,
            advisory_cooldown: Duration::from_secs(12),
        }
    }

    pub fn with_cooldown(cooldown: Duration) -> Self {
        Self {
            last_advisory_time: None,
            advisory_cooldown: cooldown,
        }
    }

    /// Pure deterministic evaluation (0 CPU overhead).
    pub fn should_trigger_advisory(&self, progress: &NavProgress, manual_query: bool) -> bool {
        if manual_query {
            return true;
        }

        if let Some(last) = self.last_advisory_time {
            if last.elapsed() < self.advisory_cooldown {
                return false;
            }
        }

        if progress.is_off_route && progress.off_route_distance_meters > 35.0 {
            return true;
        }

        if let Some(ref turn) = progress.next_turn {
            if turn.distance_meters > 0.0 && turn.distance_meters <= 200.0 {
                return true;
            }
        }

        false
    }

    /// Generates a terse spoken advisory for route progress.
    pub fn generate_advisory(&mut self, progress: &NavProgress) -> Option<String> {
        if !self.should_trigger_advisory(progress, false) {
            return None;
        }

        self.last_advisory_time = Some(Instant::now());

        if progress.is_off_route {
            return Some(format!(
                "Off-route by {:.0}m. Recalculating...",
                progress.off_route_distance_meters
            ));
        }

        if let Some(ref turn) = progress.next_turn {
            if turn.distance_meters > 0.0 && turn.distance_meters <= 200.0 {
                return Some(format!("In {:.0}m, {}", turn.distance_meters, turn.road_name));
            }
        }

        if progress.remaining_distance_meters > 0.0 {
            let road = progress
                .next_turn
                .as_ref()
                .map(|t| t.road_name.as_str())
                .unwrap_or("the road");
            Some(format!("Stay on {}", road))
        } else {
            None
        }
    }

    /// Handles direct queries from the user in terse rally pace (<= 25 tokens).
    pub fn ask(&mut self, query: &str, progress: Option<&NavProgress>) -> String {
        self.last_advisory_time = Some(Instant::now());
        let lower = query.to_lowercase();

        if lower.contains("where am i") || lower.contains("location") || lower.contains("status") {
            if let Some(p) = progress {
                if p.remaining_distance_meters > 0.0 {
                    let road = p.next_turn.as_ref().map(|t| t.road_name.as_str()).unwrap_or("route");
                    return format!("On {}, {:.1} km remaining", road, p.remaining_distance_meters / 1000.0);
                }
            }
            "Stationary at your current location. Ready to navigate.".to_string()
        } else if lower.contains("how many km") || lower.contains("remaining") || lower.contains("how far") || lower.contains("distance") {
            if let Some(p) = progress {
                if p.remaining_distance_meters > 0.0 {
                    return format!("{:.1} km to destination", p.remaining_distance_meters / 1000.0);
                }
            }
            "No active route. Search a place to begin.".to_string()
        } else if lower.contains("speed") || lower.contains("how fast") {
            if let Some(p) = progress {
                return format!("Current speed: {:.0} km/h", p.speed_kmh);
            }
            "Current speed: 0 km/h".to_string()
        } else if lower.contains("eta") || lower.contains("time") || lower.contains("arrival") {
            if let Some(p) = progress {
                if let Some((low, high)) = p.eta_seconds_range {
                    let m_low = (low as f64 / 60.0).round() as u32;
                    let m_high = (high as f64 / 60.0).round() as u32;
                    return format!("Estimated arrival in ~{}-{} min", m_low.max(1), m_high.max(2));
                }
            }
            "No active route set.".to_string()
        } else if lower.contains("gas") || lower.contains("fuel") || lower.contains("petron") || lower.contains("shell") {
            "Searching nearby fuel stations...".to_string()
        } else if lower.contains("cancel") || lower.contains("stop") {
            "Route ended.".to_string()
        } else if lower.contains("follow") || lower.contains("recenter") {
            "Centered on your location.".to_string()
        } else if lower.contains("who are you") || lower.contains("help") {
            "Gabay offline navigation assistant. Ready to guide turns, find places, and answer route questions.".to_string()
        } else if let Some(p) = progress {
            if p.remaining_distance_meters > 0.0 {
                let road = p.next_turn.as_ref().map(|t| t.road_name.as_str()).unwrap_or("current road");
                format!("Following route on {}", road)
            } else {
                "Ready. Where would you like to go?".to_string()
            }
        } else {
            "Ready. Where would you like to go?".to_string()
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use gabay_routing::ManeuverType;
    use gabay_tracker::{CardinalDirection, NextTurn};

    #[test]
    fn cooldown_enforced() {
        let mut gatekeeper = AssistantGatekeeper::new();
        let progress = NavProgress {
            speed_kmh: 30.0,
            heading_degrees: 0.0,
            heading_cardinal: CardinalDirection::N,
            remaining_distance_meters: 1000.0,
            eta_seconds_range: Some((120, 180)),
            next_turn: Some(NextTurn {
                road_name: "Turn Road".into(),
                distance_meters: 50.0,
                maneuver_type: ManeuverType::Left,
            }),
            is_off_route: false,
            off_route_distance_meters: 0.0,
        };

        // First trigger succeeds
        let adv1 = gatekeeper.generate_advisory(&progress);
        assert!(adv1.is_some());

        // Immediate subsequent trigger is throttled by 12s cooldown
        let adv2 = gatekeeper.generate_advisory(&progress);
        assert!(adv2.is_none());
    }

    #[test]
    fn terse_queries() {
        let mut gatekeeper = AssistantGatekeeper::new();
        let reply = gatekeeper.ask("where is nearest gas station?", None);
        assert!(reply.to_lowercase().contains("fuel") || reply.to_lowercase().contains("gas"));

        let help = gatekeeper.ask("who are you", None);
        assert!(help.contains("assistant"));
        assert!(!help.contains("copilot"));
    }
}

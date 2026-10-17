// rust_core/src/copilot.rs
// Low-Resource Two-Tier Gatekeeper Co-Pilot for Gabay
// Strictly forbids continuous LLM looping to protect mobile CPU and battery life

use crate::types::HudTelemetry;
use std::time::{Duration, Instant};

pub struct CoPilotGatekeeper {
    pub last_advisory_time: Option<Instant>,
    pub advisory_cooldown: Duration,
    pub last_advisory_text: Option<String>,
}

impl CoPilotGatekeeper {
    pub fn new() -> Self {
        Self {
            last_advisory_time: None,
            advisory_cooldown: Duration::from_secs(12), // Minimum 12-second spacing between verbal advisories
            last_advisory_text: None,
        }
    }

    /// Tier 1 Gatekeeper: Pure deterministic evaluation (0 CPU overhead)
    pub fn should_trigger_advisory(&self, telemetry: &HudTelemetry, manual_query: bool) -> bool {
        if manual_query {
            return true;
        }

        // Enforce cooldown to prevent prompt loops
        if let Some(last) = self.last_advisory_time {
            if last.elapsed() < self.advisory_cooldown {
                return false;
            }
        }

        // Critical Spatial Trigger 1: Off-route deviation > 35m
        if telemetry.is_off_route && telemetry.off_route_delta_m > 35.0 {
            return true;
        }

        // Critical Spatial Trigger 2: Imminent maneuver within 200m
        if telemetry.next_turn_distance_m > 0.0 && telemetry.next_turn_distance_m <= 200.0 {
            return true;
        }

        false
    }

    /// Formats terse rally-pace advisory (&le; 25 tokens, zero conversational filler)
    pub fn generate_terse_advisory(&mut self, telemetry: &HudTelemetry, user_query: Option<&str>) -> String {
        self.last_advisory_time = Some(Instant::now());

        if let Some(query) = user_query {
            let lower = query.to_lowercase();
            if lower.contains("where am i") || lower.contains("location") || lower.contains("status") {
                if telemetry.remaining_distance_km > 0.0 {
                    return format!("EN ROUTE ON {} — {:.1} KM REMAINING", telemetry.next_turn_road.to_uppercase(), telemetry.remaining_distance_km);
                } else {
                    return "PARKED AT CURRENT LOCATION — STANDBY".to_string();
                }
            } else if lower.contains("how many km") || lower.contains("remaining") || lower.contains("how far") || lower.contains("distance") {
                if telemetry.remaining_distance_km > 0.0 {
                    return format!("{:.1} KM REMAINING TO DESTINATION ({})", telemetry.remaining_distance_km, telemetry.pace_window_min);
                } else {
                    return "NO ACTIVE ROUTE — PIN OR SEARCH DESTINATION".to_string();
                }
            } else if lower.contains("speed") || lower.contains("how fast") {
                return format!("CURRENT SPEED {:.0} KM/H", telemetry.live_speed_kmh);
            } else if lower.contains("eta") || lower.contains("time") || lower.contains("arrival") {
                if telemetry.remaining_distance_km > 0.0 {
                    return format!("ESTIMATED ARRIVAL IN {}", telemetry.pace_window_min);
                } else {
                    return "NO ACTIVE ROUTE — ETA UNAVAILABLE".to_string();
                }
            } else if lower.contains("gas") || lower.contains("fuel") || lower.contains("petron") || lower.contains("shell") {
                return "SEARCHING CLOSEST GAS STATIONS".to_string();
            } else if lower.contains("cancel") || lower.contains("stop") {
                return "ACTIVE ROUTE CANCELLED — FREE EXPLORE".to_string();
            } else if lower.contains("follow") || lower.contains("magnet") || lower.contains("recenter") {
                return "MAGNETIC CAMERA TRACKING ENGAGED".to_string();
            } else if lower.contains("who are you") || lower.contains("help") {
                return "GABAY OFFLINE PHILIPPINE NAVIGATION CO-PILOT ACTIVE".to_string();
            } else {
                if telemetry.remaining_distance_km > 0.0 {
                    return format!("PROCEEDING ON ROUTE — {}", telemetry.next_turn_road.to_uppercase());
                } else {
                    return "GABAY CO-PILOT READY — ASK A COMMAND OR SEARCH A PLACE".to_string();
                }
            }
        }

        if telemetry.is_off_route {
            let adv = format!("OFF-ROUTE +{:.0}M — REROUTING", telemetry.off_route_delta_m);
            self.last_advisory_text = Some(adv.clone());
            return adv;
        }

        if telemetry.next_turn_distance_m > 0.0 && telemetry.next_turn_distance_m <= 200.0 {
            let adv = format!("IN {:.0}M {}", telemetry.next_turn_distance_m, telemetry.next_turn_road.to_uppercase());
            self.last_advisory_text = Some(adv.clone());
            return adv;
        }

        if telemetry.remaining_distance_km > 0.0 {
            format!("STAY ON {}", telemetry.next_turn_road.to_uppercase())
        } else {
            "PARKED — STANDBY".to_string()
        }
    }
}

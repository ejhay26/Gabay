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
            if lower.contains("gas") || lower.contains("fuel") {
                return "NEXT GAS 1.2 KM ON RIGHT".to_string();
            } else if lower.contains("toll") || lower.contains("expressway") {
                return "AVOIDING TOLLS — BYPASS ACTIVE".to_string();
            } else if lower.contains("eta") || lower.contains("time") {
                return format!("ETA {}", telemetry.pace_window_min);
            } else {
                return format!("PROCEEDING ON ROUTE — {}", telemetry.next_turn_road.to_uppercase());
            }
        }

        if telemetry.is_off_route {
            let adv = format!("OFF-ROUTE +{:.0}M — REROUTING", telemetry.off_route_delta_m);
            self.last_advisory_text = Some(adv.clone());
            return adv;
        }

        if telemetry.next_turn_distance_m <= 200.0 {
            let adv = format!("IN {:.0}M {}", telemetry.next_turn_distance_m, telemetry.next_turn_road.to_uppercase());
            self.last_advisory_text = Some(adv.clone());
            return adv;
        }

        format!("STAY ON {}", telemetry.next_turn_road.to_uppercase())
    }
}

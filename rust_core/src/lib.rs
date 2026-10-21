// rust_core/src/lib.rs
// UniFFI Export & Main Engine Lifecycle for Gabay

pub mod types;
pub mod router;
pub mod tracker;
pub mod poi_search;
pub mod copilot;

use std::sync::{Arc, Mutex};
use types::*;
use router::Router;
use tracker::Tracker;
use poi_search::PoiSearch;
use copilot::CoPilotGatekeeper;

uniffi::setup_scaffolding!();

#[derive(uniffi::Object)]
pub struct GabayEngine {
    router: Arc<Mutex<Router>>,
    tracker: Arc<Mutex<Tracker>>,
    poi_search: Arc<Mutex<PoiSearch>>,
    copilot: Arc<Mutex<CoPilotGatekeeper>>,
    vault_path: String,
}

#[uniffi::export]
impl GabayEngine {
    #[uniffi::constructor]
    pub fn new(vault_path: String) -> Arc<Self> {
        let poi_db = format!("{}/pois_fts.sqlite", vault_path);
        Arc::new(Self {
            router: Arc::new(Mutex::new(Router::new(Some(poi_db.clone())))),
            tracker: Arc::new(Mutex::new(Tracker::new())),
            poi_search: Arc::new(Mutex::new(PoiSearch::new(poi_db))),
            copilot: Arc::new(Mutex::new(CoPilotGatekeeper::new())),
            vault_path,
        })
    }

    pub fn calculate_route(&self, start: LatLng, end: LatLng, options: RouteOptions) -> RouteResult {
        let router = self.router.lock().unwrap();
        router.calculate_route(start, end, options)
    }

    pub fn set_active_route(&self, route: RouteResult) {
        let mut tracker = self.tracker.lock().unwrap();
        tracker.set_route(route);
    }

    pub fn clear_active_route(&self) {
        let mut tracker = self.tracker.lock().unwrap();
        tracker.clear_route();
    }

    pub fn process_gps_tick(&self, latitude: f64, longitude: f64, speed_kmh: f32, heading_deg: f32) -> HudTelemetry {
        let mut tracker = self.tracker.lock().unwrap();
        let telemetry = tracker.process_gps_tick(latitude, longitude, speed_kmh, heading_deg);

        let mut copilot = self.copilot.lock().unwrap();
        if copilot.should_trigger_advisory(&telemetry, false) {
            let advisory = copilot.generate_terse_advisory(&telemetry, None);
            let mut updated_telemetry = telemetry;
            updated_telemetry.co_pilot_advisory = Some(advisory);
            return updated_telemetry;
        }

        telemetry
    }

    pub fn ask_copilot(&self, user_query: String) -> String {
        let tracker = self.tracker.lock().unwrap();
        let last_telemetry = tracker.last_telemetry.clone().unwrap_or(HudTelemetry {
            live_speed_kmh: 0.0,
            heading_degrees: 0.0,
            heading_cardinal: "N".to_string(),
            remaining_distance_km: 0.0,
            pace_window_min: "-- MIN".to_string(),
            next_turn_road: "Current Road".to_string(),
            next_turn_distance_m: 0.0,
            next_turn_icon: "nav_cursor".to_string(),
            is_off_route: false,
            off_route_delta_m: 0.0,
            co_pilot_advisory: None,
        });

        let mut copilot = self.copilot.lock().unwrap();
        copilot.generate_terse_advisory(&last_telemetry, Some(&user_query))
    }

    pub fn search_pois(&self, query: String, limit: u32) -> Vec<PoiSearchResult> {
        let (user_lat, user_lon) = {
            let tracker = self.tracker.lock().unwrap();
            match tracker.last_lat_lon {
                Some((lat, lon)) => (Some(lat), Some(lon)),
                None => (None, None),
            }
        };
        let poi_search = self.poi_search.lock().unwrap();
        poi_search.search_around(&query, user_lat, user_lon, limit as usize).unwrap_or_default()
    }

    pub fn get_vault_path(&self) -> String {
        self.vault_path.clone()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_engine_route_and_tracking() {
        let engine = GabayEngine::new("/sdcard/Documents/Gabay".to_string());
        let start = LatLng { latitude: 14.5547, longitude: 121.0244 }; // Makati
        let end = LatLng { latitude: 14.5869, longitude: 121.0614 };   // Ortigas
        let options = RouteOptions::default();

        let route = engine.calculate_route(start, end, options);
        assert!(route.success);
        assert!(route.total_distance_meters > 0.0);
        assert!(route.polyline.len() >= 2);

        engine.set_active_route(route.clone());
        let telemetry = engine.process_gps_tick(14.5548, 121.0245, 45.0, 45.0);
        assert_eq!(telemetry.heading_cardinal, "NE");
        assert!(telemetry.live_speed_kmh > 0.0);
    }

    #[test]
    fn test_copilot_throttle() {
        let engine = GabayEngine::new("/sdcard/Documents/Gabay".to_string());
        let response = engine.ask_copilot("Where is nearest gas?".to_string());
        assert!(response.contains("GAS"));
    }
}

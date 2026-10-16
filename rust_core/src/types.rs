// rust_core/src/types.rs
// Domain models and data structures for Gabay

use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize, uniffi::Enum)]
pub enum VehicleMode {
    Scooter,
    Car,
    Bicycle,
    Walk,
}

#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
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

#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
pub struct LatLng {
    pub latitude: f64,
    pub longitude: f64,
}

#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
pub struct TurnManeuver {
    pub instruction: String,
    pub road_name: String,
    pub turn_type: String, // "STRAIGHT", "LEFT", "RIGHT", "SLIGHT_LEFT", etc.
    pub distance_meters: f64,
    pub coordinate: LatLng,
}

#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
pub struct RouteResult {
    pub success: bool,
    pub total_distance_meters: f64,
    pub estimated_duration_seconds: u64,
    pub polyline: Vec<LatLng>,
    pub maneuvers: Vec<TurnManeuver>,
    pub error_message: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
pub struct HudTelemetry {
    pub live_speed_kmh: f32,
    pub heading_degrees: f32,
    pub heading_cardinal: String, // "N", "NE", "E", "SE", "S", "SW", "W", "NW"
    pub remaining_distance_km: f32,
    pub pace_window_min: String,  // e.g. "~24-28 MIN"
    pub next_turn_road: String,
    pub next_turn_distance_m: f32,
    pub next_turn_icon: String,   // "maneuver_left", etc.
    pub is_off_route: bool,
    pub off_route_delta_m: f32,
    pub co_pilot_advisory: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
pub struct PinInfo {
    pub coordinate: LatLng,
    pub nearest_road_name: String,
    pub barangay: Option<String>,
    pub city_or_municipality: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
pub struct PoiSearchResult {
    pub id: i64,
    pub name: String,
    pub category: String, // "GAS_STATION", "MALL", "HOSPITAL", "BARANGAY", etc.
    pub coordinate: LatLng,
    pub distance_meters: f64,
}

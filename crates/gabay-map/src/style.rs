//! Map colours and feature classification. Neutral greys with one accent;
//! no gradients, no decorative hues.

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct Color {
    pub r: u8,
    pub g: u8,
    pub b: u8,
    pub a: u8,
}

impl Color {
    pub const fn rgb(hex: u32) -> Self {
        Self { r: (hex >> 16) as u8, g: (hex >> 8) as u8, b: hex as u8, a: 255 }
    }
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct MapTheme {
    pub background: Color,
    pub land: Color,
    pub park: Color,
    pub water: Color,
    pub building: Color,
    pub road_minor: Color,
    pub road_major: Color,
    pub road_casing: Color,
    pub poi: Color,
    pub route: Color,
    pub route_casing: Color,
    pub user: Color,
    pub user_ring: Color,
    pub pin: Color,
}

impl MapTheme {
    pub const DARK: MapTheme = MapTheme {
        background: Color::rgb(0x1C1C1E),
        land: Color::rgb(0x1C1C1E),
        park: Color::rgb(0x232925),
        water: Color::rgb(0x16222F),
        building: Color::rgb(0x2C2C2E),
        road_minor: Color::rgb(0x3A3A3C),
        road_major: Color::rgb(0x636366),
        road_casing: Color::rgb(0x111113),
        poi: Color::rgb(0x8E8E93),
        route: Color::rgb(0x0A84FF),
        route_casing: Color::rgb(0x0B2A4D),
        user: Color::rgb(0x0A84FF),
        user_ring: Color::rgb(0xFFFFFF),
        pin: Color::rgb(0xFF453A),
    };

    pub const LIGHT: MapTheme = MapTheme {
        background: Color::rgb(0xF2F2F7),
        land: Color::rgb(0xF2F2F7),
        park: Color::rgb(0xDDE6DB),
        water: Color::rgb(0xBCD2E6),
        building: Color::rgb(0xE5E5EA),
        road_minor: Color::rgb(0xFFFFFF),
        road_major: Color::rgb(0xFFFFFF),
        road_casing: Color::rgb(0xC7C7CC),
        poi: Color::rgb(0x8E8E93),
        route: Color::rgb(0x007AFF),
        route_casing: Color::rgb(0xFFFFFF),
        user: Color::rgb(0x007AFF),
        user_ring: Color::rgb(0xFFFFFF),
        pin: Color::rgb(0xFF3B30),
    };
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum Role {
    Land,
    Park,
    Water,
    Building,
    RoadMinor,
    RoadMajor,
    Poi,
}

const CLASS_KEYS: [&str; 5] = ["class", "highway", "kind", "type", "fclass"];

const MAJOR_ROADS: [&str; 12] = [
    "motorway", "trunk", "primary", "secondary", "tertiary", "major", "highway", "motorway_link",
    "trunk_link", "primary_link", "secondary_link", "arterial",
];

const PARK_CLASSES: [&str; 8] =
    ["park", "forest", "wood", "grass", "garden", "nature_reserve", "meadow", "recreation_ground"];

/// Maps a layer (and optional class) to a drawing role. Layer names follow
/// both the Kotlin app's style (`roads`, `buildings`, ...) and the sample
/// archive builder (`transportation`, ...). Unknown layers return `None`.
pub fn classify(layer: &str, class: Option<&str>) -> Option<Role> {
    let name = layer.to_ascii_lowercase();
    match name.as_str() {
        "earth" | "land" => Some(Role::Land),
        "water" | "waterway" | "ocean" => Some(Role::Water),
        "park" | "parks" => Some(Role::Park),
        "landuse" | "landcover" => match class {
            Some(c) if PARK_CLASSES.contains(&c) => Some(Role::Park),
            _ => Some(Role::Land),
        },
        "building" | "buildings" => Some(Role::Building),
        "poi" | "pois" => Some(Role::Poi),
        "road" | "roads" | "transportation" | "transport" | "highway" => {
            Some(match class {
                Some(c) if MAJOR_ROADS.contains(&c) => Role::RoadMajor,
                _ => Role::RoadMinor,
            })
        }
        "roads_major" => Some(Role::RoadMajor),
        "roads_minor" => Some(Role::RoadMinor),
        _ => None,
    }
}

/// First non-empty class-like property of a feature.
pub fn class_of<'a>(
    layer: &'a crate::mvt::Layer,
    feature: &crate::mvt::Feature,
) -> Option<&'a str> {
    CLASS_KEYS.iter().find_map(|k| layer.prop_str(feature, k))
}

/// Stroke width in logical pixels for a road at `zoom`.
/// Scales roads smoothly, tapering down gracefully to hairline widths at low zoom levels.
pub fn road_width(major: bool, zoom: f64) -> f32 {
    let base = if major { 2.8 } else { 1.6 };
    let scale = 1.40f64.powf(zoom - 14.0).clamp(0.15, 5.0);
    (base * scale) as f32
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn classifies_known_layers() {
        assert_eq!(classify("roads", Some("primary")), Some(Role::RoadMajor));
        assert_eq!(classify("roads", Some("residential")), Some(Role::RoadMinor));
        assert_eq!(classify("transportation", None), Some(Role::RoadMinor));
        assert_eq!(classify("Water", None), Some(Role::Water));
        assert_eq!(classify("landuse", Some("forest")), Some(Role::Park));
        assert_eq!(classify("landuse", Some("industrial")), Some(Role::Land));
        assert_eq!(classify("buildings", None), Some(Role::Building));
        assert_eq!(classify("mystery", None), None);
    }

    #[test]
    fn road_width_grows_with_zoom_and_is_bounded() {
        assert!(road_width(true, 16.0) > road_width(true, 12.0));
        assert!(road_width(true, 14.0) > road_width(false, 14.0));
        assert!(road_width(false, 0.0) >= 1.6 * 0.15 - 1e-6);
        assert!(road_width(true, 30.0) <= 2.8 * 5.0 + 1e-6);
    }
}

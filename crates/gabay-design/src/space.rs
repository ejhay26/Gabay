//! Spacing, radii and hit-target tokens (logical pixels, 4-pt grid).

pub const S1: f32 = 4.0;
pub const S2: f32 = 8.0;
pub const S3: f32 = 12.0;
pub const S4: f32 = 16.0;
pub const S5: f32 = 20.0;
pub const S6: f32 = 24.0;
pub const S8: f32 = 32.0;

/// Continuous-corner radii. Large radii are fine here: glass surfaces are
/// capsules and rounded rects, unlike the old square HUD.
pub const RADIUS_CONTROL: f32 = 22.0;
pub const RADIUS_CARD: f32 = 20.0;
pub const RADIUS_SHEET: f32 = 34.0;

/// Minimum interactive size.
pub const HIT_MIN: f32 = 44.0;

/// iOS-style toggle geometry.
pub const SWITCH_W: f32 = 51.0;
pub const SWITCH_H: f32 = 31.0;
pub const SWITCH_THUMB: f32 = 27.0;
pub const SWITCH_PAD: f32 = 2.0;

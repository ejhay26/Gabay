//! Design tokens for Gabay. Pure data: no GUI, no GPU, no platform code.
//!
//! Everything visual that more than one crate needs (colours, spacing, motion,
//! glass presets) is defined here exactly once.

pub mod color;
pub mod glass;
pub mod motion;
pub mod space;
pub mod type_scale;

pub use color::{Palette, Rgba, Scheme};
pub use glass::{GlassSpec, GlassStyle};

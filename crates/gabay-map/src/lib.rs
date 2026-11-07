//! Map logic with no GPU and no windowing: camera maths, vector-tile decoding,
//! styling, and a flat draw list that the shell paints. Everything here is
//! unit-tested on the host.
//!
//! Not implemented: 3D tilt and building extrusion. The camera is a flat
//! (optionally rotated) Web-Mercator view.

pub mod geo;
pub mod loader;
pub mod mvt;
pub mod scene;
pub mod style;

pub use geo::{Bounds, Camera, LonLat, TileId};
pub use loader::VectorSource;
pub use scene::{build_scene, overlay_items, DrawItem, Overlays, TileData};
pub use style::{Color, MapTheme};

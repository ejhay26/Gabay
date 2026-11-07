//! Engine-facing domain types and startup plumbing. No UI, no GPU.
//!
//! - [`deferred`]: run slow loads off the UI thread; the UI only polls.
//! - [`admin`]: barangay / municipality / province lookup by point-in-polygon.
//! - [`poi`]: search result type and its human-readable place label.

pub mod admin;
pub mod deferred;
pub mod poi;

pub use admin::{Admin, AdminIndex, Area, Level};
pub use deferred::{Deferred, Status};
pub use poi::{Poi, SearchHit};

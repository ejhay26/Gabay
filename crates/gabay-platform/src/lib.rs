//! Platform glue. The rest of the app only sees the traits and types here;
//! JNI exists only inside `android` and only on `target_os = "android"`.

pub mod backend;
pub mod error;
pub mod flow;
pub mod permission;

#[cfg(target_os = "android")]
pub mod android;

pub use backend::{FakeBackend, PermissionBackend};
pub use error::PlatformError;
pub use flow::{PermissionFlow, Snapshot};
pub use permission::{Grant, Permission};

/// The backend for the current target: JNI on Android, an in-memory fake
/// (everything granted) elsewhere so the UI can run on a desktop for layout work.
pub fn default_backend() -> Box<dyn PermissionBackend> {
    #[cfg(target_os = "android")]
    {
        Box::new(android::AndroidBackend::new())
    }
    #[cfg(not(target_os = "android"))]
    {
        Box::new(FakeBackend::all_granted())
    }
}

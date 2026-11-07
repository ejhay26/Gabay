use std::collections::HashMap;
use std::sync::Mutex;

use crate::error::PlatformError;
use crate::permission::{Grant, Permission};

/// What the UI needs from the OS. Implementations must be cheap to call:
/// `status` is polled while a request is pending.
pub trait PermissionBackend: Send + Sync {
    fn status(&self, permission: Permission) -> Result<Grant, PlatformError>;

    /// Shows the system dialog, or opens the relevant Settings screen for
    /// permissions where `needs_settings_trip()` is true. Returns immediately;
    /// the outcome is observed by polling `status`.
    fn request(&self, permission: Permission) -> Result<(), PlatformError>;

    /// Opens this app's detail page in system Settings (for permanently
    /// denied permissions).
    fn open_app_settings(&self) -> Result<(), PlatformError>;
}

/// In-memory backend for desktop layout work and for tests.
pub struct FakeBackend {
    grants: Mutex<HashMap<Permission, Grant>>,
    /// Permissions that become granted as soon as they are requested.
    grant_on_request: bool,
}

impl FakeBackend {
    pub fn all_granted() -> Self {
        let grants = Permission::FLOW_ORDER.iter().map(|p| (*p, Grant::Granted)).collect();
        Self { grants: Mutex::new(grants), grant_on_request: true }
    }

    pub fn all_denied(grant_on_request: bool) -> Self {
        let grants = Permission::FLOW_ORDER.iter().map(|p| (*p, Grant::Denied)).collect();
        Self { grants: Mutex::new(grants), grant_on_request }
    }

    pub fn set(&self, permission: Permission, grant: Grant) {
        if let Ok(mut map) = self.grants.lock() {
            map.insert(permission, grant);
        }
    }
}

impl PermissionBackend for FakeBackend {
    fn status(&self, permission: Permission) -> Result<Grant, PlatformError> {
        let map = self.grants.lock().map_err(|_| PlatformError::Call("lock poisoned".into()))?;
        Ok(map.get(&permission).copied().unwrap_or(Grant::Denied))
    }

    fn request(&self, permission: Permission) -> Result<(), PlatformError> {
        if self.grant_on_request {
            self.set(permission, Grant::Granted);
        }
        Ok(())
    }

    fn open_app_settings(&self) -> Result<(), PlatformError> {
        Ok(())
    }
}

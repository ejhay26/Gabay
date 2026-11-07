//! First-launch permission state machine. Pure logic over a `PermissionBackend`.
//!
//! Rules:
//! * Ask every permission, in `Permission::FLOW_ORDER`, one at a time.
//! * A permission is asked at most once per flow. If it is still not granted
//!   afterwards (denied, or "don't ask again"), the flow moves on and the UI
//!   offers an "Open Settings" action at the end.
//! * Only `is_required()` permissions gate entry to the app.

use crate::backend::PermissionBackend;
use crate::error::PlatformError;
use crate::permission::{Grant, Permission};

#[derive(Clone, Debug, PartialEq, Eq)]
pub struct Snapshot {
    pub items: Vec<(Permission, Grant)>,
}

impl Snapshot {
    pub fn read(backend: &dyn PermissionBackend) -> Result<Self, PlatformError> {
        let mut items = Vec::with_capacity(Permission::FLOW_ORDER.len());
        for permission in Permission::FLOW_ORDER {
            items.push((permission, backend.status(permission)?));
        }
        Ok(Self { items })
    }

    pub fn grant(&self, permission: Permission) -> Grant {
        self.items
            .iter()
            .find(|(p, _)| *p == permission)
            .map(|(_, g)| *g)
            .unwrap_or(Grant::Denied)
    }

    /// True when every required permission is satisfied.
    pub fn required_satisfied(&self) -> bool {
        self.items.iter().all(|(p, g)| !p.is_required() || g.is_satisfied())
    }

    /// Permissions the user ended up without, in flow order.
    pub fn missing(&self) -> Vec<Permission> {
        self.items.iter().filter(|(_, g)| !g.is_satisfied()).map(|(p, _)| *p).collect()
    }
}

#[derive(Clone, Debug, Default)]
pub struct PermissionFlow {
    attempted: Vec<Permission>,
}

impl PermissionFlow {
    pub fn new() -> Self {
        Self::default()
    }

    /// The next permission to ask, or `None` when the flow is finished.
    pub fn next_step(&self, snapshot: &Snapshot) -> Option<Permission> {
        Permission::FLOW_ORDER.iter().copied().find(|p| {
            if self.attempted.contains(p) || snapshot.grant(*p).is_satisfied() {
                return false;
            }
            // Background location may only be asked once foreground is granted.
            if *p == Permission::BackgroundLocation {
                return snapshot.grant(Permission::Location).is_satisfied();
            }
            true
        })
    }

    pub fn mark_attempted(&mut self, permission: Permission) {
        if !self.attempted.contains(&permission) {
            self.attempted.push(permission);
        }
    }

    pub fn was_attempted(&self, permission: Permission) -> bool {
        self.attempted.contains(&permission)
    }

    /// 1-based position of `permission` for "Step n of m" captions.
    pub fn position(permission: Permission) -> (usize, usize) {
        let idx = Permission::FLOW_ORDER.iter().position(|p| *p == permission).unwrap_or(0);
        (idx + 1, Permission::FLOW_ORDER.len())
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::backend::FakeBackend;

    #[test]
    fn asks_in_order_then_finishes() {
        let backend = FakeBackend::all_denied(true);
        let mut flow = PermissionFlow::new();
        let mut asked = Vec::new();
        for _ in 0..10 {
            let snap = Snapshot::read(&backend).unwrap();
            let Some(next) = flow.next_step(&snap) else { break };
            asked.push(next);
            flow.mark_attempted(next);
            backend.request(next).unwrap();
        }
        assert_eq!(asked, Permission::FLOW_ORDER.to_vec());
        assert!(Snapshot::read(&backend).unwrap().required_satisfied());
    }

    #[test]
    fn denied_permission_is_not_asked_twice() {
        let backend = FakeBackend::all_denied(false);
        let mut flow = PermissionFlow::new();
        let snap = Snapshot::read(&backend).unwrap();
        assert_eq!(flow.next_step(&snap), Some(Permission::Location));
        flow.mark_attempted(Permission::Location);
        let snap = Snapshot::read(&backend).unwrap();
        assert_eq!(flow.next_step(&snap), Some(Permission::Microphone));
        assert!(!snap.required_satisfied());
    }

    #[test]
    fn background_location_waits_for_foreground() {
        let backend = FakeBackend::all_denied(false);
        let mut flow = PermissionFlow::new();
        for p in [Permission::Location, Permission::Microphone, Permission::Notifications, Permission::VaultStorage] {
            flow.mark_attempted(p);
        }
        let snap = Snapshot::read(&backend).unwrap();
        assert_eq!(flow.next_step(&snap), None);
        backend.set(Permission::Location, Grant::Granted);
        let snap = Snapshot::read(&backend).unwrap();
        assert_eq!(flow.next_step(&snap), Some(Permission::BackgroundLocation));
    }

    #[test]
    fn not_applicable_counts_as_satisfied() {
        let backend = FakeBackend::all_denied(false);
        backend.set(Permission::Notifications, Grant::NotApplicable);
        let snap = Snapshot::read(&backend).unwrap();
        assert!(!snap.missing().contains(&Permission::Notifications));
    }
}

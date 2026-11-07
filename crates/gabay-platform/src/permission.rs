/// Every permission Gabay asks for, in the order the first-launch flow asks.
#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash)]
pub enum Permission {
    /// ACCESS_FINE_LOCATION (+ coarse).
    Location,
    /// RECORD_AUDIO, for the voice assistant.
    Microphone,
    /// POST_NOTIFICATIONS (Android 13+).
    Notifications,
    /// All-files access, so `/sdcard/Documents/Gabay/` is readable.
    VaultStorage,
    /// ACCESS_BACKGROUND_LOCATION; must be asked after `Location`.
    BackgroundLocation,
}

impl Permission {
    /// Order used by the first-launch flow. In-app dialogs come first; the two
    /// Settings round-trips come last so the user is never thrown out of the
    /// app before they have seen every other prompt.
    pub const FLOW_ORDER: [Permission; 5] = [
        Permission::Location,
        Permission::Microphone,
        Permission::Notifications,
        Permission::VaultStorage,
        Permission::BackgroundLocation,
    ];

    /// The app cannot do its job without these.
    pub const fn is_required(self) -> bool {
        matches!(self, Permission::Location | Permission::VaultStorage)
    }

    /// True when granting means leaving the app for a Settings screen.
    pub const fn needs_settings_trip(self) -> bool {
        matches!(self, Permission::VaultStorage | Permission::BackgroundLocation)
    }
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum Grant {
    Granted,
    Denied,
    /// Does not exist on this OS version (e.g. notifications below Android 13).
    NotApplicable,
}

impl Grant {
    /// `NotApplicable` counts as satisfied: there is nothing left to ask.
    pub const fn is_satisfied(self) -> bool {
        matches!(self, Grant::Granted | Grant::NotApplicable)
    }
}

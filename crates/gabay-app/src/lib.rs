//! Entry point. Launch does no file I/O beyond what the first frame needs:
//! the map opens its archive on the first paint (header only), and search,
//! routing and model load on background threads.

use std::any::Any;
use std::path::PathBuf;
use std::sync::{Arc, Mutex};

use dioxus::prelude::*;
use gabay_assistant::AssistantGatekeeper;
use gabay_engine::{Deferred, SearchHit, Status};
use gabay_map::LonLat;
use gabay_routing::{RouteOptions, Router};
use gabay_search::PoiDb;
use gabay_tracker::Tracker;
use gabay_ui::{App, Env, RouteInfo, Services, Telemetry};

/// Live search, routing, telemetry and assistant loaded asynchronously.
struct Backend {
    search: Deferred<PoiDb>,
    router: Deferred<Router>,
    tracker: Mutex<Tracker>,
    assistant: Mutex<AssistantGatekeeper>,
    current_pos: Mutex<Option<LonLat>>,
}

impl Backend {
    fn new(vault: PathBuf) -> Self {
        let search_path = vault.join("pois_fts.sqlite");
        let search = Deferred::spawn("search", move || PoiDb::open(&search_path).map_err(|e| e.to_string()));

        let router_path = vault.join("ph.gabay");
        let router = Deferred::spawn("router", move || Router::open(&router_path).map_err(|e| e.to_string()));

        Self {
            search,
            router,
            tracker: Mutex::new(Tracker::new()),
            assistant: Mutex::new(AssistantGatekeeper::new()),
            current_pos: Mutex::new(None),
        }
    }
}

impl Services for Backend {
    fn search_status(&self) -> Status {
        self.search.status()
    }

    fn search(&self, query: &str, near: Option<LonLat>) -> Vec<SearchHit> {
        let Some(db) = self.search.get() else { return Vec::new() };
        match db.search(query, near.map(|p| (p.lat, p.lon)), 20) {
            Ok(v) => v,
            Err(e) => {
                log::error!("search failed: {e}");
                Vec::new()
            }
        }
    }

    fn route(&self, to: LonLat) -> Result<RouteInfo, String> {
        let router = match self.router.get() {
            Some(r) => r,
            None => match self.router.status() {
                Status::Loading => return Err("Road graph is still loading. Please wait a moment.".into()),
                Status::Failed(e) => return Err(format!("Road graph unavailable: {e}")),
                Status::Ready => unreachable!(),
            },
        };

        // Use last tracked position or fall back to center of Manila if GPS not yet established
        let start = self.current_pos.lock().ok().and_then(|p| *p)
            .unwrap_or_else(|| LonLat::new(120.98, 14.60));

        let plan = router.calculate_route(start, to, RouteOptions::default())
            .map_err(|e| e.to_string())?;

        if let Ok(mut trk) = self.tracker.lock() {
            trk.set_route(plan.clone());
        }

        Ok(RouteInfo {
            points: plan.points,
            distance_m: plan.total_distance_meters,
            duration_s: plan.estimated_duration_seconds,
        })
    }

    fn telemetry(&self) -> Option<Telemetry> {
        let trk = self.tracker.lock().ok()?;
        let progress = trk.last_progress()?;
        let pos = trk.last_position().unwrap_or_else(|| LonLat::new(120.98, 14.60));
        Some(Telemetry {
            speed_kmh: progress.speed_kmh,
            heading_deg: progress.heading_degrees,
            position: pos,
        })
    }

    fn ask(&self, text: &str) -> Result<String, String> {
        let progress = self.tracker.lock().ok().and_then(|t| t.last_progress().cloned());
        let mut ast = self.assistant.lock().map_err(|_| "Assistant state poisoned".to_string())?;
        Ok(ast.ask(text, progress.as_ref()))
    }
}

fn vault_dir() -> PathBuf {
    if cfg!(target_os = "android") {
        PathBuf::from("/sdcard/Documents/Gabay")
    } else {
        std::env::var_os("GABAY_VAULT").map(PathBuf::from).unwrap_or_else(|| PathBuf::from("vault"))
    }
}

fn root() -> Element {
    use_context_provider(|| Env {
        services: Arc::new(Backend::new(vault_dir())),
        permissions: Arc::from(gabay_platform::default_backend()),
        vault_dir: vault_dir(),
    });
    rsx! { App {} }
}

pub fn run() {
    let config: Vec<Box<dyn Any>> = Vec::new();
    dioxus_native::launch_cfg(root, Vec::new(), config);
}

#[cfg(target_os = "android")]
#[no_mangle]
fn android_main(app: android_activity::AndroidApp) {
    android_logger::init_once(android_logger::Config::default().with_max_level(log::LevelFilter::Info));
    blitz_shell::set_android_app(app);
    run();
}

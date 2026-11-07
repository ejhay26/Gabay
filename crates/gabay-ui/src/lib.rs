//! Screens. All slow work (search, routing, assistant) runs off the UI task.
//!
//! Not implemented: satellite and topography layers, route re-planning while
//! driving, turn-by-turn instructions.

mod layout;

use std::sync::{mpsc::Sender, Arc};
use std::time::Duration;

use dioxus::prelude::*;
use dioxus_native::use_wgpu;
use gabay_engine::poi::format_distance;
use gabay_engine::{SearchHit, Status};
use gabay_map::{LonLat, Overlays};
use gabay_platform::{Permission, PermissionBackend, PermissionFlow, Snapshot};
use gabay_shell::{MapMsg, MapPaintSource};

const CSS: &str = include_str!("style.css");
const MAX_TEXT_CHARS: usize = 120;

#[derive(Clone, Debug, PartialEq)]
pub struct RouteInfo {
    pub points: Vec<LonLat>,
    pub distance_m: f64,
    pub duration_s: f64,
}

#[derive(Clone, Copy, Debug, PartialEq)]
pub struct Telemetry {
    pub speed_kmh: f32,
    pub heading_deg: f32,
    pub position: LonLat,
}

/// What the UI needs from the engine. Methods run on blocking threads.
pub trait Services: Send + Sync + 'static {
    fn search_status(&self) -> Status;
    fn search(&self, query: &str, near: Option<LonLat>) -> Vec<SearchHit>;
    fn route(&self, to: LonLat) -> Result<RouteInfo, String>;
    fn telemetry(&self) -> Option<Telemetry>;
    fn ask(&self, text: &str) -> Result<String, String>;
}

/// Provided as root context by the app crate.
#[derive(Clone)]
pub struct Env {
    pub services: Arc<dyn Services>,
    pub permissions: Arc<dyn PermissionBackend>,
    pub vault_dir: std::path::PathBuf,
}

fn clean(q: &str) -> String {
    q.trim().chars().take(MAX_TEXT_CHARS).collect()
}

/// "9 min", "1 h 05 min"; `None` for NaN or negative input.
pub fn format_duration(secs: f64) -> Option<String> {
    if !secs.is_finite() || secs < 0.0 {
        return None;
    }
    let m = (secs / 60.0).round() as u64;
    Some(if m < 60 { format!("{} min", m.max(1)) } else { format!("{} h {:02} min", m / 60, m % 60) })
}

fn send(tx: &Sender<MapMsg>, m: MapMsg) {
    // A closed channel means the painter is gone (window closing); ignore.
    let _ = tx.send(m);
}

fn describe(p: Permission) -> (&'static str, &'static str) {
    match p {
        Permission::Location => ("Location", "Shows where you are and starts routes from there."),
        Permission::Microphone => ("Microphone", "Lets you speak to the assistant."),
        Permission::Notifications => ("Notifications", "Shows route status while the screen is off."),
        Permission::VaultStorage => ("File access", "Reads the map, search and model files in Documents/Gabay."),
        Permission::BackgroundLocation => ("Background location", "Keeps guidance running when the app is not in front."),
    }
}

async fn async_sleep(d: std::time::Duration) {
    let (tx, rx) = futures_channel::oneshot::channel();
    std::thread::Builder::new()
        .name("gabay-sleep".into())
        .spawn(move || {
            std::thread::sleep(d);
            let _ = tx.send(());
        })
        .ok();
    let _ = rx.await;
}

async fn spawn_blocking<F, T>(f: F) -> Option<T>
where
    F: FnOnce() -> T + Send + 'static,
    T: Send + 'static,
{
    let (tx, rx) = futures_channel::oneshot::channel();
    std::thread::Builder::new()
        .name("gabay-blocking".into())
        .spawn(move || {
            let res = f();
            let _ = tx.send(res);
        })
        .ok();
    rx.await.ok()
}

#[component]
pub fn App() -> Element {
    let env = use_context::<Env>();
    let backend = env.permissions.clone();
    let mut snap = use_signal({
        let b = backend.clone();
        move || Snapshot::read(&*b).ok()
    });
    let mut flow = use_signal(PermissionFlow::new);

    use_future({
        let b = backend.clone();
        move || {
            let b = b.clone();
            async move {
                loop {
                    async_sleep(Duration::from_millis(500)).await;
                    match Snapshot::read(&*b) {
                        Ok(s) => {
                            let done = s.required_satisfied();
                            snap.set(Some(s));
                            if done {
                                break;
                            }
                        }
                        Err(_) => break,
                    }
                }
            }
        }
    });

    // A failed status read never blocks the app.
    let step = snap().and_then(|s| if s.required_satisfied() { None } else { flow.read().next_step(&s) });

    rsx! {
        style { {CSS} }
        if let Some(p) = step {
            {
                let (title, body) = describe(p);
                let (n, total) = PermissionFlow::position(p);
                let retry = flow.read().was_attempted(p);
                let b = backend.clone();
                rsx! {
                    div { class: "gate-container",
                        div { class: "gate-card",
                            div { class: "gate-step", "Step {n} of {total}" }
                            h1 { "{title}" }
                            p { "{body}" }
                            button {
                                class: "gate-btn",
                                onclick: move |_| {
                                    flow.write().mark_attempted(p);
                                    let _ = if retry || p.needs_settings_trip() { b.open_app_settings() } else { b.request(p) };
                                },
                                if retry { "Open settings" } else { "Allow" }
                            }
                        }
                    }
                }
            }
        } else {
            MapScreen {}
        }
    }
}

#[derive(Clone, Copy, PartialEq)]
enum Sheet {
    Closed,
    Route,
    Ask,
}

#[component]
fn MapScreen() -> Element {
    let env = use_context::<Env>();
    let source = MapPaintSource::new(env.vault_dir.clone());
    let tx = use_hook(|| source.sender());
    let paint_id = use_wgpu(move || source);

    let mut query = use_signal(String::new);
    let mut selected = use_signal(|| None::<SearchHit>);
    let mut drag = use_signal(|| None::<(f64, f64)>);
    let mut sheet = use_signal(|| Sheet::Closed);
    let mut route = use_signal(|| None::<RouteInfo>);
    let mut note = use_signal(|| None::<String>);
    let mut busy = use_signal(|| false);
    let mut navigating = use_signal(|| false);
    let mut tele = use_signal(|| None::<Telemetry>);
    let mut dark = use_signal(|| true);
    let mut ask_text = use_signal(String::new);
    let mut reply = use_signal(|| None::<String>);

    let svc = env.services.clone();
    let hits = use_resource({
        let svc = svc.clone();
        move || {
            let q = clean(&query());
            let svc = svc.clone();
            async move {
                if q.chars().count() < 2 {
                    return Vec::new();
                }
                spawn_blocking(move || svc.search(&q, None)).await.unwrap_or_default()
            }
        }
    });
    let list: Vec<SearchHit> = hits.read().clone().unwrap_or_default();
    let showing_results = !list.is_empty() && selected().is_none();

    // Telemetry while navigating.
    use_future({
        let svc = svc.clone();
        move || {
            let svc = svc.clone();
            async move {
                loop {
                    async_sleep(Duration::from_secs(1)).await;
                    if navigating() {
                        let s = svc.clone();
                        let t = spawn_blocking(move || s.telemetry()).await.flatten();
                        tele.set(t);
                    }
                }
            }
        }
    });

    // Keep the GPU glass in step with what the DOM shows.
    use_effect({
        let tx = tx.clone();
        move || {
            let rows = if showing_results { list_len(&hits) } else { 0 };
            send(&tx, MapMsg::Panels(layout::panels(rows, sheet() != Sheet::Closed, navigating() && tele().is_some())));
        }
    });
    use_effect({
        let tx = tx.clone();
        move || send(&tx, MapMsg::Dark(dark()))
    });

    let (tx_a, tx_b, tx_c, tx_d, tx_e, tx_f) = (tx.clone(), tx.clone(), tx.clone(), tx.clone(), tx.clone(), tx.clone());
    let sel = selected();
    let rt = route();
    let hud = tele();
    let tool_bottom = layout::tools(sheet() != Sheet::Closed).css();

    rsx! {
        div { class: "root",
            div { class: "map",
                onmousedown: move |e| { let p = e.client_coordinates(); drag.set(Some((p.x, p.y))); },
                onmouseup: move |_| drag.set(None),
                onmousemove: move |e| {
                    let p = e.client_coordinates();
                    if let Some((x, y)) = drag() {
                        send(&tx_a, MapMsg::Pan { dx: p.x - x, dy: p.y - y });
                        drag.set(Some((p.x, p.y)));
                    }
                },
                onwheel: move |e| {
                    let p = e.client_coordinates();
                    let dy = e.delta().strip_units().y;
                    if dy.is_finite() {
                        send(&tx_b, MapMsg::ZoomAbout { x: p.x, y: p.y, delta: (-dy / 240.0).clamp(-1.0, 1.0) });
                    }
                },
                canvas { "src": paint_id }
            }

            div { class: "panel search", style: "{layout::SEARCH.css()}",
                input {
                    r#type: "text",
                    placeholder: "Search places",
                    value: "{query}",
                    maxlength: "{MAX_TEXT_CHARS}",
                    oninput: move |e| { selected.set(None); query.set(e.value()); },
                }
            }

            if showing_results {
                div { class: "panel results", style: "{layout::results(list.len()).css()}",
                    for hit in list.iter().take(layout::MAX_ROWS).cloned() {
                        {
                            let h = hit.clone();
                            let tx = tx_c.clone();
                            let sub = hit.poi.subtitle();
                            let dist = hit.distance_m.and_then(format_distance);
                            let line = match (sub.is_empty(), dist) {
                                (_, None) => sub,
                                (true, Some(d)) => d,
                                (false, Some(d)) => format!("{sub} \u{b7} {d}"),
                            };
                            rsx! {
                                button { class: "row", key: "{hit.poi.id}",
                                    onclick: move |_| {
                                        let at = LonLat::new(h.poi.lon, h.poi.lat);
                                        send(&tx, MapMsg::Center(at, 16.0));
                                        send(&tx, MapMsg::Overlays(Overlays { pin: Some(at), ..Overlays::default() }));
                                        route.set(None);
                                        note.set(None);
                                        selected.set(Some(h.clone()));
                                        sheet.set(Sheet::Route);
                                    },
                                    div { class: "name", "{hit.poi.name}" }
                                    div { class: "sub", "{line}" }
                                }
                            }
                        }
                    }
                }
            } else if query().trim().chars().count() >= 2 && sel.is_none() && hits.read().is_some() {
                div { class: "panel results note", style: "{layout::results(1).css()}",
                    match svc.search_status() {
                        Status::Loading => "Search index is loading.",
                        Status::Failed(_) => "Search is unavailable. Check the vault files.",
                        Status::Ready => "No matches.",
                    }
                }
            }

            if !showing_results && navigating() {
                if let Some(t) = hud {
                    div { class: "panel hud", style: "{layout::HUD.css()}",
                        b { "{t.speed_kmh.round() as i32}" }
                        div { class: "step", "km/h" }
                    }
                }
            }

            div { class: "panel tools", style: "{tool_bottom}",
                button { onclick: move |_| send(&tx_d, MapMsg::ZoomAbout { x: -1.0, y: -1.0, delta: 1.0 }), "+" }
                button { onclick: move |_| send(&tx_e, MapMsg::ZoomAbout { x: -1.0, y: -1.0, delta: -1.0 }), "\u{2212}" }
                button { onclick: move |_| dark.set(!dark()), if dark() { "Light" } else { "Dark" } }
            }

            if sheet() != Sheet::Closed {
                div { class: "panel sheet", style: "{layout::SHEET.css()}",
                    div { class: "tabs",
                        button { class: if sheet() == Sheet::Route { "on" } else { "" }, onclick: move |_| sheet.set(Sheet::Route), "Route" }
                        button { class: if sheet() == Sheet::Ask { "on" } else { "" }, onclick: move |_| sheet.set(Sheet::Ask), "Assistant" }
                        button { onclick: move |_| { sheet.set(Sheet::Closed); navigating.set(false); }, "Close" }
                    }
                    if sheet() == Sheet::Route {
                        if let Some(h) = sel.clone() {
                            div { class: "name", "{h.poi.name}" }
                            div { class: "sub", "{h.poi.subtitle()}" }
                            if let Some(r) = rt.clone() {
                                div { class: "facts",
                                    div { b { "{format_distance(r.distance_m).unwrap_or_default()}" } span { "distance" } }
                                    div { b { "{format_duration(r.duration_s).unwrap_or_default()}" } span { "time" } }
                                }
                                button { class: "act", onclick: move |_| navigating.set(!navigating()),
                                    if navigating() { "Stop" } else { "Start" }
                                }
                            } else {
                                button { class: if busy() { "act off" } else { "act" }, disabled: busy(),
                                    onclick: {
                                        let svc = svc.clone();
                                        let tx = tx_f.clone();
                                        let h = h.clone();
                                        move |_| {
                                            busy.set(true);
                                            note.set(None);
                                            let svc = svc.clone();
                                            let tx = tx.clone();
                                            let to = LonLat::new(h.poi.lon, h.poi.lat);
                                            spawn(async move {
                                                let out = spawn_blocking(move || svc.route(to)).await;
                                                match out {
                                                    Some(Ok(r)) => {
                                                        send(&tx, MapMsg::Overlays(Overlays { route: r.points.clone(), pin: Some(to), ..Overlays::default() }));
                                                        route.set(Some(r));
                                                    }
                                                    Some(Err(e)) => note.set(Some(e)),
                                                    None => note.set(Some("Routing stopped unexpectedly.".into())),
                                                }
                                                busy.set(false);
                                            });
                                        }
                                    },
                                    if busy() { "Finding route" } else { "Get route" }
                                }
                            }
                            if let Some(n) = note() { div { class: "sub", "{n}" } }
                        } else {
                            div { class: "sub", "Pick a place to plan a route." }
                        }
                    } else {
                        input { class: "ask", r#type: "text", placeholder: "Ask about the way, the road or the area", maxlength: "{MAX_TEXT_CHARS}",
                            value: "{ask_text}",
                            oninput: move |e| ask_text.set(e.value()),
                            onkeydown: {
                                let svc = svc.clone();
                                move |e: KeyboardEvent| {
                                    if e.key() != Key::Enter { return; }
                                    let q = clean(&ask_text());
                                    if q.is_empty() || busy() { return; }
                                    busy.set(true);
                                    let svc = svc.clone();
                                    spawn(async move {
                                        let out = spawn_blocking(move || svc.ask(&q)).await;
                                        reply.set(Some(match out {
                                            Some(Ok(t)) => t,
                                            Some(Err(e)) => e,
                                            None => "The assistant stopped unexpectedly.".into(),
                                        }));
                                        busy.set(false);
                                    });
                                }
                            },
                        }
                        if busy() { div { class: "reply", "Thinking" } }
                        else if let Some(t) = reply() { div { class: "reply", "{t}" } }
                    }
                }
            }
        }
    }
}

fn list_len(hits: &Resource<Vec<SearchHit>>) -> usize {
    hits.read().as_ref().map_or(0, |v| v.len())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn text_is_trimmed_and_bounded() {
        assert_eq!(clean("  abc "), "abc");
        assert_eq!(clean(&"ñ".repeat(500)).chars().count(), 120);
    }

    #[test]
    fn durations() {
        assert_eq!(format_duration(20.0).as_deref(), Some("1 min"));
        assert_eq!(format_duration(540.0).as_deref(), Some("9 min"));
        assert_eq!(format_duration(3900.0).as_deref(), Some("1 h 05 min"));
        assert_eq!(format_duration(f64::NAN), None);
        assert_eq!(format_duration(-5.0), None);
    }
}

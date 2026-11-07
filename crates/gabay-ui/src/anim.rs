//! Spring-driven values for the UI. One frame ticker runs only while a value
//! is moving, so an idle screen costs nothing.

use std::time::{Duration, Instant};

use dioxus::prelude::*;
use futures_util::StreamExt;
use gabay_design::motion::Spring;

use crate::async_sleep;

#[derive(Clone, Copy)]
pub struct Anim {
    value: Signal<f32>,
    spring: Signal<Spring>,
    running: Signal<bool>,
}

pub fn use_anim(initial: f32, preset: fn(f32) -> Spring) -> Anim {
    Anim { value: use_signal(|| initial), spring: use_signal(|| preset(initial)), running: use_signal(|| false) }
}

/// ~60 Hz ticks from a helper thread; `None` if the thread cannot start.
fn ticker() -> Option<futures_channel::mpsc::UnboundedReceiver<()>> {
    let (tx, rx) = futures_channel::mpsc::unbounded();
    std::thread::Builder::new()
        .name("gabay-frame".into())
        .spawn(move || loop {
            std::thread::sleep(Duration::from_millis(16));
            if tx.unbounded_send(()).is_err() {
                break;
            }
        })
        .ok()?;
    Some(rx)
}

impl Anim {
    /// Current value; reading it re-renders the component on every frame.
    pub fn value(&self) -> f32 {
        (self.value)()
    }

    /// Value clamped to 0..=1 for opacity and progress uses.
    pub fn unit(&self) -> f32 {
        self.value().clamp(0.0, 1.0)
    }

    /// Moves toward `target`; safe to call mid-flight (velocity is kept).
    pub fn go(self, target: f32) {
        if !target.is_finite() {
            return;
        }
        let (mut value, mut spring, mut running) = (self.value, self.spring, self.running);
        spring.write().target = target;
        if *running.peek() {
            return;
        }
        let Some(mut frames) = ticker() else {
            value.set(target); // no timer: jump, never stall
            return;
        };
        running.set(true);
        spawn(async move {
            let mut last = Instant::now();
            while frames.next().await.is_some() {
                let now = Instant::now();
                let dt = now.duration_since(last).as_secs_f32();
                last = now;
                let (v, settled) = {
                    let mut s = spring.write();
                    s.step(dt);
                    if s.is_settled() {
                        s.value = s.target;
                        s.velocity = 0.0;
                    }
                    (s.value, s.is_settled())
                };
                value.set(v);
                if settled {
                    break;
                }
            }
            running.set(false);
        });
    }
}

/// Runs `f` after `d` without blocking the UI.
pub fn after(d: Duration, f: impl FnOnce() + 'static) {
    spawn(async move {
        async_sleep(d).await;
        f();
    });
}

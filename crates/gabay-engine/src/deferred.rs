//! A value computed on a background thread. `poll` never blocks, so the first
//! frame never waits on the road graph, POI database or language model.

use std::panic::{catch_unwind, AssertUnwindSafe};
use std::sync::{Arc, Mutex};
use std::thread;

#[derive(Clone, Debug, PartialEq, Eq)]
pub enum Status {
    Loading,
    Ready,
    Failed(String),
}

enum State<T> {
    Loading,
    Ready(Arc<T>),
    Failed(String),
}

pub struct Deferred<T> {
    state: Arc<Mutex<State<T>>>,
}

impl<T> Clone for Deferred<T> {
    fn clone(&self) -> Self {
        Self { state: Arc::clone(&self.state) }
    }
}

impl<T: Send + Sync + 'static> Deferred<T> {
    /// Starts `job` on a named thread. Panics inside `job` become `Failed`.
    pub fn spawn<F>(name: &str, job: F) -> Self
    where
        F: FnOnce() -> Result<T, String> + Send + 'static,
    {
        let state = Arc::new(Mutex::new(State::Loading));
        let slot = Arc::clone(&state);
        let label = name.to_owned();
        let spawned = thread::Builder::new().name(format!("gabay-{name}")).spawn(move || {
            let outcome = match catch_unwind(AssertUnwindSafe(job)) {
                Ok(Ok(v)) => State::Ready(Arc::new(v)),
                Ok(Err(e)) => State::Failed(e),
                Err(_) => State::Failed(format!("{label} panicked")),
            };
            if let Ok(mut s) = slot.lock() {
                *s = outcome;
            }
        });
        if let Err(e) = spawned {
            if let Ok(mut s) = state.lock() {
                *s = State::Failed(format!("{name}: cannot start thread: {e}"));
            }
        }
        Self { state }
    }

    /// Non-blocking. `Some` once loading has succeeded.
    pub fn get(&self) -> Option<Arc<T>> {
        match &*self.state.lock().ok()? {
            State::Ready(v) => Some(Arc::clone(v)),
            _ => None,
        }
    }

    pub fn status(&self) -> Status {
        match self.state.lock() {
            Ok(s) => match &*s {
                State::Loading => Status::Loading,
                State::Ready(_) => Status::Ready,
                State::Failed(e) => Status::Failed(e.clone()),
            },
            Err(_) => Status::Failed("state poisoned".into()),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::sync::mpsc;
    use std::time::{Duration, Instant};

    fn wait<T: Send + Sync + 'static>(d: &Deferred<T>) -> Status {
        let end = Instant::now() + Duration::from_secs(5);
        while d.status() == Status::Loading && Instant::now() < end {
            thread::sleep(Duration::from_millis(2));
        }
        d.status()
    }

    #[test]
    fn spawn_returns_before_slow_job_finishes() {
        let (tx, rx) = mpsc::channel::<()>();
        let d = Deferred::spawn("slow", move || {
            rx.recv().map_err(|e| e.to_string())?;
            Ok(7u32)
        });
        assert_eq!(d.status(), Status::Loading);
        assert!(d.get().is_none());
        tx.send(()).unwrap();
        assert_eq!(wait(&d), Status::Ready);
        assert_eq!(*d.get().unwrap(), 7);
    }

    #[test]
    fn error_and_panic_become_failed() {
        let e = Deferred::<u8>::spawn("err", || Err("no file".into()));
        assert_eq!(wait(&e), Status::Failed("no file".into()));
        let p = Deferred::<u8>::spawn("boom", || panic!("x"));
        assert!(matches!(wait(&p), Status::Failed(m) if m.contains("panicked")));
        assert!(p.get().is_none());
    }

    #[test]
    fn clones_share_state() {
        let d = Deferred::spawn("c", || Ok(1u8));
        let d2 = d.clone();
        assert_eq!(wait(&d), Status::Ready);
        assert!(d2.get().is_some());
    }
}

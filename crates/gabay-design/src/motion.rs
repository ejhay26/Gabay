//! Easing curves and a damped spring. Pure math, frame-rate independent.

use std::f32::consts::PI;

/// Maps `t` from `[start, end]` to `0.0..=1.0`, clamped. A degenerate range
/// (`end <= start`) is treated as a step at `start`.
pub fn phase(t: f32, start: f32, end: f32) -> f32 {
    if end <= start {
        return if t >= start { 1.0 } else { 0.0 };
    }
    ((t - start) / (end - start)).clamp(0.0, 1.0)
}

pub fn out_cubic(x: f32) -> f32 {
    1.0 - (1.0 - x).powi(3)
}

pub fn in_cubic(x: f32) -> f32 {
    x * x * x
}

pub fn out_back(x: f32) -> f32 {
    const C1: f32 = 1.70158;
    const C3: f32 = C1 + 1.0;
    1.0 + C3 * (x - 1.0).powi(3) + C1 * (x - 1.0).powi(2)
}

/// Half-sine pulse over `[0, width]`; zero outside.
pub fn pulse(dt: f32, width: f32) -> f32 {
    if width > 0.0 && (0.0..=width).contains(&dt) {
        (PI * dt / width).sin()
    } else {
        0.0
    }
}

pub fn lerp(a: f32, b: f32, t: f32) -> f32 {
    a + (b - a) * t
}

/// Damped spring toward `target`. Semi-implicit Euler, sub-stepped so large
/// frame gaps cannot blow it up.
#[derive(Clone, Copy, Debug)]
pub struct Spring {
    pub value: f32,
    pub velocity: f32,
    pub target: f32,
    pub stiffness: f32,
    pub damping_ratio: f32,
}

impl Spring {
    /// Matches the feel of the iOS toggle thumb (stiffness 450, ratio ~0.8).
    pub const fn snappy(value: f32) -> Self {
        Self { value, velocity: 0.0, target: value, stiffness: 450.0, damping_ratio: 0.8 }
    }

    /// Sheets sliding in: quick, one small overshoot.
    pub const fn sheet(value: f32) -> Self {
        Self { value, velocity: 0.0, target: value, stiffness: 320.0, damping_ratio: 0.86 }
    }

    /// Shape-to-shape morphs (search pill to full screen): softer, a visible
    /// wobble at the end, which is what makes the surface feel liquid.
    pub const fn morph(value: f32) -> Self {
        Self { value, velocity: 0.0, target: value, stiffness: 230.0, damping_ratio: 0.74 }
    }

    pub fn step(&mut self, dt_seconds: f32) {
        const MAX_SUBSTEP: f32 = 1.0 / 240.0;
        let mut remaining = dt_seconds.clamp(0.0, 0.1);
        let damping = 2.0 * self.damping_ratio * self.stiffness.sqrt();
        while remaining > 0.0 {
            let h = remaining.min(MAX_SUBSTEP);
            let accel = self.stiffness * (self.target - self.value) - damping * self.velocity;
            self.velocity += accel * h;
            self.value += self.velocity * h;
            remaining -= h;
        }
    }

    pub fn is_settled(&self) -> bool {
        (self.target - self.value).abs() < 1e-3 && self.velocity.abs() < 1e-3
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn phase_is_clamped_and_handles_empty_range() {
        assert_eq!(phase(-1.0, 0.0, 10.0), 0.0);
        assert_eq!(phase(20.0, 0.0, 10.0), 1.0);
        assert_eq!(phase(5.0, 5.0, 5.0), 1.0);
    }

    #[test]
    fn out_back_ends_at_one() {
        assert!((out_back(1.0) - 1.0).abs() < 1e-6);
    }

    #[test]
    fn spring_converges() {
        let mut s = Spring::snappy(0.0);
        s.target = 1.0;
        for _ in 0..240 {
            s.step(1.0 / 60.0);
        }
        assert!(s.is_settled());
    }
}

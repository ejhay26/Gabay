//! Liquid-glass shader and its uniform block. No GPU API is used here: the
//! shell owns the wgpu device and uploads [`GlassUniform::to_bytes`].
//!
//! [`Panel`] describes a glass rectangle once; the shell resolves it to pixels
//! for the GPU and the UI turns it into CSS, so both always agree.
//!
//! Not implemented: drop shadow (draw it separately), multi-surface batching.

/// WGSL source. Entry points: `vs_main`, `fs_main`.
/// Bindings: 0 = backdrop texture, 1 = sampler, 2 = [`GlassUniform`].
pub const SHADER: &str = include_str!("glass.wgsl");

/// Size of the uniform block in bytes (six `vec4<f32>`).
pub const UNIFORM_BYTES: usize = 96;

/// Tuning independent of any design-token crate; the shell converts into it.
#[derive(Clone, Copy, Debug, PartialEq)]
pub struct Params {
    pub blur_radius: f32,
    pub refraction_height: f32,
    pub refraction_amount: f32,
    pub saturation: f32,
    pub dispersion: f32,
    pub rim: f32,
}

#[derive(Clone, Copy, Debug, PartialEq)]
pub struct GlassUniform {
    /// x, y, width, height in physical pixels.
    pub rect: [f32; 4],
    /// Corner radii: top-left, top-right, bottom-right, bottom-left.
    pub radii: [f32; 4],
    pub lens: [f32; 4],
    pub wash: [f32; 4],
    pub rim: [f32; 4],
    pub view: [f32; 4],
}

fn finite(v: f32, fallback: f32) -> f32 {
    if v.is_finite() {
        v
    } else {
        fallback
    }
}

impl GlassUniform {
    /// Builds a uniform block. Every input is clamped into a range the shader
    /// handles, so bad inputs degrade to a plain panel instead of NaNs.
    /// `scale` converts logical pixels in `params` and `radius` to physical pixels.
    #[allow(clippy::too_many_arguments)]
    pub fn new(
        rect: [f32; 4],
        radius: f32,
        params: Params,
        scale: f32,
        wash_rgba: [f32; 4],
        rim_rgb: [f32; 3],
        viewport: [f32; 2],
        press: f32,
    ) -> Self {
        let scale = finite(scale, 1.0).clamp(0.5, 8.0);
        let w = finite(rect[2], 0.0).max(0.0);
        let h = finite(rect[3], 0.0).max(0.0);
        let r = (finite(radius, 0.0) * scale).clamp(0.0, w.min(h) * 0.5);
        Self {
            rect: [finite(rect[0], 0.0), finite(rect[1], 0.0), w, h],
            radii: [r; 4],
            lens: [
                (finite(params.refraction_height, 0.0) * scale).max(0.0),
                (finite(params.refraction_amount, 0.0) * scale).max(0.0),
                (finite(params.blur_radius, 0.0) * scale).clamp(0.0, 32.0 * scale),
                finite(params.saturation, 1.0).clamp(0.0, 3.0),
            ],
            wash: wash_rgba.map(|v| finite(v, 0.0).clamp(0.0, 1.0)),
            rim: [
                finite(rim_rgb[0], 1.0).clamp(0.0, 1.0),
                finite(rim_rgb[1], 1.0).clamp(0.0, 1.0),
                finite(rim_rgb[2], 1.0).clamp(0.0, 1.0),
                finite(params.rim, 0.0).clamp(0.0, 1.0),
            ],
            view: [
                finite(viewport[0], 1.0).max(1.0),
                finite(viewport[1], 1.0).max(1.0),
                finite(params.dispersion, 0.0).clamp(0.0, 1.0),
                finite(press, 0.0).clamp(0.0, 1.0),
            ],
        }
    }

    /// Little-endian bytes in shader layout.
    pub fn to_bytes(&self) -> [u8; UNIFORM_BYTES] {
        let mut out = [0u8; UNIFORM_BYTES];
        let all = [self.rect, self.radii, self.lens, self.wash, self.rim, self.view];
        for (i, v) in all.iter().flatten().enumerate() {
            out[i * 4..i * 4 + 4].copy_from_slice(&v.to_le_bytes());
        }
        out
    }
}

/// A rectangle anchored to the viewport edges, in logical pixels.
/// Horizontal: `left`+`right`, or one of them plus `width`. Vertical: `top`+`bottom`
/// (height is then derived), or one of them plus `height`.
#[derive(Clone, Copy, Debug, PartialEq)]
pub struct Panel {
    pub left: Option<f32>,
    pub right: Option<f32>,
    pub top: Option<f32>,
    pub bottom: Option<f32>,
    pub width: Option<f32>,
    pub height: f32,
}

impl Panel {
    /// `[x, y, w, h]` for a viewport, or `None` when under-specified or empty.
    pub fn resolve(&self, vw: f32, vh: f32) -> Option<[f32; 4]> {
        let (x, w) = match (self.left, self.right, self.width) {
            (Some(l), Some(r), _) => (l, vw - l - r),
            (Some(l), None, Some(w)) => (l, w),
            (None, Some(r), Some(w)) => (vw - r - w, w),
            _ => return None,
        };
        let (y, h) = match (self.top, self.bottom) {
            (Some(t), Some(b)) => (t, vh - t - b),
            (Some(t), None) => (t, self.height),
            (None, Some(b)) => (vh - b - self.height, self.height),
            _ => return None,
        };
        let r = [x, y, w, h];
        (r.iter().all(|v| v.is_finite()) && w > 0.0 && h > 0.0).then_some(r)
    }

    pub fn css(&self) -> String {
        let mut s = String::from("position:absolute;");
        for (k, v) in [("left", self.left), ("right", self.right), ("top", self.top), ("bottom", self.bottom), ("width", self.width)] {
            if let Some(v) = v {
                s += &format!("{k}:{v}px;");
            }
        }
        if !(self.top.is_some() && self.bottom.is_some()) {
            s += &format!("height:{}px;", self.height);
        }
        s
    }
}

/// Rust mirror of the shader's geometry, used by tests and by hit-testing.
pub mod cpu {
    /// Signed distance to a rounded rectangle centred at the origin.
    pub fn sd_rounded_rect(p: [f32; 2], half: [f32; 2], radius: f32) -> f32 {
        let q = [p[0].abs() - (half[0] - radius), p[1].abs() - (half[1] - radius)];
        let outside = (q[0].max(0.0).powi(2) + q[1].max(0.0).powi(2)).sqrt();
        outside + q[0].max(q[1]).min(0.0) - radius
    }

    /// Displacement magnitude at `inside` pixels from the edge.
    pub fn refraction(inside: f32, band: f32, amount: f32) -> f32 {
        if band <= 0.0 || inside >= band {
            return 0.0;
        }
        let t = 1.0 - inside / band;
        (1.0 - (1.0 - t * t).max(0.0).sqrt()) * amount
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    const PARAMS: Params = Params {
        blur_radius: 2.0,
        refraction_height: 12.0,
        refraction_amount: 24.0,
        saturation: 1.5,
        dispersion: 0.0,
        rim: 0.8,
    };

    #[test]
    fn shader_parses_and_validates() {
        let module = naga::front::wgsl::parse_str(SHADER).expect("WGSL parse");
        let mut validator = naga::valid::Validator::new(
            naga::valid::ValidationFlags::all(),
            naga::valid::Capabilities::all(),
        );
        validator.validate(&module).expect("WGSL validation");
        let names: Vec<_> = module.entry_points.iter().map(|e| e.name.as_str()).collect();
        assert!(names.contains(&"vs_main") && names.contains(&"fs_main"));
    }

    #[test]
    fn uniform_layout_is_96_bytes_and_ordered() {
        let u = GlassUniform::new(
            [10.0, 20.0, 300.0, 80.0],
            22.0,
            PARAMS,
            2.0,
            [0.1, 0.1, 0.1, 0.2],
            [1.0, 1.0, 1.0],
            [1080.0, 2400.0],
            0.5,
        );
        let b = u.to_bytes();
        assert_eq!(b.len(), 96);
        let f = |i: usize| f32::from_le_bytes([b[i * 4], b[i * 4 + 1], b[i * 4 + 2], b[i * 4 + 3]]);
        assert_eq!((f(0), f(1), f(2), f(3)), (10.0, 20.0, 300.0, 80.0));
        assert_eq!(f(4), 40.0_f32.min(40.0)); // radius 22*2 = 44, clamped to half of 80
        assert_eq!(f(8), 24.0); // refraction height 12 * scale 2
        assert_eq!(f(23), 0.5); // press
    }

    #[test]
    fn bad_inputs_become_finite() {
        let u = GlassUniform::new(
            [f32::NAN, f32::INFINITY, -5.0, f32::NAN],
            f32::NAN,
            Params { blur_radius: f32::NAN, refraction_height: -1.0, ..PARAMS },
            f32::NAN,
            [f32::NAN; 4],
            [f32::NAN; 3],
            [0.0, f32::NAN],
            f32::NAN,
        );
        assert!(u.to_bytes().chunks_exact(4).all(|c| {
            f32::from_le_bytes([c[0], c[1], c[2], c[3]]).is_finite()
        }));
        assert!(u.view[0] >= 1.0 && u.view[1] >= 1.0);
    }

    #[test]
    fn panel_resolves_and_rejects() {
        let p = Panel { left: Some(16.0), right: Some(16.0), top: Some(40.0), bottom: None, width: None, height: 50.0 };
        assert_eq!(p.resolve(400.0, 800.0), Some([16.0, 40.0, 368.0, 50.0]));
        let r = Panel { left: None, right: Some(10.0), top: None, bottom: Some(20.0), width: Some(50.0), height: 100.0 };
        assert_eq!(r.resolve(400.0, 800.0), Some([340.0, 680.0, 50.0, 100.0]));
        assert_eq!(r.resolve(30.0, 800.0).map(|v| v[0]), Some(-30.0));
        let none = Panel { left: None, right: None, top: Some(0.0), bottom: None, width: None, height: 5.0 };
        assert_eq!(none.resolve(400.0, 800.0), None);
        assert_eq!(p.resolve(20.0, 800.0), None); // negative width
        assert!(p.css().contains("height:50px") && p.css().contains("left:16px"));
    }

    #[test]
    fn sdf_matches_known_points() {
        // 100x50 rect, radius 10.
        assert!((cpu::sd_rounded_rect([0.0, 0.0], [50.0, 25.0], 10.0) + 25.0).abs() < 1e-5);
        assert!(cpu::sd_rounded_rect([50.0, 0.0], [50.0, 25.0], 10.0).abs() < 1e-5);
        assert!(cpu::sd_rounded_rect([60.0, 0.0], [50.0, 25.0], 10.0) > 9.9);
    }

    #[test]
    fn refraction_is_strongest_at_the_edge_and_zero_inside() {
        assert!((cpu::refraction(0.0, 12.0, 24.0) - 24.0).abs() < 1e-5);
        assert_eq!(cpu::refraction(12.0, 12.0, 24.0), 0.0);
        assert!(cpu::refraction(3.0, 12.0, 24.0) > cpu::refraction(9.0, 12.0, 24.0));
        assert_eq!(cpu::refraction(5.0, 0.0, 24.0), 0.0);
    }
}

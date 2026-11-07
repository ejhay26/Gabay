//! Liquid-glass presets. All lengths are in logical pixels (dp).
//!
//! The refraction model is the one documented in `docs/CONVX_LIQUID_GLASS.md`:
//! a rounded-rect SDF band at the edge of a surface displaces the backdrop
//! sample point outward/inward, then a specular rim is added on top.

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum GlassStyle {
    /// Clear glass: the backdrop reads through.
    Regular,
    /// Tinted by the accent colour; used for the single primary action.
    Prominent,
}

#[derive(Clone, Copy, Debug, PartialEq)]
pub struct GlassSpec {
    pub blur_radius: f32,
    /// Width of the edge band that bends light.
    pub refraction_height: f32,
    /// Maximum displacement at the very edge.
    pub refraction_amount: f32,
    pub saturation: f32,
    /// 0.0 disables chromatic dispersion.
    pub dispersion: f32,
    /// Specular rim intensity, 0.0..=1.0.
    pub rim: f32,
}

impl GlassSpec {
    /// Bars and toolbars that float over content.
    pub const CHROME: GlassSpec = GlassSpec {
        blur_radius: 2.0,
        refraction_height: 12.0,
        refraction_amount: 24.0,
        saturation: 1.5,
        dispersion: 0.0,
        rim: 0.8,
    };

    /// Controls under a finger (toggles, buttons): deeper lens so a press reads.
    pub const CONTROL: GlassSpec = GlassSpec {
        refraction_height: 16.0,
        refraction_amount: 28.0,
        ..Self::CHROME
    };

    /// Large surfaces; heavier blur, deeper lens.
    pub const SHEET: GlassSpec = GlassSpec {
        blur_radius: 8.0,
        refraction_height: 20.0,
        refraction_amount: 32.0,
        ..Self::CHROME
    };

    /// Returns a copy with every field forced into a range the shader handles.
    pub fn sanitized(self) -> GlassSpec {
        GlassSpec {
            blur_radius: self.blur_radius.clamp(0.0, 32.0),
            refraction_height: self.refraction_height.max(0.0),
            refraction_amount: self.refraction_amount.max(0.0),
            saturation: self.saturation.clamp(0.0, 3.0),
            dispersion: self.dispersion.clamp(0.0, 1.0),
            rim: self.rim.clamp(0.0, 1.0),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn sanitize_clamps_everything() {
        let wild = GlassSpec {
            blur_radius: 999.0,
            refraction_height: -5.0,
            refraction_amount: -1.0,
            saturation: 10.0,
            dispersion: 5.0,
            rim: 9.0,
        };
        let s = wild.sanitized();
        assert_eq!(s.blur_radius, 32.0);
        assert_eq!(s.refraction_height, 0.0);
        assert_eq!(s.refraction_amount, 0.0);
        assert_eq!(s.saturation, 3.0);
        assert_eq!(s.dispersion, 1.0);
        assert_eq!(s.rim, 1.0);
    }
}

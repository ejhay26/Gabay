//! Colour tokens.
//!
//! Neutral system-style greys plus exactly two hues: one accent (blue) and one
//! danger (red). No gradients, no secondary brand hues.

/// 8-bit sRGB colour with straight alpha.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct Rgba {
    pub r: u8,
    pub g: u8,
    pub b: u8,
    pub a: u8,
}

impl Rgba {
    pub const fn rgb(hex: u32) -> Self {
        Self {
            r: ((hex >> 16) & 0xFF) as u8,
            g: ((hex >> 8) & 0xFF) as u8,
            b: (hex & 0xFF) as u8,
            a: 255,
        }
    }

    /// Returns this colour with alpha set from a 0.0..=1.0 fraction (clamped).
    pub fn with_alpha(self, alpha: f32) -> Self {
        let a = (alpha.clamp(0.0, 1.0) * 255.0).round() as u8;
        Self { a, ..self }
    }

    /// Straight-alpha floats in 0.0..=1.0, for GPU uniforms.
    pub fn to_f32(self) -> [f32; 4] {
        [
            f32::from(self.r) / 255.0,
            f32::from(self.g) / 255.0,
            f32::from(self.b) / 255.0,
            f32::from(self.a) / 255.0,
        ]
    }

    /// CSS `rgba(...)` string for the DOM layer.
    pub fn css(self) -> String {
        format!(
            "rgba({},{},{},{:.3})",
            self.r,
            self.g,
            self.b,
            f32::from(self.a) / 255.0
        )
    }
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum Scheme {
    Light,
    Dark,
}

/// Semantic colours for one scheme. Components read roles, never raw hex.
#[derive(Clone, Copy, Debug)]
pub struct Palette {
    pub canvas: Rgba,
    pub label: Rgba,
    pub label_secondary: Rgba,
    pub label_tertiary: Rgba,
    pub separator: Rgba,
    pub fill: Rgba,
    pub accent: Rgba,
    pub on_accent: Rgba,
    pub danger: Rgba,
    /// Base wash laid over the backdrop inside a glass surface.
    pub glass_wash: Rgba,
    /// Rim light on glass edges.
    pub glass_rim: Rgba,
}

impl Palette {
    pub const DARK: Palette = Palette {
        canvas: Rgba::rgb(0x000000),
        label: Rgba::rgb(0xFFFFFF),
        label_secondary: Rgba { r: 235, g: 235, b: 245, a: 153 },
        label_tertiary: Rgba { r: 235, g: 235, b: 245, a: 77 },
        separator: Rgba { r: 84, g: 84, b: 88, a: 153 },
        fill: Rgba { r: 120, g: 120, b: 128, a: 82 },
        accent: Rgba::rgb(0x0A84FF),
        on_accent: Rgba::rgb(0xFFFFFF),
        danger: Rgba::rgb(0xFF453A),
        glass_wash: Rgba { r: 28, g: 28, b: 30, a: 46 },
        glass_rim: Rgba { r: 255, g: 255, b: 255, a: 115 },
    };

    pub const LIGHT: Palette = Palette {
        canvas: Rgba::rgb(0xFFFFFF),
        label: Rgba::rgb(0x000000),
        label_secondary: Rgba { r: 60, g: 60, b: 67, a: 153 },
        label_tertiary: Rgba { r: 60, g: 60, b: 67, a: 77 },
        separator: Rgba { r: 60, g: 60, b: 67, a: 73 },
        fill: Rgba { r: 120, g: 120, b: 128, a: 51 },
        accent: Rgba::rgb(0x007AFF),
        on_accent: Rgba::rgb(0xFFFFFF),
        danger: Rgba::rgb(0xFF3B30),
        glass_wash: Rgba { r: 255, g: 255, b: 255, a: 56 },
        glass_rim: Rgba { r: 255, g: 255, b: 255, a: 200 },
    };

    pub const fn for_scheme(scheme: Scheme) -> Palette {
        match scheme {
            Scheme::Dark => Self::DARK,
            Scheme::Light => Self::LIGHT,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn rgb_unpacks_channels() {
        let c = Rgba::rgb(0x0A84FF);
        assert_eq!((c.r, c.g, c.b, c.a), (0x0A, 0x84, 0xFF, 255));
    }

    #[test]
    fn alpha_is_clamped() {
        assert_eq!(Rgba::rgb(0).with_alpha(2.0).a, 255);
        assert_eq!(Rgba::rgb(0).with_alpha(-1.0).a, 0);
    }
}

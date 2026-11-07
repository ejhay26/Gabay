//! Type scale. Uses the platform UI font; no bundled display font.

#[derive(Clone, Copy, Debug, PartialEq)]
pub struct TextStyle {
    pub size: f32,
    pub weight: u16,
    /// Letter spacing in em.
    pub tracking_em: f32,
    pub line_height: f32,
    /// Tabular figures, for values that update live (speed, distance).
    pub tabular: bool,
}

const fn style(size: f32, weight: u16, tracking_em: f32, line_height: f32) -> TextStyle {
    TextStyle { size, weight, tracking_em, line_height, tabular: false }
}

pub const LARGE_TITLE: TextStyle = style(34.0, 700, 0.011, 41.0);
pub const TITLE: TextStyle = style(22.0, 700, 0.016, 28.0);
pub const HEADLINE: TextStyle = style(17.0, 600, -0.022, 22.0);
pub const BODY: TextStyle = style(17.0, 400, -0.022, 22.0);
pub const SUBHEAD: TextStyle = style(15.0, 400, -0.016, 20.0);
pub const FOOTNOTE: TextStyle = style(13.0, 400, -0.006, 18.0);
pub const CAPTION: TextStyle = style(12.0, 500, 0.0, 16.0);

/// Live numeric readout (speed, remaining distance).
pub const METRIC: TextStyle =
    TextStyle { size: 34.0, weight: 600, tracking_em: 0.0, line_height: 40.0, tabular: true };

/// CSS font stack: system UI font first, so Android uses its native face.
pub const FONT_STACK: &str = "system-ui, -apple-system, Roboto, sans-serif";

impl TextStyle {
    /// Inline CSS declarations for this style.
    pub fn css(&self) -> String {
        let mut s = format!(
            "font-family:{};font-size:{}px;font-weight:{};letter-spacing:{}em;line-height:{}px;",
            FONT_STACK, self.size, self.weight, self.tracking_em, self.line_height
        );
        if self.tabular {
            s.push_str("font-variant-numeric:tabular-nums;");
        }
        s
    }
}

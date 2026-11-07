//! Glass surfaces. One source for both the GPU glass pass and the DOM.

use gabay_glass::{Panel, Surface};

pub const SHEET_H: f32 = 200.0;
pub const PILL_RADIUS: f32 = 26.0;
pub const CARD_RADIUS: f32 = 22.0;
pub const SHEET_RADIUS: f32 = 28.0;
/// Below this progress a morphing surface counts as closed.
pub const EPS: f32 = 0.02;
const M: f32 = 16.0;

pub const SEARCH: Panel = Panel { left: Some(M), right: Some(M), top: Some(44.0), bottom: None, width: None, height: 52.0 };
pub const SEARCH_FULL: Panel = Panel { left: Some(0.0), right: Some(0.0), top: Some(0.0), bottom: Some(0.0), width: None, height: 0.0 };
pub const SHEET: Panel = Panel { left: Some(M), right: Some(M), top: None, bottom: Some(M), width: None, height: SHEET_H };
pub const SHEET_HIDDEN: Panel = Panel { bottom: Some(-(SHEET_H + 24.0)), ..SHEET };
pub const HUD: Panel = Panel { left: Some(M), right: None, top: Some(108.0), bottom: None, width: Some(132.0), height: 76.0 };

pub fn tools(sheet_open: bool) -> Panel {
    let bottom = if sheet_open { M + SHEET_H + 12.0 } else { 24.0 };
    Panel { left: None, right: Some(M), top: None, bottom: Some(bottom), width: Some(52.0), height: 156.0 }
}

pub fn search_surface(t: f32) -> Surface {
    Surface { from: SEARCH, to: SEARCH_FULL, t, radius_from: PILL_RADIUS, radius_to: 0.0 }
}

pub fn tools_surface(sheet_t: f32) -> Surface {
    Surface { from: tools(false), to: tools(true), t: sheet_t, radius_from: CARD_RADIUS, radius_to: CARD_RADIUS }
}

pub fn sheet_surface(sheet_t: f32) -> Surface {
    Surface { from: SHEET_HIDDEN, to: SHEET, t: sheet_t, radius_from: SHEET_RADIUS, radius_to: SHEET_RADIUS }
}

/// Everything that should be glass right now. While search is open it covers
/// the screen, so nothing else is drawn.
pub fn surfaces(search_t: f32, sheet_t: f32, hud: bool) -> Vec<Surface> {
    let mut v = vec![search_surface(search_t)];
    if search_t > EPS {
        return v;
    }
    v.push(tools_surface(sheet_t));
    if sheet_t > EPS {
        v.push(sheet_surface(sheet_t));
    } else if hud {
        v.push(Surface::still(HUD, CARD_RADIUS));
    }
    v
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn open_search_hides_everything_else() {
        assert_eq!(surfaces(1.0, 1.0, true).len(), 1);
        assert_eq!(surfaces(0.0, 0.0, false).len(), 2);
        assert_eq!(surfaces(0.0, 1.0, true).len(), 3);
    }

    #[test]
    fn tools_stay_above_the_sheet_at_every_progress() {
        for i in 0..=10 {
            let t = i as f32 / 10.0;
            let (tools, _) = tools_surface(t).resolve(400.0, 900.0).unwrap();
            let (sheet, _) = sheet_surface(t).resolve(400.0, 900.0).unwrap();
            assert!(tools[1] + tools[3] <= sheet[1] + 1.0 || t < 0.5, "t={t}");
        }
        let (tools, _) = tools_surface(1.0).resolve(400.0, 900.0).unwrap();
        let (sheet, _) = sheet_surface(1.0).resolve(400.0, 900.0).unwrap();
        assert!(tools[1] + tools[3] < sheet[1]);
    }

    #[test]
    fn full_search_covers_the_viewport() {
        let (r, radius) = search_surface(1.0).resolve(390.0, 844.0).unwrap();
        assert_eq!(r, [0.0, 0.0, 390.0, 844.0]);
        assert_eq!(radius, 0.0);
    }
}

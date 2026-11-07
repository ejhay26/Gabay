//! Panel geometry. One source for both the GPU glass pass and the DOM.

use gabay_glass::Panel;

pub const ROW_H: f32 = 60.0;
pub const MAX_ROWS: usize = 5;
pub const SHEET_H: f32 = 200.0;
const M: f32 = 16.0;

pub const SEARCH: Panel = Panel { left: Some(M), right: Some(M), top: Some(44.0), bottom: None, width: None, height: 52.0 };
pub const SHEET: Panel = Panel { left: Some(M), right: Some(M), top: None, bottom: Some(M), width: None, height: SHEET_H };
pub const HUD: Panel = Panel { left: Some(M), right: None, top: Some(108.0), bottom: None, width: Some(132.0), height: 76.0 };

pub fn results(rows: usize) -> Panel {
    let rows = rows.clamp(1, MAX_ROWS);
    Panel { left: Some(M), right: Some(M), top: Some(108.0), bottom: None, width: None, height: rows as f32 * ROW_H }
}

pub fn tools(sheet_open: bool) -> Panel {
    let bottom = if sheet_open { M + SHEET_H + 12.0 } else { 24.0 };
    Panel { left: None, right: Some(M), top: None, bottom: Some(bottom), width: Some(52.0), height: 156.0 }
}

/// Everything that should be glass for the current state. `rows == 0` means no result list.
pub fn panels(rows: usize, sheet_open: bool, hud: bool) -> Vec<Panel> {
    let mut v = vec![SEARCH, tools(sheet_open)];
    if rows > 0 {
        v.push(results(rows));
    } else if hud {
        v.push(HUD);
    }
    if sheet_open {
        v.push(SHEET);
    }
    v
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn results_height_is_bounded() {
        assert_eq!(results(99).height, ROW_H * MAX_ROWS as f32);
        assert_eq!(results(0).height, ROW_H);
    }

    #[test]
    fn tools_clear_the_sheet_and_hud_yields_to_results() {
        let open = tools(true).resolve(400.0, 900.0).unwrap();
        let sheet = SHEET.resolve(400.0, 900.0).unwrap();
        assert!(open[1] + open[3] < sheet[1]);
        assert_eq!(panels(3, false, true).len(), 3);
        assert!(panels(0, true, true).contains(&HUD));
        assert!(!panels(2, true, true).contains(&HUD));
    }
}

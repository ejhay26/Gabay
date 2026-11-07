//! Turns decoded tiles and overlays into a flat, ordered draw list in screen
//! pixels. The shell paints it; nothing here touches the GPU.

use std::sync::Arc;

use crate::geo::{Camera, LonLat, TileId};
use crate::mvt::{GeomKind, Layer};
use crate::style::{class_of, classify, road_width, Color, MapTheme, Role};

#[derive(Clone, Debug, PartialEq)]
pub enum DrawItem {
    /// Even-odd fill; each ring is one closed contour.
    Fill { rings: Vec<Vec<[f32; 2]>>, color: Color },
    Stroke { points: Vec<[f32; 2]>, width: f32, color: Color },
    Dot { center: [f32; 2], radius: f32, color: Color },
}

#[derive(Clone)]
pub struct TileData {
    pub id: TileId,
    pub layers: Arc<Vec<Layer>>,
}

#[derive(Clone, Debug, Default)]
pub struct Overlays {
    pub route: Vec<LonLat>,
    /// Position and heading in degrees clockwise from north.
    pub user: Option<(LonLat, f32)>,
    pub pin: Option<LonLat>,
}

/// Fills are drawn first, then road casings, then road bodies, then points.
fn z_order(role: Role, is_line: bool, casing: bool) -> u8 {
    match (role, is_line) {
        (Role::Land, _) => 0,
        (Role::Park, _) => 1,
        (Role::Water, false) => 2,
        (Role::Water, true) => 3,
        (Role::Building, _) => 4,
        (Role::RoadMinor, _) => if casing { 10 } else { 11 },
        (Role::RoadMajor, _) => if casing { 12 } else { 13 },
        (Role::Poi, _) => 20,
    }
}

const CULL_MARGIN: f32 = 64.0;

fn off_screen(points: &[[f32; 2]], w: f32, h: f32) -> bool {
    let (mut min_x, mut min_y, mut max_x, mut max_y) = (f32::MAX, f32::MAX, f32::MIN, f32::MIN);
    for p in points {
        min_x = min_x.min(p[0]);
        max_x = max_x.max(p[0]);
        min_y = min_y.min(p[1]);
        max_y = max_y.max(p[1]);
    }
    max_x < -CULL_MARGIN || max_y < -CULL_MARGIN || min_x > w + CULL_MARGIN || min_y > h + CULL_MARGIN
}

pub fn build_scene(cam: &Camera, tiles: &[TileData], theme: &MapTheme) -> Vec<DrawItem> {
    let mut items: Vec<(u8, DrawItem)> = Vec::new();
    let (vw, vh) = (cam.width as f32, cam.height as f32);

    for tile in tiles {
        let n = f64::from(1u32 << tile.id.z.min(24));
        for layer in tile.layers.iter() {
            let ext = f64::from(layer.extent);
            let to_screen = |p: [i32; 2]| -> [f32; 2] {
                let wx = (f64::from(tile.id.x) + f64::from(p[0]) / ext) / n;
                let wy = (f64::from(tile.id.y) + f64::from(p[1]) / ext) / n;
                let s = cam.world_to_screen([wx, wy]);
                [s[0] as f32, s[1] as f32]
            };

            for feature in &layer.features {
                let Some(role) = classify(&layer.name, class_of(layer, feature)) else {
                    continue;
                };
                match feature.kind {
                    GeomKind::Polygon => {
                        let color = match role {
                            Role::Land => theme.land,
                            Role::Park => theme.park,
                            Role::Water => theme.water,
                            Role::Building if cam.zoom >= 14.5 => theme.building,
                            _ => continue,
                        };
                        let rings: Vec<Vec<[f32; 2]>> = feature
                            .parts
                            .iter()
                            .filter(|r| r.len() >= 3)
                            .map(|r| r.iter().map(|p| to_screen(*p)).collect())
                            .collect();
                        if rings.is_empty() || rings.iter().all(|r| off_screen(r, vw, vh)) {
                            continue;
                        }
                        items.push((z_order(role, false, false), DrawItem::Fill { rings, color }));
                    }
                    GeomKind::Line => {
                        for part in feature.parts.iter().filter(|p| p.len() >= 2) {
                            let pts: Vec<[f32; 2]> = part.iter().map(|p| to_screen(*p)).collect();
                            if off_screen(&pts, vw, vh) {
                                continue;
                            }
                            match role {
                                Role::RoadMinor | Role::RoadMajor => {
                                    let major = role == Role::RoadMajor;
                                    // Low-zoom filtering: Hide minor roads when zoomed out below zoom 11.5 (Google Maps behavior)
                                    if !major && cam.zoom < 11.5 {
                                        continue;
                                    }
                                    let w = road_width(major, cam.zoom);
                                    let body = if major { theme.road_major } else { theme.road_minor };

                                    // Proportional casing: only draw casing when zoom >= 12.0 for major and >= 13.0 for minor
                                    let casing_extra = if cam.zoom >= 14.0 {
                                        if major { 2.0 } else { 1.5 }
                                    } else if cam.zoom >= 12.0 {
                                        if major {
                                            (cam.zoom - 11.0) as f32 * 0.6
                                        } else if cam.zoom >= 13.0 {
                                            (cam.zoom - 12.5) as f32 * 0.8
                                        } else {
                                            0.0
                                        }
                                    } else {
                                        0.0
                                    };

                                    if casing_extra > 0.0 {
                                        items.push((
                                            z_order(role, true, true),
                                            DrawItem::Stroke {
                                                points: pts.clone(),
                                                width: w + casing_extra,
                                                color: theme.road_casing,
                                            },
                                        ));
                                    }
                                    items.push((
                                        z_order(role, true, false),
                                        DrawItem::Stroke { points: pts, width: w, color: body },
                                    ));
                                }
                                Role::Water => items.push((
                                    z_order(role, true, false),
                                    DrawItem::Stroke {
                                        points: pts,
                                        width: road_width(false, cam.zoom).max(1.0),
                                        color: theme.water,
                                    },
                                )),
                                _ => {}
                            }
                        }
                    }
                    GeomKind::Point if role == Role::Poi && cam.zoom >= 14.0 => {
                        for part in &feature.parts {
                            if let Some(p) = part.first() {
                                let c = to_screen(*p);
                                if !off_screen(&[c], vw, vh) {
                                    items.push((
                                        z_order(role, false, false),
                                        DrawItem::Dot { center: c, radius: 3.0, color: theme.poi },
                                    ));
                                }
                            }
                        }
                    }
                    _ => {}
                }
            }
        }
    }
    items.sort_by_key(|(z, _)| *z); // stable: keeps feature order within a layer
    items.into_iter().map(|(_, i)| i).collect()
}

/// Route, destination pin and the user marker, drawn above the map.
pub fn overlay_items(cam: &Camera, o: &Overlays, theme: &MapTheme) -> Vec<DrawItem> {
    let to = |p: LonLat| {
        let s = cam.project(p);
        [s[0] as f32, s[1] as f32]
    };
    let mut items = Vec::new();
    if o.route.len() >= 2 {
        let pts: Vec<[f32; 2]> = o.route.iter().map(|p| to(*p)).collect();
        items.push(DrawItem::Stroke { points: pts.clone(), width: 9.0, color: theme.route_casing });
        items.push(DrawItem::Stroke { points: pts, width: 6.0, color: theme.route });
    }
    if let Some(p) = o.pin {
        let c = to(p);
        items.push(DrawItem::Dot { center: c, radius: 9.0, color: theme.user_ring });
        items.push(DrawItem::Dot { center: c, radius: 6.0, color: theme.pin });
    }
    if let Some((p, heading)) = o.user {
        let c = to(p);
        // Heading is a compass bearing; the camera bearing rotates the screen.
        let (s, co) = (f64::from(heading) - cam.bearing).to_radians().sin_cos();
        let tip = [c[0] + (s * 22.0) as f32, c[1] - (co * 22.0) as f32];
        items.push(DrawItem::Stroke { points: vec![c, tip], width: 4.0, color: theme.user });
        items.push(DrawItem::Dot { center: c, radius: 11.0, color: theme.user_ring });
        items.push(DrawItem::Dot { center: c, radius: 8.0, color: theme.user });
    }
    items
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::mvt::{decode, testutil::*};

    fn camera_over_tile() -> (Camera, TileId) {
        // Centre of tile z14 (13680, 7520): fully inside the viewport.
        let n = 16384.0;
        let center = LonLat::from_world([(13680.5) / n, (7520.5) / n]);
        let cam = Camera::new(center, 14.0, 400.0, 800.0);
        (cam, TileId { z: 14, x: 13680, y: 7520 })
    }

    fn tile_data(id: TileId, bytes: &[u8]) -> TileData {
        TileData { id, layers: Arc::new(decode(bytes).unwrap()) }
    }

    #[test]
    fn road_emits_casing_then_body_in_tile_centre() {
        let (cam, id) = camera_over_tile();
        let geom = line_geometry(&[(1024, 1024), (3072, 3072)]);
        let t = tile(&[layer("roads", 2, &geom, Some("primary"))]);
        let items = build_scene(&cam, &[tile_data(id, &t)], &MapTheme::DARK);
        assert_eq!(items.len(), 2);
        match (&items[0], &items[1]) {
            (
                DrawItem::Stroke { width: wc, color: cc, .. },
                DrawItem::Stroke { width: wb, color: cb, points },
            ) => {
                assert!(wc > wb);
                assert_eq!(*cc, MapTheme::DARK.road_casing);
                assert_eq!(*cb, MapTheme::DARK.road_major);
                assert_eq!(points.len(), 2);
            }
            other => panic!("unexpected {other:?}"),
        }
    }

    #[test]
    fn water_draws_below_roads() {
        let (cam, id) = camera_over_tile();
        let road = line_geometry(&[(0, 0), (4000, 4000)]);
        // MoveTo(1) + LineTo(2) + ClosePath => triangle polygon.
        let poly = [9, zz(100), zz(100), (2 << 3) | 2, zz(3000), zz(0), zz(0), zz(3000), 15];
        let t = tile(&[layer("roads", 2, &road, None), layer("water", 3, &poly, None)]);
        let items = build_scene(&cam, &[tile_data(id, &t)], &MapTheme::DARK);
        assert!(matches!(items.first(), Some(DrawItem::Fill { .. })));
        assert!(matches!(items.last(), Some(DrawItem::Stroke { .. })));
    }

    #[test]
    fn off_screen_features_are_culled() {
        let (mut cam, id) = camera_over_tile();
        cam.center = LonLat::new(cam.center.lon + 5.0, cam.center.lat);
        let geom = line_geometry(&[(1024, 1024), (3072, 3072)]);
        let t = tile(&[layer("roads", 2, &geom, None)]);
        assert!(build_scene(&cam, &[tile_data(id, &t)], &MapTheme::DARK).is_empty());
    }

    #[test]
    fn minor_roads_are_culled_at_low_zoom() {
        let (mut cam, id) = camera_over_tile();
        cam.zoom = 10.0;
        let geom = line_geometry(&[(1024, 1024), (3072, 3072)]);
        let t = tile(&[layer("roads", 2, &geom, Some("residential"))]);
        assert!(build_scene(&cam, &[tile_data(id, &t)], &MapTheme::DARK).is_empty());

        // Major roads are still visible at zoom 10
        let t_major = tile(&[layer("roads", 2, &geom, Some("motorway"))]);
        assert!(!build_scene(&cam, &[tile_data(id, &t_major)], &MapTheme::DARK).is_empty());
    }

    #[test]
    fn unknown_layers_and_low_zoom_buildings_are_skipped() {
        let (mut cam, id) = camera_over_tile();
        cam.zoom = 12.0;
        let poly = [9, zz(100), zz(100), (2 << 3) | 2, zz(3000), zz(0), zz(0), zz(3000), 15];
        let t = tile(&[layer("buildings", 3, &poly, None), layer("mystery", 3, &poly, None)]);
        assert!(build_scene(&cam, &[tile_data(id, &t)], &MapTheme::DARK).is_empty());
    }

    #[test]
    fn overlays_draw_route_pin_and_user() {
        let (cam, _) = camera_over_tile();
        let o = Overlays {
            route: vec![cam.center, LonLat::new(cam.center.lon + 0.001, cam.center.lat)],
            user: Some((cam.center, 90.0)),
            pin: Some(cam.center),
        };
        let items = overlay_items(&cam, &o, &MapTheme::DARK);
        assert_eq!(items.len(), 2 + 2 + 3);
        let empty = overlay_items(&cam, &Overlays::default(), &MapTheme::DARK);
        assert!(empty.is_empty());
    }

    #[test]
    fn user_heading_is_relative_to_camera_bearing() {
        let (mut cam, _) = camera_over_tile();
        let o = Overlays { user: Some((cam.center, 0.0)), ..Overlays::default() };
        let up = overlay_items(&cam, &o, &MapTheme::DARK);
        cam.bearing = 90.0; // north now points left
        let left = overlay_items(&cam, &o, &MapTheme::DARK);
        let tip = |items: &[DrawItem]| match &items[0] {
            DrawItem::Stroke { points, .. } => points[1],
            _ => panic!(),
        };
        assert!(tip(&up)[1] < 400.0 - 20.0); // above centre
        assert!(tip(&left)[0] < 200.0 - 20.0); // left of centre
    }
}

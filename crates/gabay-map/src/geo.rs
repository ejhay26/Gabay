//! Web-Mercator camera and tile coverage.

use std::f64::consts::PI;

/// Tile edge in logical pixels at integer zoom (MapLibre convention).
pub const TILE_PX: f64 = 512.0;
const MAX_LAT: f64 = 85.051_128_78;
pub const MAX_VISIBLE_TILES: usize = 64;

#[derive(Clone, Copy, Debug, PartialEq)]
pub struct LonLat {
    pub lon: f64,
    pub lat: f64,
}

impl LonLat {
    pub const fn new(lon: f64, lat: f64) -> Self {
        Self { lon, lat }
    }

    /// Normalised Mercator coordinates, both in `0.0..=1.0`, y pointing south.
    pub fn to_world(self) -> [f64; 2] {
        let lat = self.lat.clamp(-MAX_LAT, MAX_LAT).to_radians();
        let x = (self.lon + 180.0) / 360.0;
        let y = 0.5 - (PI / 4.0 + lat / 2.0).tan().ln() / (2.0 * PI);
        [x, y]
    }

    pub fn from_world(w: [f64; 2]) -> Self {
        let lon = w[0] * 360.0 - 180.0;
        let lat = (PI * (1.0 - 2.0 * w[1])).sinh().atan().to_degrees();
        Self { lon, lat }
    }
}

#[derive(Clone, Copy, Debug, PartialEq)]
pub struct Bounds {
    pub min_lon: f64,
    pub min_lat: f64,
    pub max_lon: f64,
    pub max_lat: f64,
}

impl Bounds {
    /// Flagship region; used when the vault has no readable archive.
    pub const PHILIPPINES: Bounds =
        Bounds { min_lon: 116.5, min_lat: 4.5, max_lon: 127.0, max_lat: 21.5 };

    /// `None` unless the box is finite, ordered and inside valid lon/lat ranges.
    pub fn validated(self) -> Option<Self> {
        let ok = self.min_lon >= -180.0
            && self.max_lon <= 180.0
            && self.min_lat >= -90.0
            && self.max_lat <= 90.0
            && self.min_lon < self.max_lon
            && self.min_lat < self.max_lat;
        ok.then_some(self)
    }
}

#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash)]
pub struct TileId {
    pub z: u8,
    pub x: u32,
    pub y: u32,
}

#[derive(Clone, Copy, Debug, PartialEq)]
pub struct Camera {
    pub center: LonLat,
    pub zoom: f64,
    /// Degrees. The compass direction that points up on screen.
    pub bearing: f64,
    /// Viewport in logical pixels.
    pub width: f64,
    pub height: f64,
}

impl Camera {
    pub const MIN_ZOOM: f64 = 5.5;
    pub const MAX_ZOOM: f64 = 19.0;

    pub fn new(center: LonLat, zoom: f64, width: f64, height: f64) -> Self {
        Self { center, zoom, bearing: 0.0, width, height }
    }

    fn world_size(&self) -> f64 {
        TILE_PX * self.zoom.exp2()
    }

    fn rotate(v: [f64; 2], degrees: f64) -> [f64; 2] {
        let (s, c) = degrees.to_radians().sin_cos();
        [v[0] * c - v[1] * s, v[0] * s + v[1] * c]
    }

    pub fn world_to_screen(&self, w: [f64; 2]) -> [f64; 2] {
        let c = self.center.to_world();
        let size = self.world_size();
        let d = Self::rotate([(w[0] - c[0]) * size, (w[1] - c[1]) * size], -self.bearing);
        [d[0] + self.width / 2.0, d[1] + self.height / 2.0]
    }

    pub fn screen_to_world(&self, s: [f64; 2]) -> [f64; 2] {
        let c = self.center.to_world();
        let size = self.world_size();
        let d = Self::rotate([s[0] - self.width / 2.0, s[1] - self.height / 2.0], self.bearing);
        [c[0] + d[0] / size, c[1] + d[1] / size]
    }

    pub fn project(&self, p: LonLat) -> [f64; 2] {
        self.world_to_screen(p.to_world())
    }

    pub fn unproject(&self, s: [f64; 2]) -> LonLat {
        LonLat::from_world(self.screen_to_world(s))
    }

    /// Drag: the map content follows the finger by `(dx, dy)` pixels.
    pub fn pan_pixels(&mut self, dx: f64, dy: f64) {
        let target = self.screen_to_world([self.width / 2.0 - dx, self.height / 2.0 - dy]);
        self.center = LonLat::from_world(target);
    }

    /// Pinch or wheel zoom that keeps the point under `focal` fixed.
    pub fn zoom_about(&mut self, focal: [f64; 2], delta: f64) {
        let anchor = self.screen_to_world(focal);
        self.zoom = (self.zoom + delta).clamp(Self::MIN_ZOOM, Self::MAX_ZOOM);
        let now = self.world_to_screen(anchor);
        self.pan_pixels(focal[0] - now[0], focal[1] - now[1]);
    }

    pub fn rotate_by(&mut self, degrees: f64) {
        self.bearing = (self.bearing + degrees).rem_euclid(360.0);
    }

    /// Hard limit to the region the archive covers.
    pub fn clamp_to(&mut self, b: Bounds) {
        self.zoom = self.zoom.clamp(Self::MIN_ZOOM, Self::MAX_ZOOM);
        self.center.lon = self.center.lon.clamp(b.min_lon, b.max_lon);
        self.center.lat = self.center.lat.clamp(b.min_lat, b.max_lat);
    }

    /// Tiles that cover the viewport at `tile_zoom`, nearest to the centre first.
    pub fn visible_tiles(&self, tile_zoom: u8) -> Vec<TileId> {
        let corners = [
            [0.0, 0.0],
            [self.width, 0.0],
            [0.0, self.height],
            [self.width, self.height],
        ]
        .map(|s| self.screen_to_world(s));
        let (mut min_x, mut min_y) = (f64::MAX, f64::MAX);
        let (mut max_x, mut max_y) = (f64::MIN, f64::MIN);
        for c in corners {
            min_x = min_x.min(c[0]);
            max_x = max_x.max(c[0]);
            min_y = min_y.min(c[1]);
            max_y = max_y.max(c[1]);
        }
        let n = f64::from(1u32 << tile_zoom.min(24));
        let last = n - 1.0;
        let lo_x = (min_x * n).floor().clamp(0.0, last) as u32;
        let hi_x = (max_x * n).floor().clamp(0.0, last) as u32;
        let lo_y = (min_y * n).floor().clamp(0.0, last) as u32;
        let hi_y = (max_y * n).floor().clamp(0.0, last) as u32;

        let c = self.center.to_world();
        let mut tiles: Vec<(f64, TileId)> = Vec::new();
        for x in lo_x..=hi_x {
            for y in lo_y..=hi_y {
                let dx = (f64::from(x) + 0.5) / n - c[0];
                let dy = (f64::from(y) + 0.5) / n - c[1];
                tiles.push((dx * dx + dy * dy, TileId { z: tile_zoom, x, y }));
            }
        }
        tiles.sort_by(|a, b| a.0.total_cmp(&b.0));
        tiles.truncate(MAX_VISIBLE_TILES);
        tiles.into_iter().map(|(_, t)| t).collect()
    }

    /// Integer zoom of the tiles to load, limited to what the archive holds.
    pub fn tile_zoom(&self, min_zoom: u8, max_zoom: u8) -> u8 {
        let z = self.zoom.floor().max(0.0) as u8;
        z.clamp(min_zoom, max_zoom.max(min_zoom))
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn cam() -> Camera {
        Camera::new(LonLat::new(120.9064, 15.3129), 12.0, 400.0, 800.0)
    }

    #[test]
    fn world_roundtrip() {
        let p = LonLat::new(121.0244, 14.5547);
        let q = LonLat::from_world(p.to_world());
        assert!((p.lon - q.lon).abs() < 1e-9 && (p.lat - q.lat).abs() < 1e-9);
    }

    #[test]
    fn centre_projects_to_viewport_centre() {
        let c = cam();
        let s = c.project(c.center);
        assert!((s[0] - 200.0).abs() < 1e-6 && (s[1] - 400.0).abs() < 1e-6);
    }

    #[test]
    fn screen_roundtrip_with_rotation() {
        let mut c = cam();
        c.bearing = 37.0;
        let p = c.unproject([50.0, 700.0]);
        let s = c.project(p);
        assert!((s[0] - 50.0).abs() < 1e-6 && (s[1] - 700.0).abs() < 1e-6);
    }

    #[test]
    fn east_is_up_at_bearing_90() {
        let mut c = cam();
        c.bearing = 90.0;
        let east = LonLat::new(c.center.lon + 0.01, c.center.lat);
        let s = c.project(east);
        assert!(s[1] < 400.0, "east should be above centre, got {s:?}");
        assert!((s[0] - 200.0).abs() < 1e-3);
    }

    #[test]
    fn pan_moves_content_with_finger() {
        let mut c = cam();
        let marker = LonLat::new(120.91, 15.31);
        let before = c.project(marker);
        c.pan_pixels(30.0, -20.0);
        let after = c.project(marker);
        assert!((after[0] - before[0] - 30.0).abs() < 1e-6);
        assert!((after[1] - before[1] + 20.0).abs() < 1e-6);
    }

    #[test]
    fn zoom_keeps_focal_point_fixed() {
        let mut c = cam();
        c.bearing = 20.0;
        let focal = [310.0, 150.0];
        let anchor = c.unproject(focal);
        c.zoom_about(focal, 1.5);
        let s = c.project(anchor);
        assert!((s[0] - focal[0]).abs() < 1e-6 && (s[1] - focal[1]).abs() < 1e-6);
        assert!((c.zoom - 13.5).abs() < 1e-9);
    }

    #[test]
    fn zoom_is_clamped() {
        let mut c = cam();
        c.zoom_about([0.0, 0.0], 100.0);
        assert_eq!(c.zoom, Camera::MAX_ZOOM);
        c.zoom_about([0.0, 0.0], -100.0);
        assert_eq!(c.zoom, Camera::MIN_ZOOM);
    }

    #[test]
    fn visible_tiles_include_centre_tile_first() {
        let c = cam();
        let tiles = c.visible_tiles(12);
        assert!(!tiles.is_empty() && tiles.len() <= MAX_VISIBLE_TILES);
        let w = c.center.to_world();
        let n = 4096.0;
        let centre = TileId { z: 12, x: (w[0] * n) as u32, y: (w[1] * n) as u32 };
        assert_eq!(tiles[0], centre);
    }

    #[test]
    fn clamp_keeps_centre_inside_bounds() {
        let mut c = cam();
        c.center = LonLat::new(10.0, -40.0);
        c.clamp_to(Bounds::PHILIPPINES);
        assert_eq!(c.center, LonLat::new(116.5, 4.5));
    }

    #[test]
    fn bounds_validation_rejects_garbage() {
        assert!(Bounds { min_lon: 10.0, min_lat: 0.0, max_lon: 5.0, max_lat: 1.0 }.validated().is_none());
        assert!(Bounds { min_lon: f64::NAN, min_lat: 0.0, max_lon: 5.0, max_lat: 1.0 }.validated().is_none());
        assert!(Bounds::PHILIPPINES.validated().is_some());
    }

    #[test]
    fn tile_zoom_follows_archive_range() {
        let mut c = cam();
        c.zoom = 17.4;
        assert_eq!(c.tile_zoom(0, 14), 14);
        c.zoom = 3.0;
        assert_eq!(c.tile_zoom(6, 14), 6);
    }
}

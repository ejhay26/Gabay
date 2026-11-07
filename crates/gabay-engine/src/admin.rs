//! Resolves which barangay, municipality/city and province a point is in.
//! Areas come from OSM admin boundaries (admin_level 4 / 6 / 10) prepared at
//! data-build time. Coordinates are (lon, lat).

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum Level {
    Province,
    Municipality,
    Barangay,
}

#[derive(Clone, Debug)]
pub struct Area {
    pub level: Level,
    pub name: String,
    /// Closed or open rings; tested with the even-odd rule.
    pub rings: Vec<Vec<[f64; 2]>>,
}

#[derive(Clone, Debug, Default, PartialEq, Eq)]
pub struct Admin {
    pub barangay: Option<String>,
    pub municipality: Option<String>,
    pub province: Option<String>,
}

struct Prepared {
    area: Area,
    bbox: [f64; 4], // min_lon, min_lat, max_lon, max_lat
    size: f64,
}

#[derive(Default)]
pub struct AdminIndex {
    areas: Vec<Prepared>,
}

fn bbox_of(rings: &[Vec<[f64; 2]>]) -> Option<[f64; 4]> {
    let mut b = [f64::MAX, f64::MAX, f64::MIN, f64::MIN];
    let mut any = false;
    for p in rings.iter().flatten() {
        if !p[0].is_finite() || !p[1].is_finite() {
            return None;
        }
        any = true;
        b = [b[0].min(p[0]), b[1].min(p[1]), b[2].max(p[0]), b[3].max(p[1])];
    }
    any.then_some(b)
}

fn inside(rings: &[Vec<[f64; 2]>], lon: f64, lat: f64) -> bool {
    let mut odd = false;
    for ring in rings {
        let n = ring.len();
        if n < 3 {
            continue;
        }
        let mut j = n - 1;
        for i in 0..n {
            let (a, b) = (ring[i], ring[j]);
            if (a[1] > lat) != (b[1] > lat) && lon < (b[0] - a[0]) * (lat - a[1]) / (b[1] - a[1]) + a[0] {
                odd = !odd;
            }
            j = i;
        }
    }
    odd
}

impl AdminIndex {
    /// Areas with no usable geometry are dropped.
    pub fn new(areas: Vec<Area>) -> Self {
        let areas = areas
            .into_iter()
            .filter_map(|area| {
                let bbox = bbox_of(&area.rings)?;
                Some(Prepared { size: (bbox[2] - bbox[0]) * (bbox[3] - bbox[1]), bbox, area })
            })
            .collect();
        Self { areas }
    }

    pub fn len(&self) -> usize {
        self.areas.len()
    }

    pub fn is_empty(&self) -> bool {
        self.areas.is_empty()
    }

    /// For each level, the smallest containing area wins (handles enclaves).
    pub fn resolve(&self, lon: f64, lat: f64) -> Admin {
        let mut best: [Option<&Prepared>; 3] = [None; 3];
        for p in &self.areas {
            if lon < p.bbox[0] || lon > p.bbox[2] || lat < p.bbox[1] || lat > p.bbox[3] {
                continue;
            }
            if !inside(&p.area.rings, lon, lat) {
                continue;
            }
            let slot = &mut best[p.area.level as usize];
            if slot.is_none_or(|cur| p.size < cur.size) {
                *slot = Some(p);
            }
        }
        let name = |i: usize| best[i].map(|p| p.area.name.clone());
        Admin { province: name(0), municipality: name(1), barangay: name(2) }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn square(level: Level, name: &str, x0: f64, y0: f64, x1: f64, y1: f64) -> Area {
        Area {
            level,
            name: name.into(),
            rings: vec![vec![[x0, y0], [x1, y0], [x1, y1], [x0, y1]]],
        }
    }

    fn index() -> AdminIndex {
        AdminIndex::new(vec![
            square(Level::Province, "Metro Manila", 121.0, 14.4, 121.2, 14.8),
            square(Level::Municipality, "Pasig City", 121.05, 14.5, 121.12, 14.62),
            square(Level::Barangay, "Manggahan", 121.08, 14.57, 121.10, 14.60),
            square(Level::Province, "Cavite", 120.6, 14.0, 121.0, 14.5),
            square(Level::Municipality, "General Trias", 120.85, 14.3, 120.95, 14.42),
            square(Level::Barangay, "Manggahan", 120.88, 14.33, 120.90, 14.35),
        ])
    }

    #[test]
    fn same_barangay_name_resolves_to_the_right_city() {
        let i = index();
        let pasig = i.resolve(121.09, 14.58);
        assert_eq!(pasig.municipality.as_deref(), Some("Pasig City"));
        assert_eq!(pasig.province.as_deref(), Some("Metro Manila"));
        let cavite = i.resolve(120.89, 14.34);
        assert_eq!(cavite.municipality.as_deref(), Some("General Trias"));
        assert_eq!(cavite.province.as_deref(), Some("Cavite"));
        assert_eq!(cavite.barangay.as_deref(), Some("Manggahan"));
    }

    #[test]
    fn outside_everything_is_empty() {
        assert_eq!(index().resolve(125.0, 8.0), Admin::default());
    }

    #[test]
    fn smallest_area_wins_and_bad_geometry_is_dropped() {
        let mut v = vec![
            square(Level::Barangay, "Big", 0.0, 0.0, 10.0, 10.0),
            square(Level::Barangay, "Small", 4.0, 4.0, 6.0, 6.0),
            Area { level: Level::Barangay, name: "Nan".into(), rings: vec![vec![[f64::NAN, 0.0]; 3]] },
            Area { level: Level::Barangay, name: "Empty".into(), rings: vec![] },
        ];
        let i = AdminIndex::new(std::mem::take(&mut v));
        assert_eq!(i.len(), 2);
        assert_eq!(i.resolve(5.0, 5.0).barangay.as_deref(), Some("Small"));
        assert_eq!(i.resolve(1.0, 1.0).barangay.as_deref(), Some("Big"));
    }
}

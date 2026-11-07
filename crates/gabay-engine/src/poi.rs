//! Search result type and its place label, e.g. "Manggahan" with the subtitle
//! "Pasig City, Metro Manila".

use crate::admin::Admin;

#[derive(Clone, Debug, PartialEq)]
pub struct Poi {
    pub id: i64,
    pub name: String,
    pub category: String,
    pub lon: f64,
    pub lat: f64,
    pub admin: Admin,
}

#[derive(Clone, Debug, PartialEq)]
pub struct SearchHit {
    pub poi: Poi,
    /// Metres from the user, when a fix is available.
    pub distance_m: Option<f64>,
}

/// "City of Pasig" -> "Pasig City"; "Municipality of X" -> "X".
pub fn normalize_municipality(raw: &str) -> String {
    let s = raw.trim();
    if let Some(rest) = strip_prefix_ci(s, "city of ") {
        return format!("{} City", rest.trim());
    }
    if let Some(rest) = strip_prefix_ci(s, "municipality of ") {
        return rest.trim().to_owned();
    }
    s.to_owned()
}

fn strip_prefix_ci<'a>(s: &'a str, prefix: &str) -> Option<&'a str> {
    let head = s.get(..prefix.len())?;
    head.eq_ignore_ascii_case(prefix).then(|| &s[prefix.len()..])
}

fn same(a: &str, b: &str) -> bool {
    a.trim().eq_ignore_ascii_case(b.trim())
}

impl Poi {
    /// "Pasig City, Metro Manila". Skips parts that are empty or repeat the
    /// POI's own name or an earlier part.
    pub fn subtitle(&self) -> String {
        let mut parts: Vec<String> = Vec::new();
        let candidates = [
            self.admin.barangay.clone(),
            self.admin.municipality.as_deref().map(normalize_municipality),
            self.admin.province.clone(),
        ];
        for c in candidates.into_iter().flatten() {
            let c = c.trim().to_owned();
            if c.is_empty() || same(&c, &self.name) || parts.iter().any(|p| same(p, &c)) {
                continue;
            }
            parts.push(c);
        }
        parts.join(", ")
    }

    /// One-line form: "Manggahan, Pasig City".
    pub fn label(&self) -> String {
        let municipality = self.admin.municipality.as_deref().map(normalize_municipality);
        match municipality {
            Some(m) if !same(&m, &self.name) => format!("{}, {}", self.name, m),
            _ => self.name.clone(),
        }
    }
}

/// "850 m" / "1.2 km" / "12 km"; `None` for NaN or negative input.
pub fn format_distance(m: f64) -> Option<String> {
    if !m.is_finite() || m < 0.0 {
        return None;
    }
    Some(if m < 1000.0 {
        format!("{} m", (m / 10.0).round() as i64 * 10)
    } else if m < 10_000.0 {
        format!("{:.1} km", m / 1000.0)
    } else {
        format!("{} km", (m / 1000.0).round() as i64)
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    fn poi(name: &str, b: Option<&str>, m: Option<&str>, p: Option<&str>) -> Poi {
        Poi {
            id: 1,
            name: name.into(),
            category: "PLACE".into(),
            lon: 0.0,
            lat: 0.0,
            admin: Admin {
                barangay: b.map(Into::into),
                municipality: m.map(Into::into),
                province: p.map(Into::into),
            },
        }
    }

    #[test]
    fn manggahan_pasig_vs_cavite_are_distinguishable() {
        let a = poi("Manggahan", Some("Manggahan"), Some("City of Pasig"), Some("Metro Manila"));
        let b = poi("Manggahan", Some("Manggahan"), Some("General Trias"), Some("Cavite"));
        assert_eq!(a.label(), "Manggahan, Pasig City");
        assert_eq!(b.label(), "Manggahan, General Trias");
        assert_eq!(a.subtitle(), "Pasig City, Metro Manila");
        assert_eq!(b.subtitle(), "General Trias, Cavite");
    }

    #[test]
    fn missing_admin_degrades_to_plain_name() {
        let p = poi("Jollibee", None, None, None);
        assert_eq!(p.label(), "Jollibee");
        assert_eq!(p.subtitle(), "");
    }

    #[test]
    fn barangay_differing_from_name_is_kept_and_duplicates_dropped() {
        let p = poi("Petron", Some("San Roque"), Some("Pasig City"), Some("Pasig City"));
        assert_eq!(p.subtitle(), "San Roque, Pasig City");
    }

    #[test]
    fn normalizes_prefixes_without_panicking_on_short_or_multibyte_text() {
        assert_eq!(normalize_municipality("City of Pasig"), "Pasig City");
        assert_eq!(normalize_municipality("MUNICIPALITY OF Jaen"), "Jaen");
        assert_eq!(normalize_municipality("Peñaranda"), "Peñaranda");
        assert_eq!(normalize_municipality("ñ"), "ñ");
        assert_eq!(normalize_municipality(""), "");
    }

    #[test]
    fn distance_format() {
        assert_eq!(format_distance(849.0).as_deref(), Some("850 m"));
        assert_eq!(format_distance(1234.0).as_deref(), Some("1.2 km"));
        assert_eq!(format_distance(15_400.0).as_deref(), Some("15 km"));
        assert_eq!(format_distance(f64::NAN), None);
        assert_eq!(format_distance(-1.0), None);
    }
}

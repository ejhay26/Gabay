//! Place search over `pois_fts.sqlite`. Opens the file read-only once, reports
//! every failure as an error, and returns barangay / municipality / province
//! when the database has those columns (see tools/add_admin_columns.py).

use std::path::Path;
use std::sync::Mutex;

use gabay_engine::{Admin, Poi, SearchHit};
use rusqlite::{params_from_iter, types::Value, Connection, OpenFlags};

pub const MAX_LIMIT: usize = 50;
const MAX_QUERY_CHARS: usize = 120;
const MAX_TOKENS: usize = 8;

#[derive(Debug, thiserror::Error)]
pub enum SearchError {
    #[error("cannot open {0}: {1}")]
    Open(String, rusqlite::Error),
    #[error("database has no `pois` table")]
    NoPoisTable,
    #[error("query failed: {0}")]
    Query(#[from] rusqlite::Error),
    #[error("search state poisoned")]
    Poisoned,
}

/// Query words that mean a category rather than a name. Matching uses the
/// category column (case-insensitive) plus the listed brand names.
struct Group {
    words: &'static [&'static str],
    categories: &'static [&'static str],
    brands: &'static [&'static str],
}

const GROUPS: &[Group] = &[
    Group { words: &["gas", "fuel", "gas station", "gas stations"], categories: &["GAS_STATION", "FUEL", "GAS"], brands: &["Petron", "Shell", "Caltex", "Cleanfuel", "Phoenix", "Seaoil", "PTT"] },
    Group { words: &["hospital", "hospitals", "clinic", "clinics"], categories: &["HOSPITAL", "CLINIC"], brands: &[] },
    Group { words: &["school", "schools", "college", "colleges", "university"], categories: &["SCHOOL", "UNIVERSITY", "COLLEGE"], brands: &[] },
    Group { words: &["food", "restaurant", "restaurants", "fast food"], categories: &["RESTAURANT", "FAST_FOOD", "CAFE"], brands: &["Jollibee", "McDonald", "Mang Inasal", "KFC"] },
    Group { words: &["government", "city hall", "municipal hall", "police", "civic"], categories: &["GOVERNMENT", "CIVIC", "TOWN_HALL", "POLICE"], brands: &[] },
    Group { words: &["market", "markets", "mall", "malls", "supermarket"], categories: &["MARKET", "SUPERMARKET", "MALL", "COMMERCIAL"], brands: &[] },
];

pub struct PoiDb {
    conn: Mutex<Connection>,
    has_admin: bool,
}

fn haversine_m(a: (f64, f64), b: (f64, f64)) -> f64 {
    let (dlat, dlon) = ((b.0 - a.0).to_radians(), (b.1 - a.1).to_radians());
    let h = (dlat / 2.0).sin().powi(2) + a.0.to_radians().cos() * b.0.to_radians().cos() * (dlon / 2.0).sin().powi(2);
    2.0 * 6_371_000.0 * h.sqrt().min(1.0).asin()
}

fn escape_like(s: &str) -> String {
    s.replace('\\', "\\\\").replace('%', "\\%").replace('_', "\\_")
}

/// Alphanumeric words only, so quoting them for FTS5 is always safe.
fn tokens(q: &str) -> Vec<String> {
    q.chars()
        .take(MAX_QUERY_CHARS)
        .collect::<String>()
        .to_lowercase()
        .split(|c: char| !c.is_alphanumeric())
        .filter(|w| !w.is_empty())
        .take(MAX_TOKENS)
        .map(|w| if w == "nust" { "neust".to_owned() } else { w.to_owned() })
        .collect()
}

impl PoiDb {
    pub fn open(path: &Path) -> Result<Self, SearchError> {
        let conn = Connection::open_with_flags(path, OpenFlags::SQLITE_OPEN_READ_ONLY | OpenFlags::SQLITE_OPEN_NO_MUTEX)
            .map_err(|e| SearchError::Open(path.display().to_string(), e))?;
        Self::from_connection(conn)
    }

    pub fn from_connection(conn: Connection) -> Result<Self, SearchError> {
        let cols: Vec<String> = conn
            .prepare("PRAGMA table_info(pois)")?
            .query_map([], |r| r.get::<_, String>(1))?
            .collect::<Result<_, _>>()?;
        if cols.is_empty() {
            return Err(SearchError::NoPoisTable);
        }
        let has_admin = ["barangay", "municipality", "province"].iter().all(|c| cols.iter().any(|x| x == c));
        if !has_admin {
            log::warn!("pois has no admin columns; results will not show city or province");
        }
        Ok(Self { conn: Mutex::new(conn), has_admin })
    }

    pub fn has_admin(&self) -> bool {
        self.has_admin
    }

    /// `near` is (lat, lon). Results are nearest-first when it is given.
    pub fn search(&self, query: &str, near: Option<(f64, f64)>, limit: usize) -> Result<Vec<SearchHit>, SearchError> {
        let limit = limit.clamp(1, MAX_LIMIT);
        let toks = tokens(query);
        if toks.is_empty() {
            return Ok(Vec::new());
        }
        let phrase = toks.join(" ");
        let cols = if self.has_admin {
            "p.id, p.name, p.category, p.latitude, p.longitude, p.barangay, p.municipality, p.province"
        } else {
            "p.id, p.name, p.category, p.latitude, p.longitude, NULL, NULL, NULL"
        };
        let candidates = (limit * 10) as i64;
        let conn = self.conn.lock().map_err(|_| SearchError::Poisoned)?;

        let (sql, args): (String, Vec<Value>) = if let Some(g) = GROUPS.iter().find(|g| g.words.contains(&phrase.as_str())) {
            let cat_marks = vec!["?"; g.categories.len()].join(",");
            let mut args: Vec<Value> = g.categories.iter().map(|c| Value::from((*c).to_owned())).collect();
            let mut sql = format!("SELECT {cols} FROM pois p WHERE upper(p.category) IN ({cat_marks})");
            for b in g.brands {
                sql += " OR p.name LIKE ? ESCAPE '\\'";
                args.push(Value::from(format!("%{}%", escape_like(b))));
            }
            sql += " LIMIT ?";
            args.push(Value::from(candidates));
            (sql, args)
        } else {
            let fts = toks.iter().map(|t| format!("\"{t}\"*")).collect::<Vec<_>>().join(" ");
            (
                format!("SELECT {cols} FROM pois_fts f JOIN pois p ON f.rowid = p.id WHERE pois_fts MATCH ? LIMIT ?"),
                vec![Value::from(fts), Value::from(candidates)],
            )
        };
        let mut rows = Self::run(&conn, &sql, args)?;

        if rows.is_empty() && !GROUPS.iter().any(|g| g.words.contains(&phrase.as_str())) {
            // FTS found nothing: substring match on every word.
            let like = vec!["p.name LIKE ? ESCAPE '\\'"; toks.len()].join(" AND ");
            let mut args: Vec<Value> = toks.iter().map(|t| Value::from(format!("%{}%", escape_like(t)))).collect();
            args.push(Value::from(candidates));
            rows = Self::run(&conn, &format!("SELECT {cols} FROM pois p WHERE {like} LIMIT ?"), args)?;
        }
        drop(conn);

        let mut hits: Vec<SearchHit> = rows
            .into_iter()
            .map(|poi| {
                let distance_m = near.map(|n| haversine_m(n, (poi.lat, poi.lon)));
                SearchHit { poi, distance_m }
            })
            .collect();
        let starts = |h: &SearchHit| !h.poi.name.to_lowercase().starts_with(&toks[0]);
        hits.sort_by(|a, b| {
            let (da, db) = (a.distance_m.unwrap_or(f64::MAX), b.distance_m.unwrap_or(f64::MAX));
            da.total_cmp(&db).then(starts(a).cmp(&starts(b))).then(a.poi.name.len().cmp(&b.poi.name.len()))
        });
        hits.truncate(limit);
        Ok(hits)
    }

    fn run(conn: &Connection, sql: &str, args: Vec<Value>) -> Result<Vec<Poi>, SearchError> {
        let mut stmt = conn.prepare_cached(sql)?;
        let rows = stmt.query_map(params_from_iter(args), |r| {
            Ok(Poi {
                id: r.get(0)?,
                name: r.get(1)?,
                category: r.get(2)?,
                lat: r.get(3)?,
                lon: r.get(4)?,
                admin: Admin { barangay: r.get(5)?, municipality: r.get(6)?, province: r.get(7)? },
            })
        })?;
        Ok(rows.collect::<Result<_, _>>()?)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn db(admin: bool) -> PoiDb {
        let c = Connection::open_in_memory().unwrap();
        let extra = if admin { ", barangay TEXT, municipality TEXT, province TEXT" } else { "" };
        c.execute_batch(&format!(
            "CREATE TABLE pois(id INTEGER PRIMARY KEY, name TEXT, category TEXT, latitude REAL, longitude REAL{extra});
             CREATE VIRTUAL TABLE pois_fts USING fts5(name, category);"
        ))
        .unwrap();
        let rows = [
            ("Manggahan", "PLACE", 14.58, 121.09, "Manggahan", "City of Pasig", "Metro Manila"),
            ("Manggahan", "PLACE", 14.34, 120.89, "Manggahan", "General Trias", "Cavite"),
            ("Petron Ayala", "Fuel", 14.55, 121.02, "Makati", "Makati City", "Metro Manila"),
            ("Gaswiling", "PLACE", 15.0, 120.0, "X", "Y", "Z"),
            ("100% Pure_Gold", "SHOP", 14.0, 121.0, "A", "B", "C"),
        ];
        for (n, cat, la, lo, b, m, p) in rows {
            if admin {
                c.execute("INSERT INTO pois(name,category,latitude,longitude,barangay,municipality,province) VALUES(?,?,?,?,?,?,?)", rusqlite::params![n, cat, la, lo, b, m, p]).unwrap();
            } else {
                c.execute("INSERT INTO pois(name,category,latitude,longitude) VALUES(?,?,?,?)", rusqlite::params![n, cat, la, lo]).unwrap();
            }
            c.execute("INSERT INTO pois_fts(rowid,name,category) VALUES(last_insert_rowid(),?,?)", rusqlite::params![n, cat]).unwrap();
        }
        PoiDb::from_connection(c).unwrap()
    }

    #[test]
    fn both_manggahans_come_back_with_their_cities() {
        let d = db(true);
        let hits = d.search("Manggahan", None, 10).unwrap();
        let labels: Vec<_> = hits.iter().map(|h| h.poi.label()).collect();
        assert!(labels.contains(&"Manggahan, Pasig City".to_owned()));
        assert!(labels.contains(&"Manggahan, General Trias".to_owned()));
    }

    #[test]
    fn nearest_first_when_location_known() {
        let d = db(true);
        let hits = d.search("manggahan", Some((14.34, 120.89)), 10).unwrap();
        assert_eq!(hits[0].poi.admin.province.as_deref(), Some("Cavite"));
        assert!(hits[0].distance_m.unwrap() < 1.0);
    }

    #[test]
    fn category_word_uses_category_not_substring() {
        let d = db(true);
        let names: Vec<_> = d.search("gas", None, 10).unwrap().into_iter().map(|h| h.poi.name).collect();
        assert_eq!(names, vec!["Petron Ayala"]); // not "Gaswiling"
    }

    #[test]
    fn works_without_admin_columns_and_survives_hostile_input() {
        let d = db(false);
        assert!(!d.has_admin());
        assert_eq!(d.search("manggahan", None, 10).unwrap().len(), 2);
        for q in ["", "   ", "\"", "a OR b", "* ) (", "'; DROP TABLE pois;--", "%", &"x".repeat(5000)] {
            assert!(d.search(q, None, 5).is_ok(), "query {q:?}");
        }
        assert_eq!(d.search("%", None, 5).unwrap().len(), 0);
        assert_eq!(d.search("100", None, 5).unwrap().len(), 1);
    }

    #[test]
    fn limit_is_clamped_and_missing_table_is_an_error() {
        assert!(db(true).search("manggahan", None, 0).unwrap().len() == 1);
        let c = Connection::open_in_memory().unwrap();
        assert!(matches!(PoiDb::from_connection(c), Err(SearchError::NoPoisTable)));
        assert!(matches!(PoiDb::open(std::path::Path::new("/nope/x.sqlite")), Err(SearchError::Open(..))));
    }
}

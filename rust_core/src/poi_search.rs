// rust_core/src/poi_search.rs
// Offline SQLite FTS5 landmark, barangay, and POI search for Gabay
// Features: Category-specific filtering, distance-based sorting, and bounding box spatial querying

use crate::types::{LatLng, PoiSearchResult};
use rusqlite::{params, Connection, Result};
use std::path::Path;

pub struct PoiSearch {
    db_path: String,
}

impl PoiSearch {
    pub fn new<P: AsRef<Path>>(path: P) -> Self {
        Self {
            db_path: path.as_ref().to_string_lossy().to_string(),
        }
    }

    /// Computes haversine distance in meters
    fn haversine_distance(lat1: f64, lon1: f64, lat2: f64, lon2: f64) -> f64 {
        const EARTH_RADIUS: f64 = 6_371_000.0;
        let d_lat = (lat2 - lat1).to_radians();
        let d_lon = (lon2 - lon1).to_radians();
        let a = (d_lat / 2.0).sin().powi(2)
            + lat1.to_radians().cos() * lat2.to_radians().cos() * (d_lon / 2.0).sin().powi(2);
        2.0 * EARTH_RADIUS * a.sqrt().asin()
    }

    /// Queries landmarks/POIs with category awareness and distance ranking
    pub fn search_around(
        &self,
        query: &str,
        user_lat: Option<f64>,
        user_lon: Option<f64>,
        limit: usize,
    ) -> Result<Vec<PoiSearchResult>> {
        if !Path::new(&self.db_path).exists() {
            return Ok(Vec::new());
        }

        let conn = Connection::open(&self.db_path)?;
        let q_clean = query.trim();
        let q_lower = q_clean.to_lowercase();

        let mut results = Vec::new();

        // 1. Category-specific queries (prevent "Gas" matching locality "Gaswiling")
        let category_sql = match q_lower.as_str() {
            "gas" | "fuel" | "gas station" | "gas stations" | "category:gas" | "category:gas_station" => {
                Some("SELECT id, name, category, latitude, longitude FROM pois WHERE category IN ('GAS_STATION', 'FUEL', 'GAS') OR name LIKE '%Petron%' OR name LIKE '%Shell%' OR name LIKE '%Caltex%' OR name LIKE '%Cleanfuel%' OR name LIKE '%Phoenix%' OR name LIKE '%Seaoil%' OR name LIKE '%PTT%' OR name LIKE '%Total%' OR name LIKE '%Gasoline%' OR name LIKE '%Gas Station%' LIMIT 100")
            }
            "hospital" | "hospitals" | "clinic" | "clinics" | "category:hospital" => {
                Some("SELECT id, name, category, latitude, longitude FROM pois WHERE category IN ('HOSPITAL', 'CLINIC') OR name LIKE '%Hospital%' OR name LIKE '%Clinic%' OR name LIKE '%Medical%' LIMIT 100")
            }
            "school" | "schools" | "college" | "university" | "colleges" | "category:school" => {
                Some("SELECT id, name, category, latitude, longitude FROM pois WHERE category IN ('SCHOOL', 'UNIVERSITY', 'COLLEGE') OR name LIKE '%School%' OR name LIKE '%College%' OR name LIKE '%University%' OR name LIKE '%Academy%' OR name LIKE '%NEUST%' LIMIT 100")
            }
            "food" | "restaurant" | "restaurants" | "fast food" | "category:food" => {
                Some("SELECT id, name, category, latitude, longitude FROM pois WHERE category IN ('RESTAURANT', 'FAST_FOOD', 'CAFE') OR name LIKE '%Jollibee%' OR name LIKE '%McDonald%' OR name LIKE '%Inasal%' OR name LIKE '%KFC%' OR name LIKE '%Restaurant%' LIMIT 100")
            }
            "civic" | "government" | "city hall" | "municipal hall" | "barangay" | "category:civic" => {
                Some("SELECT id, name, category, latitude, longitude FROM pois WHERE category IN ('GOVERNMENT', 'CIVIC', 'TOWN_HALL', 'POLICE') OR name LIKE '%Municipal Hall%' OR name LIKE '%City Hall%' OR name LIKE '%Barangay%' OR name LIKE '%Police%' LIMIT 100")
            }
            "market" | "markets" | "mall" | "malls" | "supermarket" | "category:market" => {
                Some("SELECT id, name, category, latitude, longitude FROM pois WHERE category IN ('MARKET', 'SUPERMARKET', 'MALL', 'COMMERCIAL') OR name LIKE '%Market%' OR name LIKE '%Mall%' OR name LIKE '%WalterMart%' OR name LIKE '%Robinsons%' OR name LIKE '%SM%' LIMIT 100")
            }
            _ => None,
        };

        if let Some(sql) = category_sql {
            if let Ok(mut stmt) = conn.prepare(sql) {
                if let Ok(rows) = stmt.query_map([], |row| {
                    Ok((
                        row.get::<_, i64>(0)?,
                        row.get::<_, String>(1)?,
                        row.get::<_, String>(2)?,
                        row.get::<_, f64>(3)?,
                        row.get::<_, f64>(4)?,
                    ))
                }) {
                    for r in rows.flatten() {
                        let (id, name, category, lat, lon) = r;
                        let dist = match (user_lat, user_lon) {
                            (Some(ulat), Some(ulon)) => Self::haversine_distance(ulat, ulon, lat, lon),
                            _ => 0.0,
                        };
                        results.push(PoiSearchResult {
                            id,
                            name,
                            category,
                            coordinate: LatLng { latitude: lat, longitude: lon },
                            distance_meters: dist,
                        });
                    }
                }
            }
        } else {
            // General query: Try FTS5 match
            let candidate_limit = (limit.max(5) * 10) as i64;
            if let Ok(mut stmt) = conn.prepare(
                "SELECT p.id, p.name, p.category, p.latitude, p.longitude 
                 FROM pois_fts f
                 JOIN pois p ON f.rowid = p.id
                 WHERE pois_fts MATCH ?1 
                 LIMIT ?2",
            ) {
                if let Ok(rows) = stmt.query_map(params![formatted_query, candidate_limit], |row| {
                    Ok((
                        row.get::<_, i64>(0)?,
                        row.get::<_, String>(1)?,
                        row.get::<_, String>(2)?,
                        row.get::<_, f64>(3)?,
                        row.get::<_, f64>(4)?,
                    ))
                }) {
                    for r in rows.flatten() {
                        let (id, name, category, lat, lon) = r;
                        let dist = match (user_lat, user_lon) {
                            (Some(ulat), Some(ulon)) => Self::haversine_distance(ulat, ulon, lat, lon),
                            _ => 0.0,
                        };
                        results.push(PoiSearchResult {
                            id,
                            name,
                            category,
                            coordinate: LatLng { latitude: lat, longitude: lon },
                            distance_meters: dist,
                        });
                    }
                }
            }

            // Resilient Fallback: If FTS5 yields 0 results, query table pois directly with LIKE
            if results.is_empty() {
                let like_query = format!("%{}%", q_clean);
                if let Ok(mut fallback_stmt) = conn.prepare(
                    "SELECT id, name, category, latitude, longitude 
                     FROM pois 
                     WHERE name LIKE ?1 OR category LIKE ?1 
                     LIMIT ?2",
                ) {
                    if let Ok(fallback_rows) = fallback_stmt.query_map(params![like_query, candidate_limit], |row| {
                        Ok((
                            row.get::<_, i64>(0)?,
                            row.get::<_, String>(1)?,
                            row.get::<_, String>(2)?,
                            row.get::<_, f64>(3)?,
                            row.get::<_, f64>(4)?,
                        ))
                    }) {
                        for r in fallback_rows.flatten() {
                            let (id, name, category, lat, lon) = r;
                            let dist = match (user_lat, user_lon) {
                                (Some(ulat), Some(ulon)) => Self::haversine_distance(ulat, ulon, lat, lon),
                                _ => 0.0,
                            };
                            results.push(PoiSearchResult {
                                id,
                                name,
                                category,
                                coordinate: LatLng { latitude: lat, longitude: lon },
                                distance_meters: dist,
                            });
                        }
                    }
                }
            }
        }

        // Sort by distance if user location is available
        if user_lat.is_some() && user_lon.is_some() {
            results.sort_by(|a, b| a.distance_meters.partial_cmp(&b.distance_meters).unwrap_or(std::cmp::Ordering::Equal));
        }

        results.truncate(limit);
        Ok(results)
    }

    /// Backward-compatible search
    pub fn search(&self, query: &str, limit: usize) -> Result<Vec<PoiSearchResult>> {
        self.search_around(query, None, None, limit)
    }

    /// Queries all POIs inside a bounding box for rendering on MapCanvas
    pub fn get_pois_in_bbox(
        &self,
        min_lat: f64,
        max_lat: f64,
        min_lon: f64,
        max_lon: f64,
        limit: usize,
    ) -> Result<Vec<PoiSearchResult>> {
        if !Path::new(&self.db_path).exists() {
            return Ok(Vec::new());
        }

        let conn = Connection::open(&self.db_path)?;
        let mut stmt = conn.prepare(
            "SELECT id, name, category, latitude, longitude 
             FROM pois 
             WHERE latitude BETWEEN ?1 AND ?2 AND longitude BETWEEN ?3 AND ?4
             LIMIT ?5",
        )?;

        let rows = stmt.query_map(params![min_lat, max_lat, min_lon, max_lon, limit as i64], |row| {
            Ok(PoiSearchResult {
                id: row.get(0)?,
                name: row.get(1)?,
                category: row.get(2)?,
                coordinate: LatLng {
                    latitude: row.get(3)?,
                    longitude: row.get(4)?,
                },
                distance_meters: 0.0,
            })
        })?;

        let mut results = Vec::new();
        for r in rows.flatten() {
            results.push(r);
        }
        Ok(results)
    }
}

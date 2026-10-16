// rust_core/src/poi_search.rs
// Offline SQLite FTS5 landmark, barangay, and POI search for Gabay

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

    /// Initializes FTS5 tables if database is newly created
    pub fn init_db(&self) -> Result<()> {
        let conn = Connection::open(&self.db_path)?;
        conn.execute(
            "CREATE TABLE IF NOT EXISTS pois (
                id INTEGER PRIMARY KEY,
                name TEXT NOT NULL,
                category TEXT NOT NULL,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL
            )",
            [],
        )?;

        conn.execute(
            "CREATE VIRTUAL TABLE IF NOT EXISTS pois_fts USING fts5(
                name,
                category,
                content='pois',
                content_rowid='id'
            )",
            [],
        )?;

        Ok(())
    }

    /// Queries landmarks/POIs using FTS5 full-text matching
    pub fn search(&self, query: &str, limit: usize) -> Result<Vec<PoiSearchResult>> {
        if !Path::new(&self.db_path).exists() {
            return Ok(Vec::new());
        }

        let conn = Connection::open(&self.db_path)?;
        let mut stmt = conn.prepare(
            "SELECT p.id, p.name, p.category, p.latitude, p.longitude 
             FROM pois_fts f
             JOIN pois p ON f.rowid = p.id
             WHERE pois_fts MATCH ?1
             LIMIT ?2",
        )?;

        let formatted_query = format!("{}*", query.trim());
        let rows = stmt.query_map(params![formatted_query, limit as i64], |row| {
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
        for r in rows {
            if let Ok(item) = r {
                results.push(item);
            }
        }
        Ok(results)
    }
}

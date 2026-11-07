//! Vector tile source: PMTiles archive + decode + a small bounded cache.

use std::collections::{HashMap, VecDeque};
use std::sync::{Arc, Mutex};

use gabay_vault::PmTiles;

use crate::geo::{Bounds, TileId};
use crate::mvt::{self, Layer};

const CACHE_CAPACITY: usize = 128;

struct Cache {
    map: HashMap<TileId, Option<Arc<Vec<Layer>>>>,
    order: VecDeque<TileId>,
}

pub struct VectorSource {
    archive: PmTiles,
    cache: Mutex<Cache>,
}

impl VectorSource {
    pub fn new(archive: PmTiles) -> Self {
        Self {
            archive,
            cache: Mutex::new(Cache { map: HashMap::new(), order: VecDeque::new() }),
        }
    }

    pub fn zoom_range(&self) -> (u8, u8) {
        let h = self.archive.header();
        (h.min_zoom, h.max_zoom)
    }

    /// The archive's own bounding box, or the Philippines when it is invalid.
    pub fn bounds(&self) -> Bounds {
        let h = self.archive.header();
        Bounds { min_lon: h.min_lon, min_lat: h.min_lat, max_lon: h.max_lon, max_lat: h.max_lat }
            .validated()
            .unwrap_or(Bounds::PHILIPPINES)
    }

    /// `None` for a missing or undecodable tile. Failures are logged and
    /// cached so a bad tile is not re-read every frame.
    pub fn tile(&self, id: TileId) -> Option<Arc<Vec<Layer>>> {
        if let Ok(cache) = self.cache.lock() {
            if let Some(hit) = cache.map.get(&id) {
                return hit.clone();
            }
        }
        let loaded = self.load(id);
        if let Ok(mut cache) = self.cache.lock() {
            if cache.order.len() >= CACHE_CAPACITY {
                if let Some(old) = cache.order.pop_front() {
                    cache.map.remove(&old);
                }
            }
            if cache.map.insert(id, loaded.clone()).is_none() {
                cache.order.push_back(id);
            }
        }
        loaded
    }

    fn load(&self, id: TileId) -> Option<Arc<Vec<Layer>>> {
        match self.archive.get_tile_decoded(id.z, id.x, id.y) {
            Ok(Some(bytes)) => match mvt::decode(&bytes) {
                Ok(layers) => Some(Arc::new(layers)),
                Err(e) => {
                    log::warn!("tile {id:?}: {e}");
                    None
                }
            },
            Ok(None) => None,
            Err(e) => {
                log::warn!("tile {id:?}: {e}");
                None
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use gabay_vault::MemSource;

    #[test]
    fn invalid_archive_is_an_error_and_bounds_fall_back() {
        assert!(PmTiles::from_source(Box::new(MemSource(vec![0; 200]))).is_err());
        // Bounds fallback is covered in geo::tests (validated()).
    }
}

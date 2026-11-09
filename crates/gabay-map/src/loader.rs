//! Vector tile source: PMTiles archive + decode + a small bounded cache.

use std::collections::{HashMap, HashSet, VecDeque};
use std::sync::{Arc, Mutex};

use gabay_vault::PmTiles;

use crate::geo::{Bounds, TileId};
use crate::mvt::{self, Layer};

const CACHE_CAPACITY: usize = 256;

struct Cache {
    map: HashMap<TileId, Option<Arc<Vec<Layer>>>>,
    order: VecDeque<TileId>,
    in_flight: HashSet<TileId>,
}

pub struct VectorSource {
    archive: Arc<PmTiles>,
    cache: Arc<Mutex<Cache>>,
}

impl VectorSource {
    pub fn new(archive: PmTiles) -> Self {
        Self {
            archive: Arc::new(archive),
            cache: Arc::new(Mutex::new(Cache {
                map: HashMap::new(),
                order: VecDeque::new(),
                in_flight: HashSet::new(),
            })),
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

    /// `None` for a missing or undecodable tile. Loads asynchronously on
    /// background threads so the UI render thread never blocks on disk I/O or gzip decompression.
    pub fn tile(&self, id: TileId) -> Option<Arc<Vec<Layer>>> {
        if let Ok(mut cache) = self.cache.lock() {
            if let Some(hit) = cache.map.get(&id) {
                return hit.clone();
            }
            if cache.in_flight.contains(&id) {
                return None;
            }
            cache.in_flight.insert(id);
        }

        let archive = self.archive.clone();
        let cache = self.cache.clone();
        std::thread::Builder::new()
            .name(format!("tile-{id:?}"))
            .spawn(move || {
                let loaded = Self::load_tile(&archive, id);
                if let Ok(mut c) = cache.lock() {
                    c.in_flight.remove(&id);
                    if c.order.len() >= CACHE_CAPACITY {
                        if let Some(old) = c.order.pop_front() {
                            c.map.remove(&old);
                        }
                    }
                    if c.map.insert(id, loaded).is_none() {
                        c.order.push_back(id);
                    }
                }
            })
            .ok();

        None
    }

    fn load_tile(archive: &PmTiles, id: TileId) -> Option<Arc<Vec<Layer>>> {
        match archive.get_tile_decoded(id.z, id.x, id.y) {
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

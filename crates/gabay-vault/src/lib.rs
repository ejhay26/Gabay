//! Read-only access to the offline vault (`/sdcard/Documents/Gabay/`).
//!
//! Contains a PMTiles v3 reader. No `unsafe`, no memory mapping: reads go
//! through a `ByteSource`, so tests can run on in-memory archives.

use std::collections::HashMap;
use std::fs::File;
use std::io::{Read, Seek, SeekFrom};
use std::path::{Path, PathBuf};
use std::sync::{Arc, Mutex};

use flate2::read::GzDecoder;

/// Upper bounds that keep a corrupt or hostile archive from exhausting memory.
const MAX_DIR_BYTES: u64 = 64 * 1024 * 1024;
const MAX_TILE_BYTES: u64 = 16 * 1024 * 1024;
const MAX_DIR_DEPTH: usize = 4;
const MAX_CACHED_DIRS: usize = 64;
const HEADER_LEN: usize = 127;

#[derive(Debug, thiserror::Error)]
pub enum VaultError {
    #[error("i/o error: {0}")]
    Io(#[from] std::io::Error),
    #[error("not a PMTiles archive")]
    BadMagic,
    #[error("unsupported PMTiles version {0}")]
    UnsupportedVersion(u8),
    #[error("corrupt archive: {0}")]
    Corrupt(&'static str),
    #[error("tile coordinate out of range")]
    BadCoordinate,
}

pub type Result<T> = std::result::Result<T, VaultError>;

// ---------------------------------------------------------------- byte sources

pub trait ByteSource: Send + Sync {
    fn read_at(&self, offset: u64, len: usize) -> Result<Vec<u8>>;
}

pub struct FileSource {
    file: Mutex<File>,
    len: u64,
}

impl FileSource {
    pub fn open(path: &Path) -> Result<Self> {
        let file = File::open(path)?;
        let len = file.metadata()?.len();
        Ok(Self { file: Mutex::new(file), len })
    }
}

impl ByteSource for FileSource {
    fn read_at(&self, offset: u64, len: usize) -> Result<Vec<u8>> {
        let end = offset.checked_add(len as u64).ok_or(VaultError::Corrupt("offset overflow"))?;
        if end > self.len {
            return Err(VaultError::Corrupt("read past end of file"));
        }
        let mut file = self.file.lock().map_err(|_| VaultError::Corrupt("file lock poisoned"))?;
        file.seek(SeekFrom::Start(offset))?;
        let mut buf = vec![0u8; len];
        file.read_exact(&mut buf)?;
        Ok(buf)
    }
}

pub struct MemSource(pub Vec<u8>);

impl ByteSource for MemSource {
    fn read_at(&self, offset: u64, len: usize) -> Result<Vec<u8>> {
        let start = usize::try_from(offset).map_err(|_| VaultError::Corrupt("offset"))?;
        let end = start.checked_add(len).ok_or(VaultError::Corrupt("offset overflow"))?;
        self.0.get(start..end).map(<[u8]>::to_vec).ok_or(VaultError::Corrupt("read past end"))
    }
}

// ---------------------------------------------------------------------- header

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum Compression {
    Unknown,
    None,
    Gzip,
    Other(u8),
}

impl Compression {
    fn from_u8(v: u8) -> Self {
        match v {
            0 => Self::Unknown,
            1 => Self::None,
            2 => Self::Gzip,
            o => Self::Other(o),
        }
    }
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum TileType {
    Unknown,
    Mvt,
    Png,
    Jpeg,
    Webp,
    Avif,
    Other(u8),
}

impl TileType {
    fn from_u8(v: u8) -> Self {
        match v {
            0 => Self::Unknown,
            1 => Self::Mvt,
            2 => Self::Png,
            3 => Self::Jpeg,
            4 => Self::Webp,
            5 => Self::Avif,
            o => Self::Other(o),
        }
    }
}

#[derive(Clone, Debug, PartialEq)]
pub struct Header {
    pub root_dir_offset: u64,
    pub root_dir_length: u64,
    pub metadata_offset: u64,
    pub metadata_length: u64,
    pub leaf_dirs_offset: u64,
    pub leaf_dirs_length: u64,
    pub tile_data_offset: u64,
    pub tile_data_length: u64,
    pub internal_compression: Compression,
    pub tile_compression: Compression,
    pub tile_type: TileType,
    pub min_zoom: u8,
    pub max_zoom: u8,
    /// Degrees, WGS84.
    pub min_lon: f64,
    pub min_lat: f64,
    pub max_lon: f64,
    pub max_lat: f64,
}

fn le_u64(b: &[u8], at: usize) -> u64 {
    let mut a = [0u8; 8];
    a.copy_from_slice(&b[at..at + 8]);
    u64::from_le_bytes(a)
}

fn le_i32_e7(b: &[u8], at: usize) -> f64 {
    let mut a = [0u8; 4];
    a.copy_from_slice(&b[at..at + 4]);
    f64::from(i32::from_le_bytes(a)) / 1e7
}

impl Header {
    pub fn parse(b: &[u8]) -> Result<Self> {
        if b.len() < HEADER_LEN {
            return Err(VaultError::Corrupt("header too short"));
        }
        if &b[0..7] != b"PMTiles" {
            return Err(VaultError::BadMagic);
        }
        if b[7] != 3 {
            return Err(VaultError::UnsupportedVersion(b[7]));
        }
        Ok(Self {
            root_dir_offset: le_u64(b, 8),
            root_dir_length: le_u64(b, 16),
            metadata_offset: le_u64(b, 24),
            metadata_length: le_u64(b, 32),
            leaf_dirs_offset: le_u64(b, 40),
            leaf_dirs_length: le_u64(b, 48),
            tile_data_offset: le_u64(b, 56),
            tile_data_length: le_u64(b, 64),
            internal_compression: Compression::from_u8(b[97]),
            tile_compression: Compression::from_u8(b[98]),
            tile_type: TileType::from_u8(b[99]),
            min_zoom: b[100],
            max_zoom: b[101],
            min_lon: le_i32_e7(b, 102),
            min_lat: le_i32_e7(b, 106),
            max_lon: le_i32_e7(b, 110),
            max_lat: le_i32_e7(b, 114),
        })
    }
}

// ----------------------------------------------------------------- tile ids

/// Hilbert-curve tile id used by PMTiles v3 (zoom levels are concatenated).
pub fn zxy_to_tileid(z: u8, x: u32, y: u32) -> Result<u64> {
    if z > 26 {
        return Err(VaultError::BadCoordinate);
    }
    let n: u64 = 1u64 << z;
    if u64::from(x) >= n || u64::from(y) >= n {
        return Err(VaultError::BadCoordinate);
    }
    // Number of tiles in all lower zoom levels: (4^z - 1) / 3.
    let mut acc: u64 = ((1u64 << (2 * u32::from(z))) - 1) / 3;
    let (mut tx, mut ty) = (u64::from(x), u64::from(y));
    let mut s = n / 2;
    while s > 0 {
        let rx = u64::from(tx & s > 0);
        let ry = u64::from(ty & s > 0);
        acc += s * s * ((3 * rx) ^ ry);
        if ry == 0 {
            if rx == 1 {
                tx = n - 1 - tx;
                ty = n - 1 - ty;
            }
            std::mem::swap(&mut tx, &mut ty);
        }
        s /= 2;
    }
    Ok(acc)
}

// -------------------------------------------------------------- directories

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
struct Entry {
    tile_id: u64,
    offset: u64,
    length: u64,
    /// 0 means this entry points at a leaf directory.
    run_length: u64,
}

fn read_varint(buf: &[u8], pos: &mut usize) -> Result<u64> {
    let mut result = 0u64;
    let mut shift = 0u32;
    loop {
        let byte = *buf.get(*pos).ok_or(VaultError::Corrupt("varint truncated"))?;
        *pos += 1;
        result |= u64::from(byte & 0x7f) << shift;
        if byte & 0x80 == 0 {
            return Ok(result);
        }
        shift += 7;
        if shift > 63 {
            return Err(VaultError::Corrupt("varint too long"));
        }
    }
}

fn parse_directory(buf: &[u8]) -> Result<Vec<Entry>> {
    let mut pos = 0usize;
    let count = read_varint(buf, &mut pos)?;
    // Every entry needs at least four bytes, so this also bounds the allocation.
    if count > (buf.len() as u64) / 4 + 1 {
        return Err(VaultError::Corrupt("directory entry count"));
    }
    let count = count as usize;
    let mut entries = vec![Entry { tile_id: 0, offset: 0, length: 0, run_length: 0 }; count];

    let mut last_id = 0u64;
    for e in entries.iter_mut() {
        last_id = last_id.checked_add(read_varint(buf, &mut pos)?).ok_or(VaultError::Corrupt("tile id"))?;
        e.tile_id = last_id;
    }
    for e in entries.iter_mut() {
        e.run_length = read_varint(buf, &mut pos)?;
    }
    for e in entries.iter_mut() {
        e.length = read_varint(buf, &mut pos)?;
    }
    for i in 0..count {
        let v = read_varint(buf, &mut pos)?;
        entries[i].offset = if v == 0 {
            if i == 0 {
                return Err(VaultError::Corrupt("first offset is zero"));
            }
            let prev = entries[i - 1];
            prev.offset.checked_add(prev.length).ok_or(VaultError::Corrupt("offset overflow"))?
        } else {
            v - 1
        };
    }
    Ok(entries)
}

fn find_entry(entries: &[Entry], id: u64) -> Option<Entry> {
    match entries.binary_search_by(|e| e.tile_id.cmp(&id)) {
        Ok(i) => Some(entries[i]),
        Err(0) => None,
        Err(i) => {
            let e = entries[i - 1];
            if e.run_length == 0 || id - e.tile_id < e.run_length {
                Some(e)
            } else {
                None
            }
        }
    }
}

fn gunzip(bytes: &[u8], limit: u64) -> Result<Vec<u8>> {
    let mut out = Vec::new();
    GzDecoder::new(bytes).take(limit + 1).read_to_end(&mut out)?;
    if out.len() as u64 > limit {
        return Err(VaultError::Corrupt("decompressed data too large"));
    }
    Ok(out)
}

// ------------------------------------------------------------------ archive

pub struct PmTiles {
    source: Box<dyn ByteSource>,
    header: Header,
    dirs: Mutex<HashMap<u64, Arc<Vec<Entry>>>>,
}

impl PmTiles {
    pub fn open(path: &Path) -> Result<Self> {
        Self::from_source(Box::new(FileSource::open(path)?))
    }

    pub fn from_source(source: Box<dyn ByteSource>) -> Result<Self> {
        let header = Header::parse(&source.read_at(0, HEADER_LEN)?)?;
        Ok(Self { source, header, dirs: Mutex::new(HashMap::new()) })
    }

    pub fn header(&self) -> &Header {
        &self.header
    }

    /// JSON metadata document, decompressed. Empty when the archive has none.
    pub fn metadata_json(&self) -> Result<String> {
        let h = &self.header;
        if h.metadata_length == 0 {
            return Ok(String::new());
        }
        if h.metadata_length > MAX_DIR_BYTES {
            return Err(VaultError::Corrupt("metadata too large"));
        }
        let raw = self.source.read_at(h.metadata_offset, h.metadata_length as usize)?;
        let bytes = self.decode_internal(raw)?;
        String::from_utf8(bytes).map_err(|_| VaultError::Corrupt("metadata is not utf-8"))
    }

    fn decode_internal(&self, raw: Vec<u8>) -> Result<Vec<u8>> {
        match self.header.internal_compression {
            Compression::None | Compression::Unknown => Ok(raw),
            Compression::Gzip => gunzip(&raw, MAX_DIR_BYTES),
            Compression::Other(_) => Err(VaultError::Corrupt("unsupported internal compression")),
        }
    }

    fn directory(&self, offset: u64, length: u64) -> Result<Arc<Vec<Entry>>> {
        if let Ok(cache) = self.dirs.lock() {
            if let Some(hit) = cache.get(&offset) {
                return Ok(Arc::clone(hit));
            }
        }
        if length == 0 || length > MAX_DIR_BYTES {
            return Err(VaultError::Corrupt("directory length"));
        }
        let raw = self.source.read_at(offset, length as usize)?;
        let entries = Arc::new(parse_directory(&self.decode_internal(raw)?)?);
        if let Ok(mut cache) = self.dirs.lock() {
            if cache.len() >= MAX_CACHED_DIRS {
                cache.clear();
            }
            cache.insert(offset, Arc::clone(&entries));
        }
        Ok(entries)
    }

    /// Raw tile bytes as stored (still compressed per `tile_compression`).
    /// `Ok(None)` means the archive has no such tile.
    pub fn get_tile(&self, z: u8, x: u32, y: u32) -> Result<Option<Vec<u8>>> {
        let id = zxy_to_tileid(z, x, y)?;
        let h = &self.header;
        let (mut offset, mut length) = (h.root_dir_offset, h.root_dir_length);
        for _ in 0..MAX_DIR_DEPTH {
            let dir = self.directory(offset, length)?;
            let Some(entry) = find_entry(&dir, id) else {
                return Ok(None);
            };
            if entry.run_length > 0 {
                if entry.length > MAX_TILE_BYTES {
                    return Err(VaultError::Corrupt("tile too large"));
                }
                let start = h
                    .tile_data_offset
                    .checked_add(entry.offset)
                    .ok_or(VaultError::Corrupt("tile offset"))?;
                return self.source.read_at(start, entry.length as usize).map(Some);
            }
            offset = h
                .leaf_dirs_offset
                .checked_add(entry.offset)
                .ok_or(VaultError::Corrupt("leaf offset"))?;
            length = entry.length;
        }
        Err(VaultError::Corrupt("directory nesting too deep"))
    }

    /// Tile bytes with `tile_compression` removed, ready for a decoder.
    pub fn get_tile_decoded(&self, z: u8, x: u32, y: u32) -> Result<Option<Vec<u8>>> {
        let Some(raw) = self.get_tile(z, x, y)? else {
            return Ok(None);
        };
        match self.header.tile_compression {
            Compression::Gzip => gunzip(&raw, MAX_TILE_BYTES * 4).map(Some),
            Compression::None | Compression::Unknown => Ok(Some(raw)),
            Compression::Other(_) => Err(VaultError::Corrupt("unsupported tile compression")),
        }
    }
}

// -------------------------------------------------------------------- vault

#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash)]
pub enum Layer {
    Vector,
    Satellite,
    Topography,
}

impl Layer {
    pub const fn file_name(self) -> &'static str {
        match self {
            Layer::Vector => "philippines_vector.pmtiles",
            Layer::Satellite => "satellite_base.pmtiles",
            Layer::Topography => "topography_contours.pmtiles",
        }
    }
}

pub const GRAPH_FILE: &str = "ph.gabay";
pub const POI_FILE: &str = "pois_fts.sqlite";
pub const MODEL_FILE: &str = "model.gguf";

#[derive(Clone, Debug, PartialEq, Eq)]
pub enum FileStatus {
    Present { bytes: u64 },
    Missing,
    Unreadable(String),
}

pub struct Vault {
    dir: PathBuf,
}

impl Vault {
    pub fn new(dir: impl Into<PathBuf>) -> Self {
        Self { dir: dir.into() }
    }

    pub fn dir(&self) -> &Path {
        &self.dir
    }

    pub fn path_of(&self, name: &str) -> PathBuf {
        self.dir.join(name)
    }

    pub fn status_of(&self, name: &str) -> FileStatus {
        match std::fs::metadata(self.path_of(name)) {
            Ok(m) if m.is_file() => FileStatus::Present { bytes: m.len() },
            Ok(_) => FileStatus::Unreadable("not a regular file".into()),
            Err(e) if e.kind() == std::io::ErrorKind::NotFound => FileStatus::Missing,
            Err(e) => FileStatus::Unreadable(e.to_string()),
        }
    }

    /// Every file the app uses, in display order.
    pub fn report(&self) -> Vec<(&'static str, FileStatus)> {
        [
            GRAPH_FILE,
            POI_FILE,
            Layer::Vector.file_name(),
            Layer::Satellite.file_name(),
            Layer::Topography.file_name(),
            MODEL_FILE,
        ]
        .into_iter()
        .map(|name| (name, self.status_of(name)))
        .collect()
    }

    /// `Ok(None)` when the layer file is absent.
    pub fn open_layer(&self, layer: Layer) -> Result<Option<PmTiles>> {
        let path = self.path_of(layer.file_name());
        if !path.is_file() {
            return Ok(None);
        }
        PmTiles::open(&path).map(Some)
    }
}

// -------------------------------------------------------------------- tests

#[cfg(test)]
mod tests {
    use super::*;
    use flate2::write::GzEncoder;
    use flate2::Compression as Level;
    use std::io::Write;

    fn varint(mut v: u64, out: &mut Vec<u8>) {
        loop {
            let b = (v & 0x7f) as u8;
            v >>= 7;
            if v == 0 {
                out.push(b);
                return;
            }
            out.push(b | 0x80);
        }
    }

    fn gzip(data: &[u8]) -> Vec<u8> {
        let mut e = GzEncoder::new(Vec::new(), Level::default());
        e.write_all(data).unwrap();
        e.finish().unwrap()
    }

    /// (tile_id, run_length, length, offset) -> directory bytes (uncompressed).
    fn encode_dir(entries: &[(u64, u64, u64, u64)]) -> Vec<u8> {
        let mut out = Vec::new();
        varint(entries.len() as u64, &mut out);
        let mut last = 0;
        for e in entries {
            varint(e.0 - last, &mut out);
            last = e.0;
        }
        for e in entries {
            varint(e.1, &mut out);
        }
        for e in entries {
            varint(e.2, &mut out);
        }
        for (i, e) in entries.iter().enumerate() {
            if i > 0 && e.3 == entries[i - 1].3 + entries[i - 1].2 {
                varint(0, &mut out);
            } else {
                varint(e.3 + 1, &mut out);
            }
        }
        out
    }

    /// Archive with: tile (1,0,0) at id 1 and a run of two ids starting at 2
    /// in the root; tile (2,0,0) (id 5) behind a leaf directory.
    fn build_archive() -> Vec<u8> {
        let tile_a = b"AAAA".to_vec();
        let tile_b = b"BBBBBB".to_vec();
        let tile_c = b"CC".to_vec();
        let mut data = Vec::new();
        data.extend(&tile_a); // offset 0
        data.extend(&tile_b); // offset 4
        data.extend(&tile_c); // offset 10

        let leaf = gzip(&encode_dir(&[(5, 1, 2, 10)]));
        let root = gzip(&encode_dir(&[(1, 1, 4, 0), (2, 2, 6, 4), (5, 0, leaf.len() as u64, 0)]));
        let metadata = gzip(br#"{"name":"test"}"#);

        let mut file = vec![0u8; HEADER_LEN];
        let root_off = file.len() as u64;
        file.extend(&root);
        let meta_off = file.len() as u64;
        file.extend(&metadata);
        let leaf_off = file.len() as u64;
        file.extend(&leaf);
        let data_off = file.len() as u64;
        file.extend(&data);

        file[0..7].copy_from_slice(b"PMTiles");
        file[7] = 3;
        let put = |f: &mut Vec<u8>, at: usize, v: u64| f[at..at + 8].copy_from_slice(&v.to_le_bytes());
        put(&mut file, 8, root_off);
        put(&mut file, 16, root.len() as u64);
        put(&mut file, 24, meta_off);
        put(&mut file, 32, metadata.len() as u64);
        put(&mut file, 40, leaf_off);
        put(&mut file, 48, leaf.len() as u64);
        put(&mut file, 56, data_off);
        put(&mut file, 64, data.len() as u64);
        file[97] = 2; // internal: gzip
        file[98] = 1; // tiles: none
        file[99] = 1; // mvt
        file[100] = 0;
        file[101] = 2;
        file[102..106].copy_from_slice(&1_165_000_000i32.to_le_bytes()); // min lon 116.5
        file[106..110].copy_from_slice(&45_000_000i32.to_le_bytes()); // min lat 4.5
        file[110..114].copy_from_slice(&1_270_000_000i32.to_le_bytes());
        file[114..118].copy_from_slice(&215_000_000i32.to_le_bytes());
        file
    }

    fn open_test() -> PmTiles {
        PmTiles::from_source(Box::new(MemSource(build_archive()))).unwrap()
    }

    #[test]
    fn tile_ids_match_spec_vectors() {
        assert_eq!(zxy_to_tileid(0, 0, 0).unwrap(), 0);
        assert_eq!(zxy_to_tileid(1, 0, 0).unwrap(), 1);
        assert_eq!(zxy_to_tileid(1, 0, 1).unwrap(), 2);
        assert_eq!(zxy_to_tileid(1, 1, 1).unwrap(), 3);
        assert_eq!(zxy_to_tileid(1, 1, 0).unwrap(), 4);
        assert_eq!(zxy_to_tileid(2, 0, 0).unwrap(), 5);
    }

    #[test]
    fn tile_ids_are_unique_through_zoom_four() {
        let mut seen = std::collections::HashSet::new();
        for z in 0..=4u8 {
            for x in 0..(1u32 << z) {
                for y in 0..(1u32 << z) {
                    assert!(seen.insert(zxy_to_tileid(z, x, y).unwrap()));
                }
            }
        }
        assert_eq!(seen.len(), (4usize.pow(5) - 1) / 3);
    }

    #[test]
    fn rejects_out_of_range_coordinates() {
        assert!(zxy_to_tileid(1, 2, 0).is_err());
        assert!(zxy_to_tileid(27, 0, 0).is_err());
    }

    #[test]
    fn header_fields_parse() {
        let t = open_test();
        let h = t.header();
        assert_eq!(h.tile_type, TileType::Mvt);
        assert_eq!(h.internal_compression, Compression::Gzip);
        assert_eq!(h.max_zoom, 2);
        assert!((h.min_lon - 116.5).abs() < 1e-6);
        assert!((h.max_lat - 21.5).abs() < 1e-6);
    }

    #[test]
    fn reads_direct_run_and_leaf_tiles() {
        let t = open_test();
        assert_eq!(t.get_tile(1, 0, 0).unwrap().unwrap(), b"AAAA");
        // ids 2 and 3 are one run sharing the same bytes.
        assert_eq!(t.get_tile(1, 0, 1).unwrap().unwrap(), b"BBBBBB");
        assert_eq!(t.get_tile(1, 1, 1).unwrap().unwrap(), b"BBBBBB");
        // id 5 lives behind a leaf directory.
        assert_eq!(t.get_tile(2, 0, 0).unwrap().unwrap(), b"CC");
    }

    #[test]
    fn missing_tile_is_none_not_error() {
        let t = open_test();
        assert!(t.get_tile(1, 1, 0).unwrap().is_none());
        assert!(t.get_tile(0, 0, 0).unwrap().is_none());
    }

    #[test]
    fn metadata_decompresses() {
        assert_eq!(open_test().metadata_json().unwrap(), r#"{"name":"test"}"#);
    }

    #[test]
    fn bad_magic_and_truncation_are_errors() {
        let mut bad = build_archive();
        bad[0] = b'X';
        assert!(matches!(
            PmTiles::from_source(Box::new(MemSource(bad))),
            Err(VaultError::BadMagic)
        ));
        assert!(PmTiles::from_source(Box::new(MemSource(vec![0; 10]))).is_err());
    }

    #[test]
    fn hostile_directory_count_is_rejected() {
        let mut buf = Vec::new();
        varint(u64::MAX / 2, &mut buf);
        assert!(parse_directory(&buf).is_err());
    }

    #[test]
    fn varint_overflow_is_rejected() {
        let buf = [0xffu8; 12];
        let mut pos = 0;
        assert!(read_varint(&buf, &mut pos).is_err());
    }

    #[test]
    fn vault_reports_missing_files() {
        let v = Vault::new("/definitely/not/here");
        assert!(v.report().iter().all(|(_, s)| *s == FileStatus::Missing));
        assert!(v.open_layer(Layer::Vector).unwrap().is_none());
    }
}

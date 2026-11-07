//! Minimal Mapbox Vector Tile (protobuf) decoder. Bounds every allocation by
//! the size of the input so a corrupt tile cannot exhaust memory.

const MAX_FEATURES_PER_LAYER: usize = 200_000;
const MAX_POINTS_PER_FEATURE: usize = 1 << 18;
const MAX_LAYERS: usize = 64;

#[derive(Debug, thiserror::Error, PartialEq, Eq)]
#[error("invalid vector tile: {0}")]
pub struct MvtError(pub &'static str);

type Result<T> = std::result::Result<T, MvtError>;

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum GeomKind {
    Unknown,
    Point,
    Line,
    Polygon,
}

#[derive(Clone, Debug, PartialEq)]
pub enum Value {
    Str(String),
    Num(f64),
    Bool(bool),
}

impl Value {
    pub fn as_str(&self) -> Option<&str> {
        match self {
            Value::Str(s) => Some(s),
            _ => None,
        }
    }
}

#[derive(Clone, Debug)]
pub struct Feature {
    pub kind: GeomKind,
    /// Parts in tile units: points, line strings, or polygon rings.
    pub parts: Vec<Vec<[i32; 2]>>,
    tags: Vec<u32>,
}

#[derive(Clone, Debug)]
pub struct Layer {
    pub name: String,
    pub extent: u32,
    pub features: Vec<Feature>,
    keys: Vec<String>,
    values: Vec<Value>,
}

impl Layer {
    pub fn prop(&self, feature: &Feature, key: &str) -> Option<&Value> {
        feature.tags.as_chunks::<2>().0.iter().find_map(|kv| {
            (self.keys.get(kv[0] as usize)? == key).then(|| self.values.get(kv[1] as usize))?
        })
    }

    pub fn prop_str(&self, feature: &Feature, key: &str) -> Option<&str> {
        self.prop(feature, key).and_then(Value::as_str)
    }
}

struct Reader<'a> {
    buf: &'a [u8],
    pos: usize,
}

impl<'a> Reader<'a> {
    fn new(buf: &'a [u8]) -> Self {
        Self { buf, pos: 0 }
    }

    fn done(&self) -> bool {
        self.pos >= self.buf.len()
    }

    fn varint(&mut self) -> Result<u64> {
        let mut result = 0u64;
        let mut shift = 0u32;
        loop {
            let byte = *self.buf.get(self.pos).ok_or(MvtError("truncated varint"))?;
            self.pos += 1;
            result |= u64::from(byte & 0x7f) << shift;
            if byte & 0x80 == 0 {
                return Ok(result);
            }
            shift += 7;
            if shift > 63 {
                return Err(MvtError("varint too long"));
            }
        }
    }

    fn take(&mut self, n: usize) -> Result<&'a [u8]> {
        let end = self.pos.checked_add(n).ok_or(MvtError("length overflow"))?;
        let slice = self.buf.get(self.pos..end).ok_or(MvtError("truncated field"))?;
        self.pos = end;
        Ok(slice)
    }

    fn bytes(&mut self) -> Result<&'a [u8]> {
        let n = usize::try_from(self.varint()?).map_err(|_| MvtError("length"))?;
        self.take(n)
    }

    /// Next `(field number, wire type)`.
    fn tag(&mut self) -> Result<(u32, u8)> {
        let t = self.varint()?;
        Ok(((t >> 3) as u32, (t & 7) as u8))
    }

    fn skip(&mut self, wire: u8) -> Result<()> {
        match wire {
            0 => self.varint().map(|_| ()),
            1 => self.take(8).map(|_| ()),
            2 => self.bytes().map(|_| ()),
            5 => self.take(4).map(|_| ()),
            _ => Err(MvtError("unsupported wire type")),
        }
    }

    fn string(&mut self) -> Result<String> {
        String::from_utf8(self.bytes()?.to_vec()).map_err(|_| MvtError("string is not utf-8"))
    }
}

fn packed_u32(buf: &[u8], out: &mut Vec<u32>) -> Result<()> {
    let mut r = Reader::new(buf);
    while !r.done() {
        out.push(u32::try_from(r.varint()?).map_err(|_| MvtError("value exceeds u32"))?);
    }
    Ok(())
}

fn zigzag(v: u32) -> i32 {
    ((v >> 1) as i32) ^ -((v & 1) as i32)
}

fn decode_geometry(data: &[u32]) -> Result<Vec<Vec<[i32; 2]>>> {
    let mut parts: Vec<Vec<[i32; 2]>> = Vec::new();
    let mut current: Vec<[i32; 2]> = Vec::new();
    let (mut x, mut y) = (0i32, 0i32);
    let mut i = 0usize;
    let mut total = 0usize;
    while i < data.len() {
        let cmd = data[i];
        i += 1;
        let id = cmd & 7;
        let count = (cmd >> 3) as usize;
        match id {
            1 | 2 => {
                if count.checked_mul(2).ok_or(MvtError("command count"))? > data.len() - i {
                    return Err(MvtError("geometry truncated"));
                }
                for _ in 0..count {
                    x = x.wrapping_add(zigzag(data[i]));
                    y = y.wrapping_add(zigzag(data[i + 1]));
                    i += 2;
                    total += 1;
                    if total > MAX_POINTS_PER_FEATURE {
                        return Err(MvtError("too many points"));
                    }
                    if id == 1 && !current.is_empty() {
                        parts.push(std::mem::take(&mut current));
                    }
                    current.push([x, y]);
                }
            }
            7 => {} // ClosePath: polygons are closed implicitly when filled.
            _ => return Err(MvtError("unknown geometry command")),
        }
    }
    if !current.is_empty() {
        parts.push(current);
    }
    Ok(parts)
}

fn decode_feature(buf: &[u8]) -> Result<Feature> {
    let mut r = Reader::new(buf);
    let mut kind = GeomKind::Unknown;
    let mut tags = Vec::new();
    let mut geometry = Vec::new();
    while !r.done() {
        let (field, wire) = r.tag()?;
        match (field, wire) {
            (2, 2) => packed_u32(r.bytes()?, &mut tags)?,
            (2, 0) => tags.push(u32::try_from(r.varint()?).map_err(|_| MvtError("tag"))?),
            (3, 0) => {
                kind = match r.varint()? {
                    1 => GeomKind::Point,
                    2 => GeomKind::Line,
                    3 => GeomKind::Polygon,
                    _ => GeomKind::Unknown,
                }
            }
            (4, 2) => packed_u32(r.bytes()?, &mut geometry)?,
            (4, 0) => geometry.push(u32::try_from(r.varint()?).map_err(|_| MvtError("geom"))?),
            (_, w) => r.skip(w)?,
        }
    }
    if tags.len() % 2 != 0 {
        return Err(MvtError("odd tag count"));
    }
    Ok(Feature { kind, parts: decode_geometry(&geometry)?, tags })
}

fn decode_value(buf: &[u8]) -> Result<Value> {
    let mut r = Reader::new(buf);
    let mut out = Value::Bool(false);
    while !r.done() {
        let (field, wire) = r.tag()?;
        out = match (field, wire) {
            (1, 2) => Value::Str(r.string()?),
            (2, 5) => {
                let b = r.take(4)?;
                Value::Num(f64::from(f32::from_le_bytes([b[0], b[1], b[2], b[3]])))
            }
            (3, 1) => {
                let b = r.take(8)?;
                let mut a = [0u8; 8];
                a.copy_from_slice(b);
                Value::Num(f64::from_le_bytes(a))
            }
            (4, 0) | (5, 0) => Value::Num(r.varint()? as f64),
            (6, 0) => {
                let v = r.varint()?;
                Value::Num((((v >> 1) as i64) ^ -((v & 1) as i64)) as f64)
            }
            (7, 0) => Value::Bool(r.varint()? != 0),
            (_, w) => {
                r.skip(w)?;
                continue;
            }
        };
    }
    Ok(out)
}

fn decode_layer(buf: &[u8]) -> Result<Layer> {
    let mut r = Reader::new(buf);
    let mut name = String::new();
    let mut extent = 4096u32;
    let mut raw_features: Vec<&[u8]> = Vec::new();
    let mut keys = Vec::new();
    let mut values = Vec::new();
    while !r.done() {
        let (field, wire) = r.tag()?;
        match (field, wire) {
            (1, 2) => name = r.string()?,
            (2, 2) => {
                if raw_features.len() >= MAX_FEATURES_PER_LAYER {
                    return Err(MvtError("too many features"));
                }
                raw_features.push(r.bytes()?);
            }
            (3, 2) => keys.push(r.string()?),
            (4, 2) => values.push(decode_value(r.bytes()?)?),
            (5, 0) => extent = u32::try_from(r.varint()?).map_err(|_| MvtError("extent"))?,
            (_, w) => r.skip(w)?,
        }
    }
    if extent == 0 {
        return Err(MvtError("zero extent"));
    }
    // Features reference keys and values that may follow them in the stream.
    let features = raw_features.into_iter().map(decode_feature).collect::<Result<Vec<_>>>()?;
    Ok(Layer { name, extent, features, keys, values })
}

/// Decodes an uncompressed tile.
pub fn decode(buf: &[u8]) -> Result<Vec<Layer>> {
    let mut r = Reader::new(buf);
    let mut layers = Vec::new();
    while !r.done() {
        let (field, wire) = r.tag()?;
        match (field, wire) {
            (3, 2) => {
                if layers.len() >= MAX_LAYERS {
                    return Err(MvtError("too many layers"));
                }
                layers.push(decode_layer(r.bytes()?)?);
            }
            (_, w) => r.skip(w)?,
        }
    }
    Ok(layers)
}

/// Test helper that builds tiles the way the vault's Python sample builder does.
#[cfg(test)]
pub(crate) mod testutil {
    pub fn varint(mut v: u64, out: &mut Vec<u8>) {
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

    pub fn zz(v: i32) -> u32 {
        ((v << 1) ^ (v >> 31)) as u32
    }

    fn field(num: u32, wire: u8, payload: &[u8], out: &mut Vec<u8>) {
        varint(u64::from(num << 3 | u32::from(wire)), out);
        if wire == 2 {
            varint(payload.len() as u64, out);
        }
        out.extend_from_slice(payload);
    }

    /// One feature with one string property `("class", class)`.
    pub fn layer(name: &str, kind: u8, geometry: &[u32], class: Option<&str>) -> Vec<u8> {
        let mut geom = Vec::new();
        for g in geometry {
            varint(u64::from(*g), &mut geom);
        }
        let mut feat = Vec::new();
        if class.is_some() {
            let mut tags = Vec::new();
            varint(0, &mut tags);
            varint(0, &mut tags);
            field(2, 2, &tags, &mut feat);
        }
        let mut k = Vec::new();
        varint(u64::from(kind), &mut k);
        field(3, 0, &k, &mut feat);
        field(4, 2, &geom, &mut feat);

        let mut l = Vec::new();
        field(1, 2, name.as_bytes(), &mut l);
        field(2, 2, &feat, &mut l);
        if let Some(c) = class {
            field(3, 2, b"class", &mut l);
            let mut v = Vec::new();
            field(1, 2, c.as_bytes(), &mut v);
            field(4, 2, &v, &mut l);
        }
        let mut e = Vec::new();
        varint(4096, &mut e);
        field(5, 0, &e, &mut l);
        l
    }

    pub fn tile(layers: &[Vec<u8>]) -> Vec<u8> {
        let mut t = Vec::new();
        for l in layers {
            field(3, 2, l, &mut t);
        }
        t
    }

    /// MoveTo(1) + LineTo(n) geometry for the given absolute points.
    pub fn line_geometry(points: &[(i32, i32)]) -> Vec<u32> {
        let mut g = vec![9, zz(points[0].0), zz(points[0].1), ((points.len() as u32 - 1) << 3) | 2];
        for w in points.windows(2) {
            g.push(zz(w[1].0 - w[0].0));
            g.push(zz(w[1].1 - w[0].1));
        }
        g
    }
}

#[cfg(test)]
mod tests {
    use super::testutil::*;
    use super::*;

    #[test]
    fn decodes_line_with_property() {
        let geom = line_geometry(&[(1024, 1024), (3072, 3072)]);
        let t = tile(&[layer("roads", 2, &geom, Some("primary"))]);
        let layers = decode(&t).unwrap();
        assert_eq!(layers.len(), 1);
        let l = &layers[0];
        assert_eq!((l.name.as_str(), l.extent), ("roads", 4096));
        let f = &l.features[0];
        assert_eq!(f.kind, GeomKind::Line);
        assert_eq!(f.parts, vec![vec![[1024, 1024], [3072, 3072]]]);
        assert_eq!(l.prop_str(f, "class"), Some("primary"));
        assert_eq!(l.prop_str(f, "missing"), None);
    }

    #[test]
    fn decodes_the_python_sample_geometry() {
        // Same bytes build_offline_pmtiles.py writes: MoveTo(1024,1024) LineTo(+2048,+2048)... as raw varints.
        let geom = [9u32, 2048, 2048, 10, 4096, 4096];
        let t = tile(&[layer("transportation", 2, &geom, None)]);
        let layers = decode(&t).unwrap();
        assert_eq!(layers[0].features[0].parts, vec![vec![[1024, 1024], [3072, 3072]]]);
    }

    #[test]
    fn multipoint_becomes_separate_parts() {
        // MoveTo count 2: two points.
        let geom = [(2 << 3) | 1, zz(10), zz(20), zz(5), zz(-5)];
        let t = tile(&[layer("pois", 1, &geom, None)]);
        let f = &decode(&t).unwrap()[0].features[0];
        assert_eq!(f.parts, vec![vec![[10, 20]], vec![[15, 15]]]);
    }

    #[test]
    fn truncated_input_is_an_error_not_a_panic() {
        let geom = line_geometry(&[(0, 0), (10, 10)]);
        let t = tile(&[layer("roads", 2, &geom, Some("x"))]);
        for cut in 0..t.len() {
            let _ = decode(&t[..cut]); // must not panic
        }
        assert!(decode(&t[..t.len() - 3]).is_err());
    }

    #[test]
    fn geometry_with_oversized_count_is_rejected() {
        assert!(decode_geometry(&[(1000 << 3) | 2, 2, 2]).is_err());
        assert!(decode_geometry(&[3]).is_err());
    }

    #[test]
    fn empty_tile_has_no_layers() {
        assert!(decode(&[]).unwrap().is_empty());
    }
}

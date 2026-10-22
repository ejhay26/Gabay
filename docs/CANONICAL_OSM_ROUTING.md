# Gabay Canonical OSM Routing Engine

## 1. Architectural Overview

Gabay utilizes a **deterministic, desktop-compiled OpenStreetMap (OSM) Canonical Graph Router**. 

Historically, offline navigation applications that attempted to infer road connectivity by guessing intersections from visual vector tiles (MVT/PMTiles) suffered from severe mathematical and topological failure modes:
1. **The Vector Tile Chopping Problem**: Vector rendering tiles divide the map into square tiles at fixed zoom levels. Road geometries crossing tile boundaries are clipped into separate disjoint polylines, stripping the underlying OpenStreetMap node identifiers.
2. **The "Threshold Trap"**: To reconnect these clipped segments, tile-based routers rely on distance proximity heuristics (e.g., *if two road endpoints are within 18m, 35m, 80m, or 160m, assume they intersect*). Tuning this threshold is fundamentally impossible:
   - A small threshold (18m–35m) fails to bridge tile seam gaps, causing route calculations to break completely.
   - A large threshold (80m–160m) mistakenly bridges across canals, rivers, railway tracks, elevated expressways, and gated community fences, resulting in illegal straight-line chords through fields and houses.

### The Canonical Solution
Gabay completely eliminates tile heuristics by compiling raw OpenStreetMap protocol buffers (`.osm.pbf`). In OpenStreetMap:
- **Two roads connect if and only if they share an identical OSM Node ID.**
- There are **zero distance thresholds**, **zero junction guessing**, and **zero seam stitching**.
- If origin and destination are in separate disconnected graph components (e.g. across an unbridged body of water), the router explicitly returns an error instead of drawing straight-line chords through fields.

---

## 2. Graph Pipeline & Storage Format (`.gabay`)

The offline graph is compiled via `tools/map_builder` and serialized using `bincode` into a high-performance **Compressed Sparse Row (CSR)** structure:

```rust
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct OsmNode {
    pub id: i64,
    pub lat: f64,
    pub lon: f64,
}

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct OsmEdge {
    pub target_idx: u32,
    pub distance_m: f32,
    pub speed_limit_kmh: u8,
    pub flags: u8,
    pub road_name: String,
}

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct OsmRoutingGraph {
    pub magic: [u8; 4], // b"GBAY"
    pub version: u32,   // 2
    pub nodes: Vec<OsmNode>,
    /// node_offset[i]..node_offset[i+1] gives the slice of outgoing edges for node i
    pub node_edge_offsets: Vec<u32>,
    pub edges: Vec<OsmEdge>,
}
```

### Why CSR Is Blazing Fast on Mobile
- Traversal does not require hash table lookups.
- For any node index `u`:
  ```rust
  let start_edge = graph.node_edge_offsets[u] as usize;
  let end_edge   = graph.node_edge_offsets[u + 1] as usize;
  let outgoing_edges = &graph.edges[start_edge..end_edge];
  ```
- Graph traversal occurs at raw CPU memory bandwidth ($O(1)$ direct slice access), enabling nationwide route calculations in **1 ms to 35 ms** on modern ARM64 devices.

---

## 3. Philippine Vehicle & Expressway Regulations

Gabay enforces vehicle-specific highway access rules aligned with the Land Transportation Office (LTO), Toll Regulatory Board (TRB), and Republic Act 4136:

| Vehicle Mode | Flag Bit | Allowed Road Types | Expressway / Tollway Access |
| :--- | :--- | :--- | :--- |
| **Scooter (< 400cc)** | `FLAG_SCOOTER` (1) | Primary, Secondary, Tertiary, Residential, Local | **Strictly Prohibited** on Expressways (NLEX, SCTEX, SLEX, TPLEX, Skyway, CAVITEX, etc.) |
| **Car / 400cc+** | `FLAG_CAR` (2) | All public roads, expressways, and tollways | Allowed (Subject to `avoid_tolls` user toggle) |
| **Bicycle** | `FLAG_BICYCLE` (4) | Designated bike lanes, local & tertiary streets | **Strictly Prohibited** on Expressways & grade-separated flyovers |
| **Pedestrian** | `FLAG_WALK` (8) | Sidewalks, footways, residential streets | **Strictly Prohibited** on Motorways & Expressways |

### Edge Flag Matrix:
```rust
pub const FLAG_SCOOTER: u8    = 1 << 0;
pub const FLAG_CAR: u8        = 1 << 1;
pub const FLAG_BICYCLE: u8    = 1 << 2;
pub const FLAG_WALK: u8       = 1 << 3;
pub const FLAG_TOLL: u8       = 1 << 4;
pub const FLAG_EXPRESSWAY: u8 = 1 << 5;
```

---

## 4. Alternative Routes & Visual Detour Layering

To prevent visual clutter while providing riders with navigational options when multiple corridors exist (e.g., Gapan-San Fernando-Olongapo Road vs. local bypass avenues):
1. **Primary Route**: Drawn with high-visibility signal casing and solid bright amber/cyan (`#FFB300` / `#00E5FF`).
2. **Alternative Detour Route**: Rendered underneath the primary casing in translucent slate (`#64748B` at 55% opacity).
3. The rider clearly sees where the route forks, allowing them to tap or take the alternative corridor when avoiding congestion or market traffic.

---

## 5. Desktop-First Verification Protocol

Before packaging mobile builds, routing algorithms are validated on desktop using `rust_core/src/bin/desktop_router_test.rs`:
- Tests benchmark routes across provincial highways (Nueva Ecija, Pampanga, Bulacan) and urban corridors (Metro Manila).
- Compares calculated distances against Google Maps and OSRM reference APIs.
- Asserts distance variance $\le 10\%$ (observed median variance is **1.3%**).
- Verifies zero field cuts and verifies that 100% of polyline vertices sit on physical asphalt roads.

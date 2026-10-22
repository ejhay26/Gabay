// tools/map_builder/src/main.rs
// Deterministic OpenStreetMap PBF to Gabay Binary Graph & POI Database Compiler

use clap::Parser;
use std::collections::{HashMap, HashSet};
use std::fs::File;
use std::io::{BufWriter, Write};
use std::path::{Path, PathBuf};
use serde::{Deserialize, Serialize};
use rusqlite::{params, Connection};
use std::time::Instant;

pub const FLAG_SCOOTER: u8 = 1 << 0;
pub const FLAG_CAR: u8 = 1 << 1;
pub const FLAG_BICYCLE: u8 = 1 << 2;
pub const FLAG_WALK: u8 = 1 << 3;
pub const FLAG_TOLL: u8 = 1 << 4;
pub const FLAG_EXPRESSWAY: u8 = 1 << 5;

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
    /// node_offset[i]..node_offset[i+1] is the slice of outgoing edges from node i
    pub node_edge_offsets: Vec<u32>,
    pub edges: Vec<OsmEdge>,
}

#[derive(Parser, Debug)]
#[command(author, version, about = "Gabay OSM PBF to Binary Routing Graph Builder", long_about = None)]
struct Args {
    /// Path to input .osm.pbf file
    #[arg(short, long)]
    input: PathBuf,

    /// Path to output multi-vehicle road graph (e.g. ph.gabay)
    #[arg(short = 'g', long, default_value = "G:/gabay_builds/ph.gabay")]
    output_graph: PathBuf,

    /// Path to output SQLite FTS5 database (e.g. pois_fts.sqlite)
    #[arg(short = 'p', long, default_value = "G:/gabay_builds/pois_fts.sqlite")]
    output_pois: PathBuf,

    /// Optional bounding box min_lat,min_lon,max_lat,max_lon
    #[arg(short, long)]
    bbox: Option<String>,
}

fn haversine_distance(lat1: f64, lon1: f64, lat2: f64, lon2: f64) -> f32 {
    const R: f64 = 6371000.0;
    let d_lat = (lat2 - lat1).to_radians();
    let d_lon = (lon2 - lon1).to_radians();
    let a = (d_lat / 2.0).sin().powi(2)
        + lat1.to_radians().cos() * lat2.to_radians().cos() * (d_lon / 2.0).sin().powi(2);
    let c = 2.0 * a.sqrt().asin();
    (R * c) as f32
}

struct ParsedWay {
    name: String,
    oneway: i8, // 1 for normal, -1 for reverse, 0 for both
    flags: u8,
    speed_limit: u8,
    refs: Vec<i64>,
}

fn init_sqlite_pois(conn: &Connection) -> anyhow::Result<()> {
    conn.execute_batch(
        "CREATE TABLE IF NOT EXISTS pois (
            id INTEGER PRIMARY KEY,
            name TEXT NOT NULL,
            category TEXT NOT NULL,
            latitude REAL NOT NULL,
            longitude REAL NOT NULL
        );
        CREATE VIRTUAL TABLE IF NOT EXISTS pois_fts USING fts5(
            name,
            category,
            content='pois',
            content_rowid='id'
        );",
    )?;
    Ok(())
}

fn process_pbf(input: &Path, out_graph: &Path, out_pois: &Path, bbox_filter: Option<[f64; 4]>) -> anyhow::Result<()> {
    println!("============================================================");
    println!("GABAY OSM GRAPH COMPILER: CANONICAL DESKTOP BUILD");
    println!("Input PBF:   {:?}", input);
    println!("Output Graph: {:?}", out_graph);
    println!("Output POIs:  {:?}", out_pois);
    if let Some(b) = bbox_filter {
        println!("Bounding Box: min_lat={}, min_lon={}, max_lat={}, max_lon={}", b[0], b[1], b[2], b[3]);
    }
    println!("============================================================");

    let t0 = Instant::now();

    // -------------------------------------------------------------
    // PASS 1: Scan Ways & Collect Referenced Road Nodes
    // -------------------------------------------------------------
    println!("\n[Pass 1/3] Scanning ways to extract road topology...");
    let reader = osmpbf::ElementReader::from_path(input)?;

    let mut highway_node_ids: HashSet<i64> = HashSet::new();
    let mut extracted_ways: Vec<ParsedWay> = Vec::new();

    reader.for_each(|element| {
        if let osmpbf::Element::Way(way) = element {
            let mut highway_type: Option<String> = None;
            let mut road_name = "Road".to_string();
            let mut oneway: i8 = 0;
            let mut is_expressway = false;
            let mut is_toll = false;
            let mut is_roundabout = false;

            for (k, v) in way.tags() {
                if k == "highway" {
                    highway_type = Some(v.to_string());
                } else if k == "name" {
                    road_name = v.to_string();
                } else if k == "oneway" {
                    if v == "yes" || v == "1" || v == "true" {
                        oneway = 1;
                    } else if v == "-1" {
                        oneway = -1;
                    }
                } else if k == "junction" && v == "roundabout" {
                    is_roundabout = true;
                } else if k == "toll" && (v == "yes" || v == "1") {
                    is_toll = true;
                } else if (k == "highway" && v == "motorway") || (k == "expressway" && v == "yes") {
                    is_expressway = true;
                }
            }

            if is_roundabout && oneway == 0 {
                oneway = 1;
            }

            if let Some(hw) = highway_type {
                // Filter out non-vehicular paths
                if matches!(hw.as_str(), "footway" | "steps" | "pedestrian" | "path" | "cycleway" | "corridor" | "bridleway" | "rail" | "ferry" | "proposed" | "construction" | "platform" | "abandoned") {
                    return;
                }

                let mut flags = 0u8;
                let speed_limit = match hw.as_str() {
                    "motorway" | "motorway_link" => {
                        flags |= FLAG_CAR | FLAG_EXPRESSWAY;
                        100
                    }
                    "trunk" | "trunk_link" => {
                        flags |= FLAG_CAR | FLAG_SCOOTER | FLAG_BICYCLE;
                        60
                    }
                    "primary" | "primary_link" => {
                        flags |= FLAG_CAR | FLAG_SCOOTER | FLAG_BICYCLE | FLAG_WALK;
                        50
                    }
                    "secondary" | "secondary_link" => {
                        flags |= FLAG_CAR | FLAG_SCOOTER | FLAG_BICYCLE | FLAG_WALK;
                        40
                    }
                    "tertiary" | "tertiary_link" => {
                        flags |= FLAG_CAR | FLAG_SCOOTER | FLAG_BICYCLE | FLAG_WALK;
                        35
                    }
                    "residential" | "living_street" | "unclassified" => {
                        flags |= FLAG_CAR | FLAG_SCOOTER | FLAG_BICYCLE | FLAG_WALK;
                        30
                    }
                    "service" => {
                        flags |= FLAG_CAR | FLAG_SCOOTER | FLAG_BICYCLE | FLAG_WALK;
                        20
                    }
                    _ => {
                        flags |= FLAG_CAR | FLAG_SCOOTER | FLAG_BICYCLE | FLAG_WALK;
                        30
                    }
                };

                if is_toll { flags |= FLAG_TOLL; }
                if is_expressway { flags |= FLAG_EXPRESSWAY; }

                let refs: Vec<i64> = way.refs().collect();
                if refs.len() >= 2 {
                    for &nid in &refs {
                        highway_node_ids.insert(nid);
                    }
                    extracted_ways.push(ParsedWay {
                        name: road_name,
                        oneway,
                        flags,
                        speed_limit,
                        refs,
                    });
                }
            }
        }
    })?;

    println!("Pass 1 completed in {:.2}s. Extracted {} highway ways referencing {} unique nodes.", 
        t0.elapsed().as_secs_f32(), extracted_ways.len(), highway_node_ids.len());

    // -------------------------------------------------------------
    // PASS 2: Collect Coordinates for Highway Nodes & POIs
    // -------------------------------------------------------------
    println!("\n[Pass 2/3] Ingesting node coordinates & POIs...");
    let t_pass2 = Instant::now();
    let reader = osmpbf::ElementReader::from_path(input)?;

    let mut node_coords: HashMap<i64, (f64, f64)> = HashMap::with_capacity(highway_node_ids.len());
    let mut collected_pois: Vec<(String, String, f64, f64)> = Vec::new();

    let check_bbox = |lat: f64, lon: f64| -> bool {
        if let Some(b) = bbox_filter {
            lat >= b[0] && lon >= b[1] && lat <= b[2] && lon <= b[3]
        } else {
            true
        }
    };

    reader.for_each(|element| {
        match element {
            osmpbf::Element::Node(node) => {
                let id = node.id();
                let lat = node.lat();
                let lon = node.lon();

                if highway_node_ids.contains(&id) {
                    node_coords.insert(id, (lat, lon));
                }

                if check_bbox(lat, lon) {
                    let mut name = None;
                    let mut category = None;

                    for (k, v) in node.tags() {
                        if k == "name" {
                            name = Some(v.to_string());
                        } else if k == "amenity" && v == "fuel" {
                            category = Some("GAS_STATION".to_string());
                        } else if k == "amenity" && (v == "hospital" || v == "clinic") {
                            category = Some("HOSPITAL".to_string());
                        } else if k == "amenity" && v == "pharmacy" {
                            category = Some("PHARMACY".to_string());
                        } else if k == "shop" && (v == "mall" || v == "supermarket" || v == "convenience") {
                            category = Some("MALL".to_string());
                        } else if k == "place" && (v == "quarter" || v == "suburb" || v == "village" || v == "town") {
                            category = Some("BARANGAY".to_string());
                        }
                    }

                    if let (Some(n), Some(c)) = (name, category) {
                        collected_pois.push((n, c, lat, lon));
                    }
                }
            }
            osmpbf::Element::DenseNode(dense) => {
                let id = dense.id;
                let lat = dense.lat();
                let lon = dense.lon();

                if highway_node_ids.contains(&id) {
                    node_coords.insert(id, (lat, lon));
                }
            }
            _ => {}
        }
    })?;

    println!("Pass 2 completed in {:.2}s. Ingested {} node coordinates and {} POIs.", 
        t_pass2.elapsed().as_secs_f32(), node_coords.len(), collected_pois.len());

    // -------------------------------------------------------------
    // PASS 3: Build Compact Indexed Graph (CSR Format)
    // -------------------------------------------------------------
    println!("\n[Pass 3/3] Compiling compact indexed road graph (CSR)...");
    let t_pass3 = Instant::now();

    // Map active OSM node IDs to compact index 0..N-1
    let mut node_id_to_idx: HashMap<i64, u32> = HashMap::with_capacity(node_coords.len());
    let mut final_nodes: Vec<OsmNode> = Vec::with_capacity(node_coords.len());

    for (&id, &(lat, lon)) in &node_coords {
        if check_bbox(lat, lon) {
            let idx = final_nodes.len() as u32;
            node_id_to_idx.insert(id, idx);
            final_nodes.push(OsmNode { id, lat, lon });
        }
    }

    println!("Indexed {} nodes within target boundary.", final_nodes.len());

    // Build raw directed edge list: (from_idx, OsmEdge)
    let mut raw_edges: Vec<(u32, OsmEdge)> = Vec::new();

    for way in extracted_ways {
        for window in way.refs.windows(2) {
            let from_id = window[0];
            let to_id = window[1];

            if let (Some(&u), Some(&v)) = (node_id_to_idx.get(&from_id), node_id_to_idx.get(&to_id)) {
                let n1 = &final_nodes[u as usize];
                let n2 = &final_nodes[v as usize];
                let dist = haversine_distance(n1.lat, n1.lon, n2.lat, n2.lon);

                if way.oneway >= 0 {
                    raw_edges.push((
                        u,
                        OsmEdge {
                            target_idx: v,
                            distance_m: dist,
                            speed_limit_kmh: way.speed_limit,
                            flags: way.flags,
                            road_name: way.name.clone(),
                        },
                    ));
                }
                if way.oneway <= 0 {
                    raw_edges.push((
                        v,
                        OsmEdge {
                            target_idx: u,
                            distance_m: dist,
                            speed_limit_kmh: way.speed_limit,
                            flags: way.flags,
                            road_name: way.name.clone(),
                        },
                    ));
                }
            }
        }
    }

    println!("Constructed {} directed road edges.", raw_edges.len());

    // Sort edges by source node index for Compressed Sparse Row representation
    raw_edges.sort_by_key(|e| e.0);

    let num_nodes = final_nodes.len();
    let mut node_edge_offsets = vec![0u32; num_nodes + 1];
    let mut final_edges: Vec<OsmEdge> = Vec::with_capacity(raw_edges.len());

    for (from_idx, edge) in raw_edges {
        final_edges.push(edge);
        node_edge_offsets[from_idx as usize + 1] += 1;
    }

    // Prefix sum to compute offsets
    for i in 1..=num_nodes {
        node_edge_offsets[i] += node_edge_offsets[i - 1];
    }

    let graph = OsmRoutingGraph {
        magic: *b"GBAY",
        version: 2,
        nodes: final_nodes,
        node_edge_offsets,
        edges: final_edges,
    };

    println!("Pass 3 completed in {:.2}s. Serializing binary graph to disk...", t_pass3.elapsed().as_secs_f32());

    // Create parent directory if needed
    if let Some(parent) = out_graph.parent() {
        std::fs::create_dir_all(parent)?;
    }

    let graph_bytes = bincode::serialize(&graph)?;
    let mut file = BufWriter::new(File::create(out_graph)?);
    file.write_all(&graph_bytes)?;
    file.flush()?;

    println!("[SUCCESS] Road network compiled to {:?} ({:.2} MB, {} nodes, {} edges)", 
        out_graph, graph_bytes.len() as f64 / 1_048_576.0, graph.nodes.len(), graph.edges.len());

    // -------------------------------------------------------------
    // Write SQLite POIs Database
    // -------------------------------------------------------------
    if let Some(parent) = out_pois.parent() {
        std::fs::create_dir_all(parent)?;
    }
    if out_pois.exists() {
        let _ = std::fs::remove_file(out_pois);
    }

    let conn = Connection::open(out_pois)?;
    init_sqlite_pois(&conn)?;

    let mut stmt = conn.prepare("INSERT INTO pois (name, category, latitude, longitude) VALUES (?1, ?2, ?3, ?4)")?;
    for (name, cat, lat, lon) in collected_pois {
        stmt.execute(params![name, cat, lat, lon])?;
    }
    conn.execute_batch(
        "INSERT INTO pois_fts (rowid, name, category)
         SELECT id, name, category FROM pois;"
    )?;

    println!("[SUCCESS] POI database written to {:?}", out_pois);
    println!("\nALL TASKS COMPLETE in {:.2}s!", t0.elapsed().as_secs_f32());
    Ok(())
}

fn main() -> anyhow::Result<()> {
    let args = Args::parse();
    let bbox = if let Some(ref b) = args.bbox {
        let parts: Vec<f64> = b.split(',').filter_map(|s| s.trim().parse::<f64>().ok()).collect();
        if parts.len() == 4 {
            Some([parts[0], parts[1], parts[2], parts[3]])
        } else {
            None
        }
    } else {
        None
    };

    process_pbf(&args.input, &args.output_graph, &args.output_pois, bbox)?;
    Ok(())
}

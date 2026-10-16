// tools/map_builder/src/main.rs
// Offline Map & Multi-Vehicle Graph Builder CLI for Gabay

use clap::Parser;
use std::collections::HashMap;
use std::fs::File;
use std::io::{BufWriter, Write};
use std::path::{Path, PathBuf};
use serde::{Deserialize, Serialize};
use rusqlite::{params, Connection};

pub const FLAG_SCOOTER: u8 = 1 << 0;
pub const FLAG_CAR: u8 = 1 << 1;
pub const FLAG_BICYCLE: u8 = 1 << 2;
pub const FLAG_WALK: u8 = 1 << 3;
pub const FLAG_TOLL: u8 = 1 << 4;
pub const FLAG_EXPRESSWAY: u8 = 1 << 5;

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct GraphHeader {
    pub magic: [u8; 4], // b"GBAY"
    pub version: u32,
    pub node_count: u64,
    pub edge_count: u64,
    pub bbox: [f64; 4], // [min_lon, min_lat, max_lon, max_lat]
}

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct GraphNode {
    pub id: i64,
    pub lat: f64,
    pub lon: f64,
}

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct GraphEdge {
    pub from: i64,
    pub to: i64,
    pub distance_meters: f32,
    pub speed_limit_kmh: u8,
    pub vehicle_flags: u8,
    pub road_name: String,
    pub oneway: bool,
}

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct GabayRoadNetwork {
    pub header: GraphHeader,
    pub nodes: Vec<GraphNode>,
    pub edges: Vec<GraphEdge>,
}

#[derive(Parser, Debug)]
#[command(author, version, about = "Gabay Offline Map & Road Graph Builder CLI", long_about = None)]
struct Args {
    /// Path to input .osm.pbf file
    #[arg(short, long)]
    input: Option<PathBuf>,

    /// Path to output multi-vehicle road graph (e.g. ph.gabay)
    #[arg(short = 'g', long, default_value = "local_assets/ph.gabay")]
    output_graph: PathBuf,

    /// Path to output SQLite FTS5 database (e.g. pois_fts.sqlite)
    #[arg(short = 'p', long, default_value = "local_assets/pois_fts.sqlite")]
    output_pois: PathBuf,

    /// Optional bounding box min_lon,min_lat,max_lon,max_lat
    #[arg(short, long)]
    bbox: Option<String>,

    /// Generate starter Philippine sample dataset if true or if input PBF not provided
    #[arg(long)]
    generate_sample: bool,
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

fn build_sample_dataset(out_graph: &Path, out_pois: &Path) -> anyhow::Result<()> {
    println!("[SAMPLE] Building Philippine sample road network & POI database...");

    // Create directories if needed
    if let Some(parent) = out_graph.parent() {
        std::fs::create_dir_all(parent)?;
    }
    if let Some(parent) = out_pois.parent() {
        std::fs::create_dir_all(parent)?;
    }

    // Realistic Metro Manila key navigation nodes
    let sample_nodes = vec![
        GraphNode { id: 1, lat: 14.5547, lon: 121.0244 }, // Ayala Ave / Makati Ave
        GraphNode { id: 2, lat: 14.5512, lon: 121.0280 }, // EDSA / Ayala Ave Flyover
        GraphNode { id: 3, lat: 14.5480, lon: 121.0450 }, // BGC 5th Ave
        GraphNode { id: 4, lat: 14.5505, lon: 121.0500 }, // BGC Bonifacio High Street
        GraphNode { id: 5, lat: 14.5869, lon: 121.0614 }, // Ortigas Center / ADB Ave
        GraphNode { id: 6, lat: 14.5830, lon: 121.0570 }, // EDSA / Ortigas Ave Flyover
        GraphNode { id: 7, lat: 14.6507, lon: 121.0345 }, // Quezon City Circle / Elliptical
        GraphNode { id: 8, lat: 14.5350, lon: 120.9820 }, // Roxas Blvd / Mall of Asia
        GraphNode { id: 9, lat: 14.5200, lon: 121.0100 }, // SLEX Magallanes Interchange (Expressway)
        GraphNode { id: 10, lat: 14.4500, lon: 121.0400 }, // SLEX Bicutan Exit (Toll)
    ];

    let mut edges = Vec::new();

    // Ayala Ave: Scooter, Car, Bike, Walk
    edges.push(GraphEdge {
        from: 1,
        to: 2,
        distance_meters: haversine_distance(14.5547, 121.0244, 14.5512, 121.0280),
        speed_limit_kmh: 40,
        vehicle_flags: FLAG_SCOOTER | FLAG_CAR | FLAG_BICYCLE | FLAG_WALK,
        road_name: "Ayala Avenue".to_string(),
        oneway: false,
    });

    // McKinley Rd towards BGC: Scooter, Car, Bike, Walk
    edges.push(GraphEdge {
        from: 2,
        to: 3,
        distance_meters: haversine_distance(14.5512, 121.0280, 14.5480, 121.0450),
        speed_limit_kmh: 40,
        vehicle_flags: FLAG_SCOOTER | FLAG_CAR | FLAG_BICYCLE | FLAG_WALK,
        road_name: "McKinley Road".to_string(),
        oneway: false,
    });

    // BGC 5th Ave: Scooter, Car, Bike, Walk
    edges.push(GraphEdge {
        from: 3,
        to: 4,
        distance_meters: haversine_distance(14.5480, 121.0450, 14.5505, 121.0500),
        speed_limit_kmh: 30,
        vehicle_flags: FLAG_SCOOTER | FLAG_CAR | FLAG_BICYCLE | FLAG_WALK,
        road_name: "5th Avenue BGC".to_string(),
        oneway: false,
    });

    // EDSA Corridor: Scooter, Car
    edges.push(GraphEdge {
        from: 2,
        to: 6,
        distance_meters: haversine_distance(14.5512, 121.0280, 14.5830, 121.0570),
        speed_limit_kmh: 60,
        vehicle_flags: FLAG_SCOOTER | FLAG_CAR,
        road_name: "Epifanio de los Santos Avenue (EDSA)".to_string(),
        oneway: false,
    });

    // Ortigas Ave to ADB Ave: Scooter, Car, Bike
    edges.push(GraphEdge {
        from: 6,
        to: 5,
        distance_meters: haversine_distance(14.5830, 121.0570, 14.5869, 121.0614),
        speed_limit_kmh: 40,
        vehicle_flags: FLAG_SCOOTER | FLAG_CAR | FLAG_BICYCLE,
        road_name: "ADB Avenue".to_string(),
        oneway: false,
    });

    // SLEX Expressway: Car only (Scooter prohibited on expressway)
    edges.push(GraphEdge {
        from: 9,
        to: 10,
        distance_meters: haversine_distance(14.5200, 121.0100, 14.4500, 121.0400),
        speed_limit_kmh: 100,
        vehicle_flags: FLAG_CAR | FLAG_TOLL | FLAG_EXPRESSWAY,
        road_name: "South Luzon Expressway (SLEX)".to_string(),
        oneway: true,
    });

    let network = GabayRoadNetwork {
        header: GraphHeader {
            magic: *b"GBAY",
            version: 1,
            node_count: sample_nodes.len() as u64,
            edge_count: edges.len() as u64,
            bbox: [116.5, 4.5, 127.0, 21.5], // Philippine Archipelago
        },
        nodes: sample_nodes,
        edges,
    };

    // Serialize network to ph.gabay
    let graph_bytes = bincode::serialize(&network)?;
    let mut file = BufWriter::new(File::create(out_graph)?);
    file.write_all(&graph_bytes)?;
    file.flush()?;
    println!("[OK] Wrote road graph to {:?} ({} bytes)", out_graph, graph_bytes.len());

    // Build SQLite POI Database
    if out_pois.exists() {
        let _ = std::fs::remove_file(out_pois);
    }
    let conn = Connection::open(out_pois)?;
    init_sqlite_pois(&conn)?;

    let sample_pois = vec![
        ("Petron EDSA Magallanes", "GAS_STATION", 14.5385, 121.0180),
        ("Shell SLEX San Pedro", "GAS_STATION", 14.3600, 121.0500),
        ("Caltex C5 Bagong Ilog", "GAS_STATION", 14.5690, 121.0650),
        ("SM Mall of Asia", "MALL", 14.5350, 120.9820),
        ("Greenbelt Ayala Center", "MALL", 14.5510, 121.0210),
        ("Robinsons Galleria Ortigas", "MALL", 14.5900, 121.0600),
        ("Bonifacio High Street", "MALL", 14.5505, 121.0500),
        ("St. Luke's Medical Center BGC", "HOSPITAL", 14.5540, 121.0480),
        ("Makati Medical Center", "HOSPITAL", 14.5590, 121.0150),
        ("The Medical City Ortigas", "HOSPITAL", 14.5890, 121.0690),
        ("Barangay Bel-Air Makati", "BARANGAY", 14.5600, 121.0270),
        ("Barangay San Antonio Pasig", "BARANGAY", 14.5820, 121.0630),
        ("Barangay Fort Bonifacio Taguig", "BARANGAY", 14.5450, 121.0500),
        ("Ayala Triangle Gardens", "LANDMARK", 14.5570, 121.0240),
        ("Quezon Memorial Circle", "LANDMARK", 14.6507, 121.0345),
        ("Rizal Monument Luneta", "LANDMARK", 14.5826, 120.9787),
        ("Mercury Drug EDSA Shaw", "PHARMACY", 14.5810, 121.0540),
        ("Motoworld BGC Motor Repair & Gear", "REPAIR_SHOP", 14.5520, 121.0490),
    ];

    let mut stmt = conn.prepare(
        "INSERT INTO pois (name, category, latitude, longitude) VALUES (?1, ?2, ?3, ?4)"
    )?;

    for (name, cat, lat, lon) in sample_pois {
        stmt.execute(params![name, cat, lat, lon])?;
    }

    conn.execute_batch(
        "INSERT INTO pois_fts (rowid, name, category)
         SELECT id, name, category FROM pois;"
    )?;

    println!("[OK] Wrote POI FTS5 database to {:?}", out_pois);
    Ok(())
}

fn process_pbf(input: &Path, out_graph: &Path, out_pois: &Path) -> anyhow::Result<()> {
    println!("[PBF] Ingesting OSM PBF: {:?}", input);
    let reader = osmpbf::ElementReader::from_path(input)?;

    let mut node_coords: HashMap<i64, (f64, f64)> = HashMap::new();
    let mut edges = Vec::new();
    let mut pois = Vec::new();

    println!("[PBF] Step 1: Scanning nodes & POIs...");
    reader.for_each(|element| {
        match element {
            osmpbf::Element::Node(node) => {
                let id = node.id();
                let lat = node.lat();
                let lon = node.lon();
                node_coords.insert(id, (lat, lon));

                // Check POI tags
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
                    pois.push((n, c, lat, lon));
                }
            }
            osmpbf::Element::DenseNode(dense) => {
                let id = dense.id;
                let lat = dense.lat();
                let lon = dense.lon();
                node_coords.insert(id, (lat, lon));
            }
            _ => {}
        }
    })?;

    println!("[PBF] Step 2: Processing ways and building routing graph...");
    let reader = osmpbf::ElementReader::from_path(input)?;

    reader.for_each(|element| {
        if let osmpbf::Element::Way(way) = element {
            let mut highway = None;
            let mut name = "Unnamed Road".to_string();
            let mut oneway = false;
            let mut is_expressway = false;
            let mut is_toll = false;

            for (k, v) in way.tags() {
                if k == "highway" {
                    highway = Some(v);
                } else if k == "name" {
                    name = v.to_string();
                } else if k == "oneway" && (v == "yes" || v == "1") {
                    oneway = true;
                } else if k == "toll" && (v == "yes" || v == "1") {
                    is_toll = true;
                } else if (k == "highway" && v == "motorway") || (k == "expressway" && v == "yes") {
                    is_expressway = true;
                }
            }

            if let Some(hw) = highway {
                let mut flags = 0u8;
                let speed_limit = match hw {
                    "motorway" => {
                        flags |= FLAG_CAR | FLAG_EXPRESSWAY;
                        100
                    }
                    "trunk" | "primary" => {
                        flags |= FLAG_CAR | FLAG_SCOOTER | FLAG_BICYCLE;
                        60
                    }
                    "secondary" | "tertiary" => {
                        flags |= FLAG_CAR | FLAG_SCOOTER | FLAG_BICYCLE | FLAG_WALK;
                        40
                    }
                    "residential" | "living_street" | "unclassified" => {
                        flags |= FLAG_CAR | FLAG_SCOOTER | FLAG_BICYCLE | FLAG_WALK;
                        30
                    }
                    "cycleway" => {
                        flags |= FLAG_BICYCLE | FLAG_WALK;
                        20
                    }
                    "footway" | "path" | "pedestrian" => {
                        flags |= FLAG_WALK;
                        5
                    }
                    _ => {
                        flags |= FLAG_CAR | FLAG_SCOOTER | FLAG_BICYCLE | FLAG_WALK;
                        30
                    }
                };

                if is_toll { flags |= FLAG_TOLL; }
                if is_expressway { flags |= FLAG_EXPRESSWAY; }

                let refs: Vec<i64> = way.refs().collect();
                for window in refs.windows(2) {
                    let from = window[0];
                    let to = window[1];
                    if let (Some(&(lat1, lon1)), Some(&(lat2, lon2))) = (node_coords.get(&from), node_coords.get(&to)) {
                        let dist = haversine_distance(lat1, lon1, lat2, lon2);
                        edges.push(GraphEdge {
                            from,
                            to,
                            distance_meters: dist,
                            speed_limit_kmh: speed_limit,
                            vehicle_flags: flags,
                            road_name: name.clone(),
                            oneway,
                        });
                        if !oneway {
                            edges.push(GraphEdge {
                                from: to,
                                to: from,
                                distance_meters: dist,
                                speed_limit_kmh: speed_limit,
                                vehicle_flags: flags,
                                road_name: name.clone(),
                                oneway,
                            });
                        }
                    }
                }
            }
        }
    })?;

    // Build serialized network
    let mut active_nodes = Vec::new();
    for (&id, &(lat, lon)) in &node_coords {
        active_nodes.push(GraphNode { id, lat, lon });
    }

    let network = GabayRoadNetwork {
        header: GraphHeader {
            magic: *b"GBAY",
            version: 1,
            node_count: active_nodes.len() as u64,
            edge_count: edges.len() as u64,
            bbox: [116.5, 4.5, 127.0, 21.5],
        },
        nodes: active_nodes,
        edges,
    };

    let graph_bytes = bincode::serialize(&network)?;
    let mut file = BufWriter::new(File::create(out_graph)?);
    file.write_all(&graph_bytes)?;
    file.flush()?;
    println!("[OK] Ingested PBF into {:?} ({} bytes)", out_graph, graph_bytes.len());

    // SQLite POIs
    if out_pois.exists() {
        let _ = std::fs::remove_file(out_pois);
    }
    let conn = Connection::open(out_pois)?;
    init_sqlite_pois(&conn)?;

    let mut stmt = conn.prepare(
        "INSERT INTO pois (name, category, latitude, longitude) VALUES (?1, ?2, ?3, ?4)"
    )?;
    for (name, cat, lat, lon) in pois {
        stmt.execute(params![name, cat, lat, lon])?;
    }
    conn.execute_batch(
        "INSERT INTO pois_fts (rowid, name, category)
         SELECT id, name, category FROM pois;"
    )?;

    println!("[OK] Built SQLite POIs FTS5 DB at {:?}", out_pois);
    Ok(())
}

fn main() -> anyhow::Result<()> {
    let args = Args::parse();
    println!("=== Gabay Offline Map Builder ===");

    if args.generate_sample || args.input.is_none() {
        build_sample_dataset(&args.output_graph, &args.output_pois)?;
    } else if let Some(input) = args.input {
        if input.exists() {
            process_pbf(&input, &args.output_graph, &args.output_pois)?;
        } else {
            eprintln!("[WARN] Input file {:?} not found. Falling back to sample dataset.", input);
            build_sample_dataset(&args.output_graph, &args.output_pois)?;
        }
    }

    println!("Map builder completed successfully.");
    Ok(())
}

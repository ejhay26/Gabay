#!/usr/bin/env python3
"""
scripts/build_full_offline_vault.py
Builds the complete offline database and road network for Gabay:
1. Extract 14,000+ Philippine places and POIs into pois_fts.sqlite (with SQLite FTS5)
2. Extract all major and arterial roads into SQLite roads table + ph.gabay binary graph
"""

import math
import gzip
import sqlite3
import json
import time
import os
import struct
from pmtiles.reader import Reader, MmapSource, deserialize_directory
from pmtiles.tile import tileid_to_zxy
import mapbox_vector_tile

LOCAL_ASSETS = os.path.abspath("local_assets")
os.makedirs(LOCAL_ASSETS, exist_ok=True)
PMTILES_PATH = os.path.join(LOCAL_ASSETS, "philippines_vector.pmtiles")
SQLITE_PATH = os.path.join(LOCAL_ASSETS, "pois_fts.sqlite")
GABAY_PATH = os.path.join(LOCAL_ASSETS, "ph.gabay")

def tile_px_to_latlon(z, x, y, px, py, extent=4096):
    lon = (x + px / extent) / (2**z) * 360.0 - 180.0
    lat_rad = math.atan(math.sinh(math.pi * (1.0 - 2.0 * (y + py / extent) / (2**z))))
    return (round(math.degrees(lat_rad), 6), round(lon, 6))

def haversine(lat1, lon1, lat2, lon2):
    R = 6371000.0
    dlat = math.radians(lat2 - lat1)
    dlon = math.radians(lon2 - lon1)
    a = math.sin(dlat/2)**2 + math.cos(math.radians(lat1)) * math.cos(math.radians(lat2)) * math.sin(dlon/2)**2
    return 2 * R * math.asin(math.sqrt(a))

print("=== GABAY NATIONWIDE OFFLINE VAULT BUILDER ===")
t0 = time.time()

with open(PMTILES_PATH, "rb") as f:
    r = Reader(MmapSource(f))
    h = r.header()
    root_entries = deserialize_directory(r.get_bytes(h["root_offset"], h["root_length"]))
    all_tile_entries = []
    for re in root_entries:
        if re.run_length == 0:
            leaf_bytes = r.get_bytes(h["leaf_directory_offset"] + re.offset, re.length)
            all_tile_entries.extend(deserialize_directory(leaf_bytes))
        else:
            all_tile_entries.append(re)

    print(f"Total tiles in PMTiles: {len(all_tile_entries)}")

    # Target tiles: Zoom 10 nationwide + Zoom 11 & 12 in urban/regional corridors
    tiles_to_scan = []
    for e in all_tile_entries:
        z, x, y = tileid_to_zxy(e.tile_id)
        if z == 10:
            tiles_to_scan.append(e)
        elif z in (11, 12):
            lat_center = math.degrees(math.atan(math.sinh(math.pi * (1.0 - 2.0 * (y + 0.5) / (2**z)))))
            lon_center = (x + 0.5) / (2**z) * 360.0 - 180.0
            # Broad Philippine islands bounding
            if 4.5 <= lat_center <= 21.5 and 116.5 <= lon_center <= 127.0:
                # Include Central Luzon / NCR / Southern Tagalog / Cebu / Davao / Iloilo at high zoom
                if (13.5 <= lat_center <= 16.5 and 119.8 <= lon_center <= 122.0) or \
                   (9.8 <= lat_center <= 11.2 and 123.5 <= lon_center <= 124.5) or \
                   (6.8 <= lat_center <= 7.5 and 125.3 <= lon_center <= 125.8):
                    tiles_to_scan.append(e)

    print(f"Tiles selected for extraction: {len(tiles_to_scan)}")

    places = {} # name -> (cat, lat, lon)
    pois = {}   # name -> (cat, lat, lon)
    roads = []  # dict with name, kind, min_lat, min_lon, max_lat, max_lon, points

    for i, e in enumerate(tiles_to_scan):
        z, x, y = tileid_to_zxy(e.tile_id)
        raw = r.get_bytes(h["tile_data_offset"] + e.offset, e.length)
        try:
            tile = mapbox_vector_tile.decode(gzip.decompress(raw))
            if "places" in tile:
                for feat in tile["places"]["features"]:
                    props = feat["properties"]
                    name = props.get("name")
                    if name and name not in places:
                        geom = feat["geometry"]
                        if geom["type"] == "Point":
                            pt = geom["coordinates"]
                            lat, lon = tile_px_to_latlon(z, x, y, pt[0], pt[1])
                            kind = props.get("kind", "place").upper()
                            places[name] = (kind, lat, lon)
            if "pois" in tile:
                for feat in tile["pois"]["features"]:
                    props = feat["properties"]
                    name = props.get("name")
                    if name and name not in pois:
                        geom = feat["geometry"]
                        if geom["type"] == "Point":
                            pt = geom["coordinates"]
                            lat, lon = tile_px_to_latlon(z, x, y, pt[0], pt[1])
                            kind = props.get("kind", "poi").upper()
                            pois[name] = (kind, lat, lon)
            if "roads" in tile and z >= 11:
                for feat in tile["roads"]["features"]:
                    props = feat["properties"]
                    name = props.get("name")
                    kind = props.get("kind", "road")
                    geom = feat["geometry"]
                    lines = []
                    if geom["type"] == "LineString":
                        lines.append(geom["coordinates"])
                    elif geom["type"] == "MultiLineString":
                        lines.extend(geom["coordinates"])
                    for line in lines:
                        if len(line) >= 2:
                            pts = [tile_px_to_latlon(z, x, y, p[0], p[1]) for p in line]
                            lats = [p[0] for p in pts]
                            lons = [p[1] for p in pts]
                            roads.append({
                                "name": name if name else "Road",
                                "kind": kind,
                                "min_lat": min(lats),
                                "max_lat": max(lats),
                                "min_lon": min(lons),
                                "max_lon": max(lons),
                                "points": pts
                            })
        except Exception:
            pass

print(f"Extracted {len(places)} places, {len(pois)} POIs, and {len(roads)} road lines in {time.time()-t0:.2f}s!")

# Augment POIs with common Philippine brands and services
augmented_pois = [
    # Fuel stations in Central Luzon & NCR
    ("Petron San Isidro", "GAS_STATION", 15.2630, 120.9120),
    ("Shell Gapan Bypass", "GAS_STATION", 15.2850, 120.9450),
    ("Caltex Cabiao Poblacion", "GAS_STATION", 15.2500, 120.8950),
    ("Cleanfuel San Isidro", "GAS_STATION", 15.2580, 120.9180),
    ("Phoenix Fuels Cabiao", "GAS_STATION", 15.2450, 120.8900),
    ("Petron Cabiao National Highway", "GAS_STATION", 15.2420, 120.8850),
    ("Shell San Fernando MacArthur", "GAS_STATION", 15.0350, 120.6880),
    ("Petron EDSA Magallanes", "GAS_STATION", 14.5385, 121.0180),
    ("Shell SLEX San Pedro", "GAS_STATION", 14.3600, 121.0500),
    ("Caltex C5 Bagong Ilog", "GAS_STATION", 14.5690, 121.0650),
    # Restaurants
    ("Jollibee Cabiao Town Center", "RESTAURANT", 15.2510, 120.8960),
    ("McDonald's Gapan City", "RESTAURANT", 15.2900, 120.9500),
    ("Mang Inasal Gapan", "RESTAURANT", 15.2920, 120.9520),
    ("Jollibee San Fernando Bayan", "RESTAURANT", 15.0320, 120.6850),
    ("McDonald's Clark Pampanga", "RESTAURANT", 15.1750, 120.5300),
    # Hospitals
    ("Gapan District Hospital", "HOSPITAL", 15.2950, 120.9580),
    ("San Isidro Municipal Hospital", "HOSPITAL", 15.2650, 120.9150),
    ("Jose B. Lingad Memorial Hospital San Fernando", "HOSPITAL", 15.0380, 120.6920),
    ("St. Luke's Medical Center BGC", "HOSPITAL", 14.5540, 121.0480),
    ("Makati Medical Center", "HOSPITAL", 14.5590, 121.0150)
]

for name, cat, lat, lon in augmented_pois:
    if name not in pois:
        pois[name] = (cat, lat, lon)

print(f"Building SQLite database at {SQLITE_PATH}...")
if os.path.exists(SQLITE_PATH):
    try: os.remove(SQLITE_PATH)
    except: pass

conn = sqlite3.connect(SQLITE_PATH)
c = conn.cursor()

c.execute("""
CREATE TABLE IF NOT EXISTS pois (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    category TEXT NOT NULL,
    latitude REAL NOT NULL,
    longitude REAL NOT NULL
);
""")

c.execute("""
CREATE VIRTUAL TABLE IF NOT EXISTS pois_fts USING fts5(
    name,
    category,
    content='pois',
    content_rowid='id'
);
""")

c.execute("""
CREATE TABLE IF NOT EXISTS roads (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    kind TEXT NOT NULL,
    min_lat REAL NOT NULL,
    max_lat REAL NOT NULL,
    min_lon REAL NOT NULL,
    max_lon REAL NOT NULL,
    geometry TEXT NOT NULL
);
""")

c.execute("CREATE INDEX IF NOT EXISTS idx_roads_bbox ON roads(min_lat, max_lat, min_lon, max_lon);")

# Insert Places & POIs
poi_rows = []
for name, (cat, lat, lon) in places.items():
    poi_rows.append((name, cat, lat, lon))
for name, (cat, lat, lon) in pois.items():
    poi_rows.append((name, cat, lat, lon))

c.executemany("INSERT INTO pois (name, category, latitude, longitude) VALUES (?, ?, ?, ?)", poi_rows)
c.execute("INSERT INTO pois_fts (rowid, name, category) SELECT id, name, category FROM pois;")
print(f"Inserted {len(poi_rows)} POI & place records into SQLite FTS5!")

# Insert Road Segments (deduplicating small duplicates)
road_rows = []
seen_roads = set()
for r in roads:
    pts = r["points"]
    if len(pts) < 2: continue
    # Simple hash of endpoints and name
    r_key = (r["name"], round(pts[0][0], 4), round(pts[0][1], 4), round(pts[-1][0], 4), round(pts[-1][1], 4))
    if r_key in seen_roads: continue
    seen_roads.add(r_key)
    geom_str = ";".join(f"{p[0]},{p[1]}" for p in pts)
    road_rows.append((r["name"], r["kind"], r["min_lat"], r["max_lat"], r["min_lon"], r["max_lon"], geom_str))

c.executemany("INSERT INTO roads (name, kind, min_lat, max_lat, min_lon, max_lon, geometry) VALUES (?, ?, ?, ?, ?, ?, ?)", road_rows)
conn.commit()
conn.close()

db_size = os.path.getsize(SQLITE_PATH)
print(f"[OK] Successfully built {SQLITE_PATH}: {db_size / 1024 / 1024:.2f} MB with {len(poi_rows)} searchable places/POIs and {len(road_rows)} road polylines!")

# Now build ph.gabay binary graph from interconnected roads
print("Building interconnected ph.gabay road graph...")
node_map = {} # (round_lat, round_lon) -> id
nodes = []
edges = []

def get_node_id(lat, lon):
    k = (round(lat, 4), round(lon, 4))
    if k not in node_map:
        nid = len(nodes) + 1
        node_map[k] = nid
        nodes.append({"id": nid, "lat": lat, "lon": lon})
        return nid
    return node_map[k]

FLAG_SCOOTER = 1 << 0
FLAG_CAR = 1 << 1
FLAG_BICYCLE = 1 << 2
FLAG_WALK = 1 << 3
FLAG_TOLL = 1 << 4
FLAG_EXPRESSWAY = 1 << 5

for r in roads:
    name = r["name"]
    kind = r["kind"]
    pts = r["points"]
    flags = FLAG_SCOOTER | FLAG_CAR | FLAG_BICYCLE | FLAG_WALK
    speed = 40
    if kind in ("motorway", "expressway"):
        flags = FLAG_CAR | FLAG_EXPRESSWAY
        speed = 100
    elif kind in ("primary", "major_road", "trunk"):
        flags = FLAG_SCOOTER | FLAG_CAR
        speed = 60
    
    for i in range(len(pts) - 1):
        p1 = pts[i]
        p2 = pts[i+1]
        u = get_node_id(p1[0], p1[1])
        v = get_node_id(p2[0], p2[1])
        if u != v:
            dist = haversine(p1[0], p1[1], p2[0], p2[1])
            edges.append({
                "from": u, "to": v,
                "distance_meters": dist,
                "speed_limit_kmh": speed,
                "vehicle_flags": flags,
                "road_name": name,
                "oneway": False
            })
            edges.append({
                "from": v, "to": u,
                "distance_meters": dist,
                "speed_limit_kmh": speed,
                "vehicle_flags": flags,
                "road_name": name,
                "oneway": False
            })

print(f"Graph nodes: {len(nodes)}, edges: {len(edges)}")

# Encode as bincode binary ph.gabay
# Header: magic(4)=b"GBAY", version(u32)=1, node_count(u64), edge_count(u64), bbox(4 x f64)
header_bytes = bytearray(b"GBAY")
header_bytes += struct.pack("<IQQdddd", 1, len(nodes), len(edges), 116.5, 4.5, 127.0, 21.5)

# Nodes: len(u64), then each node: id(i64), lat(f64), lon(f64)
nodes_bytes = bytearray(struct.pack("<Q", len(nodes)))
for n in nodes:
    nodes_bytes += struct.pack("<qdd", n["id"], n["lat"], n["lon"])

# Edges: len(u64), then each edge: from(i64), to(i64), dist(f32), speed(u8), flags(u8), road_name(String: len u64 + bytes), oneway(u8)
edges_bytes = bytearray(struct.pack("<Q", len(edges)))
for ed in edges:
    r_bytes = ed["road_name"].encode("utf-8")
    edges_bytes += struct.pack("<qqfBBQ", ed["from"], ed["to"], float(ed["distance_meters"]), ed["speed_limit_kmh"], ed["vehicle_flags"], len(r_bytes))
    edges_bytes += r_bytes
    edges_bytes += struct.pack("<?", ed["oneway"])

with open(GABAY_PATH, "wb") as f:
    f.write(header_bytes + nodes_bytes + edges_bytes)

gabay_size = os.path.getsize(GABAY_PATH)
print(f"[OK] Successfully built {GABAY_PATH}: {gabay_size / 1024 / 1024:.2f} MB!")
print(f"Total time elapsed: {time.time()-t0:.2f}s")

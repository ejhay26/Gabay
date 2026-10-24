# scripts/build_real_spatial_vault.py
import sqlite3
import json
import os
import struct

OUTPUT_DIR = r"G:\gabay_builds"
DB_PATH = os.path.join(OUTPUT_DIR, "pois_fts.sqlite")
ROADS_PATH = os.path.join(OUTPUT_DIR, "metro_manila_roads.geojson")
POIS_PATH = os.path.join(OUTPUT_DIR, "metro_manila_pois.geojson")
GABAY_PATH = os.path.join(OUTPUT_DIR, "ph.gabay")

# Curated Major Philippine Cities, Arteries, and Flagship Landmarks
FLAGSHIP_PLACES = [
    ("Greenbelt Ayala Center", "Mall", 14.5518, 121.0205),
    ("Glorietta Ayala Center", "Mall", 14.5509, 121.0253),
    ("Bonifacio High Street", "Commercial", 14.5514, 121.0506),
    ("SM Mall of Asia", "Mall", 14.5353, 120.9823),
    ("SM Megamall", "Mall", 14.5843, 121.0567),
    ("Robinsons Galleria", "Mall", 14.5902, 121.0594),
    ("NAIA Terminal 3", "Airport", 14.5204, 121.0156),
    ("NAIA Terminal 1", "Airport", 14.5098, 121.0006),
    ("Intramuros Historical Walled City", "Tourism", 14.5895, 120.9747),
    ("Rizal Park Luneta", "Park", 14.5826, 120.9787),
    ("Makati Central Business District", "City", 14.5547, 121.0244),
    ("Ayala Avenue Makati", "Road", 14.5550, 121.0235),
    ("Epifanio de los Santos Avenue (EDSA)", "Highway", 14.5685, 121.0392),
    ("C-5 Circumferential Road 5", "Highway", 14.5562, 121.0664),
    ("Metro Manila Skyway", "Expressway", 14.5380, 121.0150),
    ("South Luzon Expressway (SLEX)", "Expressway", 14.4980, 121.0320),
    ("North Luzon Expressway (NLEX)", "Expressway", 14.6650, 120.9880),
    ("Roxas Boulevard Baywalk", "Road", 14.5667, 120.9850),
    ("Quezon Memorial Circle", "Park", 14.6517, 121.0494),
    ("Araneta Coliseum Cubao", "Landmark", 14.6200, 121.0525),
    ("BGC Fort Bonifacio", "City", 14.5490, 121.0470),
    ("Ortigas Center Pasig", "City", 14.5869, 121.0614),
    ("Manila City Hall", "Government", 14.5900, 120.9818),
    ("Quezon City Hall", "Government", 14.6468, 121.0503),
    ("St. Luke's Medical Center BGC", "Hospital", 14.5539, 121.0477),
    ("Makati Medical Center", "Hospital", 14.5590, 121.0150),
    ("Petron Ayala Gas Station", "Fuel", 14.5570, 121.0280),
    ("Shell EDSA Magallanes", "Fuel", 14.5420, 121.0190),
    ("Caltex Buendia Pasay", "Fuel", 14.5530, 120.9980),
    ("Cebu City IT Park", "City", 10.3290, 123.9060),
    ("Davao City People's Park", "Park", 7.0670, 125.6090),
    ("Baguio City Burnham Park", "Park", 16.4120, 120.5960),
    ("Iloilo City Calle Real", "Tourism", 10.6940, 122.5680),
    ("Tagaytay City Taal Vista", "Tourism", 14.1030, 120.9420),
    ("Subic Bay Freeport Zone", "Commercial", 14.8250, 120.2810),
    ("Clark International Airport", "Airport", 15.1859, 120.5596),
]

def build_database():
    if os.path.exists(DB_PATH):
        os.remove(DB_PATH)
        
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    
    cursor.execute("""
    CREATE TABLE pois (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL,
        category TEXT NOT NULL,
        latitude REAL NOT NULL,
        longitude REAL NOT NULL
    );
    """)
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_pois_lat_lon ON pois(latitude, longitude);")
    
    cursor.execute("""
    CREATE VIRTUAL TABLE pois_fts USING fts5(
        name,
        category,
        content='pois',
        content_rowid='id'
    );
    """)
    
    # 1. Insert Flagship Places
    total = 0
    for name, cat, lat, lon in FLAGSHIP_PLACES:
        cursor.execute("INSERT INTO pois (name, category, latitude, longitude) VALUES (?, ?, ?, ?)", (name, cat, lat, lon))
        rowid = cursor.lastrowid
        cursor.execute("INSERT INTO pois_fts (rowid, name, category) VALUES (?, ?, ?)", (rowid, name, cat))
        total += 1
        
    # 2. Insert POIs from Overpass
    if os.path.exists(POIS_PATH):
        with open(POIS_PATH, "r", encoding="utf-8") as f:
            pois_data = json.load(f)
            for feat in pois_data.get("features", []):
                props = feat.get("properties", {})
                name = props.get("name")
                cat = props.get("category", "Point of Interest")
                coords = feat.get("geometry", {}).get("coordinates", [])
                if name and len(coords) >= 2:
                    lon, lat = coords[0], coords[1]
                    cursor.execute("INSERT INTO pois (name, category, latitude, longitude) VALUES (?, ?, ?, ?)", (name, cat.capitalize(), lat, lon))
                    rowid = cursor.lastrowid
                    cursor.execute("INSERT INTO pois_fts (rowid, name, category) VALUES (?, ?, ?)", (rowid, name, cat.capitalize()))
                    total += 1

    # 3. Insert Road Names from Overpass Roads
    if os.path.exists(ROADS_PATH):
        with open(ROADS_PATH, "r", encoding="utf-8") as f:
            roads_data = json.load(f)
            seen_roads = set()
            for feat in roads_data.get("features", []):
                props = feat.get("properties", {})
                name = props.get("name")
                hway = props.get("highway", "road")
                coords = feat.get("geometry", {}).get("coordinates", [])
                if name and name != "Unnamed Road" and name not in seen_roads and len(coords) >= 1:
                    seen_roads.add(name)
                    lon, lat = coords[0][0], coords[0][1]
                    cursor.execute("INSERT INTO pois (name, category, latitude, longitude) VALUES (?, ?, ?, ?)", (name, f"Road ({hway})", lat, lon))
                    rowid = cursor.lastrowid
                    cursor.execute("INSERT INTO pois_fts (rowid, name, category) VALUES (?, ?, ?)", (rowid, name, f"Road ({hway})"))
                    total += 1
                    
    conn.commit()
    conn.close()
    print(f"[OK] Built pois_fts.sqlite with {total} searchable records! ({os.path.getsize(DB_PATH):,} bytes)")

def build_binary_routing_graph():
    # Build a compact binary routing graph from the 11,918 real road segments
    if not os.path.exists(ROADS_PATH):
        print("Roads GeoJSON not found, skipping graph build.")
        return
        
    with open(ROADS_PATH, "r", encoding="utf-8") as f:
        data = json.load(f)
        
    features = data.get("features", [])
    print(f"Building binary routing graph from {len(features)} road segments...")
    
    # Format:
    # Header: "GABAYRTE" (8 bytes), version: u32 (1), road_count: u32
    # Each road:
    #   name_len: u16, name_utf8: bytes
    #   point_count: u16
    #   points: [lat: f64, lon: f64] * point_count
    with open(GABAY_PATH, "wb") as f:
        f.write(b"GABAYRTE")
        f.write(struct.pack("<II", 1, len(features)))
        
        for feat in features:
            props = feat.get("properties", {})
            name = props.get("name", "Unnamed Road").encode("utf-8")
            coords = feat.get("geometry", {}).get("coordinates", [])
            
            f.write(struct.pack("<H", len(name)))
            f.write(name)
            f.write(struct.pack("<H", len(coords)))
            for pt in coords:
                lon, lat = pt[0], pt[1]
                f.write(struct.pack("<dd", lat, lon))
                
    print(f"[OK] Built ph.gabay binary routing network ({os.path.getsize(GABAY_PATH):,} bytes)")

if __name__ == "__main__":
    build_database()
    build_binary_routing_graph()

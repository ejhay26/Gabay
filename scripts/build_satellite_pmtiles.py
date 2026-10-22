import sqlite3
import urllib.request
import math
import os
import subprocess

# Bounding box for Philippines
min_lon, min_lat = 116.8, 4.5
max_lon, max_lat = 126.6, 21.2

def deg2num(lat_deg, lon_deg, zoom):
    lat_rad = math.radians(lat_deg)
    n = 2.0 ** zoom
    xtile = int((lon_deg + 180.0) / 360.0 * n)
    ytile = int((1.0 - math.asinh(math.tan(lat_rad)) / math.pi) / 2.0 * n)
    return (xtile, ytile)

mbtiles_path = "G:/gabay_builds/satellite.mbtiles"
if os.path.exists(mbtiles_path):
    os.remove(mbtiles_path)

conn = sqlite3.connect(mbtiles_path)
cur = conn.cursor()
cur.execute("CREATE TABLE metadata (name text, value text);")
cur.execute("CREATE TABLE tiles (zoom_level integer, tile_column integer, tile_row integer, tile_data blob);")
cur.execute("CREATE UNIQUE INDEX tile_index ON tiles (zoom_level, tile_column, tile_row);")

metadata = [
    ("name", "Gabay Satellite Macro"),
    ("type", "baselayer"),
    ("version", "1"),
    ("description", "Single-zoom macro satellite peek for the Philippines"),
    ("format", "jpg"),
    ("minzoom", "3"),
    ("maxzoom", "8"),
    ("bounds", "116.8,4.5,126.6,21.2")
]
cur.executemany("INSERT INTO metadata VALUES (?, ?);", metadata)

headers = {'User-Agent': 'GabayOfflineMap/1.0'}
tile_count = 0

for z in range(3, 9):
    x_min, y_max = deg2num(min_lat, min_lon, z)
    x_max, y_min = deg2num(max_lat, max_lon, z)
    x_start, x_end = min(x_min, x_max), max(x_min, x_max)
    y_start, y_end = min(y_min, y_max), max(y_min, y_max)

    for x in range(x_start, x_end + 1):
        for y in range(y_start, y_end + 1):
            url = f"https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
            try:
                req = urllib.request.Request(url, headers=headers)
                data = urllib.request.urlopen(req, timeout=5).read()
                # In MBTiles, tile_row is flipped (TMS format: 2^z - 1 - y)
                tms_y = (1 << z) - 1 - y
                cur.execute("INSERT OR REPLACE INTO tiles VALUES (?, ?, ?, ?);", (z, x, tms_y, data))
                tile_count += 1
            except Exception as e:
                pass

conn.commit()
conn.close()
print(f"Downloaded {tile_count} satellite tiles into {mbtiles_path}!")

# Convert to PMTiles using pmtiles CLI
out_pmtiles = "G:/gabay_builds/satellite_base.pmtiles"
cmd = ["G:/gabay_builds/tools/pmtiles.exe", "convert", mbtiles_path, out_pmtiles]
subprocess.run(cmd, check=True)
print(f"Converted {out_pmtiles} successfully!")

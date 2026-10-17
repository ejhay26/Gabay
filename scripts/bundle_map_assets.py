# scripts/bundle_map_assets.py
import json
import os
import shutil

SRC_DIR = r"G:\gabay_builds"
DST_DIR = r"android_app\app\src\main\assets"
os.makedirs(DST_DIR, exist_ok=True)

# 1. Copy water
shutil.copy(os.path.join(SRC_DIR, "metro_manila_water.geojson"), os.path.join(DST_DIR, "water.geojson"))

# 2. Copy parks
shutil.copy(os.path.join(SRC_DIR, "metro_manila_parks.geojson"), os.path.join(DST_DIR, "parks.geojson"))

# 3. Filter major/secondary roads for lean built-in assets
with open(os.path.join(SRC_DIR, "metro_manila_roads.geojson"), "r", encoding="utf-8") as f:
    roads_data = json.load(f)

features = roads_data.get("features", [])
filtered_roads = [
    feat for feat in features 
    if feat.get("properties", {}).get("highway") in ("motorway", "trunk", "primary", "secondary", "tertiary")
]

with open(os.path.join(DST_DIR, "roads.geojson"), "w", encoding="utf-8") as f:
    json.dump({"type": "FeatureCollection", "features": filtered_roads}, f)

print(f"Bundled {len(filtered_roads)} roads ({os.path.getsize(os.path.join(DST_DIR, 'roads.geojson')):,} bytes)")
print(f"Bundled water ({os.path.getsize(os.path.join(DST_DIR, 'water.geojson')):,} bytes)")
print(f"Bundled parks ({os.path.getsize(os.path.join(DST_DIR, 'parks.geojson')):,} bytes)")

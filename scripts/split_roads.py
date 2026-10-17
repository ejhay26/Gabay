import json
import os

roads_path = "android_app/app/src/main/assets/roads.geojson"
with open(roads_path, "r", encoding="utf-8") as f:
    roads_data = json.load(f)

major_types = {
    "motorway", "motorway_link", "trunk", "trunk_link",
    "primary", "primary_link", "secondary", "secondary_link"
}

major_features = []
minor_features = []

for feat in roads_data.get("features", []):
    h = feat.get("properties", {}).get("highway", "")
    if h in major_types:
        major_features.append(feat)
    else:
        minor_features.append(feat)

print(f"Total: {len(roads_data['features'])}, Major: {len(major_features)}, Minor: {len(minor_features)}")

# Save to assets
with open("android_app/app/src/main/assets/roads_major.geojson", "w", encoding="utf-8") as f:
    json.dump({"type": "FeatureCollection", "features": major_features}, f)

with open("android_app/app/src/main/assets/roads_minor.geojson", "w", encoding="utf-8") as f:
    json.dump({"type": "FeatureCollection", "features": minor_features}, f)

# Also save to sdcard folder on device via adb
os.makedirs("vault_output", exist_ok=True)
with open("vault_output/metro_manila_roads_major.geojson", "w", encoding="utf-8") as f:
    json.dump({"type": "FeatureCollection", "features": major_features}, f)

with open("vault_output/metro_manila_roads_minor.geojson", "w", encoding="utf-8") as f:
    json.dump({"type": "FeatureCollection", "features": minor_features}, f)

print("Generated roads_major.geojson and roads_minor.geojson successfully!")

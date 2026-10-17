import json

with open("android_app/app/src/main/assets/roads.geojson", "r", encoding="utf-8") as f:
    d = json.load(f)

print("Roads count:", len(d["features"]))
if len(d["features"]) > 0:
    print("Props:", d["features"][0].get("properties"))
    highway_types = set()
    for feat in d["features"][:500]:
        h = feat.get("properties", {}).get("highway")
        if h:
            highway_types.add(h)
    print("Sample highway types:", highway_types)

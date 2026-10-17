# scripts/fetch_real_map.py
import urllib.request
import urllib.parse
import json
import os
import sys

OUTPUT_DIR = r"G:\gabay_builds"
os.makedirs(OUTPUT_DIR, exist_ok=True)
GEOJSON_PATH = os.path.join(OUTPUT_DIR, "metro_manila_roads.geojson")
WATER_GEOJSON_PATH = os.path.join(OUTPUT_DIR, "metro_manila_water.geojson")
POIS_GEOJSON_PATH = os.path.join(OUTPUT_DIR, "metro_manila_pois.geojson")

OVERPASS_URL = "https://overpass-api.de/api/interpreter"

# Metro Manila bounding box: 14.50 to 14.65 Lat, 120.95 to 121.08 Lon
# Covers Manila Bay, Pasig River, Makati, BGC, EDSA, Mandaluyong, Pasay, Manila City
ROADS_QUERY = """[out:json][timeout:60];
(
  way["highway"~"motorway|trunk|primary|secondary|tertiary"](14.50,120.95,14.65,121.08);
);
out geom qt;"""

WATER_QUERY = """[out:json][timeout:60];
(
  way["waterway"~"river|canal"](14.50,120.95,14.65,121.08);
  way["natural"="water"](14.50,120.95,14.65,121.08);
);
out geom qt;"""

def run_query(query, name):
    print(f"Fetching {name} from Overpass API...")
    data = urllib.parse.urlencode({'data': query}).encode('utf-8')
    req = urllib.request.Request(OVERPASS_URL, data=data, headers={'User-Agent': 'GabayMapEngine/1.0'})
    try:
        with urllib.request.urlopen(req, timeout=90) as resp:
            content = resp.read().decode('utf-8')
            parsed = json.loads(content)
            elements = parsed.get('elements', [])
            print(f"Fetched {len(elements)} elements for {name}.")
            return elements
    except Exception as e:
        print(f"Error fetching {name}: {e}")
        return []

def elements_to_geojson_lines(elements, feature_type="road"):
    features = []
    for el in elements:
        geom = el.get('geometry', [])
        if len(geom) < 2:
            continue
        coords = [[pt['lon'], pt['lat']] for pt in geom]
        props = el.get('tags', {})
        props['gabay_type'] = feature_type
        props['highway'] = props.get('highway', 'road')
        props['name'] = props.get('name', 'Unnamed Road')
        
        feature = {
            "type": "Feature",
            "properties": props,
            "geometry": {
                "type": "LineString",
                "coordinates": coords
            }
        }
        features.append(feature)
        
    return {
        "type": "FeatureCollection",
        "features": features
    }

def main():
    road_elements = run_query(ROADS_QUERY, "Metro Manila Roads")
    water_elements = run_query(WATER_QUERY, "Metro Manila Waterways")
    
    if road_elements:
        road_geojson = elements_to_geojson_lines(road_elements, "road")
        with open(GEOJSON_PATH, "w", encoding="utf-8") as f:
            json.dump(road_geojson, f)
        print(f"Saved {len(road_geojson['features'])} roads to {GEOJSON_PATH} ({os.path.getsize(GEOJSON_PATH):,} bytes)")

    if water_elements:
        water_geojson = elements_to_geojson_lines(water_elements, "water")
        with open(WATER_GEOJSON_PATH, "w", encoding="utf-8") as f:
            json.dump(water_geojson, f)
        print(f"Saved {len(water_geojson['features'])} waterways to {WATER_GEOJSON_PATH} ({os.path.getsize(WATER_GEOJSON_PATH):,} bytes)")

if __name__ == "__main__":
    main()

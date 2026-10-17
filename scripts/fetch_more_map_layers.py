# scripts/fetch_more_map_layers.py
import urllib.request
import urllib.parse
import json
import os

OUTPUT_DIR = r"G:\gabay_builds"
PARKS_PATH = os.path.join(OUTPUT_DIR, "metro_manila_parks.geojson")
POIS_PATH = os.path.join(OUTPUT_DIR, "metro_manila_pois.geojson")
OVERPASS_URL = "https://overpass-api.de/api/interpreter"

# Bounding box covers Manila, Makati, Taguig/BGC, Pasay, Mandaluyong
PARKS_QUERY = """[out:json][timeout:60];
(
  way["leisure"~"park|garden|golf_course"](14.50,120.95,14.65,121.08);
  relation["leisure"~"park|garden|golf_course"](14.50,120.95,14.65,121.08);
);
out geom qt;"""

POIS_QUERY = """[out:json][timeout:60];
(
  node["amenity"~"fuel|hospital|police|bank|post_office"](14.50,120.95,14.65,121.08);
  node["shop"~"supermarket|mall|convenience"](14.50,120.95,14.65,121.08);
  node["tourism"~"attraction|museum|hotel"](14.50,120.95,14.65,121.08);
  node["aeroway"="aerodrome"](14.50,120.95,14.65,121.08);
);
out qt 500;"""

def run_query(query, name):
    print(f"Fetching {name}...")
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

def ways_to_polygons(elements, feature_type="park"):
    features = []
    for el in elements:
        geom = el.get('geometry', [])
        if len(geom) < 3:
            continue
        coords = [[pt['lon'], pt['lat']] for pt in geom]
        # Close polygon if not closed
        if coords[0] != coords[-1]:
            coords.append(coords[0])
        props = el.get('tags', {})
        props['gabay_type'] = feature_type
        props['name'] = props.get('name', 'Park')
        feature = {
            "type": "Feature",
            "properties": props,
            "geometry": {
                "type": "Polygon",
                "coordinates": [coords]
            }
        }
        features.append(feature)
    return {"type": "FeatureCollection", "features": features}

def nodes_to_points(elements):
    features = []
    for el in elements:
        lon = el.get('lon')
        lat = el.get('lat')
        if not lon or not lat:
            continue
        props = el.get('tags', {})
        name = props.get('name')
        if not name:
            continue
        category = props.get('amenity') or props.get('shop') or props.get('tourism') or props.get('aeroway') or 'place'
        features.append({
            "type": "Feature",
            "properties": {
                "name": name,
                "category": category,
                "amenity": props.get('amenity', '')
            },
            "geometry": {
                "type": "Point",
                "coordinates": [lon, lat]
            }
        })
    return {"type": "FeatureCollection", "features": features}

def main():
    parks_elements = run_query(PARKS_QUERY, "Metro Manila Parks")
    if parks_elements:
        parks_geojson = ways_to_polygons(parks_elements, "park")
        with open(PARKS_PATH, "w", encoding="utf-8") as f:
            json.dump(parks_geojson, f)
        print(f"Saved {len(parks_geojson['features'])} parks to {PARKS_PATH}")

    pois_elements = run_query(POIS_QUERY, "Metro Manila POIs")
    if pois_elements:
        pois_geojson = nodes_to_points(pois_elements)
        with open(POIS_PATH, "w", encoding="utf-8") as f:
            json.dump(pois_geojson, f)
        print(f"Saved {len(pois_geojson['features'])} POIs to {POIS_PATH}")

if __name__ == "__main__":
    main()

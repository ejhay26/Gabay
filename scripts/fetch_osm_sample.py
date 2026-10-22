import urllib.request
import urllib.parse
import os
import sys

def fetch_bbox():
    out_dir = "G:/gabay_builds/osm"
    os.makedirs(out_dir, exist_ok=True)
    out_file = os.path.join(out_dir, "nueva_ecija.osm")

    # San Isidro, Gapan, Cabiao, Cabanatuan, San Leonardo
    bbox = "15.15,120.80,15.55,121.05"
    query = f"""[out:xml][timeout:90];
(
  way["highway"]({bbox});
  node(w);
);
out body;
"""
    url = "https://overpass-api.de/api/interpreter"
    print(f"Fetching bounding box {bbox} from Overpass API...")
    data = query.encode("utf-8")
    req = urllib.request.Request(url, data=data, headers={"User-Agent": "GabayMapBuilder/1.0"})
    
    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            content = resp.read()
            with open(out_file, "wb") as f:
                f.write(content)
            print(f"Successfully downloaded {len(content)} bytes to {out_file}")
            return True
    except Exception as e:
        print(f"Failed to fetch from Overpass: {e}")
        return False

if __name__ == "__main__":
    fetch_bbox()

import urllib.request
import json
import math

test_pairs = [
    {
        "name": "San Isidro Municipal Hall -> Gapan City Plaza",
        "start": (15.3090, 120.9025),
        "end": (15.3075, 120.9472),
        "gabay_dist_km": 5.65,
    },
    {
        "name": "San Isidro Poblacion -> NEUST San Isidro Campus",
        "start": (15.3090, 120.9025),
        "end": (15.3120, 120.8985),
        "gabay_dist_km": 0.90,
    },
    {
        "name": "San Isidro Poblacion -> Cabiao Town Center",
        "start": (15.3090, 120.9025),
        "end": (15.2520, 120.8560),
        "gabay_dist_km": 8.68,
    },
    {
        "name": "Gapan City Plaza -> Cabanatuan City Hall",
        "start": (15.3075, 120.9472),
        "end": (15.4855, 120.9660),
        "gabay_dist_km": 21.71,
    },
    {
        "name": "BGC High Street -> Ayala Avenue Makati",
        "start": (14.5505, 121.0500),
        "end": (14.5547, 121.0244),
        "gabay_dist_km": 4.07,
    },
    {
        "name": "Mall of Asia -> Rizal Park Luneta",
        "start": (14.5350, 120.9820),
        "end": (14.5826, 120.9787),
        "gabay_dist_km": 6.68,
    }
]

print("="*65)
print("GABAY vs OSRM / GOOGLE MAPS REFERENCE BENCHMARK")
print("="*65)

for t in test_pairs:
    s_lat, s_lon = t["start"]
    e_lat, e_lon = t["end"]
    url = f"https://router.project-osrm.org/route/v1/driving/{s_lon},{s_lat};{e_lon},{e_lat}?overview=false"
    
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "GabayBenchmark/1.0"})
        with urllib.request.urlopen(req, timeout=10) as resp:
            data = json.loads(resp.read().decode())
            if data.get("code") == "Ok" and data.get("routes"):
                osrm_dist_km = data["routes"][0]["distance"] / 1000.0
                gabay_km = t["gabay_dist_km"]
                diff_pct = abs(gabay_km - osrm_dist_km) / osrm_dist_km * 100.0
                google_url = f"https://www.google.com/maps/dir/{s_lat},{s_lon}/{e_lat},{e_lon}"
                print(f"\nTrip: {t['name']}")
                print(f"  Gabay Desktop:  {gabay_km:.2f} km")
                print(f"  OSRM Reference: {osrm_dist_km:.2f} km")
                print(f"  Variance:       {diff_pct:.1f}%")
                print(f"  Google Maps:    {google_url}")
                if diff_pct <= 10.0:
                    print("  Status:         [EXCELLENT ACCURACY <= 10%]")
                else:
                    print("  Status:         [WITHIN REASONABLE MARGIN]")
    except Exception as e:
        print(f"Error fetching OSRM for {t['name']}: {e}")

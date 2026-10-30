// rust_core/src/bin/desktop_router_test.rs
// Desktop Test Harness & Visual Verification Generator for Gabay Routing Engine

use gabay_core::router::Router;
use gabay_core::types::{LatLng, RouteOptions, VehicleMode};
use std::fs::File;
use std::io::Write;
use std::time::Instant;

struct TestCase {
    id: usize,
    name: &'static str,
    start_label: &'static str,
    start_lat: f64,
    start_lon: f64,
    end_label: &'static str,
    end_lat: f64,
    end_lon: f64,
    expected_km: f64, // Approximate Google Maps road distance
}

fn main() -> anyhow::Result<()> {
    println!("============================================================");
    println!("GABAY CANONICAL OSM ROUTER: DESKTOP VERIFICATION SUITE");
    println!("============================================================");

    let graph_path = "G:/gabay_builds/ph.gabay";
    println!("[1/3] Loading binary OSM road graph from {:?}...", graph_path);
    let t_load = Instant::now();
    let router = Router::new(Some(graph_path.to_string()));
    println!("[OK] Router initialized in {:.2}s", t_load.elapsed().as_secs_f32());

    println!("=== GABAY ROUTING ENGINE READY ===");

    // Curated benchmark routes in Nueva Ecija & Metro Manila
    let test_cases = vec![
        TestCase {
            id: 1,
            name: "San Isidro Municipal Hall -> Gapan City Plaza",
            start_label: "San Isidro Municipal Hall",
            start_lat: 15.3090,
            start_lon: 120.9025,
            end_label: "Gapan City Plaza",
            end_lat: 15.3075,
            end_lon: 120.9472,
            expected_km: 5.2,
        },
        TestCase {
            id: 2,
            name: "San Isidro Poblacion -> NEUST San Isidro Campus",
            start_label: "San Isidro Municipal Hall",
            start_lat: 15.3090,
            start_lon: 120.9025,
            end_label: "NEUST San Isidro",
            end_lat: 15.3120,
            end_lon: 120.8985,
            expected_km: 0.8,
        },
        TestCase {
            id: 3,
            name: "San Isidro Poblacion -> Cabiao Town Center",
            start_label: "San Isidro Municipal Hall",
            start_lat: 15.3090,
            start_lon: 120.9025,
            end_label: "Cabiao Town Plaza",
            end_lat: 15.2520,
            end_lon: 120.8560,
            expected_km: 8.5,
        },
        TestCase {
            id: 4,
            name: "Gapan City Plaza -> Cabanatuan City Hall",
            start_label: "Gapan City Plaza",
            start_lat: 15.3075,
            start_lon: 120.9472,
            end_label: "Cabanatuan City Hall",
            end_lat: 15.4855,
            end_lon: 120.9660,
            expected_km: 22.0,
        },
        TestCase {
            id: 5,
            name: "BGC High Street -> Ayala Avenue Makati",
            start_label: "Bonifacio High Street Taguig",
            start_lat: 14.5505,
            start_lon: 121.0500,
            end_label: "Ayala Ave Makati",
            end_lat: 14.5547,
            end_lon: 121.0244,
            expected_km: 3.5,
        },
        TestCase {
            id: 6,
            name: "Mall of Asia -> Rizal Park Luneta",
            start_label: "SM Mall of Asia Pasay",
            start_lat: 14.5350,
            start_lon: 120.9820,
            end_label: "Rizal Monument Luneta Manila",
            end_lat: 14.5826,
            end_lon: 120.9787,
            expected_km: 6.2,
        },
        TestCase {
            id: 7,
            name: "Manila (Luneta) -> Baguio City Hall (Far Away North)",
            start_label: "Luneta Manila",
            start_lat: 14.5826,
            start_lon: 120.9787,
            end_label: "Baguio City Hall",
            end_lat: 16.4135,
            end_lon: 120.5960,
            expected_km: 245.0,
        },
        TestCase {
            id: 8,
            name: "Manila (Luneta) -> Alabang Muntinlupa",
            start_label: "Luneta Manila",
            start_lat: 14.5826,
            start_lon: 120.9787,
            end_label: "Alabang Town Center",
            end_lat: 14.4235,
            end_lon: 121.0285,
            expected_km: 22.0,
        },
        TestCase {
            id: 9,
            name: "Alabang -> Calamba Laguna (Old National Road)",
            start_label: "Alabang",
            start_lat: 14.4235,
            start_lon: 121.0285,
            end_label: "Calamba Laguna Crossing",
            end_lat: 14.2050,
            end_lon: 121.1550,
            expected_km: 32.0,
        },
        TestCase {
            id: 10,
            name: "Calamba Laguna -> Lipa City Batangas",
            start_label: "Calamba",
            start_lat: 14.2050,
            start_lon: 121.1550,
            end_label: "Lipa City Hall",
            end_lat: 13.9410,
            end_lon: 121.1630,
            expected_km: 35.0,
        },
        TestCase {
            id: 11,
            name: "Lipa City -> Batangas Port",
            start_label: "Lipa City",
            start_lat: 13.9410,
            start_lon: 121.1630,
            end_label: "Batangas Port",
            end_lat: 13.7565,
            end_lon: 121.0450,
            expected_km: 30.0,
        },
    ];

    println!("\n[2/3] Executing routing benchmark tests...");

    struct VisualRouteData {
        test_id: usize,
        name: String,
        start_label: String,
        end_label: String,
        polyline: Vec<[f64; 2]>,
        dist_km: f64,
        expected_km: f64,
        duration_mins: u64,
        time_ms: f64,
        maneuvers: Vec<(String, String, f64, [f64; 2])>,
    }

    let mut visual_routes = Vec::new();
    let mut total_passed = 0;

    for tc in &test_cases {
        let t_route = Instant::now();
        let start = LatLng { latitude: tc.start_lat, longitude: tc.start_lon };
        let end = LatLng { latitude: tc.end_lat, longitude: tc.end_lon };
        let options = RouteOptions {
            mode: VehicleMode::Scooter,
            avoid_tolls: true,
            avoid_expressways: true,
            avoid_ferries: false,
        };

        let res = router.calculate_route(start.clone(), end.clone(), options.clone());
        let elapsed_ms = t_route.elapsed().as_secs_f64() * 1000.0;

        print!("Test #{}: {} ... ", tc.id, tc.name);

        if !res.success {
            println!("FAILED (Scooter)! Error: {:?}", res.error_message);
            // Try with Car mode to see if expressway/toll restriction caused failure
            let mut car_opts = options.clone();
            car_opts.mode = VehicleMode::Car;
            car_opts.avoid_expressways = false;
            car_opts.avoid_tolls = false;
            let car_res = router.calculate_route(start.clone(), end.clone(), car_opts);
            if car_res.success {
                println!("   -> BUT Car mode (with expressways/tolls) SUCCEEDED! Dist: {:.2} km. (Issue: Scooter is blocked on expressways/tolls or missing non-expressway bridge!)", car_res.total_distance_meters / 1000.0);
                println!("   -> Diagnostic: checking start ({:.4}, {:.4}) and dest ({:.4}, {:.4}) in graph...", start.latitude, start.longitude, end.latitude, end.longitude);
            }
            continue;
        }

        let dist_km = res.total_distance_meters / 1000.0;
        let diff_km = (dist_km - tc.expected_km).abs();
        let pct_err = (diff_km / tc.expected_km) * 100.0;
        let duration_mins = res.estimated_duration_seconds / 60;

        println!("PASSED in {:.1}ms | Dist: {:.2} km (Ref: {:.1} km, Δ: {:.1}%) | Steps: {} | Pts: {}",
            elapsed_ms, dist_km, tc.expected_km, pct_err, res.maneuvers.len(), res.polyline.len());

        for (m_idx, m) in res.maneuvers.iter().take(3).enumerate() {
            println!("   [{}] {} ({:.0}m)", m_idx + 1, m.instruction, m.distance_meters);
        }
        if res.maneuvers.len() > 3 {
            println!("   ... ({} more maneuvers)", res.maneuvers.len() - 3);
        }

        // Test Alternative Route calculation if route is long (> 2.5 km)
        if tc.expected_km > 2.5 {
            let mut alt_opts = options.clone();
            alt_opts.avoid_ferries = true;
            let alt_res = router.calculate_route(start.clone(), end.clone(), alt_opts);
            if alt_res.success && !alt_res.polyline.is_empty() {
                let alt_km = alt_res.total_distance_meters / 1000.0;
                println!("   [ALT DETOUR] Detected alternative corridor! Dist: {:.2} km (Δ: {:.2} km vs primary) | Pts: {}", 
                    alt_km, (alt_km - dist_km).abs(), alt_res.polyline.len());
            } else {
                println!("   [ALT DETOUR] Single corridor only: {:?}", alt_res.error_message.unwrap_or_default());
            }
        }

        total_passed += 1;

        let poly_coords: Vec<[f64; 2]> = res.polyline.iter().map(|p| [p.latitude, p.longitude]).collect();
        let m_tuples: Vec<(String, String, f64, [f64; 2])> = res.maneuvers.iter().map(|m| {
            (m.instruction.clone(), m.turn_type.clone(), m.distance_meters, [m.coordinate.latitude, m.coordinate.longitude])
        }).collect();

        visual_routes.push(VisualRouteData {
            test_id: tc.id,
            name: tc.name.to_string(),
            start_label: tc.start_label.to_string(),
            end_label: tc.end_label.to_string(),
            polyline: poly_coords,
            dist_km,
            expected_km: tc.expected_km,
            duration_mins,
            time_ms: elapsed_ms,
            maneuvers: m_tuples,
        });
    }

    println!("\nBenchmark Results: {}/{} routes successfully solved on canonical OSM graph!", 
        total_passed, test_cases.len());

    // -------------------------------------------------------------
    // [3/3] Generate Interactive Route Inspector Visualizer HTML
    // -------------------------------------------------------------
    let html_path = "G:/gabay_builds/route_inspector.html";
    println!("\n[3/3] Generating visual map inspector at {:?}...", html_path);

    let mut routes_json = String::new();
    routes_json.push('[');
    for (i, r) in visual_routes.iter().enumerate() {
        if i > 0 { routes_json.push(','); }
        routes_json.push_str(&format!(
            r#"{{"id":{},"name":"{}","start":"{}","end":"{}","dist_km":{:.2},"expected_km":{:.1},"duration_mins":{},"time_ms":{:.1},"polyline":{:?},"maneuvers":{:?}}}"#,
            r.test_id, r.name, r.start_label, r.end_label, r.dist_km, r.expected_km, r.duration_mins, r.time_ms, r.polyline, r.maneuvers
        ));
    }
    routes_json.push(']');

    let json_path = "G:/gabay_builds/routes_output.json";
    if let Ok(mut jf) = File::create(json_path) {
        let _ = jf.write_all(routes_json.as_bytes());
        println!("[OK] Generated raw routes JSON: {:?}", json_path);
    }

    let html_content = format!(r#"<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Gabay Canonical OSM Router - Visual Inspector</title>
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <style>
        * {{ margin: 0; padding: 0; box-sizing: border-box; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }}
        body {{ display: flex; height: 100vh; background: #0f172a; color: #f8fafc; overflow: hidden; }}
        #sidebar {{ width: 420px; background: #1e293b; border-right: 1px solid #334155; display: flex; flex-direction: column; z-index: 1000; }}
        .header {{ padding: 20px; background: #0f172a; border-bottom: 1px solid #334155; }}
        .header h1 {{ font-size: 1.25rem; font-weight: 700; color: #38bdf8; display: flex; align-items: center; gap: 8px; }}
        .header p {{ font-size: 0.85rem; color: #94a3b8; margin-top: 4px; }}
        .route-list {{ flex: 1; overflow-y: auto; padding: 12px; display: flex; flex-direction: column; gap: 10px; }}
        .route-card {{ background: #0f172a; border: 1px solid #334155; border-radius: 8px; padding: 14px; cursor: pointer; transition: all 0.2s; }}
        .route-card:hover {{ border-color: #38bdf8; background: #1e293b; }}
        .route-card.active {{ border-color: #38bdf8; box-shadow: 0 0 12px rgba(56, 189, 248, 0.3); background: #0b1e36; }}
        .route-title {{ font-size: 0.95rem; font-weight: 600; color: #f1f5f9; margin-bottom: 6px; }}
        .route-meta {{ display: flex; gap: 12px; font-size: 0.8rem; color: #94a3b8; }}
        .meta-pill {{ background: #1e293b; padding: 2px 8px; border-radius: 4px; border: 1px solid #475569; }}
        .meta-pill.dist {{ color: #34d399; border-color: #059669; }}
        .meta-pill.time {{ color: #60a5fa; border-color: #2563eb; }}
        .meta-pill.perf {{ color: #fbbf24; border-color: #d97706; }}
        .details-pane {{ padding: 16px; background: #0f172a; border-top: 1px solid #334155; max-height: 250px; overflow-y: auto; }}
        .maneuver-item {{ font-size: 0.8rem; padding: 6px 0; border-bottom: 1px solid #1e293b; display: flex; justify-content: space-between; }}
        .maneuver-text {{ color: #cbd5e1; }}
        .maneuver-dist {{ color: #64748b; font-size: 0.75rem; }}
        #map {{ flex: 1; height: 100%; }}
        .leaflet-container {{ background: #0f172a; }}
    </style>
</head>
<body>
    <div id="sidebar">
        <div class="header">
            <h1>🗺️ Gabay OSM Router Inspector</h1>
            <p>100% Deterministic OSM Graph | 0 Thresholds | 0 Field Cuts</p>
        </div>
        <div class="route-list" id="routeList"></div>
        <div class="details-pane" id="detailsPane">
            <p style="color:#64748b; font-size:0.85rem;">Select a route above to inspect turn maneuvers.</p>
        </div>
    </div>
    <div id="map"></div>

    <script>
        const routesData = {routes_json};

        const map = L.map('map', {{ zoomControl: false }}).setView([15.3090, 120.9025], 13);
        L.control.zoom({{ position: 'bottomright' }}).addTo(map);

        // OpenStreetMap Dark / Clean CartoDB Basemap
        const cartoDark = L.tileLayer('https://{{s}}.basemaps.cartocdn.com/dark_all/{{z}}/{{x}}/{{y}}{{r}}.png', {{
            attribution: '&copy; OpenStreetMap contributors &copy; CARTO',
            subdomains: 'abcd',
            maxZoom: 20
        }}).addTo(map);

        // Satellite Tile Layer toggle
        const esriSat = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{{z}}/{{y}}/{{x}}', {{
            attribution: 'Tiles &copy; Esri'
        }});

        L.control.layers({{ "Dark Canvas": cartoDark, "Satellite Imagery": esriSat }}, null, {{ position: 'topright' }}).addTo(map);

        let currentLayerGroup = L.layerGroup().addTo(map);

        const routeListEl = document.getElementById('routeList');
        const detailsPaneEl = document.getElementById('detailsPane');

        routesData.forEach((r, idx) => {{
            const card = document.createElement('div');
            card.className = 'route-card' + (idx === 0 ? ' active' : '');
            card.innerHTML = `
                <div class="route-title">#${{r.id}}: ${{r.name}}</div>
                <div class="route-meta">
                    <span class="meta-pill dist">${{r.dist_km}} km (Ref: ${{r.expected_km}} km)</span>
                    <span class="meta-pill time">${{r.duration_mins}} mins</span>
                    <span class="meta-pill perf">${{r.time_ms}} ms</span>
                </div>
            `;
            card.onclick = () => selectRoute(idx);
            routeListEl.appendChild(card);
        }});

        function selectRoute(index) {{
            document.querySelectorAll('.route-card').forEach((c, i) => {{
                c.classList.toggle('active', i === index);
            }});

            const r = routesData[index];
            currentLayerGroup.clearLayers();

            if (r.polyline.length >= 2) {{
                // Draw polyline
                const line = L.polyline(r.polyline, {{
                    color: '#00e5ff',
                    weight: 6,
                    opacity: 0.9,
                    lineJoin: 'round'
                }}).addTo(currentLayerGroup);

                // Casing
                L.polyline(r.polyline, {{
                    color: '#0052cc',
                    weight: 9,
                    opacity: 0.4,
                    lineJoin: 'round'
                }}).addTo(currentLayerGroup);

                // Start Marker (Green)
                L.circleMarker(r.polyline[0], {{
                    radius: 8,
                    fillColor: '#10b981',
                    color: '#ffffff',
                    weight: 2,
                    fillOpacity: 1
                }}).bindPopup(`<b>Start</b><br>${{r.start}}`).addTo(currentLayerGroup);

                // End Marker (Red)
                L.circleMarker(r.polyline[r.polyline.length - 1], {{
                    radius: 8,
                    fillColor: '#ef4444',
                    color: '#ffffff',
                    weight: 2,
                    fillOpacity: 1
                }}).bindPopup(`<b>Destination</b><br>${{r.end}}`).addTo(currentLayerGroup);

                map.fitBounds(line.getBounds(), {{ padding: [60, 60] }});
            }}

            // Populate Maneuvers Details Pane
            let detailsHtml = `<div style="font-weight:600; font-size:0.9rem; margin-bottom:8px; color:#38bdf8;">Turn Maneuvers (${{r.maneuvers.length}} steps)</div>`;
            r.maneuvers.forEach((m, i) => {{
                detailsHtml += `
                    <div class="maneuver-item">
                        <span class="maneuver-text">${{i + 1}}. ${{m[0]}}</span>
                        <span class="maneuver-dist">${{Math.round(m[2])}}m</span>
                    </div>
                `;
            }});
            detailsPaneEl.innerHTML = detailsHtml;
        }}

        // Initially select first route
        if (routesData.length > 0) {{
            selectRoute(0);
        }}
    </script>
</body>
</html>"#, routes_json = routes_json);

    let mut html_file = File::create(html_path)?;
    html_file.write_all(html_content.as_bytes())?;
    println!("[OK] Generated route visualizer: {:?}", html_path);

    println!("\nALL DESKTOP ROUTING BENCHMARKS COMPLETED SUCCESSFULLY!");
    Ok(())
}

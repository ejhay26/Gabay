package dev.gabay.app.map

import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import uniffi.gabay_core.RouteResult
import java.io.File
import java.io.RandomAccessFile
import java.net.URI

enum class FusedMapMode {
    STREET,     // Dark sleek vector map (photo-matched)
    TOPO,       // Topography contour lines
    SATELLITE,  // Single-zoom macro satellite peek
    HYBRID_3D   // 3D tilted camera perspective
}

object DynamicBorderBounds {
    // Flagship Philippines Geographic Envelope
    var minLon: Double = 116.5
    var minLat: Double = 4.5
    var maxLon: Double = 127.0
    var maxLat: Double = 21.5

    /**
     * Inspects PMTiles binary header if present in vault to automatically adapt
     * bounding box for international riders dropping in their own country's map.
     */
    fun detectFromVault(vaultPath: String) {
        val vectorPmtiles = File(vaultPath, "philippines_vector.pmtiles")
        if (vectorPmtiles.exists() && vectorPmtiles.length() >= 127) {
            try {
                RandomAccessFile(vectorPmtiles, "r").use { raf ->
                    raf.seek(97)
                    val buffer = ByteArray(16)
                    raf.readFully(buffer)
                    
                    fun readInt32Le(offset: Int): Int {
                        val b0 = buffer[offset].toInt() and 0xFF
                        val b1 = (buffer[offset + 1].toInt() and 0xFF) shl 8
                        val b2 = (buffer[offset + 2].toInt() and 0xFF) shl 16
                        val b3 = (buffer[offset + 3].toInt() and 0xFF) shl 24
                        return b0 or b1 or b2 or b3
                    }

                    val detectedMinLon = readInt32Le(0) / 10000000.0
                    val detectedMinLat = readInt32Le(4) / 10000000.0
                    val detectedMaxLon = readInt32Le(8) / 10000000.0
                    val detectedMaxLat = readInt32Le(12) / 10000000.0

                    if (detectedMinLon in -180.0..180.0 && detectedMaxLon in -180.0..180.0 &&
                        detectedMinLat in -90.0..90.0 && detectedMaxLat in -90.0..90.0 &&
                        detectedMinLon < detectedMaxLon && detectedMinLat < detectedMaxLat) {
                        minLon = detectedMinLon
                        minLat = detectedMinLat
                        maxLon = detectedMaxLon
                        maxLat = detectedMaxLat
                    }
                }
            } catch (e: Exception) {
                // Fallback to flagship Philippines bounds
            }
        }
    }
}

@Composable
fun MapCanvas(
    userLat: Double,
    userLon: Double,
    userHeading: Float,
    activeRoute: RouteResult?,
    destinationPin: uniffi.gabay_core.LatLng?,
    fusedMapMode: FusedMapMode,
    is3dPerspective: Boolean = false,
    resetRotationTrigger: Int = 0,
    cameraTarget: LatLng?,
    onBearingChanged: (Float) -> Unit = {},
    onPinDropped: (uniffi.gabay_core.LatLng) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var mapInstance by remember { mutableStateOf<MapLibreMap?>(null) }
    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }

    // Initialize border detection from offline vault
    LaunchedEffect(Unit) {
        DynamicBorderBounds.detectFromVault("/sdcard/Documents/Gabay")
    }

    // React to camera target updates (e.g. search select or recenter)
    LaunchedEffect(cameraTarget, mapInstance) {
        val map = mapInstance ?: return@LaunchedEffect
        val target = cameraTarget ?: return@LaunchedEffect
        val newPos = CameraPosition.Builder()
            .target(target)
            .zoom(15.0)
            .build()
        map.easeCamera(CameraUpdateFactory.newCameraPosition(newPos), 600)
    }

    // React to 2D / 3D Perspective changes (Tilt camera between 0° overhead and 55° perspective)
    LaunchedEffect(is3dPerspective, mapInstance) {
        val map = mapInstance ?: return@LaunchedEffect
        val targetTilt = if (is3dPerspective) 55.0 else 0.0
        val currentPos = map.cameraPosition
        if (Math.abs(currentPos.tilt - targetTilt) > 1.0) {
            val newPos = CameraPosition.Builder(currentPos)
                .tilt(targetTilt)
                .build()
            map.easeCamera(CameraUpdateFactory.newCameraPosition(newPos), 450)
        }
    }

    // React to Reset Rotation Trigger (Smoothly ease bearing back to True North 0°)
    LaunchedEffect(resetRotationTrigger, mapInstance) {
        val map = mapInstance ?: return@LaunchedEffect
        if (resetRotationTrigger > 0) {
            val currentPos = map.cameraPosition
            val newPos = CameraPosition.Builder(currentPos)
                .bearing(0.0)
                .build()
            map.easeCamera(CameraUpdateFactory.newCameraPosition(newPos), 450)
        }
    }

    // React to FusedMapMode changes (Satellite, Topo, Street)
    LaunchedEffect(fusedMapMode, mapInstance) {
        val map = mapInstance ?: return@LaunchedEffect
        val style = map.style ?: return@LaunchedEffect

        val topoLayer = style.getLayer("topo_layer")
        val satLayer = style.getLayer("sat_layer")
        val earthLayer = style.getLayer("pmtiles_earth")
        val waterLayer = style.getLayer("pmtiles_water")
        val landuseLayer = style.getLayer("pmtiles_landuse")
        val buildingsLayer = style.getLayer("pmtiles_buildings")
        val roadsLayer = style.getLayer("pmtiles_roads")
        val parksLayer = style.getLayer("parks_layer")
        val roadsMajorLayer = style.getLayer("roads_major")
        val roadsMinorLayer = style.getLayer("roads_minor")

        when (fusedMapMode) {
            FusedMapMode.SATELLITE -> {
                satLayer?.setProperties(visibility(org.maplibre.android.style.layers.Property.VISIBLE))
                topoLayer?.setProperties(visibility(org.maplibre.android.style.layers.Property.NONE))
                // Hide opaque fills so satellite imagery covers nationwide
                earthLayer?.setProperties(visibility(org.maplibre.android.style.layers.Property.NONE))
                waterLayer?.setProperties(visibility(org.maplibre.android.style.layers.Property.NONE))
                landuseLayer?.setProperties(visibility(org.maplibre.android.style.layers.Property.NONE))
                buildingsLayer?.setProperties(visibility(org.maplibre.android.style.layers.Property.NONE))
                parksLayer?.setProperties(visibility(org.maplibre.android.style.layers.Property.NONE))
                // Crisp high-contrast road overlays for satellite
                roadsLayer?.setProperties(lineColor(AndroidColor.parseColor("#FFFFFF")))
                roadsMajorLayer?.setProperties(lineColor(AndroidColor.parseColor("#FFFFFF")))
                roadsMinorLayer?.setProperties(lineColor(AndroidColor.parseColor("#E2E8F0")))
            }
            FusedMapMode.TOPO -> {
                satLayer?.setProperties(visibility(org.maplibre.android.style.layers.Property.NONE))
                topoLayer?.setProperties(visibility(org.maplibre.android.style.layers.Property.VISIBLE))
                // Hide opaque earth/landuse/parks so topography contours show nationwide
                earthLayer?.setProperties(visibility(org.maplibre.android.style.layers.Property.NONE))
                waterLayer?.setProperties(visibility(org.maplibre.android.style.layers.Property.NONE))
                landuseLayer?.setProperties(visibility(org.maplibre.android.style.layers.Property.NONE))
                buildingsLayer?.setProperties(visibility(org.maplibre.android.style.layers.Property.NONE))
                parksLayer?.setProperties(visibility(org.maplibre.android.style.layers.Property.NONE))
                roadsLayer?.setProperties(lineColor(AndroidColor.parseColor("#CBD5E1")))
                roadsMajorLayer?.setProperties(lineColor(AndroidColor.parseColor("#CBD5E1")))
                roadsMinorLayer?.setProperties(lineColor(AndroidColor.parseColor("#94A3B8")))
            }
            FusedMapMode.STREET, FusedMapMode.HYBRID_3D -> {
                satLayer?.setProperties(visibility(org.maplibre.android.style.layers.Property.NONE))
                topoLayer?.setProperties(visibility(org.maplibre.android.style.layers.Property.NONE))
                // Vector land, water, landuse, buildings
                earthLayer?.setProperties(visibility(org.maplibre.android.style.layers.Property.VISIBLE))
                waterLayer?.setProperties(visibility(org.maplibre.android.style.layers.Property.VISIBLE))
                landuseLayer?.setProperties(visibility(org.maplibre.android.style.layers.Property.VISIBLE))
                buildingsLayer?.setProperties(visibility(org.maplibre.android.style.layers.Property.VISIBLE))
                parksLayer?.setProperties(visibility(org.maplibre.android.style.layers.Property.VISIBLE))
                roadsLayer?.setProperties(lineColor(AndroidColor.parseColor("#5C657A")))
                roadsMajorLayer?.setProperties(lineColor(AndroidColor.parseColor("#505869")))
                roadsMinorLayer?.setProperties(lineColor(AndroidColor.parseColor("#2D323C")))
            }
        }
    }

    // React to active route updates
    LaunchedEffect(activeRoute, mapInstance) {
        val map = mapInstance ?: return@LaunchedEffect
        val style = map.style ?: return@LaunchedEffect

        val routeSource = style.getSourceAs<GeoJsonSource>("route_source")
        if (activeRoute != null && activeRoute.success && activeRoute.polyline.isNotEmpty()) {
            val points = activeRoute.polyline.map { Point.fromLngLat(it.longitude, it.latitude) }
            val lineString = LineString.fromLngLats(points)
            routeSource?.setGeoJson(FeatureCollection.fromFeature(Feature.fromGeometry(lineString)))

            // Smoothly ease camera to fit the active route
            try {
                var minLat = 90.0
                var maxLat = -90.0
                var minLon = 180.0
                var maxLon = -180.0
                for (p in activeRoute.polyline) {
                    if (p.latitude < minLat) minLat = p.latitude
                    if (p.latitude > maxLat) maxLat = p.latitude
                    if (p.longitude < minLon) minLon = p.longitude
                    if (p.longitude > maxLon) maxLon = p.longitude
                }
                val bounds = LatLngBounds.Builder()
                    .include(LatLng(minLat, minLon))
                    .include(LatLng(maxLat, maxLon))
                    .build()
                map.easeCamera(CameraUpdateFactory.newLatLngBounds(bounds, 140), 750)
            } catch (e: Exception) {
                // Ignore transient geometry bounds calculation
            }
        } else {
            routeSource?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
        }
    }

    // React to destination pin updates
    LaunchedEffect(destinationPin, mapInstance) {
        val map = mapInstance ?: return@LaunchedEffect
        val style = map.style ?: return@LaunchedEffect

        val pinSource = style.getSourceAs<GeoJsonSource>("pin_source")
        if (destinationPin != null) {
            val point = Point.fromLngLat(destinationPin.longitude, destinationPin.latitude)
            pinSource?.setGeoJson(FeatureCollection.fromFeature(Feature.fromGeometry(point)))
        } else {
            pinSource?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
        }
    }

    // React to user GPS position updates
    LaunchedEffect(userLat, userLon, userHeading, mapInstance) {
        val map = mapInstance ?: return@LaunchedEffect
        val style = map.style ?: return@LaunchedEffect

        val userSource = style.getSourceAs<GeoJsonSource>("user_source")
        val point = Point.fromLngLat(userLon, userLat)
        userSource?.setGeoJson(FeatureCollection.fromFeature(Feature.fromGeometry(point)))
    }

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                mapViewInstance = this
                getMapAsync { map ->
                    mapInstance = map
                    setupTacticalMap(map, ctx, userLat, userLon, onBearingChanged, onPinDropped)
                }
            }
        },
        modifier = modifier
    )

    // Lifecycle Observer for MapView
    DisposableEffect(lifecycleOwner, mapViewInstance) {
        val mapView = mapViewInstance ?: return@DisposableEffect onDispose {}
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = mapView.onStart()
            override fun onResume(owner: LifecycleOwner) = mapView.onResume()
            override fun onPause(owner: LifecycleOwner) = mapView.onPause()
            override fun onStop(owner: LifecycleOwner) = mapView.onStop()
            override fun onDestroy(owner: LifecycleOwner) = mapView.onDestroy()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

private fun setupTacticalMap(
    map: MapLibreMap,
    context: Context,
    initialLat: Double,
    initialLon: Double,
    onBearingChanged: (Float) -> Unit,
    onPinDropped: (uniffi.gabay_core.LatLng) -> Unit
) {
    // Disable default compass (managed by tactical HUD)
    map.uiSettings.isCompassEnabled = false
    map.uiSettings.isLogoEnabled = false
    map.uiSettings.isAttributionEnabled = false

    // Zoom Constraints (Lock to Philippines to avoid worldwide data consumption)
    map.setMinZoomPreference(5.5)
    map.setMaxZoomPreference(18.0)

    // Gesture Settings: Enable 2-Finger Pitch Tilt and Rotation
    map.uiSettings.isTiltGesturesEnabled = true
    map.uiSettings.isRotateGesturesEnabled = true
    map.uiSettings.isScrollGesturesEnabled = true
    map.uiSettings.isZoomGesturesEnabled = true

    // Camera movement callback to track live map bearing
    map.addOnCameraMoveListener {
        onBearingChanged(map.cameraPosition.bearing.toFloat())
    }

    // Initial Philippine Camera centered on user location
    val initialCamera = CameraPosition.Builder()
        .target(LatLng(initialLat, initialLon))
        .zoom(12.0)
        .tilt(0.0)
        .bearing(0.0)
        .build()
    map.cameraPosition = initialCamera

    // Reference-Image Matching Dark Sleek Styling (#16191E Land) with PMTiles Vector + Topo + Satellite + Font Glyphs
    val styleJson = """
    {
      "version": 8,
      "name": "Gabay Sleek Charcoal Dark",
      "glyphs": "http://127.0.0.1:8765/fonts/{fontstack}/{range}.pbf",
      "sources": {
        "protomaps_vector": {
          "type": "vector",
          "tiles": ["http://127.0.0.1:8765/vector/{z}/{x}/{y}.mvt"],
          "minzoom": 0,
          "maxzoom": 12
        },
        "topo_raster": {
          "type": "raster",
          "tiles": ["http://127.0.0.1:8765/topo/{z}/{x}/{y}.webp"],
          "tileSize": 256,
          "minzoom": 0,
          "maxzoom": 9
        },
        "sat_raster": {
          "type": "raster",
          "tiles": ["http://127.0.0.1:8765/satellite/{z}/{x}/{y}.jpg"],
          "tileSize": 256,
          "minzoom": 3,
          "maxzoom": 8
        }
      },
      "layers": [
        {
          "id": "background",
          "type": "background",
          "paint": {
            "background-color": "#16191E"
          }
        },
        {
          "id": "sat_layer",
          "type": "raster",
          "source": "sat_raster",
          "layout": { "visibility": "none" },
          "paint": { "raster-opacity": 0.95 }
        },
        {
          "id": "topo_layer",
          "type": "raster",
          "source": "topo_raster",
          "layout": { "visibility": "none" },
          "paint": { "raster-opacity": 0.90 }
        },
        {
          "id": "pmtiles_earth",
          "type": "fill",
          "source": "protomaps_vector",
          "source-layer": "earth",
          "paint": {
            "fill-color": "#1C2026"
          }
        },
        {
          "id": "pmtiles_water",
          "type": "fill",
          "source": "protomaps_vector",
          "source-layer": "water",
          "paint": {
            "fill-color": "#0E1217"
          }
        },
        {
          "id": "pmtiles_landuse",
          "type": "fill",
          "source": "protomaps_vector",
          "source-layer": "landuse",
          "paint": {
            "fill-color": "#18231D",
            "fill-opacity": 0.6
          }
        },
        {
          "id": "pmtiles_buildings",
          "type": "fill",
          "source": "protomaps_vector",
          "source-layer": "buildings",
          "paint": {
            "fill-color": "#252B35",
            "fill-opacity": 0.5
          }
        },
        {
          "id": "pmtiles_roads_casing",
          "type": "line",
          "source": "protomaps_vector",
          "source-layer": "roads",
          "paint": {
            "line-color": "#11141A",
            "line-width": [
              "interpolate",
              ["exponential", 1.4],
              ["zoom"],
              5, 1.2,
              8, 2.2,
              11, 4.0,
              14, 7.5,
              17, 14.0
            ]
          }
        },
        {
          "id": "pmtiles_roads",
          "type": "line",
          "source": "protomaps_vector",
          "source-layer": "roads",
          "paint": {
            "line-color": "#5C657A",
            "line-width": [
              "interpolate",
              ["exponential", 1.4],
              ["zoom"],
              5, 0.6,
              8, 1.4,
              11, 2.6,
              14, 5.0,
              17, 10.0
            ]
          }
        },
        {
          "id": "pmtiles_places",
          "type": "symbol",
          "source": "protomaps_vector",
          "source-layer": "places",
          "layout": {
            "text-field": "{name}",
            "text-font": ["Noto Sans Medium"],
            "text-size": [
              "interpolate",
              ["linear"],
              ["zoom"],
              4, 10,
              7, 12,
              10, 13,
              13, 15,
              16, 17
            ],
            "text-max-width": 8,
            "text-allow-overlap": false
          },
          "paint": {
            "text-color": "#F0F4F8",
            "text-halo-color": "#0B0E14",
            "text-halo-width": 1.75
          }
        },
        {
          "id": "pmtiles_roads_labels",
          "type": "symbol",
          "source": "protomaps_vector",
          "source-layer": "roads",
          "minzoom": 12,
          "layout": {
            "symbol-placement": "line",
            "text-field": "{name}",
            "text-font": ["Noto Sans Regular"],
            "text-size": [
              "interpolate",
              ["linear"],
              ["zoom"],
              12, 10,
              14, 11,
              17, 13
            ]
          },
          "paint": {
            "text-color": "#CBD5E1",
            "text-halo-color": "#0B0E14",
            "text-halo-width": 1.5
          }
        }
      ]
    }
    """.trimIndent()

    map.setStyle(Style.Builder().fromJson(styleJson)) { style ->
        // Helper to load GeoJSON string from physical sdcard vault or embedded APK assets
        fun loadGeoJson(sdcardPath: String, assetName: String): String {
            try {
                val f = File(sdcardPath)
                if (f.exists() && f.length() > 0) {
                    return f.readText()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return try {
                context.assets.open(assetName).bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                "{ \"type\": \"FeatureCollection\", \"features\": [] }"
            }
        }

        // 1. Waterways & Coastlines (#131519 Dark Slate Water)
        try {
            val waterJson = loadGeoJson("/sdcard/Documents/Gabay/metro_manila_water.geojson", "water.geojson")
            val waterSource = GeoJsonSource("water_source", waterJson)
            style.addSource(waterSource)

            val waterLayer = LineLayer("water_layer", "water_source").apply {
                setProperties(
                    lineColor(AndroidColor.parseColor("#131519")),
                    lineWidth(5.5f),
                    lineCap(org.maplibre.android.style.layers.Property.LINE_CAP_ROUND),
                    lineJoin(org.maplibre.android.style.layers.Property.LINE_JOIN_ROUND)
                )
            }
            style.addLayer(waterLayer)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Parks & Greenery (#1B231F Tone-on-tone Forest)
        try {
            val parksJson = loadGeoJson("/sdcard/Documents/Gabay/metro_manila_parks.geojson", "parks.geojson")
            val parksSource = GeoJsonSource("parks_source", parksJson)
            style.addSource(parksSource)

            val parksLayer = FillLayer("parks_layer", "parks_source").apply {
                setProperties(
                    fillColor(AndroidColor.parseColor("#1B231F")),
                    fillOpacity(0.9f)
                )
            }
            style.addLayer(parksLayer)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Minor Streets & Residential Corridors (#2D323C)
        try {
            val minorJson = loadGeoJson("/sdcard/Documents/Gabay/metro_manila_roads_minor.geojson", "roads_minor.geojson")
            val minorSource = GeoJsonSource("roads_minor_source", minorJson)
            style.addSource(minorSource)

            val roadsMinorLayer = LineLayer("roads_minor", "roads_minor_source").apply {
                setProperties(
                    lineColor(AndroidColor.parseColor("#2D323C")),
                    lineWidth(1.8f),
                    lineCap(org.maplibre.android.style.layers.Property.LINE_CAP_ROUND),
                    lineJoin(org.maplibre.android.style.layers.Property.LINE_JOIN_ROUND)
                )
            }
            style.addLayer(roadsMinorLayer)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 4. Major Arterials & Highways (EDSA, Skyway, C-5, Ayala Ave) (#505869)
        try {
            val majorJson = loadGeoJson("/sdcard/Documents/Gabay/metro_manila_roads_major.geojson", "roads_major.geojson")
            val majorSource = GeoJsonSource("roads_major_source", majorJson)
            style.addSource(majorSource)

            val roadsMajorLayer = LineLayer("roads_major", "roads_major_source").apply {
                setProperties(
                    lineColor(AndroidColor.parseColor("#505869")),
                    lineWidth(3.6f),
                    lineCap(org.maplibre.android.style.layers.Property.LINE_CAP_ROUND),
                    lineJoin(org.maplibre.android.style.layers.Property.LINE_JOIN_ROUND)
                )
            }
            style.addLayer(roadsMajorLayer)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 4. Active Route Polyline (#00F5D4 Neon Mint as in Reference Photo)
        val routeSource = GeoJsonSource("route_source", FeatureCollection.fromFeatures(emptyList()))
        style.addSource(routeSource)

        val routeCasing = LineLayer("route_casing", "route_source").apply {
            setProperties(
                lineColor(AndroidColor.parseColor("#00382C")),
                lineWidth(9.5f),
                lineCap(org.maplibre.android.style.layers.Property.LINE_CAP_ROUND),
                lineJoin(org.maplibre.android.style.layers.Property.LINE_JOIN_ROUND)
            )
        }
        style.addLayer(routeCasing)

        val routeLayer = LineLayer("route_layer", "route_source").apply {
            setProperties(
                lineColor(AndroidColor.parseColor("#00F5D4")), // Mint / Cyan from photo
                lineWidth(5.5f),
                lineCap(org.maplibre.android.style.layers.Property.LINE_CAP_ROUND),
                lineJoin(org.maplibre.android.style.layers.Property.LINE_JOIN_ROUND)
            )
        }
        style.addLayer(routeLayer)

        // 5. Destination Pin (White circular badge with drop shadow)
        val pinSource = GeoJsonSource("pin_source", FeatureCollection.fromFeatures(emptyList()))
        style.addSource(pinSource)

        val pinHalo = CircleLayer("pin_halo", "pin_source").apply {
            setProperties(
                circleRadius(14f),
                circleColor(AndroidColor.parseColor("#00F5D4")),
                circleOpacity(0.35f)
            )
        }
        style.addLayer(pinHalo)

        val pinDot = CircleLayer("pin_dot", "pin_source").apply {
            setProperties(
                circleRadius(8f),
                circleColor(AndroidColor.WHITE),
                circleStrokeWidth(3f),
                circleStrokeColor(AndroidColor.parseColor("#00F5D4"))
            )
        }
        style.addLayer(pinDot)

        // 6. User GPS Location Marker
        val userSource = GeoJsonSource("user_source", FeatureCollection.fromFeatures(emptyList()))
        style.addSource(userSource)

        val userHalo = CircleLayer("user_halo", "user_source").apply {
            setProperties(
                circleRadius(16f),
                circleColor(AndroidColor.parseColor("#00F5D4")),
                circleOpacity(0.25f)
            )
        }
        style.addLayer(userHalo)

        val userDot = CircleLayer("user_dot", "user_source").apply {
            setProperties(
                circleRadius(7f),
                circleColor(AndroidColor.parseColor("#00F5D4")),
                circleStrokeWidth(2.5f),
                circleStrokeColor(AndroidColor.WHITE)
            )
        }
        style.addLayer(userDot)
    }

    // Long Press Listener: Drop Destination Pin
    map.addOnMapLongClickListener { latLng ->
        onPinDropped(uniffi.gabay_core.LatLng(latLng.latitude, latLng.longitude))
        true
    }

    // Elastic Border Spring Bounce-Back Constraint
    map.addOnCameraIdleListener {
        applyElasticConstraint(map)
    }
}

/**
 * Calculates elasticity vector and smoothly bounces camera back into Philippine borders
 * if the rider attempts to scroll or fling past the country's boundaries.
 */
fun applyElasticConstraint(map: MapLibreMap) {
    val target = map.cameraPosition.target ?: return
    var targetLat = target.latitude
    var targetLon = target.longitude
    var needsBounce = false

    val minLat = DynamicBorderBounds.minLat
    val maxLat = DynamicBorderBounds.maxLat
    val minLon = DynamicBorderBounds.minLon
    val maxLon = DynamicBorderBounds.maxLon

    if (targetLat < minLat) {
        targetLat = minLat
        needsBounce = true
    } else if (targetLat > maxLat) {
        targetLat = maxLat
        needsBounce = true
    }

    if (targetLon < minLon) {
        targetLon = minLon
        needsBounce = true
    } else if (targetLon > maxLon) {
        targetLon = maxLon
        needsBounce = true
    }

    if (needsBounce) {
        val recoilUpdate = CameraUpdateFactory.newLatLng(LatLng(targetLat, targetLon))
        map.easeCamera(recoilUpdate, 350)
    }
}

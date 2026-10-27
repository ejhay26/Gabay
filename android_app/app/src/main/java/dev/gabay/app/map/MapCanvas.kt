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
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.expressions.Expression.*
import org.maplibre.android.style.sources.GeoJsonSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
                    raf.seek(102)
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
    userSpeed: Float = 0.0f,
    activeRoute: RouteResult?,
    alternativeRoute: RouteResult? = null,
    destinationPin: uniffi.gabay_core.LatLng?,
    fusedMapMode: FusedMapMode,
    is3dPerspective: Boolean = false,
    isFollowingVehicle: Boolean = true,
    isNavigating: Boolean = false,
    isSunlightLightMode: Boolean = dev.gabay.app.ui.GabayColors.isSunlightLightMode,
    resetRotationTrigger: Int = 0,
    cameraTarget: LatLng?,
    onBearingChanged: (Float) -> Unit = {},
    onFollowModeChanged: (Boolean) -> Unit = {},
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
        val currentPos = map.cameraPosition
        val targetTilt = if (is3dPerspective || isNavigating) 48.0 else currentPos.tilt
        val targetBearing = if (isNavigating || is3dPerspective) {
            if (userHeading != 0f) userHeading.toDouble() else currentPos.bearing
        } else {
            currentPos.bearing
        }
        val targetZoom = if (isNavigating) 16.5 else 15.5
        val newPos = CameraPosition.Builder(currentPos)
            .target(target)
            .zoom(targetZoom)
            .tilt(targetTilt)
            .bearing(targetBearing)
            .build()
        map.easeCamera(CameraUpdateFactory.newCameraPosition(newPos), 450)
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
        val waterwayLayer = style.getLayer("pmtiles_waterway")
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
                waterwayLayer?.setProperties(visibility(org.maplibre.android.style.layers.Property.NONE))
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
                waterLayer?.setProperties(visibility(org.maplibre.android.style.layers.Property.VISIBLE))
                waterwayLayer?.setProperties(visibility(org.maplibre.android.style.layers.Property.VISIBLE))
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
                waterwayLayer?.setProperties(visibility(org.maplibre.android.style.layers.Property.VISIBLE))
                landuseLayer?.setProperties(visibility(org.maplibre.android.style.layers.Property.VISIBLE))
                buildingsLayer?.setProperties(visibility(org.maplibre.android.style.layers.Property.VISIBLE))
                parksLayer?.setProperties(visibility(org.maplibre.android.style.layers.Property.VISIBLE))
                roadsLayer?.setProperties(lineColor(AndroidColor.parseColor("#5C657A")))
                roadsMajorLayer?.setProperties(lineColor(AndroidColor.parseColor("#505869")))
                roadsMinorLayer?.setProperties(lineColor(AndroidColor.parseColor("#2D323C")))
            }
        }
    }

    // React to sunlight light mode toggle (switch vector fills and route glow)
    LaunchedEffect(isSunlightLightMode, mapInstance) {
        val map = mapInstance ?: return@LaunchedEffect
        val style = map.style ?: return@LaunchedEffect

        val bgLayer = style.getLayer("background")
        val earthLayer = style.getLayer("pmtiles_earth")
        val waterLayer = style.getLayer("pmtiles_water")
        val waterwayLayer = style.getLayer("pmtiles_waterway")
        val landuseLayer = style.getLayer("pmtiles_landuse")
        val buildingsLayer = style.getLayer("pmtiles_buildings")
        val roadsCasingLayer = style.getLayer("pmtiles_roads_casing")
        val roadsLayer = style.getLayer("pmtiles_roads")
        val roadsMajorLayer = style.getLayer("roads_major")
        val roadsMinorLayer = style.getLayer("roads_minor")
        val routeLayer = style.getLayer("route_layer")
        val routeCasing = style.getLayer("route_casing")
        val poisLayer = style.getLayer("pmtiles_pois")
        val roadLabels = style.getLayer("pmtiles_roads_labels")
        val sqlitePoiLabel = style.getLayer("sqlite_poi_label")

        if (isSunlightLightMode) {
            bgLayer?.setProperties(backgroundColor(AndroidColor.parseColor("#F1F5F9")))
            earthLayer?.setProperties(fillColor(AndroidColor.parseColor("#F1F5F9")))
            waterLayer?.setProperties(fillColor(AndroidColor.parseColor("#BAE6FD")))
            waterwayLayer?.setProperties(lineColor(AndroidColor.parseColor("#7DD3FC")))
            landuseLayer?.setProperties(fillColor(AndroidColor.parseColor("#DCFCE7")))
            buildingsLayer?.setProperties(fillColor(AndroidColor.parseColor("#E2E8F0")))
            roadsCasingLayer?.setProperties(lineColor(AndroidColor.parseColor("#CBD5E1")))
            roadsLayer?.setProperties(lineColor(AndroidColor.parseColor("#64748B")))
            roadsMajorLayer?.setProperties(lineColor(AndroidColor.parseColor("#475569")))
            roadsMinorLayer?.setProperties(lineColor(AndroidColor.parseColor("#94A3B8")))
            routeLayer?.setProperties(lineColor(AndroidColor.parseColor("#C2410C")))
            routeCasing?.setProperties(lineColor(AndroidColor.parseColor("#7C2D12")))
            poisLayer?.setProperties(textColor(AndroidColor.parseColor("#0F172A")), textHaloColor(AndroidColor.WHITE))
            roadLabels?.setProperties(textColor(AndroidColor.parseColor("#334155")), textHaloColor(AndroidColor.WHITE))
            sqlitePoiLabel?.setProperties(textColor(AndroidColor.parseColor("#0F172A")), textHaloColor(AndroidColor.WHITE))
        } else {
            bgLayer?.setProperties(backgroundColor(AndroidColor.parseColor("#16191E")))
            earthLayer?.setProperties(fillColor(AndroidColor.parseColor("#16191E")))
            waterLayer?.setProperties(fillColor(AndroidColor.parseColor("#111822")))
            waterwayLayer?.setProperties(lineColor(AndroidColor.parseColor("#182230")))
            landuseLayer?.setProperties(fillColor(AndroidColor.parseColor("#1A2420")))
            buildingsLayer?.setProperties(fillColor(AndroidColor.parseColor("#252B35")))
            roadsCasingLayer?.setProperties(lineColor(AndroidColor.parseColor("#11141A")))
            roadsLayer?.setProperties(lineColor(AndroidColor.parseColor("#5C657A")))
            roadsMajorLayer?.setProperties(lineColor(AndroidColor.parseColor("#505869")))
            roadsMinorLayer?.setProperties(lineColor(AndroidColor.parseColor("#2D323C")))
            routeLayer?.setProperties(lineColor(AndroidColor.parseColor("#FFB300")))
            routeCasing?.setProperties(lineColor(AndroidColor.parseColor("#4D3200")))
            poisLayer?.setProperties(textColor(AndroidColor.parseColor("#FFB300")), textHaloColor(AndroidColor.parseColor("#080B10")))
            roadLabels?.setProperties(textColor(AndroidColor.parseColor("#CBD5E1")), textHaloColor(AndroidColor.parseColor("#0B0E14")))
            sqlitePoiLabel?.setProperties(textColor(AndroidColor.parseColor("#F1F5F9")), textHaloColor(AndroidColor.parseColor("#0B0E14")))
        }
    }

    // React to active route updates & dynamic progress slicing along user's travel
    LaunchedEffect(activeRoute, userLat, userLon, isNavigating, mapInstance) {
        val map = mapInstance ?: return@LaunchedEffect
        val style = map.style ?: return@LaunchedEffect

        val routeSource = style.getSourceAs<GeoJsonSource>("route_source")
        if (activeRoute != null && activeRoute.success && activeRoute.polyline.isNotEmpty()) {
            val fullPoly = activeRoute.polyline
            val renderedPoints: List<Point> = if (isNavigating && fullPoly.size >= 2) {
                // Find closest road segment along the polyline using perpendicular projection
                val cosLat = Math.cos(Math.toRadians(userLat))
                var bestSegmentIdx = 0
                var minDistanceSq = Double.MAX_VALUE

                for (i in 0 until fullPoly.size - 1) {
                    val p1 = fullPoly[i]
                    val p2 = fullPoly[i + 1]

                    val dx = (p2.longitude - p1.longitude) * cosLat * 111320.0
                    val dy = (p2.latitude - p1.latitude) * 111320.0
                    val segLenSq = dx * dx + dy * dy

                    val udx = (userLon - p1.longitude) * cosLat * 111320.0
                    val udy = (userLat - p1.latitude) * 111320.0

                    val dSq = if (segLenSq < 1e-4) {
                        udx * udx + udy * udy
                    } else {
                        val t = ((udx * dx + udy * dy) / segLenSq).coerceIn(0.0, 1.0)
                        val projX = dx * t
                        val projY = dy * t
                        val ex = udx - projX
                        val ey = udy - projY
                        ex * ex + ey * ey
                    }

                    if (dSq < minDistanceSq) {
                        minDistanceSq = dSq
                        bestSegmentIdx = i
                    }
                }

                // Slice off all passed segments behind rider; render from user position forward to destination
                val ahead = fullPoly.subList(bestSegmentIdx + 1, fullPoly.size)
                if (ahead.isNotEmpty()) {
                    listOf(Point.fromLngLat(userLon, userLat)) + ahead.map { Point.fromLngLat(it.longitude, it.latitude) }
                } else {
                    listOf(Point.fromLngLat(userLon, userLat), Point.fromLngLat(fullPoly.last().longitude, fullPoly.last().latitude))
                }
            } else {
                fullPoly.map { Point.fromLngLat(it.longitude, it.latitude) }
            }

            if (renderedPoints.size >= 2) {
                val lineString = LineString.fromLngLats(renderedPoints)
                routeSource?.setGeoJson(FeatureCollection.fromFeature(Feature.fromGeometry(lineString)))
            } else {
                routeSource?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
            }

            // Smoothly ease camera to fit the active route ONLY on initial calculation (when not actively driving)
            if (!isNavigating) {
                try {
                    var minLat = 90.0
                    var maxLat = -90.0
                    var minLon = 180.0
                    var maxLon = -180.0
                    for (p in fullPoly) {
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
            }
        } else {
            routeSource?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
        }
    }

    // React to alternative route updates (rendered semi-translucent)
    LaunchedEffect(alternativeRoute, mapInstance) {
        val map = mapInstance ?: return@LaunchedEffect
        val style = map.style ?: return@LaunchedEffect
        val altSource = style.getSourceAs<GeoJsonSource>("alt_route_source")
        if (alternativeRoute != null && alternativeRoute.success && alternativeRoute.polyline.isNotEmpty()) {
            val points = alternativeRoute.polyline.map { Point.fromLngLat(it.longitude, it.latitude) }
            val lineString = LineString.fromLngLats(points)
            altSource?.setGeoJson(FeatureCollection.fromFeature(Feature.fromGeometry(lineString)))
        } else {
            altSource?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
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

    // React to user GPS position & continuous magnetic follow / phone heading orientation
    LaunchedEffect(userLat, userLon, userHeading, userSpeed, isFollowingVehicle, is3dPerspective, isNavigating, mapInstance) {
        val map = mapInstance ?: return@LaunchedEffect
        val style = map.style ?: return@LaunchedEffect

        val userSource = style.getSourceAs<GeoJsonSource>("user_source")
        val point = Point.fromLngLat(userLon, userLat)
        userSource?.setGeoJson(FeatureCollection.fromFeature(Feature.fromGeometry(point)))

        if (isFollowingVehicle) {
            val targetTilt = if (is3dPerspective || isNavigating) 48.0 else 0.0
            val targetBearing = if (isNavigating || is3dPerspective) {
                // In navigation or 3D mode, continuously orient the map in the direction of the phone/vehicle
                if (userSpeed > 1.5f || userHeading != 0f) userHeading.toDouble() else map.cameraPosition.bearing
            } else {
                0.0 // 2D tactical map free-explore is strictly locked to True North (0.0°)
            }
            val currentPos = map.cameraPosition
            val navZoom = if (isNavigating) {
                if (currentPos.zoom < 15.0) 16.5 else currentPos.zoom
            } else {
                if (currentPos.zoom < 13.5) 15.5 else currentPos.zoom
            }

            // Magnetic response: smoothly track position & bearing changes
            val curTarget = currentPos.target
            val distToCam = if (curTarget != null) {
                val res = FloatArray(1)
                android.location.Location.distanceBetween(userLat, userLon, curTarget.latitude, curTarget.longitude, res)
                res[0]
            } else 999f

            var bearingDiff = Math.abs(currentPos.bearing - targetBearing)
            bearingDiff = Math.min(bearingDiff, 360.0 - bearingDiff)

            val shouldMoveCam = isNavigating || (userSpeed >= 1.5f) || (distToCam > 5.0f) || (bearingDiff > 1.2)

            if (shouldMoveCam) {
                val newPos = CameraPosition.Builder(currentPos)
                    .target(LatLng(userLat, userLon))
                    .zoom(navZoom)
                    .bearing(targetBearing)
                    .tilt(targetTilt)
                    .build()

                map.easeCamera(CameraUpdateFactory.newCameraPosition(newPos), 240)
            }
        }
    }

    // Query and render visible POIs on map whenever camera or user location changes
    LaunchedEffect(mapInstance, userLat, userLon) {
        val map = mapInstance ?: return@LaunchedEffect
        queryAndRenderVisiblePois(map)
    }

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                mapViewInstance = this
                getMapAsync { map ->
                    mapInstance = map
                    setupTacticalMap(map, ctx, userLat, userLon, onBearingChanged, onFollowModeChanged, onPinDropped)
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
    onFollowModeChanged: (Boolean) -> Unit,
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

    // When rider manually gestures or pans the map, temporarily disengage magnetic lock
    map.addOnCameraMoveStartedListener { reason ->
        if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
            onFollowModeChanged(false)
        }
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
          "paint": {
            "raster-opacity": 0.65,
            "raster-saturation": -1.0,
            "raster-contrast": 0.35,
            "raster-brightness-max": 0.55
          }
        },
        {
          "id": "pmtiles_earth",
          "type": "fill",
          "source": "protomaps_vector",
          "source-layer": "earth",
          "paint": {
            "fill-color": "#16191E"
          }
        },
        {
          "id": "pmtiles_water",
          "type": "fill",
          "source": "protomaps_vector",
          "source-layer": "water",
          "filter": ["==", "${'$'}type", "Polygon"],
          "paint": {
            "fill-color": "#111822"
          }
        },
        {
          "id": "pmtiles_waterway",
          "type": "line",
          "source": "protomaps_vector",
          "source-layer": "water",
          "filter": ["==", "${'$'}type", "LineString"],
          "layout": {
            "line-cap": "round",
            "line-join": "round"
          },
          "paint": {
            "line-color": "#182230",
            "line-width": [
              "interpolate",
              ["exponential", 1.4],
              ["zoom"],
              6, 0.6,
              10, 1.2,
              14, 2.8,
              17, 5.0
            ]
          }
        },
        {
          "id": "pmtiles_landuse",
          "type": "fill",
          "source": "protomaps_vector",
          "source-layer": "landuse",
          "filter": ["all", ["==", "${'$'}type", "Polygon"], ["!in", "kind", "farmland", "farm", "paddy", "basin"]],
          "paint": {
            "fill-color": "#1A2420",
            "fill-opacity": 0.4
          }
        },
        {
          "id": "pmtiles_buildings",
          "type": "fill",
          "source": "protomaps_vector",
          "source-layer": "buildings",
          "filter": ["==", "${'$'}type", "Polygon"],
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
          "layout": {
            "line-cap": "round",
            "line-join": "round"
          },
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
          "layout": {
            "line-cap": "round",
            "line-join": "round"
          },
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
            "text-padding": 3,
            "text-optional": true,
            "text-allow-overlap": false
          },
          "paint": {
            "text-color": "#F0F4F8",
            "text-halo-color": "#0B0E14",
            "text-halo-width": 1.75
          }
        },
        {
          "id": "pmtiles_pois",
          "type": "symbol",
          "source": "protomaps_vector",
          "source-layer": "pois",
          "minzoom": 8,
          "layout": {
            "text-field": "{name}",
            "text-font": ["Noto Sans Regular"],
            "text-size": [
              "interpolate",
              ["linear"],
              ["zoom"],
              8, 9,
              11, 11,
              14, 13,
              17, 15
            ],
            "text-max-width": 8,
            "text-padding": 2,
            "text-optional": true,
            "text-allow-overlap": false
          },
          "paint": {
            "text-color": "#FFB300",
            "text-halo-color": "#080B10",
            "text-halo-width": 1.5
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

        // 3.5. Alternative Detour Route Polyline (Translucent Slate underneath primary route)
        val altRouteSource = GeoJsonSource("alt_route_source", FeatureCollection.fromFeatures(emptyList()))
        style.addSource(altRouteSource)

        val altRouteLayer = LineLayer("alt_route_layer", "alt_route_source").apply {
            setProperties(
                lineColor(AndroidColor.parseColor("#64748B")),
                lineWidth(4.5f),
                lineOpacity(0.55f),
                lineCap(org.maplibre.android.style.layers.Property.LINE_CAP_ROUND),
                lineJoin(org.maplibre.android.style.layers.Property.LINE_JOIN_ROUND)
            )
        }
        style.addLayer(altRouteLayer)

        // 4. Active Route Polyline (#FFB300 Tactical Solar Gold / Signal Amber)
        val routeSource = GeoJsonSource("route_source", FeatureCollection.fromFeatures(emptyList()))
        style.addSource(routeSource)

        val routeCasing = LineLayer("route_casing", "route_source").apply {
            setProperties(
                lineColor(AndroidColor.parseColor("#4D3200")),
                lineWidth(9.5f),
                lineCap(org.maplibre.android.style.layers.Property.LINE_CAP_ROUND),
                lineJoin(org.maplibre.android.style.layers.Property.LINE_JOIN_ROUND)
            )
        }
        style.addLayer(routeCasing)

        val routeLayer = LineLayer("route_layer", "route_source").apply {
            setProperties(
                lineColor(AndroidColor.parseColor("#FFB300")),
                lineWidth(5.5f),
                lineCap(org.maplibre.android.style.layers.Property.LINE_CAP_ROUND),
                lineJoin(org.maplibre.android.style.layers.Property.LINE_JOIN_ROUND)
            )
        }
        style.addLayer(routeLayer)

        // 5. Destination Pin (Amber circular badge with drop shadow)
        val pinSource = GeoJsonSource("pin_source", FeatureCollection.fromFeatures(emptyList()))
        style.addSource(pinSource)

        val pinHalo = CircleLayer("pin_halo", "pin_source").apply {
            setProperties(
                circleRadius(14f),
                circleColor(AndroidColor.parseColor("#FFB300")),
                circleOpacity(0.35f)
            )
        }
        style.addLayer(pinHalo)

        val pinDot = CircleLayer("pin_dot", "pin_source").apply {
            setProperties(
                circleRadius(8f),
                circleColor(AndroidColor.WHITE),
                circleStrokeWidth(3f),
                circleStrokeColor(AndroidColor.parseColor("#FFB300"))
            )
        }
        style.addLayer(pinDot)

        // 6. User GPS Location Marker
        val userSource = GeoJsonSource("user_source", FeatureCollection.fromFeatures(emptyList()))
        style.addSource(userSource)

        val userHalo = CircleLayer("user_halo", "user_source").apply {
            setProperties(
                circleRadius(16f),
                circleColor(AndroidColor.parseColor("#FFB300")),
                circleOpacity(0.25f)
            )
        }
        style.addLayer(userHalo)

        val userDot = CircleLayer("user_dot", "user_source").apply {
            setProperties(
                circleRadius(7f),
                circleColor(AndroidColor.parseColor("#FFB300")),
                circleStrokeWidth(2.5f),
                circleStrokeColor(AndroidColor.WHITE)
            )
        }
        style.addLayer(userDot)

        // 7. Rich SQLite Offline POIs (NEUST, hospitals, clinics, schools, municipal halls, barangays)
        val sqlitePoiSource = GeoJsonSource("sqlite_poi_source", FeatureCollection.fromFeatures(emptyList()))
        style.addSource(sqlitePoiSource)

        val sqlitePoiCircle = CircleLayer("sqlite_poi_circle", "sqlite_poi_source").apply {
            minZoom = 9.5f
            setProperties(
                circleRadius(
                    interpolate(
                        linear(),
                        zoom(),
                        stop(9.5f, literal(3.5f)),
                        stop(12.0f, literal(5.0f)),
                        stop(16.0f, literal(7.0f))
                    )
                ),
                circleColor(
                    match(
                        get("category"),
                        literal("#FFB300"), // Solar Amber Default
                        stop("HOSPITAL", literal("#EF4444")),
                        stop("CLINIC", literal("#EF4444")),
                        stop("PHARMACY", literal("#EF4444")),
                        stop("SCHOOL", literal("#38BDF8")),
                        stop("UNIVERSITY", literal("#38BDF8")),
                        stop("COLLEGE", literal("#38BDF8")),
                        stop("GAS", literal("#10B981")),
                        stop("FUEL", literal("#10B981")),
                        stop("FOOD", literal("#FB923C")),
                        stop("RESTAURANT", literal("#FB923C")),
                        stop("GOVERNMENT", literal("#A855F7")),
                        stop("POLICE", literal("#A855F7")),
                        stop("FIRE", literal("#A855F7"))
                    )
                ),
                circleStrokeWidth(1.5f),
                circleStrokeColor(AndroidColor.parseColor("#0B0E14"))
            )
        }
        style.addLayer(sqlitePoiCircle)

        val sqlitePoiLabel = SymbolLayer("sqlite_poi_label", "sqlite_poi_source").apply {
            minZoom = 11.0f
            setProperties(
                textField("{name}"),
                textFont(arrayOf("Noto Sans Regular")),
                textSize(
                    interpolate(
                        linear(),
                        zoom(),
                        stop(11.0f, literal(10.0f)),
                        stop(13.0f, literal(11.5f)),
                        stop(16.0f, literal(13.5f))
                    )
                ),
                textColor(if (dev.gabay.app.ui.GabayColors.isSunlightLightMode) AndroidColor.parseColor("#0F172A") else AndroidColor.parseColor("#F1F5F9")),
                textHaloColor(if (dev.gabay.app.ui.GabayColors.isSunlightLightMode) AndroidColor.WHITE else AndroidColor.parseColor("#0B0E14")),
                textHaloWidth(1.75f),
                textAnchor(org.maplibre.android.style.layers.Property.TEXT_ANCHOR_TOP),
                textOffset(arrayOf(0f, 0.7f)),
                textMaxWidth(8.0f),
                textAllowOverlap(false),
                textOptional(true)
            )
        }
        style.addLayer(sqlitePoiLabel)

        // Seed visible POIs immediately upon style initialization
        queryAndRenderVisiblePois(map)
    }

    // Single Tap Listener: Tap on POI on map to select destination
    map.addOnMapClickListener { latLng ->
        val screenPoint = map.projection.toScreenLocation(latLng)
        val features = map.queryRenderedFeatures(screenPoint, "sqlite_poi_circle", "sqlite_poi_label")
        if (features.isNotEmpty()) {
            val poiPoint = features.first().geometry() as? Point
            if (poiPoint != null) {
                onFollowModeChanged(false)
                onPinDropped(uniffi.gabay_core.LatLng(poiPoint.latitude(), poiPoint.longitude()))
                return@addOnMapClickListener true
            }
        }
        false
    }

    // Long Press Listener: Drop Custom Destination Pin
    map.addOnMapLongClickListener { latLng ->
        onFollowModeChanged(false)
        onPinDropped(uniffi.gabay_core.LatLng(latLng.latitude, latLng.longitude))
        true
    }

    // Elastic Border Spring Bounce-Back Constraint & POI Viewport Query
    map.addOnCameraIdleListener {
        applyElasticConstraint(map)
        queryAndRenderVisiblePois(map)
    }
}

/**
 * Queries offline SQLite POIs within the current visible viewport and updates the map.
 * Guarantees institutes (NEUST San Isidro), hospitals, gas stations, schools, and restaurants
 * are dynamically rendered on the actual map canvas.
 */
fun queryAndRenderVisiblePois(map: MapLibreMap) {
    val style = map.style ?: return
    val poiSource = style.getSourceAs<GeoJsonSource>("sqlite_poi_source") ?: return
    val zoom = map.cameraPosition.zoom
    if (zoom < 9.5) {
        poiSource.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
        return
    }

    val bounds = map.projection.visibleRegion.latLngBounds
    val minLat = bounds.latitudeSouth
    val maxLat = bounds.latitudeNorth
    val minLon = bounds.longitudeWest
    val maxLon = bounds.longitudeEast

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val dbFile = File("/sdcard/Documents/Gabay/pois_fts.sqlite")
            if (dbFile.exists()) {
                val db = android.database.sqlite.SQLiteDatabase.openDatabase(dbFile.absolutePath, null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY)
                val cursor = db.rawQuery(
                    "SELECT name, category, latitude, longitude FROM pois WHERE latitude BETWEEN ? AND ? AND longitude BETWEEN ? AND ? LIMIT 350",
                    arrayOf(minLat.toString(), maxLat.toString(), minLon.toString(), maxLon.toString())
                )
                val features = mutableListOf<Feature>()
                while (cursor.moveToNext()) {
                    val name = cursor.getString(0) ?: continue
                    val cat = cursor.getString(1) ?: "GENERAL"
                    val lat = cursor.getDouble(2)
                    val lon = cursor.getDouble(3)
                    val feat = Feature.fromGeometry(Point.fromLngLat(lon, lat))
                    feat.addStringProperty("name", name)
                    feat.addStringProperty("category", cat)
                    features.add(feat)
                }
                cursor.close()
                db.close()
                withContext(Dispatchers.Main) {
                    poiSource.setGeoJson(FeatureCollection.fromFeatures(features))
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("MapCanvas", "Error querying visible POIs: ${e.message}")
        }
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

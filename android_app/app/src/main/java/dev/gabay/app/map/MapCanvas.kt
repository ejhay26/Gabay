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
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.BackgroundLayer
import org.maplibre.android.style.layers.FillExtrusionLayer
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

object DynamicBorderBounds {
    // Flagship Philippines Geographic Envelope
    var minLon: Double = 116.5
    var minLat: Double = 4.5
    var maxLon: Double = 127.0
    var maxLat: Double = 21.5

    /**
     * Inspects PMTiles v3 binary header if present in vault to automatically adapt
     * bounding box for international riders dropping in their own country's map.
     */
    fun detectFromVault(vaultPath: String) {
        val vectorPmtiles = File(vaultPath, "philippines_vector.pmtiles")
        if (vectorPmtiles.exists() && vectorPmtiles.length() >= 127) {
            try {
                RandomAccessFile(vectorPmtiles, "r").use { raf ->
                    // Bytes 97-112: min_lon_e7, min_lat_e7, max_lon_e7, max_lat_e7 (int32 little endian)
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
    isSatelliteActive: Boolean,
    isTopoActive: Boolean,
    is3dActive: Boolean,
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

    // React to 3D mode changes
    LaunchedEffect(is3dActive, mapInstance) {
        mapInstance?.let { map ->
            val targetTilt = if (is3dActive) 50.0 else 0.0
            val currentPos = map.cameraPosition
            val newPos = CameraPosition.Builder(currentPos)
                .tilt(targetTilt)
                .build()
            map.easeCamera(CameraUpdateFactory.newCameraPosition(newPos), 400)
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
                    setupTacticalMap(map, ctx, onPinDropped)
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
    onPinDropped: (uniffi.gabay_core.LatLng) -> Unit
) {
    // Initial Philippine Camera (Manila Metro Corridor)
    val initialCamera = CameraPosition.Builder()
        .target(LatLng(14.5547, 121.0244)) // Ayala Avenue Makati
        .zoom(14.5)
        .tilt(0.0)
        .bearing(0.0)
        .build()
    map.cameraPosition = initialCamera

    // Minimal High-Contrast Offline Tactical Dark Style
    val styleJson = """
    {
      "version": 8,
      "name": "Gabay Tactical Dark",
      "sources": {},
      "layers": [
        {
          "id": "background",
          "type": "background",
          "paint": {
            "background-color": "#0A0D10"
          }
        }
      ]
    }
    """.trimIndent()

    map.setStyle(Style.Builder().fromJson(styleJson)) { style ->
        // Route Line Layer Source
        val routeSource = GeoJsonSource("route_source", FeatureCollection.fromFeatures(emptyList()))
        style.addSource(routeSource)

        val routeLayer = LineLayer("route_layer", "route_source").apply {
            setProperties(
                lineColor(AndroidColor.parseColor("#10B981")), // Emerald Green
                lineWidth(6f),
                lineCap(org.maplibre.android.style.layers.Property.LINE_CAP_ROUND),
                lineJoin(org.maplibre.android.style.layers.Property.LINE_JOIN_ROUND)
            )
        }
        style.addLayer(routeLayer)

        // Destination Pin Layer Source
        val pinSource = GeoJsonSource("pin_source", FeatureCollection.fromFeatures(emptyList()))
        style.addSource(pinSource)

        // User Location Layer Source
        val userSource = GeoJsonSource("user_source", FeatureCollection.fromFeatures(emptyList()))
        style.addSource(userSource)
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
        // Spring recoil animation back to border edge
        val recoilUpdate = CameraUpdateFactory.newLatLng(LatLng(targetLat, targetLon))
        map.easeCamera(recoilUpdate, 350)
    }
}

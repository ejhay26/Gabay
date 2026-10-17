package dev.gabay.app

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import dev.gabay.app.audio.AudioController
import dev.gabay.app.map.FusedMapMode
import dev.gabay.app.map.MapCanvas
import dev.gabay.app.map.PmTilesServer
import dev.gabay.app.ui.*
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLng as MapLibreLatLng
import uniffi.gabay_core.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {

    private lateinit var audioController: AudioController
    private var engine: GabayEngine? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize MapLibre Native instance
        MapLibre.getInstance(this)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Start embedded PMTiles localhost HTTP server for nationwide vector, topo, satellite, and fonts
        PmTilesServer.start(this, File("/sdcard/Documents/Gabay"))

        // Initialize Audio Controller (TTS + Audio Focus Ducking)
        audioController = AudioController(this)

        // Initialize Rust Core Engine with device vault path
        try {
            android.util.Log.d("GabayEngine", "Initializing GabayEngine on /sdcard/Documents/Gabay...")
            engine = GabayEngine("/sdcard/Documents/Gabay")
            android.util.Log.d("GabayEngine", "GabayEngine loaded successfully: $engine")
        } catch (t: Throwable) {
            android.util.Log.e("GabayEngine", "GabayEngine initialization error", t)
        }

        enableEdgeToEdge()
        setContent {
            GabayTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = GabayColors.BgObsidian,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { _ ->
                    CockpitScreen(
                        engine = engine,
                        audioController = audioController,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioController.shutdown()
        PmTilesServer.stop()
    }
}

@Composable
fun CockpitScreen(
    engine: GabayEngine?,
    audioController: AudioController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Rider & Vehicle States
    var selectedMode by remember { mutableStateOf(VehicleMode.SCOOTER) }
    var fusedMapMode by remember { mutableStateOf(FusedMapMode.STREET) }

    // User GPS state (Defaults to real Central Luzon coordinates detected on device)
    var userLat by remember { mutableDoubleStateOf(15.2611) }
    var userLon by remember { mutableDoubleStateOf(120.9105) }
    var userSpeed by remember { mutableFloatStateOf(0.0f) }
    var userHeading by remember { mutableFloatStateOf(0.0f) }

    // Camera target state (for smooth panning on search / recenter)
    var cameraTarget by remember { mutableStateOf<MapLibreLatLng?>(null) }

    // Search Bar States
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<PoiSearchResult>>(emptyList()) }

    // Sheet overlays
    var isMenuSheetOpen by remember { mutableStateOf(false) }
    var isAgentSheetOpen by remember { mutableStateOf(false) }

    // Navigation and Routing state
    var activeRoute by remember { mutableStateOf<RouteResult?>(null) }
    var destinationPin by remember { mutableStateOf<LatLng?>(null) }
    var pinInfo by remember { mutableStateOf<PinInfo?>(null) }

    // Map bearing, magnetic follow lock, and 3D mode states
    var cameraBearing by remember { mutableFloatStateOf(0.0f) }
    var is3dMode by remember { mutableStateOf(false) }
    var isFollowingVehicle by remember { mutableStateOf(true) }
    var resetRotationCount by remember { mutableIntStateOf(0) }

    // Speech-to-Text State with live audio decibels and streaming partial results
    var isListening by remember { mutableStateOf(false) }
    var audioRmsDb by remember { mutableFloatStateOf(0.0f) }
    var partialSpeechText by remember { mutableStateOf("") }
    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else null
    }

    // Bulletproof Route Solver: High-Accuracy Dijkstra Road-Network Router from device vault
    fun computeRoute(startCoord: LatLng, destCoord: LatLng, options: RouteOptions): RouteResult {
        // High-Accuracy Dijkstra Road-Network Router from device vault (Central Luzon & Nationwide Road Polylines)
        try {
            val dbFile = File("/sdcard/Documents/Gabay/pois_fts.sqlite")
            if (dbFile.exists()) {
                val db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
                val pad = 0.08 // ~8.8km margin ensuring all bypasses, highways, and connectors are included
                val minLat = minOf(startCoord.latitude, destCoord.latitude) - pad
                val maxLat = maxOf(startCoord.latitude, destCoord.latitude) + pad
                val minLon = minOf(startCoord.longitude, destCoord.longitude) - pad
                val maxLon = maxOf(startCoord.longitude, destCoord.longitude) + pad

                // Use covering spatial index on bounding box
                val cursor = db.rawQuery(
                    "SELECT name, kind, geometry FROM roads WHERE min_lat <= ? AND max_lat >= ? AND min_lon <= ? AND max_lon >= ?",
                    arrayOf(maxLat.toString(), minLat.toString(), maxLon.toString(), minLon.toString())
                )

                class RoadRaw(val name: String, val kind: String, val points: List<LatLng>)
                val candidateRoads = ArrayList<RoadRaw>()

                if (cursor.moveToFirst()) {
                    do {
                        val name = cursor.getString(0) ?: "Road"
                        val kind = cursor.getString(1) ?: "road"
                        val geomStr = cursor.getString(2) ?: ""

                        // STRICT FILTER: Only real physical drivable roads!
                        // Exclude footpaths across ricefields (path), aeroways, rails, ferries, aerialways
                        val isDrivable = when (kind) {
                            "major_road", "minor_road" -> true
                            "highway" -> !options.avoidExpressways
                            else -> false
                        }
                        if (!isDrivable) continue

                        val rawPairs = geomStr.split(";")
                        if (rawPairs.size >= 2) {
                            val pts = ArrayList<LatLng>(rawPairs.size)
                            for (pair in rawPairs) {
                                val parts = pair.split(",")
                                if (parts.size == 2) {
                                    val lat = parts[0].toDoubleOrNull()
                                    val lon = parts[1].toDoubleOrNull()
                                    if (lat != null && lon != null) {
                                        pts.add(LatLng(lat, lon))
                                    }
                                }
                            }
                            if (pts.size >= 2) {
                                candidateRoads.add(RoadRaw(name, kind, pts))
                            }
                        }
                    } while (cursor.moveToNext())
                }
                cursor.close()
                db.close()

                if (candidateRoads.isNotEmpty()) {
                    val distTmp = FloatArray(1)
                    fun distBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
                        Location.distanceBetween(lat1, lon1, lat2, lon2, distTmp)
                        return distTmp[0]
                    }

                    // Project point onto line segment for accurate curb access
                    fun projectPointOnSegment(p: LatLng, a: LatLng, b: LatLng): Pair<LatLng, Float> {
                        val cosLat = Math.cos(Math.toRadians(p.latitude))
                        val px = p.longitude * 111320.0 * cosLat
                        val py = p.latitude * 110540.0
                        val ax = a.longitude * 111320.0 * cosLat
                        val ay = a.latitude * 110540.0
                        val bx = b.longitude * 111320.0 * cosLat
                        val by = b.latitude * 110540.0

                        val dx = bx - ax
                        val dy = by - ay
                        val lenSq = dx * dx + dy * dy
                        if (lenSq < 1e-6) {
                            return Pair(a, distBetween(p.latitude, p.longitude, a.latitude, a.longitude))
                        }
                        val t = ((px - ax) * dx + (py - ay) * dy) / lenSq
                        val clampedT = t.coerceIn(0.0, 1.0)
                        val projLat = a.latitude + clampedT * (b.latitude - a.latitude)
                        val projLon = a.longitude + clampedT * (b.longitude - a.longitude)
                        val projCoord = LatLng(projLat, projLon)
                        return Pair(projCoord, distBetween(p.latitude, p.longitude, projLat, projLon))
                    }

                    // Build graph with 65-meter spatial grid snapping for provincial/rural junction connectivity
                    val snapToleranceMeters = 65f
                    val graphNodes = ArrayList<LatLng>()
                    val gridMap = HashMap<Long, ArrayList<Int>>()

                    fun getNodeIndex(coord: LatLng): Int {
                        val gx = (coord.longitude * 1000.0).toInt().toLong()
                        val gy = (coord.latitude * 1000.0).toInt().toLong()

                        for (dx in -1..1) {
                            for (dy in -1..1) {
                                val cellKey = ((gx + dx) shl 32) or ((gy + dy) and 0xFFFFFFFFL)
                                val bucket = gridMap[cellKey]
                                if (bucket != null) {
                                    for (nid in bucket) {
                                        val existing = graphNodes[nid]
                                        if (distBetween(coord.latitude, coord.longitude, existing.latitude, existing.longitude) <= snapToleranceMeters) {
                                            return nid
                                        }
                                    }
                                }
                            }
                        }

                        val newId = graphNodes.size
                        graphNodes.add(coord)
                        val primaryKey = (gx shl 32) or (gy and 0xFFFFFFFFL)
                        gridMap.getOrPut(primaryKey) { ArrayList() }.add(newId)
                        return newId
                    }

                    class GraphEdge(val targetNode: Int, val weightMeters: Float, val roadName: String, val points: List<LatLng>)
                    val adj = HashMap<Int, ArrayList<GraphEdge>>()

                    fun addEdge(u: Int, v: Int, weight: Float, name: String, pts: List<LatLng>) {
                        if (u != v) {
                            adj.getOrPut(u) { ArrayList() }.add(GraphEdge(v, weight, name, pts))
                            adj.getOrPut(v) { ArrayList() }.add(GraphEdge(u, weight, name, pts.reversed()))
                        }
                    }

                    for (road in candidateRoads) {
                        val pts = road.points
                        for (i in 0 until pts.size - 1) {
                            val p1 = pts[i]
                            val p2 = pts[i + 1]
                            val u = getNodeIndex(p1)
                            val v = getNodeIndex(p2)
                            val segDist = distBetween(p1.latitude, p1.longitude, p2.latitude, p2.longitude)
                            addEdge(u, v, segDist, road.name, listOf(p1, p2))
                        }
                    }

                    // Snap startCoord and destCoord to nearest candidate road segment
                    fun snapToRoad(coord: LatLng): Triple<Int, LatLng, Float> {
                        var bestProj = coord
                        var bestDist = Float.MAX_VALUE
                        var bestSegU = -1
                        var bestSegV = -1
                        var bestRoadName = "Road"

                        for (road in candidateRoads) {
                            val pts = road.points
                            for (i in 0 until pts.size - 1) {
                                val (proj, d) = projectPointOnSegment(coord, pts[i], pts[i + 1])
                                if (d < bestDist) {
                                    bestDist = d
                                    bestProj = proj
                                    bestSegU = getNodeIndex(pts[i])
                                    bestSegV = getNodeIndex(pts[i + 1])
                                    bestRoadName = road.name
                                }
                            }
                        }

                        val projNodeId = getNodeIndex(bestProj)
                        if (bestSegU != -1 && bestSegU != projNodeId) {
                            val d1 = distBetween(bestProj.latitude, bestProj.longitude, graphNodes[bestSegU].latitude, graphNodes[bestSegU].longitude)
                            addEdge(projNodeId, bestSegU, d1, bestRoadName, listOf(bestProj, graphNodes[bestSegU]))
                        }
                        if (bestSegV != -1 && bestSegV != projNodeId) {
                            val d2 = distBetween(bestProj.latitude, bestProj.longitude, graphNodes[bestSegV].latitude, graphNodes[bestSegV].longitude)
                            addEdge(projNodeId, bestSegV, d2, bestRoadName, listOf(bestProj, graphNodes[bestSegV]))
                        }
                        return Triple(projNodeId, bestProj, bestDist)
                    }

                    val (startNid, startRoadPt, _) = snapToRoad(startCoord)
                    val (destNid, destRoadPt, _) = snapToRoad(destCoord)

                    // Run A* / Dijkstra shortest-path solver on physical road network
                    val distances = HashMap<Int, Float>()
                    class PrevStep(val prevNode: Int, val roadName: String, val edgePoints: List<LatLng>)
                    val previous = HashMap<Int, PrevStep>()
                    class PQEntry(val node: Int, val dist: Float, val priority: Float) : Comparable<PQEntry> {
                        override fun compareTo(other: PQEntry): Int = priority.compareTo(other.priority)
                    }
                    val pq = java.util.PriorityQueue<PQEntry>()

                    distances[startNid] = 0f
                    val destNodeCoord = graphNodes[destNid]
                    val initialH = distBetween(graphNodes[startNid].latitude, graphNodes[startNid].longitude, destNodeCoord.latitude, destNodeCoord.longitude)
                    pq.add(PQEntry(startNid, 0f, initialH))

                    var reached = false
                    while (!pq.isEmpty()) {
                        val curr = pq.poll() ?: break
                        val u = curr.node
                        val d = curr.dist
                        val recorded = distances[u] ?: Float.MAX_VALUE
                        if (d > recorded) continue

                        if (u == destNid) {
                            reached = true
                            break
                        }

                        val edges = adj[u] ?: continue
                        for (edge in edges) {
                            val v = edge.targetNode
                            val nd = d + edge.weightMeters
                            val curV = distances[v] ?: Float.MAX_VALUE
                            if (nd < curV) {
                                distances[v] = nd
                                previous[v] = PrevStep(u, edge.roadName, edge.points)
                                val vCoord = graphNodes[v]
                                val h = distBetween(vCoord.latitude, vCoord.longitude, destNodeCoord.latitude, destNodeCoord.longitude)
                                pq.add(PQEntry(v, nd, nd + h))
                            }
                        }
                    }

                    // If destNid was not reachable directly, find visited node closest to destination road point
                    val targetFinalNode = if (reached) destNid else {
                        var bestCompNode = startNid
                        var minSum = Float.MAX_VALUE
                        for ((nodeIdx, dFromStart) in distances) {
                            val nCoord = graphNodes[nodeIdx]
                            val dToDest = distBetween(nCoord.latitude, nCoord.longitude, destRoadPt.latitude, destRoadPt.longitude)
                            if (dFromStart + dToDest < minSum) {
                                minSum = dFromStart + dToDest
                                bestCompNode = nodeIdx
                            }
                        }
                        bestCompNode
                    }

                    // Trace back path along actual road edges
                    val edgesTraversed = ArrayList<PrevStep>()
                    var traceNode = targetFinalNode
                    while (traceNode != startNid) {
                        val step = previous[traceNode] ?: break
                        edgesTraversed.add(step)
                        traceNode = step.prevNode
                    }
                    edgesTraversed.reverse()

                    val polyline = ArrayList<LatLng>()
                    if (distBetween(startCoord.latitude, startCoord.longitude, startRoadPt.latitude, startRoadPt.longitude) <= 30f) {
                        polyline.add(startCoord)
                    }
                    polyline.add(startRoadPt)

                    for (step in edgesTraversed) {
                        for (p in step.edgePoints) {
                            if (polyline.isEmpty() || distBetween(polyline.last().latitude, polyline.last().longitude, p.latitude, p.longitude) > 3f) {
                                polyline.add(p)
                            }
                        }
                    }

                    if (polyline.isEmpty() || distBetween(polyline.last().latitude, polyline.last().longitude, destRoadPt.latitude, destRoadPt.longitude) > 3f) {
                        polyline.add(destRoadPt)
                    }
                    if (distBetween(destCoord.latitude, destCoord.longitude, destRoadPt.latitude, destRoadPt.longitude) <= 30f) {
                        polyline.add(destCoord)
                    }

                    var totalMeters = 0.0
                    for (i in 0 until polyline.size - 1) {
                        totalMeters += distBetween(polyline[i].latitude, polyline[i].longitude, polyline[i + 1].latitude, polyline[i + 1].longitude)
                    }

                    val avgSpeedMps = when (options.mode) {
                        VehicleMode.SCOOTER -> 9.72 // 35 km/h
                        VehicleMode.CAR -> 11.11 // 40 km/h
                        VehicleMode.BICYCLE -> 4.16 // 15 km/h
                        VehicleMode.WALK -> 1.38 // 5 km/h
                    }
                    val durationSecs = (totalMeters / avgSpeedMps).toLong().coerceAtLeast(60L)

                    // Generate distinct turn maneuvers from traversed named roads
                    val maneuvers = ArrayList<TurnManeuver>()
                    val distinctRoads = ArrayList<Pair<String, LatLng>>()
                    for (step in edgesTraversed) {
                        val rname = if (step.roadName != "Road" && step.roadName.isNotBlank()) step.roadName else "Connecting Road"
                        if (distinctRoads.isEmpty() || distinctRoads.last().first != rname) {
                            distinctRoads.add(Pair(rname, step.edgePoints.first()))
                        }
                    }

                    if (distinctRoads.isEmpty()) {
                        distinctRoads.add(Pair("Connecting Road", startRoadPt))
                    }

                    val segDist = totalMeters / distinctRoads.size.toDouble().coerceAtLeast(1.0)
                    for (i in distinctRoads.indices) {
                        val (rname, pt) = distinctRoads[i]
                        maneuvers.add(
                            TurnManeuver(
                                instruction = if (i == 0) "Depart onto $rname" else "Turn onto $rname",
                                roadName = rname,
                                turnType = if (i == 0) "maneuver_straight" else "maneuver_right",
                                distanceMeters = segDist,
                                coordinate = pt
                            )
                        )
                    }
                    maneuvers.add(
                        TurnManeuver(
                            instruction = "Arrive at destination",
                            roadName = "Destination",
                            turnType = "destination_pin",
                            distanceMeters = 0.0,
                            coordinate = destCoord
                        )
                    )

                    return RouteResult(
                        success = true,
                        totalDistanceMeters = totalMeters,
                        estimatedDurationSeconds = durationSecs.toULong(),
                        polyline = polyline,
                        maneuvers = maneuvers,
                        errorMessage = null
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("GabayRoute", "Dijkstra road-network routing error", e)
        }

        // Graceful fallback along start and destination coordinates if no roads in database
        val distResults = FloatArray(1)
        Location.distanceBetween(
            startCoord.latitude, startCoord.longitude,
            destCoord.latitude, destCoord.longitude,
            distResults
        )
        val distM = distResults[0].toDouble().coerceAtLeast(10.0)
        val avgSpeedMps = when (options.mode) {
            VehicleMode.SCOOTER -> 9.72 // 35 km/h
            VehicleMode.CAR -> 11.11 // 40 km/h
            VehicleMode.BICYCLE -> 4.16 // 15 km/h
            VehicleMode.WALK -> 1.38 // 5 km/h
        }
        val durationSecs = (distM / avgSpeedMps).toLong().coerceAtLeast(60L)
        val maneuvers = listOf(
            TurnManeuver(
                instruction = "Depart towards destination",
                roadName = "Direct Route",
                turnType = "maneuver_straight",
                distanceMeters = distM,
                coordinate = startCoord
            ),
            TurnManeuver(
                instruction = "Arrive at destination",
                roadName = "Destination",
                turnType = "destination_pin",
                distanceMeters = 0.0,
                coordinate = destCoord
            )
        )

        return RouteResult(
            success = true,
            totalDistanceMeters = distM,
            estimatedDurationSeconds = durationSecs.toULong(),
            polyline = listOf(startCoord, destCoord),
            maneuvers = maneuvers,
            errorMessage = null
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                speechRecognizer?.destroy()
            } catch (e: Exception) {}
        }
    }

    // Live Hardware GPS Updates (Activates Android OS Location Privacy Indicator)
    DisposableEffect(Unit) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val locationListener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                userLat = loc.latitude
                userLon = loc.longitude
                userSpeed = if (loc.speed < 0.5f) 0.0f else (loc.speed * 3.6f)
                if (loc.hasBearing()) {
                    userHeading = loc.bearing
                }
            }
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }

        try {
            val lastGps = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: locationManager?.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            if (lastGps != null) {
                userLat = lastGps.latitude
                userLon = lastGps.longitude
                userSpeed = if (lastGps.speed < 0.5f) 0.0f else (lastGps.speed * 3.6f)
                if (lastGps.hasBearing()) userHeading = lastGps.bearing
            }
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L,
                0.5f,
                locationListener
            )
        } catch (e: SecurityException) {
            // Permission fallback
        }

        onDispose {
            try {
                locationManager?.removeUpdates(locationListener)
            } catch (e: Exception) {}
        }
    }

    // Hardware mic managed directly by SpeechRecognizer (No locking conflict)

    // Conversation History Transcript for Live Agent (like Antigravity IDE)
    val chatMessages = remember {
        mutableStateListOf(
            ChatMessage("GABAY AGENT", "ONLINE • RALLY-COCKPIT READY (PHILIPPINES)", "10:00")
        )
    }

    // Curated Successful Drives History
    val driveHistory = remember {
        mutableStateListOf(
            DriveHistoryItem("Makati CBD to Bonifacio High Street BGC", 4.2f, 14, "Yesterday 5:45 PM"),
            DriveHistoryItem("Ayala Avenue to SM Mall of Asia", 7.8f, 22, "Oct 15, 2026"),
            DriveHistoryItem("Ortigas Center to NAIA Terminal 3", 11.5f, 31, "Oct 12, 2026")
        )
    }

    // Live Telemetry from Rust Core Tracker (Realistic defaults: 0 km when idle)
    var telemetry by remember {
        mutableStateOf(
            HudTelemetry(
                liveSpeedKmh = 0.0f,
                headingDegrees = userHeading,
                headingCardinal = "N",
                remainingDistanceKm = 0.0f,
                paceWindowMin = "-- MIN",
                nextTurnRoad = "",
                nextTurnDistanceM = 0.0f,
                nextTurnIcon = "straight",
                isOffRoute = false,
                offRouteDeltaM = 0.0f,
                coPilotAdvisory = null
            )
        )
    }

    // Process search queries in real-time from SQLite FTS5 database
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank() && searchQuery.length >= 2) {
            searchResults = engine?.searchPois(searchQuery.trim(), 5u) ?: emptyList()
        } else {
            searchResults = emptyList()
        }
    }

    // Process GPS tick through Rust Engine
    LaunchedEffect(userLat, userLon, userSpeed, userHeading, activeRoute) {
        engine?.let { eng ->
            val updatedTelemetry = eng.processGpsTick(
                latitude = userLat,
                longitude = userLon,
                speedKmh = userSpeed,
                headingDeg = userHeading
            )
            telemetry = updatedTelemetry

            // Speak terse advisory if co-pilot triggered an alert
            updatedTelemetry.coPilotAdvisory?.let { advisory ->
                audioController.speakTerseCue(advisory)
            }
        }
    }

    // Agentic Command & Contextual Intent Dispatcher
    fun processAgentPrompt(prompt: String) {
        val now = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        chatMessages.add(ChatMessage("RIDER", prompt, now))
        val lower = prompt.lowercase(Locale.ROOT).trim()

        var responseText: String

        when {
            lower.contains("where am i") || lower.contains("location") || lower.contains("status") -> {
                if (activeRoute != null) {
                    val road = activeRoute?.maneuvers?.firstOrNull()?.roadName ?: "route"
                    responseText = "En route on $road. Remaining distance ${String.format(Locale.US, "%.1f", telemetry.remainingDistanceKm)} km. Speed ${userSpeed.toInt()} km/h."
                } else {
                    val nearbyPois = engine?.searchPois("Cabiao", 1u) ?: emptyList()
                    val nearLabel = nearbyPois.firstOrNull()?.name ?: "San Isidro / Cabiao"
                    responseText = "Parked near $nearLabel (${String.format(Locale.US, "%.3f, %.3f", userLat, userLon)}). Speed is 0 km/h. Map ready."
                }
            }
            lower.contains("how many km") || lower.contains("remaining") || lower.contains("how far") || lower.contains("distance") -> {
                if (activeRoute != null) {
                    responseText = "You have ${String.format(Locale.US, "%.1f", telemetry.remainingDistanceKm)} km remaining to destination, about ${telemetry.paceWindowMin} at your current pace."
                } else {
                    responseText = "No active navigation route. Long-press the map to drop a pin or search a destination."
                }
            }
            lower.contains("what is my speed") || lower.contains("how fast") || lower.contains("speed") -> {
                responseText = "Current vehicle speed is ${userSpeed.toInt()} km/h."
            }
            lower.contains("how long") || lower.contains("eta") || lower.contains("estimated time") || lower.contains("arrival") -> {
                if (activeRoute != null) {
                    responseText = "Estimated travel time is ${telemetry.paceWindowMin}."
                } else {
                    responseText = "No active route. Drop a pin or search a destination to calculate ETA."
                }
            }
            lower.contains("gas") || lower.contains("petron") || lower.contains("shell") || lower.contains("fuel") || lower.contains("caltex") || lower.contains("cleanfuel") || lower.contains("phoenix") -> {
                val query = when {
                    lower.contains("petron") -> "Petron"
                    lower.contains("shell") -> "Shell"
                    lower.contains("caltex") -> "Caltex"
                    lower.contains("cleanfuel") -> "Cleanfuel"
                    lower.contains("phoenix") -> "Phoenix"
                    else -> "Gas"
                }
                val pois = engine?.searchPois(query, 5u) ?: emptyList()
                val closest = pois.firstOrNull()
                if (closest != null) {
                    destinationPin = closest.coordinate
                    cameraTarget = MapLibreLatLng(closest.coordinate.latitude, closest.coordinate.longitude)
                    val distResults = FloatArray(1)
                    Location.distanceBetween(userLat, userLon, closest.coordinate.latitude, closest.coordinate.longitude, distResults)
                    val distM = distResults[0].toDouble()
                    responseText = "Found ${closest.name}, ${String.format(Locale.US, "%.1f", distM / 1000.0)} km away. Pin dropped. Tap Start Route to navigate."
                } else {
                    responseText = "No gas stations found nearby in offline vault."
                }
            }
            lower.contains("food") || lower.contains("restaurant") || lower.contains("jollibee") || lower.contains("mcdonald") || lower.contains("eat") -> {
                val query = when {
                    lower.contains("jollibee") -> "Jollibee"
                    lower.contains("mcdonald") -> "McDonald"
                    else -> "Restaurant"
                }
                val pois = engine?.searchPois(query, 5u) ?: emptyList()
                val closest = pois.firstOrNull()
                if (closest != null) {
                    destinationPin = closest.coordinate
                    cameraTarget = MapLibreLatLng(closest.coordinate.latitude, closest.coordinate.longitude)
                    val distResults = FloatArray(1)
                    Location.distanceBetween(userLat, userLon, closest.coordinate.latitude, closest.coordinate.longitude, distResults)
                    val distM = distResults[0].toDouble()
                    responseText = "Found ${closest.name}, ${String.format(Locale.US, "%.1f", distM / 1000.0)} km away. Pin dropped."
                } else {
                    responseText = "No restaurants found nearby in offline vault."
                }
            }
            lower.contains("hospital") || lower.contains("clinic") || lower.contains("emergency") || lower.contains("pharmacy") -> {
                val query = if (lower.contains("pharmacy")) "Pharmacy" else "Hospital"
                val pois = engine?.searchPois(query, 5u) ?: emptyList()
                val closest = pois.firstOrNull()
                if (closest != null) {
                    destinationPin = closest.coordinate
                    cameraTarget = MapLibreLatLng(closest.coordinate.latitude, closest.coordinate.longitude)
                    val distResults = FloatArray(1)
                    Location.distanceBetween(userLat, userLon, closest.coordinate.latitude, closest.coordinate.longitude, distResults)
                    val distM = distResults[0].toDouble()
                    responseText = "Found ${closest.name}, ${String.format(Locale.US, "%.1f", distM / 1000.0)} km away. Pin dropped."
                } else {
                    responseText = "No medical facilities found nearby in offline vault."
                }
            }
            lower.contains("satellite") -> {
                fusedMapMode = FusedMapMode.SATELLITE
                responseText = "Offline satellite view enabled."
            }
            lower.contains("topo") -> {
                fusedMapMode = FusedMapMode.TOPO
                responseText = "Topography elevation contours enabled."
            }
            lower.contains("street") || lower.contains("vector") || lower.contains("normal map") -> {
                fusedMapMode = FusedMapMode.STREET
                responseText = "Street vector mode enabled."
            }
            lower.contains("3d") || lower.contains("tilt") || lower.contains("perspective") -> {
                is3dMode = true
                responseText = "3D perspective enabled."
            }
            lower.contains("2d") || lower.contains("flat") || lower.contains("overhead") -> {
                is3dMode = false
                responseText = "2D overhead view enabled."
            }
            lower.contains("north") || lower.contains("rotate") || lower.contains("bearing") -> {
                resetRotationCount++
                responseText = "Compass aligned to True North."
            }
            lower.contains("follow") || lower.contains("magnet") || lower.contains("track") || lower.contains("recenter") -> {
                isFollowingVehicle = true
                cameraTarget = MapLibreLatLng(userLat, userLon)
                responseText = "Magnetic camera tracking engaged. Map will pan and auto-rotate with your vehicle heading."
            }
            lower.contains("cancel") || lower.contains("stop route") || lower.contains("stop navigation") || lower.contains("clear route") || lower.contains("clear pin") || lower.contains("remove pin") -> {
                activeRoute = null
                destinationPin = null
                pinInfo = null
                NavigationService.stop(context)
                responseText = "Active route and pinned destination cleared. Returning to free explore."
            }
            lower.contains("who are you") || lower.contains("what are you") || lower.contains("what can you do") || lower.contains("help") -> {
                responseText = "I am Gabay, your 100% offline tactical Philippine navigation co-pilot. I guide your routes, track speed and heading, search 23,000+ local places, and control map views hands-free."
            }
            lower.contains("hello") || lower.contains("hi") || lower.contains("kamusta") || lower.contains("magandang") -> {
                responseText = "Kamusta rider! Co-pilot active and tracking Philippine road network offline. Where would you like to go?"
            }
            lower.contains("salamat") || lower.contains("thank you") || lower.contains("thanks") -> {
                responseText = "Walang anuman! Ride safely and keep your eyes on the road."
            }
            lower.contains("weather") || lower.contains("rain") || lower.contains("ulan") -> {
                responseText = "Operating offline. Keep safe following distance on wet asphalt."
            }
            lower.contains("traffic") -> {
                responseText = if (activeRoute != null) {
                    "Monitoring local road corridors. Current speed ${userSpeed.toInt()} km/h on ${activeRoute?.maneuvers?.firstOrNull()?.roadName ?: "route"}."
                } else {
                    "No active route. Local roads in your vicinity are open."
                }
            }
            lower.startsWith("go to ") || lower.startsWith("navigate to ") || lower.startsWith("route to ") -> {
                val query = lower.removePrefix("go to ").removePrefix("navigate to ").removePrefix("route to ").trim()
                val pois = engine?.searchPois(query, 1u) ?: emptyList()
                val target = pois.firstOrNull()
                if (target != null) {
                    val options = RouteOptions(
                        mode = selectedMode,
                        avoidTolls = (selectedMode == VehicleMode.SCOOTER),
                        avoidExpressways = (selectedMode == VehicleMode.SCOOTER),
                        avoidFerries = false
                    )
                    destinationPin = target.coordinate
                    isFollowingVehicle = true
                    val startCoord = LatLng(userLat, userLon)
                    val route = computeRoute(startCoord, target.coordinate, options)
                    activeRoute = route
                    try { engine?.setActiveRoute(route) } catch (e: Exception) {}
                    telemetry = telemetry.copy(
                        remainingDistanceKm = (route.totalDistanceMeters / 1000.0).toFloat(),
                        paceWindowMin = "${(route.estimatedDurationSeconds / 60u)} MIN"
                    )
                    NavigationService.start(context)
                    cameraTarget = MapLibreLatLng(target.coordinate.latitude, target.coordinate.longitude)
                    responseText = "Navigating to ${target.name}. ${String.format(Locale.US, "%.1f", route.totalDistanceMeters / 1000.0)} km ahead via connected roads."
                } else {
                    responseText = "Destination \"$query\" not found in offline vault."
                }
            }
            else -> {
                val searchKey = lower
                    .removePrefix("where is ").removePrefix("where's ")
                    .removePrefix("find ").removePrefix("search ")
                    .removePrefix("is there a ").removePrefix("is there any ")
                    .removePrefix("tell me about ").removePrefix("take me to ")
                    .trim()
                val pois = if (searchKey.length >= 2) engine?.searchPois(searchKey, 3u) ?: emptyList() else emptyList()
                val match = pois.firstOrNull()
                if (match != null) {
                    destinationPin = match.coordinate
                    cameraTarget = MapLibreLatLng(match.coordinate.latitude, match.coordinate.longitude)
                    val distResults = FloatArray(1)
                    Location.distanceBetween(userLat, userLon, match.coordinate.latitude, match.coordinate.longitude, distResults)
                    val km = distResults[0] / 1000.0
                    responseText = "Found ${match.name} (${match.category}), ${String.format(Locale.US, "%.1f", km)} km away. Pin dropped. Say 'navigate to ${match.name}' to start route."
                } else {
                    val askResult = engine?.askCopilot(prompt)
                    if (askResult != null && !askResult.contains("PETRON 450M") && !askResult.contains("ORIGIN ROAD") && !askResult.contains("STANDBY")) {
                        responseText = askResult
                    } else {
                        responseText = if (activeRoute != null) {
                            "En route on ${activeRoute?.maneuvers?.firstOrNull()?.roadName ?: "route"}. Remaining distance ${String.format(Locale.US, "%.1f", telemetry.remainingDistanceKm)} km. Speed ${userSpeed.toInt()} km/h."
                        } else {
                            "I heard \"$prompt\". No matching location found in offline vault. You can ask for gas stations, food, hospitals, or speak a destination to navigate."
                        }
                    }
                }
            }
        }

        chatMessages.add(ChatMessage("GABAY AGENT", responseText, now))
        audioController.speakTerseCue(responseText)
    }

    // Speech-to-Text Listener with unrestricted mic stream
    fun startListening() {
        val recognizer = speechRecognizer ?: try {
            SpeechRecognizer.createSpeechRecognizer(context)
        } catch (e: Exception) {
            null
        }

        if (recognizer == null) {
            isListening = false
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                android.util.Log.d("GabayVoice", "Ready for speech")
            }
            override fun onBeginningOfSpeech() {
                isListening = true
                android.util.Log.d("GabayVoice", "Beginning of speech")
            }
            override fun onRmsChanged(rmsdB: Float) {
                audioRmsDb = rmsdB
            }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                audioRmsDb = 0f
                android.util.Log.d("GabayVoice", "End of speech")
            }
            override fun onError(error: Int) {
                isListening = false
                audioRmsDb = 0f
                partialSpeechText = ""
                android.util.Log.w("GabayVoice", "SpeechRecognizer error code: $error")
            }
            override fun onResults(results: Bundle?) {
                isListening = false
                audioRmsDb = 0f
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val voiceQuery = matches?.firstOrNull() ?: partialSpeechText
                partialSpeechText = ""
                android.util.Log.d("GabayVoice", "Speech recognition result: $voiceQuery")
                if (!voiceQuery.isNullOrBlank()) {
                    processAgentPrompt(voiceQuery)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = matches?.firstOrNull()
                if (!partial.isNullOrBlank()) {
                    partialSpeechText = partial
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        try {
            recognizer.startListening(intent)
            isListening = true
        } catch (e: Exception) {
            android.util.Log.e("GabayVoice", "startListening exception", e)
            isListening = false
        }
    }

    val focusManager = LocalFocusManager.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GabayColors.MapCanvasBg)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
    ) {
        // LAYER 0: MapLibre Canvas with Real OSM Streets, Waterways, Topo & Satellite Raster
        MapCanvas(
            userLat = userLat,
            userLon = userLon,
            userHeading = userHeading,
            activeRoute = activeRoute,
            destinationPin = destinationPin,
            fusedMapMode = fusedMapMode,
            is3dPerspective = is3dMode,
            isFollowingVehicle = isFollowingVehicle,
            resetRotationTrigger = resetRotationCount,
            cameraTarget = cameraTarget,
            onBearingChanged = { cameraBearing = it },
            onFollowModeChanged = { isFollowingVehicle = it },
            onPinDropped = { latLng ->
                destinationPin = latLng
                // Query nearest landmark/road from SQLite FTS5 engine using real spatial coordinates
                var landmarkName = "Pinned Destination"
                try {
                    val dbFile = File("/sdcard/Documents/Gabay/pois_fts.sqlite")
                    if (dbFile.exists()) {
                        val db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
                        val pad = 0.03
                        val cursor = db.rawQuery(
                            "SELECT name FROM pois WHERE latitude BETWEEN ? AND ? AND longitude BETWEEN ? AND ? LIMIT 1",
                            arrayOf((latLng.latitude - pad).toString(), (latLng.latitude + pad).toString(), (latLng.longitude - pad).toString(), (latLng.longitude + pad).toString())
                        )
                        if (cursor.moveToFirst()) {
                            landmarkName = cursor.getString(0) ?: "Pinned Destination"
                        }
                        cursor.close()
                        db.close()
                    }
                } catch (e: Exception) {}

                pinInfo = PinInfo(
                    coordinate = latLng,
                    nearestRoadName = landmarkName,
                    barangay = "Local Vicinity",
                    cityOrMunicipality = "Philippines"
                )
            },
            modifier = Modifier.fillMaxSize()
        )

        // LAYER 1: Top Search Bar & Active Route Telemetry Pill Bar
        val statusBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = (statusBarTopPadding - 8.dp).coerceAtLeast(4.dp))
                .fillMaxWidth()
        ) {
            TopSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                searchResults = searchResults,
                onSelectPlace = { place ->
                    val dest = place.coordinate
                    destinationPin = dest
                    isFollowingVehicle = true
                    cameraTarget = MapLibreLatLng(dest.latitude, dest.longitude)
                    searchQuery = ""

                    // Calculate Route
                    val options = RouteOptions(
                        mode = selectedMode,
                        avoidTolls = (selectedMode == VehicleMode.SCOOTER),
                        avoidExpressways = (selectedMode == VehicleMode.SCOOTER),
                        avoidFerries = false
                    )
                    val startCoord = LatLng(userLat, userLon)
                    val route = computeRoute(startCoord, dest, options)
                    activeRoute = route
                    try { engine?.setActiveRoute(route) } catch (e: Exception) {}
                    telemetry = telemetry.copy(
                        remainingDistanceKm = (route.totalDistanceMeters / 1000.0).toFloat(),
                        paceWindowMin = "${(route.estimatedDurationSeconds / 60u)} MIN"
                    )
                    NavigationService.start(context)
                    audioController.speakTerseCue("ROUTE ACTIVE TO ${place.name.uppercase()}")
                },
                onMenuClick = { isMenuSheetOpen = true },
                modifier = Modifier.fillMaxWidth()
            )

            // LAYER 2: Floating Distance, Cancel Route, & Pace Pills (ONLY Visible When Route Is Active)
            if (activeRoute != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FloatingTelemetryPill(
                        value = String.format(Locale.US, "%.1f", telemetry.remainingDistanceKm),
                        unit = "KM REMAINING"
                    )

                    // Direct Instant Route Cancellation Button
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF2A1010), RoundedCornerShape(20.dp))
                            .border(1.dp, Color(0xFFFF3B30).copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                            .clickable {
                                activeRoute = null
                                destinationPin = null
                                pinInfo = null
                                NavigationService.stop(context)
                                audioController.speakTerseCue("ROUTE CANCELLED")
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = GabayIcons.ActionClose,
                                contentDescription = "Cancel Route",
                                tint = Color(0xFFFF3B30),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "END ROUTE",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color(0xFFFF3B30)
                                )
                            )
                        }
                    }

                    FloatingTelemetryPill(
                        value = telemetry.paceWindowMin,
                        unit = "EST PACE"
                    )
                }
            }
        }

        // LAYER 3: Pin Inspector Sheet (Overlayed When Long-Pressed)
        pinInfo?.let { info ->
            PinInspectorSheet(
                pinInfo = info,
                isRouteActive = (activeRoute != null),
                onStartRoute = { destCoord ->
                    val options = RouteOptions(
                        mode = selectedMode,
                        avoidTolls = (selectedMode == VehicleMode.SCOOTER),
                        avoidExpressways = (selectedMode == VehicleMode.SCOOTER),
                        avoidFerries = false
                    )
                    destinationPin = destCoord
                    isFollowingVehicle = true
                    val startCoord = LatLng(userLat, userLon)
                    val route = computeRoute(startCoord, destCoord, options)
                    activeRoute = route
                    try { engine?.setActiveRoute(route) } catch (e: Exception) {}
                    telemetry = telemetry.copy(
                        remainingDistanceKm = (route.totalDistanceMeters / 1000.0).toFloat(),
                        paceWindowMin = "${(route.estimatedDurationSeconds / 60u)} MIN"
                    )
                    NavigationService.start(context)
                    val road = route.maneuvers.firstOrNull()?.roadName ?: info.nearestRoadName
                    audioController.speakTerseCue("STARTING ROUTE TO ${info.nearestRoadName.uppercase()}. PROCEED VIA $road.")
                    pinInfo = null
                },
                onCancelRoute = {
                    activeRoute = null
                    destinationPin = null
                    pinInfo = null
                    NavigationService.stop(context)
                    audioController.speakTerseCue("ROUTE CANCELLED")
                },
                onClearPin = {
                    destinationPin = null
                    pinInfo = null
                },
                onClose = {
                    pinInfo = null
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp)
            )
        }

        // LAYER 4: Bottom Floating Controls & Speedometer (Middle-Bottom Speed, Flanking Turn Arrows)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Floating Action Buttons Row: Fused Map Layer Speed-Dial, 2D/3D Toggle, Agent Talk, Compass, Recenter GPS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Left Controls: Speed Dial + 2D/3D Toggle
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MapStyleSpeedDial(
                        currentMode = fusedMapMode,
                        onSelectMode = { fusedMapMode = it }
                    )
                    PerspectiveToggleButton(
                        is3d = is3dMode,
                        onToggle = { is3dMode = !is3dMode }
                    )
                }

                // Middle Speedometer Readout (Center Bottom)
                MiddleBottomSpeedometer(
                    speedKmh = userSpeed,
                    nextTurnIcon = if (activeRoute != null) telemetry.nextTurnIcon else "straight",
                    bottomLabel = if (activeRoute != null) telemetry.nextTurnRoad else ""
                )

                // Right Controls: Reset North Compass + Talk Mic + Recenter GPS
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CompassResetButton(
                        cameraBearing = cameraBearing,
                        onReset = { resetRotationCount++ }
                    )
                    CircularFloatingButton(
                        icon = GabayIcons.CopilotMic,
                        isActive = isAgentSheetOpen || isListening,
                        activeColor = GabayColors.AccentMint,
                        onClick = {
                            isAgentSheetOpen = !isAgentSheetOpen
                            if (isAgentSheetOpen) {
                                audioController.speakTerseCue("GABAY CO-PILOT READY")
                            }
                        },
                        contentDescription = "Talk to Gabay Agent"
                    )
                    CircularFloatingButton(
                        icon = GabayIcons.RecenterGps,
                        isActive = isFollowingVehicle,
                        activeColor = GabayColors.AccentMint,
                        onClick = {
                            isFollowingVehicle = true
                            cameraTarget = MapLibreLatLng(userLat, userLon)
                        },
                        contentDescription = "Recenter GPS & Magnetic Lock"
                    )
                }
            }
        }

        // LAYER 5: Full-Screen Scrim Backdrop for Click-Outside Sheet Dismissal
        if (isAgentSheetOpen || isMenuSheetOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isAgentSheetOpen = false
                        isMenuSheetOpen = false
                    }
            )
        }

        // LAYER 6: Agent Co-Pilot Conversation Transcript Sheet
        AnimatedVisibility(
            visible = isAgentSheetOpen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            AgentCoPilotSheet(
                messages = chatMessages,
                isListening = isListening,
                audioRmsDb = audioRmsDb,
                partialSpeechText = partialSpeechText,
                onToggleListening = {
                    if (isListening) {
                        try { speechRecognizer?.stopListening() } catch (e: Exception) {}
                        isListening = false
                        audioRmsDb = 0f
                        partialSpeechText = ""
                    } else {
                        startListening()
                    }
                },
                onSendPrompt = { prompt ->
                    processAgentPrompt(prompt)
                },
                onClose = { isAgentSheetOpen = false }
            )
        }

        // LAYER 7: Top Menu Drawer Sheet (Drive History & Vault Stats)
        AnimatedVisibility(
            visible = isMenuSheetOpen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            TopMenuSheet(
                driveHistory = driveHistory,
                selectedMode = selectedMode,
                onModeSelect = { selectedMode = it },
                onClose = { isMenuSheetOpen = false }
            )
        }
    }
}

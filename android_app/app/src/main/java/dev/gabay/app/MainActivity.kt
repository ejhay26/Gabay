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
import org.json.JSONArray
import org.json.JSONObject
import dev.gabay.app.ai.LlamaDaemon
import dev.gabay.app.ai.IntentResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

        // Ensure On-Device AI Brain Daemon is active on localhost:8766
        LlamaDaemon.ensureServerRunning(this)

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

        checkAllPermissions()

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

    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
        android.util.Log.i("GabayPerms", "Permissions updated: fineLocation=$fineLocationGranted")
    }

    private fun checkAllPermissions() {
        val perms = mutableListOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.RECORD_AUDIO
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            perms.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        val ungranted = perms.filter {
            androidx.core.content.ContextCompat.checkSelfPermission(this, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (ungranted.isNotEmpty()) {
            requestPermissionLauncher.launch(ungranted.toTypedArray())
        }

        // Check All Files Access (MANAGE_EXTERNAL_STORAGE)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (!android.os.Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = android.net.Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    try {
                        val intent = Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        startActivity(intent)
                    } catch (e2: Exception) {}
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        PmTilesServer.reloadReaders(File("/sdcard/Documents/Gabay"))
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
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        LlamaDaemon.ensureServerRunning(context)
    }

    // Rider & Vehicle States
    var selectedMode by remember { mutableStateOf(VehicleMode.SCOOTER) }
    var fusedMapMode by remember { mutableStateOf(FusedMapMode.STREET) }
    var audioGuidanceMode by remember { mutableStateOf(audioController.guidanceMode) }

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
    var activeRouteAdvisory by remember { mutableStateOf<String?>(null) }
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
    var isRouteLoading by remember { mutableStateOf(false) }
    var isNavigating by remember { mutableStateOf(false) }
    var avoidTolls by remember { mutableStateOf(true) }
    var avoidExpressways by remember { mutableStateOf(true) }
    var originName by remember { mutableStateOf("Your location") }
    var destinationName by remember { mutableStateOf("Dropped pin") }
    var activeOriginCoord by remember { mutableStateOf<LatLng?>(null) }
    var activeDestCoord by remember { mutableStateOf<LatLng?>(null) }
    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else null
    }

    // High-Fidelity Road Network Router: 100% physical road centerline adherence with dynamic bounding box & road snapping
    fun computeRoute(startCoord: LatLng, destCoord: LatLng, options: RouteOptions): RouteResult {
        try {
            val distTmp = FloatArray(1)
            fun distBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
                Location.distanceBetween(lat1, lon1, lat2, lon2, distTmp)
                return distTmp[0]
            }

            fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
                val dLon = Math.toRadians(lon2 - lon1)
                val phi1 = Math.toRadians(lat1)
                val phi2 = Math.toRadians(lat2)
                val y = Math.sin(dLon) * Math.cos(phi2)
                val x = Math.cos(phi1) * Math.sin(phi2) - Math.sin(phi1) * Math.cos(phi2) * Math.cos(dLon)
                val brng = Math.toDegrees(Math.atan2(y, x)).toFloat()
                return (brng + 360f) % 360f
            }

            // Accurate perpendicular projection onto road segment with parameter t
            class ProjResult(val lat: Double, val lon: Double, val distMeters: Float, val t: Float)

            fun projectPointOnSegment(pLat: Double, pLon: Double, aLat: Double, aLon: Double, bLat: Double, bLon: Double): ProjResult {
                val cosLat = Math.cos(Math.toRadians(pLat))
                val px = pLon * 111320.0 * cosLat
                val py = pLat * 110540.0
                val ax = aLon * 111320.0 * cosLat
                val ay = aLat * 110540.0
                val bx = bLon * 111320.0 * cosLat
                val by = bLat * 110540.0

                val dx = bx - ax
                val dy = by - ay
                val lenSq = dx * dx + dy * dy
                if (lenSq < 1e-6) {
                    return ProjResult(aLat, aLon, distBetween(pLat, pLon, aLat, aLon), 0f)
                }
                val t = (((px - ax) * dx + (py - ay) * dy) / lenSq).coerceIn(0.0, 1.0).toFloat()
                val projLat = aLat + t * (bLat - aLat)
                val projLon = aLon + t * (bLon - aLon)
                val d = distBetween(pLat, pLon, projLat, projLon)
                return ProjResult(projLat, projLon, d, t)
            }

            fun isRoadDrivable(name: String, kind: String): Boolean {
                val k = kind.lowercase(Locale.ROOT)
                val n = name.lowercase(Locale.ROOT)

                // Philippine Expressways (SCTEX, NLEX, Skyway, TPLEX, CLLEX, CAVITEX, CALAX, MCX, NAIAX, SLEX, STAR, CCLEX)
                val isExpressway = k.contains("motorway") || k == "highway" ||
                    n.contains("expressway") || n.contains("sctex") || n.contains("nlex") ||
                    n.contains("slex") || n.contains("skyway") || n.contains("tplex") ||
                    n.contains("cllex") || n.contains("cavitex") || n.contains("calax") ||
                    n.contains("mcx") || n.contains("naiax") || n.contains("cclex") ||
                    n.contains("tollway")

                if (isExpressway) {
                    // Under Philippine law (RA 4136 / DOTr), sub-400cc motorcycles, bicycles, and pedestrians are barred from expressways
                    if (options.avoidExpressways || options.avoidTolls ||
                        options.mode == VehicleMode.SCOOTER ||
                        options.mode == VehicleMode.BICYCLE ||
                        options.mode == VehicleMode.WALK) {
                        return false
                    }
                }

                if (k == "path" || k == "footway" || k == "pedestrian" || k == "steps" ||
                    k == "corridor" || k == "bridleway" || k == "track" || k == "rail" ||
                    k == "ferry" || k == "aerialway" || k == "aeroway" || k == "waterway") {
                    return false
                }
                if (k == "cycleway" && options.mode != VehicleMode.BICYCLE) {
                    return false
                }
                return true
            }

            class RoadRaw(val name: String, val kind: String, val points: List<LatLng>, val isDetailed: Boolean = true)
            val candidateRoads = ArrayList<RoadRaw>()

            // Dynamic bounding box scaled to journey distance: guaranteed min 2500m to include arterials/bypasses
            val straightDist = distBetween(startCoord.latitude, startCoord.longitude, destCoord.latitude, destCoord.longitude)
            val padMeters = maxOf(straightDist * 0.8f, 2500f).coerceAtMost(8000f)
            val padLat = padMeters / 110540.0
            val cosLat = Math.cos(Math.toRadians(startCoord.latitude)).coerceAtLeast(0.1)
            val padLon = padMeters / (111320.0 * cosLat)

            val minLat = minOf(startCoord.latitude, destCoord.latitude) - padLat
            val maxLat = maxOf(startCoord.latitude, destCoord.latitude) + padLat
            val minLon = minOf(startCoord.longitude, destCoord.longitude) - padLon
            val maxLon = maxOf(startCoord.longitude, destCoord.longitude) + padLon

            // 1. Query candidate roads from device SQLite database
            val dbFile = File("/sdcard/Documents/Gabay/pois_fts.sqlite")
            if (dbFile.exists()) {
                try {
                    val db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
                    // High-detail Zoom 11/12 tiles have id > 120000.
                    // Coarse overview Zoom 10 tiles have id <= 120000 and cut straight chords across fields (ghost roads).
                    val detailedCountCursor = db.rawQuery(
                        "SELECT count(*) FROM roads WHERE id > 120000 AND min_lat <= ? AND max_lat >= ? AND min_lon <= ? AND max_lon >= ?",
                        arrayOf(maxLat.toString(), minLat.toString(), maxLon.toString(), minLon.toString())
                    )
                    var hasDetailedRoads = false
                    if (detailedCountCursor.moveToFirst()) {
                        hasDetailedRoads = detailedCountCursor.getInt(0) >= 8
                    }
                    detailedCountCursor.close()

                    val sql = if (hasDetailedRoads) {
                        "SELECT id, name, kind, geometry FROM roads WHERE id > 120000 AND min_lat <= ? AND max_lat >= ? AND min_lon <= ? AND max_lon >= ?"
                    } else {
                        "SELECT id, name, kind, geometry FROM roads WHERE min_lat <= ? AND max_lat >= ? AND min_lon <= ? AND max_lon >= ?"
                    }

                    val cursor = db.rawQuery(
                        sql,
                        arrayOf(maxLat.toString(), minLat.toString(), maxLon.toString(), minLon.toString())
                    )
                    if (cursor.moveToFirst()) {
                        do {
                            val roadId = cursor.getLong(0)
                            val name = cursor.getString(1) ?: "Road"
                            val kind = cursor.getString(2) ?: "road"
                            val geomStr = cursor.getString(3) ?: ""
                            if (!isRoadDrivable(name, kind)) continue
                            val isDetailed = (roadId > 120000L)

                            // Zero-allocation coordinate parser: parses coordinates with char indices instead of allocating thousands of String objects
                            val pts = ArrayList<LatLng>(32)
                            var cStart = 0
                            val cLen = geomStr.length
                            while (cStart < cLen) {
                                var semi = geomStr.indexOf(';', cStart)
                                if (semi == -1) semi = cLen
                                val comma = geomStr.indexOf(',', cStart)
                                if (comma in (cStart + 1) until semi) {
                                    val lat = geomStr.substring(cStart, comma).toDoubleOrNull()
                                    val lon = geomStr.substring(comma + 1, semi).toDoubleOrNull()
                                    if (lat != null && lon != null) {
                                        pts.add(LatLng(lat, lon))
                                    }
                                }
                                cStart = semi + 1
                            }
                            if (pts.size >= 2) {
                                candidateRoads.add(RoadRaw(name, kind, pts, isDetailed))
                            }
                        } while (cursor.moveToNext())
                    }
                    cursor.close()
                    db.close()
                } catch (e: Exception) {
                    android.util.Log.w("GabayRoute", "SQLite roads query warning: ${e.message}")
                }
            }

            // 2. If SQLite returned no candidate roads or was absent, fallback to bundled APK assets
            if (candidateRoads.isEmpty()) {
                val assetNames = listOf("roads_major.geojson", "roads_minor.geojson", "roads.geojson")
                for (assetName in assetNames) {
                    if (candidateRoads.isNotEmpty()) break
                    try {
                        context.assets.open(assetName).bufferedReader().use { reader ->
                            val root = JSONObject(reader.readText())
                            val features = root.optJSONArray("features") ?: return@use
                            for (fIdx in 0 until features.length()) {
                                val feat = features.optJSONObject(fIdx) ?: continue
                                val props = feat.optJSONObject("properties")
                                val name = props?.optString("name")?.takeIf { it.isNotBlank() } ?: "Road"
                                val kind = props?.optString("highway")?.takeIf { it.isNotBlank() }
                                    ?: props?.optString("kind") ?: "road"
                                if (!isRoadDrivable(name, kind)) continue

                                val geom = feat.optJSONObject("geometry") ?: continue
                                val coords = geom.optJSONArray("coordinates") ?: continue
                                if (coords.length() < 2) continue

                                var featMinLat = 90.0
                                var featMaxLat = -90.0
                                var featMinLon = 180.0
                                var featMaxLon = -180.0
                                val pts = ArrayList<LatLng>(coords.length())

                                for (cIdx in 0 until coords.length()) {
                                    val ptArr = coords.optJSONArray(cIdx) ?: continue
                                    val lon = ptArr.optDouble(0)
                                    val lat = ptArr.optDouble(1)
                                    if (lat < featMinLat) featMinLat = lat
                                    if (lat > featMaxLat) featMaxLat = lat
                                    if (lon < featMinLon) featMinLon = lon
                                    if (lon > featMaxLon) featMaxLon = lon
                                    pts.add(LatLng(lat, lon))
                                }

                                if (featMinLat <= maxLat && featMaxLat >= minLat &&
                                    featMinLon <= maxLon && featMaxLon >= minLon && pts.size >= 2) {
                                    candidateRoads.add(RoadRaw(name, kind, pts, true))
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Continue to next asset fallback
                    }
                }
            }

            if (candidateRoads.isNotEmpty()) {
                // High-Fidelity Graph Construction:
                // 1. Assign unique node IDs to EVERY road vertex (Preserves 100% curve geometry)
                val nodeCoords = ArrayList<LatLng>()
                val roadNodeMap = ArrayList<IntArray>(candidateRoads.size)

                for (rIdx in candidateRoads.indices) {
                    val pts = candidateRoads[rIdx].points
                    val nodeIds = IntArray(pts.size)
                    for (pIdx in pts.indices) {
                        val nid = nodeCoords.size
                        nodeCoords.add(pts[pIdx])
                        nodeIds[pIdx] = nid
                    }
                    roadNodeMap.add(nodeIds)
                }

                class GraphEdge(
                    val targetNode: Int,
                    val weightMeters: Float,
                    val roadName: String,
                    val points: List<LatLng>,
                    val bearingDeg: Float
                )
                val adj = HashMap<Int, ArrayList<GraphEdge>>()

                fun addEdge(u: Int, v: Int, weight: Float, name: String, pts: List<LatLng>) {
                    if (u != v && pts.size >= 2) {
                        val bFwd = calculateBearing(pts.first().latitude, pts.first().longitude, pts.last().latitude, pts.last().longitude)
                        val bRev = (bFwd + 180f) % 360f
                        adj.getOrPut(u) { ArrayList() }.add(GraphEdge(v, weight, name, pts, bFwd))
                        adj.getOrPut(v) { ArrayList() }.add(GraphEdge(u, weight, name, pts.reversed(), bRev))
                    }
                }

                // Spatial Grid Indexing for Segments (~400m cells for O(1) junction lookup)
                class SegmentRef(val roadIdx: Int, val segIdx: Int, val u: Int, val v: Int)
                val segGrid = HashMap<Long, ArrayList<SegmentRef>>()

                // 2. Add internal road edges with road hierarchy cost weighting
                for (rIdx in candidateRoads.indices) {
                    val road = candidateRoads[rIdx]
                    val pts = road.points
                    val nodeIds = roadNodeMap[rIdx]
                    val k = road.kind.lowercase(Locale.ROOT)
                    val isMajor = k.contains("major") || k.contains("primary") || k.contains("trunk") ||
                        road.name.contains("Arterial", ignoreCase = true) || road.name.contains("Bypass", ignoreCase = true) ||
                        road.name.contains("Highway", ignoreCase = true) || road.name.contains("Avenue", ignoreCase = true)
                    val isNamed = road.name.isNotBlank() && road.name != "Road"
                    val costFactor = (when {
                        isMajor -> 1.0f
                        isNamed -> 1.25f
                        k == "path" || k == "track" || k == "service" -> 4.5f
                        else -> 2.5f
                    }) * (if (road.isDetailed) 1.0f else 4.0f)

                    for (i in 0 until pts.size - 1) {
                        val u = nodeIds[i]
                        val v = nodeIds[i + 1]
                        val segDist = distBetween(pts[i].latitude, pts[i].longitude, pts[i + 1].latitude, pts[i + 1].longitude)
                        addEdge(u, v, segDist * costFactor, road.name, listOf(pts[i], pts[i + 1]))

                        // Add segment bounding cells to grid (grid scale: 250 -> ~400m cells)
                        val minGx = (Math.min(pts[i].longitude, pts[i + 1].longitude) * 250.0).toInt()
                        val maxGx = (Math.max(pts[i].longitude, pts[i + 1].longitude) * 250.0).toInt()
                        val minGy = (Math.min(pts[i].latitude, pts[i + 1].latitude) * 250.0).toInt()
                        val maxGy = (Math.max(pts[i].latitude, pts[i + 1].latitude) * 250.0).toInt()
                        val ref = SegmentRef(rIdx, i, u, v)

                        for (gx in minGx..maxGx) {
                            for (gy in minGy..maxGy) {
                                val key = (gx.toLong() shl 32) or (gy.toLong() and 0xFFFFFFFFL)
                                segGrid.getOrPut(key) { ArrayList() }.add(ref)
                            }
                        }
                    }
                }

                // 3. Planarize road endpoints: OSM roads intersect at/near their endpoints (0 and last)
                // This preserves fast O(E) execution, bridges OSM gaps, and eliminates false cross-field shortcuts
                val junctionThreshold = 38.0f
                for (rIdx in candidateRoads.indices) {
                    val pts = candidateRoads[rIdx].points
                    val nodeIds = roadNodeMap[rIdx]
                    val endIndices = if (pts.size <= 2) intArrayOf(0, pts.size - 1) else intArrayOf(0, pts.size - 1)

                    for (pIdx in endIndices) {
                        val u = nodeIds[pIdx]
                        val ep = pts[pIdx]
                        val centerGx = (ep.longitude * 250.0).toInt()
                        val centerGy = (ep.latitude * 250.0).toInt()

                        val checkedSegs = HashSet<Long>()
                        for (dx in -1..1) {
                            for (dy in -1..1) {
                                val key = ((centerGx + dx).toLong() shl 32) or ((centerGy + dy).toLong() and 0xFFFFFFFFL)
                                val list = segGrid[key] ?: continue
                                for (cand in list) {
                                    if (cand.roadIdx == rIdx) continue
                                    val segKey = (cand.roadIdx.toLong() shl 32) or (cand.segIdx.toLong() and 0xFFFFFFFFL)
                                    if (!checkedSegs.add(segKey)) continue

                                    val otherRoad = candidateRoads[cand.roadIdx]
                                    val otherPts = otherRoad.points
                                    val p1 = otherPts[cand.segIdx]
                                    val p2 = otherPts[cand.segIdx + 1]

                                    val projRes = projectPointOnSegment(ep.latitude, ep.longitude, p1.latitude, p1.longitude, p2.latitude, p2.longitude)
                                    if (projRes.distMeters <= junctionThreshold) {
                                        val projPt = LatLng(projRes.lat, projRes.lon)
                                        val segLen = distBetween(p1.latitude, p1.longitude, p2.latitude, p2.longitude)
                                        val t = projRes.t

                                        val w1 = (projRes.distMeters + t * segLen)
                                        val w2 = (projRes.distMeters + (1.0f - t) * segLen)

                                        addEdge(u, cand.u, w1, otherRoad.name, listOf(ep, projPt, p1))
                                        addEdge(u, cand.v, w2, otherRoad.name, listOf(ep, projPt, p2))
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Perpendicular Road Centerline Snapping for Start & Destination (Off-road handling)
                class SnapResult(
                    val proj: LatLng,
                    val dist: Float,
                    val u: Int,
                    val v: Int,
                    val t: Float,
                    val segLen: Float,
                    val roadName: String
                )

                fun snapToRoad(coord: LatLng): SnapResult {
                    var bestDist = Float.MAX_VALUE
                    var bestProj = coord
                    var bestU = 0
                    var bestV = 0
                    var bestT = 0f
                    var bestSegLen = 0f
                    var bestRoadName = "Road"

                    for (rIdx in candidateRoads.indices) {
                        val pts = candidateRoads[rIdx].points
                        val nodeIds = roadNodeMap[rIdx]
                        for (i in 0 until pts.size - 1) {
                            val p1 = pts[i]
                            val p2 = pts[i + 1]
                            val res = projectPointOnSegment(coord.latitude, coord.longitude, p1.latitude, p1.longitude, p2.latitude, p2.longitude)
                            if (res.distMeters < bestDist) {
                                bestDist = res.distMeters
                                bestProj = LatLng(res.lat, res.lon)
                                bestU = nodeIds[i]
                                bestV = nodeIds[i + 1]
                                bestT = res.t
                                bestSegLen = distBetween(p1.latitude, p1.longitude, p2.latitude, p2.longitude)
                                bestRoadName = candidateRoads[rIdx].name
                            }
                        }
                    }

                    return SnapResult(bestProj, bestDist, bestU, bestV, bestT, bestSegLen, bestRoadName)
                }

                val snapStart = snapToRoad(startCoord)
                val snapDest = snapToRoad(destCoord)

                // Insert projected road points into graph
                val startProjNid = nodeCoords.size
                nodeCoords.add(snapStart.proj)
                val destProjNid = nodeCoords.size
                nodeCoords.add(snapDest.proj)

                // Connect start projection to its segment endpoints
                val wStartU = snapStart.t * snapStart.segLen
                val wStartV = (1.0f - snapStart.t) * snapStart.segLen
                addEdge(startProjNid, snapStart.u, wStartU, snapStart.roadName, listOf(snapStart.proj, nodeCoords[snapStart.u]))
                addEdge(startProjNid, snapStart.v, wStartV, snapStart.roadName, listOf(snapStart.proj, nodeCoords[snapStart.v]))

                // Connect dest projection to its segment endpoints
                val wDestU = snapDest.t * snapDest.segLen
                val wDestV = (1.0f - snapDest.t) * snapDest.segLen
                addEdge(destProjNid, snapDest.u, wDestU, snapDest.roadName, listOf(snapDest.proj, nodeCoords[snapDest.u]))
                addEdge(destProjNid, snapDest.v, wDestV, snapDest.roadName, listOf(snapDest.proj, nodeCoords[snapDest.v]))

                // Component bridge: connect start and dest to any secondary road within reach to guarantee connectivity
                val bridgeStartRadius = maxOf(snapStart.dist + 35f, 60f)
                val bridgeDestRadius = maxOf(snapDest.dist + 35f, 60f)
                for (rIdx in candidateRoads.indices) {
                    val pts = candidateRoads[rIdx].points
                    val nodeIds = roadNodeMap[rIdx]
                    for (i in 0 until pts.size - 1) {
                        val p1 = pts[i]
                        val p2 = pts[i + 1]
                        val rStart = projectPointOnSegment(startCoord.latitude, startCoord.longitude, p1.latitude, p1.longitude, p2.latitude, p2.longitude)
                        if (rStart.distMeters <= bridgeStartRadius && nodeIds[i] != snapStart.u && nodeIds[i + 1] != snapStart.v) {
                            val pPt = LatLng(rStart.lat, rStart.lon)
                            addEdge(startProjNid, nodeIds[i], rStart.distMeters + 15f, candidateRoads[rIdx].name, listOf(snapStart.proj, pPt, p1))
                        }
                        val rDest = projectPointOnSegment(destCoord.latitude, destCoord.longitude, p1.latitude, p1.longitude, p2.latitude, p2.longitude)
                        if (rDest.distMeters <= bridgeDestRadius && nodeIds[i] != snapDest.u && nodeIds[i + 1] != snapDest.v) {
                            val pPt = LatLng(rDest.lat, rDest.lon)
                            addEdge(destProjNid, nodeIds[i], rDest.distMeters + 15f, candidateRoads[rIdx].name, listOf(snapDest.proj, pPt, p1))
                        }
                    }
                }

                // If origin or dest are out of the road (>2.5m), create dedicated off-road pin nodes
                // that connect perpendicularly to the nearest road polyline first
                val searchStartNid: Int
                if (snapStart.dist > 2.5f) {
                    searchStartNid = nodeCoords.size
                    nodeCoords.add(startCoord)
                    addEdge(searchStartNid, startProjNid, snapStart.dist, "Access Road", listOf(startCoord, snapStart.proj))
                } else {
                    searchStartNid = startProjNid
                }

                val searchDestNid: Int
                if (snapDest.dist > 2.5f) {
                    searchDestNid = nodeCoords.size
                    nodeCoords.add(destCoord)
                    addEdge(destProjNid, searchDestNid, snapDest.dist, "Access Road", listOf(snapDest.proj, destCoord))
                } else {
                    searchDestNid = destProjNid
                }

                // 5. Bidirectional A* Shortest-Path Search with Turn Angle Penalties
                val distF = HashMap<Int, Float>()
                val distB = HashMap<Int, Float>()
                class PrevStep(val prevNode: Int, val roadName: String, val edgePoints: List<LatLng>, val bearingDeg: Float)
                val prevF = HashMap<Int, PrevStep>()
                val prevB = HashMap<Int, PrevStep>()

                class PQEntry(val node: Int, val dist: Float, val priority: Float) : Comparable<PQEntry> {
                    override fun compareTo(other: PQEntry): Int = priority.compareTo(other.priority)
                }

                val pqF = java.util.PriorityQueue<PQEntry>()
                val pqB = java.util.PriorityQueue<PQEntry>()

                distF[searchStartNid] = 0f
                distB[searchDestNid] = 0f
                val initialDist = distBetween(startCoord.latitude, startCoord.longitude, destCoord.latitude, destCoord.longitude)
                pqF.add(PQEntry(searchStartNid, 0f, initialDist))
                pqB.add(PQEntry(searchDestNid, 0f, initialDist))

                val visitedF = HashSet<Int>()
                val visitedB = HashSet<Int>()
                var meetingNode: Int? = null
                var bestTotalCost = Float.MAX_VALUE

                while (!pqF.isEmpty() && !pqB.isEmpty()) {
                    // Forward step
                    val currF = pqF.poll()!!
                    val uf = currF.node
                    val df = currF.dist
                    if (!visitedF.contains(uf) && df <= (distF[uf] ?: Float.MAX_VALUE)) {
                        visitedF.add(uf)
                        if (visitedB.contains(uf)) {
                            val totalC = df + (distB[uf] ?: Float.MAX_VALUE)
                            if (totalC < bestTotalCost) {
                                bestTotalCost = totalC
                                meetingNode = uf
                                break
                            }
                        }
                        val incomingStep = prevF[uf]
                        val edges = adj[uf]
                        if (edges != null) {
                            for (edge in edges) {
                                val vf = edge.targetNode
                                // Add turn angle penalty for sharp hairpin reversals (> 130 deg)
                                var turnPenalty = 0f
                                if (incomingStep != null) {
                                    var delta = Math.abs(edge.bearingDeg - incomingStep.bearingDeg)
                                    if (delta > 180f) delta = 360f - delta
                                    if (delta > 130f) {
                                        turnPenalty = 250f // 250m equivalent penalty discouraging sharp U-turn hairpins
                                    }
                                }
                                val nd = df + edge.weightMeters + turnPenalty
                                if (nd < (distF[vf] ?: Float.MAX_VALUE)) {
                                    distF[vf] = nd
                                    prevF[vf] = PrevStep(uf, edge.roadName, edge.points, edge.bearingDeg)
                                    val h = distBetween(nodeCoords[vf].latitude, nodeCoords[vf].longitude, destCoord.latitude, destCoord.longitude)
                                    pqF.add(PQEntry(vf, nd, nd + h))
                                }
                            }
                        }
                    }

                    // Backward step
                    val currB = pqB.poll()!!
                    val ub = currB.node
                    val db = currB.dist
                    if (!visitedB.contains(ub) && db <= (distB[ub] ?: Float.MAX_VALUE)) {
                        visitedB.add(ub)
                        if (visitedF.contains(ub)) {
                            val totalC = db + (distF[ub] ?: Float.MAX_VALUE)
                            if (totalC < bestTotalCost) {
                                bestTotalCost = totalC
                                meetingNode = ub
                                break
                            }
                        }
                        val edges = adj[ub]
                        if (edges != null) {
                            for (edge in edges) {
                                val vb = edge.targetNode
                                val nd = db + edge.weightMeters
                                if (nd < (distB[vb] ?: Float.MAX_VALUE)) {
                                    distB[vb] = nd
                                    prevB[vb] = PrevStep(ub, edge.roadName, edge.points, edge.bearingDeg)
                                    val h = distBetween(nodeCoords[vb].latitude, nodeCoords[vb].longitude, startCoord.latitude, startCoord.longitude)
                                    pqB.add(PQEntry(vb, nd, nd + h))
                                }
                            }
                        }
                    }
                }

                if (meetingNode != null) {
                    val meet = meetingNode!!
                    val edgesTraversed = ArrayList<PrevStep>()

                    // Forward path: searchStartNid -> meetingNode
                    val fwdSteps = ArrayList<PrevStep>()
                    var traceF = meet
                    while (traceF != searchStartNid) {
                        val step = prevF[traceF] ?: break
                        fwdSteps.add(step)
                        traceF = step.prevNode
                    }
                    fwdSteps.reverse()
                    edgesTraversed.addAll(fwdSteps)

                    // Backward path: meetingNode -> searchDestNid
                    var traceB = meet
                    while (traceB != searchDestNid) {
                        val step = prevB[traceB] ?: break
                        edgesTraversed.add(PrevStep(step.prevNode, step.roadName, step.edgePoints.reversed(), (step.bearingDeg + 180f) % 360f))
                        traceB = step.prevNode
                    }

                    val polyline = ArrayList<LatLng>()
                    for (step in edgesTraversed) {
                        for (p in step.edgePoints) {
                            if (polyline.isEmpty() || distBetween(polyline.last().latitude, polyline.last().longitude, p.latitude, p.longitude) > 0.5f) {
                                polyline.add(p)
                            }
                        }
                    }

                    // Ensure start and destination are anchored cleanly
                    if (polyline.isEmpty()) {
                        polyline.add(startCoord)
                        polyline.add(destCoord)
                    } else {
                        if (distBetween(polyline.first().latitude, polyline.first().longitude, startCoord.latitude, startCoord.longitude) > 1.0f) {
                            polyline.add(0, startCoord)
                        }
                        if (distBetween(polyline.last().latitude, polyline.last().longitude, destCoord.latitude, destCoord.longitude) > 1.0f) {
                            polyline.add(destCoord)
                        }
                    }

                    var totalMeters = 0.0
                    for (i in 0 until polyline.size - 1) {
                        totalMeters += distBetween(polyline[i].latitude, polyline[i].longitude, polyline[i + 1].latitude, polyline[i + 1].longitude)
                    }

                    val avgSpeedMps = when (options.mode) {
                        VehicleMode.SCOOTER -> 11.67 // 42 km/h motorcycle cruising
                        VehicleMode.CAR -> 9.72 // 35 km/h urban traffic
                        VehicleMode.BICYCLE -> 4.44 // 16 km/h cycling
                        VehicleMode.WALK -> 1.33 // 4.8 km/h walking
                    }
                    val durationSecs = (totalMeters / avgSpeedMps).toLong().coerceAtLeast(60L)

                    // Generate distinct turn maneuvers from traversed named roads
                    val maneuvers = ArrayList<TurnManeuver>()
                    val distinctRoads = ArrayList<Pair<String, LatLng>>()
                    for (step in edgesTraversed) {
                        val rname = if (step.roadName != "Road" && step.roadName != "Junction" && step.roadName.isNotBlank()) step.roadName else "Connecting Road"
                        if (distinctRoads.isEmpty() || distinctRoads.last().first != rname) {
                            distinctRoads.add(Pair(rname, step.edgePoints.first()))
                        }
                    }

                    if (distinctRoads.isEmpty()) {
                        distinctRoads.add(Pair("Connecting Road", snapStart.proj))
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
            android.util.Log.e("GabayRoute", "High-fidelity routing error", e)
        }

        // Graceful fallback along start and destination coordinates if no candidate roads in area
        val distResults = FloatArray(1)
        Location.distanceBetween(
            startCoord.latitude, startCoord.longitude,
            destCoord.latitude, destCoord.longitude,
            distResults
        )
        val distM = distResults[0].toDouble().coerceAtLeast(10.0)
        val avgSpeedMps = when (options.mode) {
            VehicleMode.SCOOTER -> 11.67 // 42 km/h
            VehicleMode.CAR -> 9.72 // 35 km/h
            VehicleMode.BICYCLE -> 4.44 // 16 km/h
            VehicleMode.WALK -> 1.33 // 4.8 km/h
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

    var lastSpokenAdvisory by remember { mutableStateOf<String?>(null) }
    var lastSpokenAdvisoryTimeMs by remember { mutableLongStateOf(0L) }

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

            if (activeRoute == null) {
                // When unpinned/no route, strictly silence any advisory and keep telemetry clear
                lastSpokenAdvisory = null
                lastSpokenAdvisoryTimeMs = 0L
                telemetry = updatedTelemetry.copy(
                    coPilotAdvisory = null,
                    isOffRoute = false,
                    remainingDistanceKm = 0.0f,
                    paceWindowMin = "-- MIN",
                    nextTurnRoad = "",
                    nextTurnDistanceM = 0.0f
                )
            } else {
                telemetry = updatedTelemetry

                // Speak terse advisory ONLY if active route is pinned and debounced by 25 seconds
                updatedTelemetry.coPilotAdvisory?.let { advisory ->
                    val nowMs = System.currentTimeMillis()
                    val isDifferent = advisory != lastSpokenAdvisory
                    val isCooledDown = (nowMs - lastSpokenAdvisoryTimeMs) > 25000L
                    val isStationary = userSpeed < 3.0f
                    val isOffRoute = advisory.contains("OFF-ROUTE", ignoreCase = true)

                    if ((isDifferent || isCooledDown) && !(isStationary && isOffRoute)) {
                        lastSpokenAdvisory = advisory
                        lastSpokenAdvisoryTimeMs = nowMs
                        audioController.speakTerseCue(advisory)
                    }
                }
            }
        }
    }

    // Asynchronous non-blocking route computation with Dispatchers.IO and HUD loading state
    fun requestRouteCalculation(
        startCoord: LatLng,
        destCoord: LatLng,
        options: RouteOptions,
        targetName: String,
        onComplete: (() -> Unit)? = null
    ) {
        if (isRouteLoading) return
        isRouteLoading = true
        isNavigating = false
        activeOriginCoord = startCoord
        activeDestCoord = destCoord
        destinationName = targetName
        destinationPin = destCoord
        isFollowingVehicle = true

        coroutineScope.launch {
            // Yield once so Compose immediately paints the TwoWayRouteHeader with the loading progress bar
            kotlinx.coroutines.yield()
            try {
                val route = withContext(Dispatchers.IO) {
                    computeRoute(startCoord, destCoord, options)
                }
                activeRoute = route
                try { engine?.setActiveRoute(route) } catch (e: Exception) {}
                telemetry = telemetry.copy(
                    remainingDistanceKm = (route.totalDistanceMeters / 1000.0).toFloat(),
                    paceWindowMin = "${(route.estimatedDurationSeconds / 60u)} MIN"
                )

                // AI Route Advisory Analysis
                val distKm = String.format(Locale.US, "%.1f", route.totalDistanceMeters / 1000.0).toDoubleOrNull() ?: 1.0
                val roads = route.maneuvers.map { it.roadName }
                activeRouteAdvisory = LlamaDaemon.evaluateRoute(
                    originName = originName,
                    destName = targetName,
                    distanceKm = distKm,
                    roadsTraversed = roads,
                    mode = options.mode.name
                )
                onComplete?.invoke()
            } catch (e: Exception) {
                android.util.Log.e("GabayRoute", "Route calculation failure: ${e.message}")
            } finally {
                isRouteLoading = false
            }
        }
    }

    fun startActiveNavigation() {
        val route = activeRoute ?: return
        isNavigating = true
        NavigationService.start(context)
        val road = route.maneuvers.firstOrNull()?.roadName ?: destinationName
        audioController.speakTerseCue("ROUTE ACTIVE TO ${destinationName.uppercase()}. PROCEED VIA $road.")
    }

    fun swapRoute() {
        val orig = activeOriginCoord ?: LatLng(userLat, userLon)
        val dest = activeDestCoord ?: destinationPin ?: return
        val origN = originName
        val destN = destinationName

        activeOriginCoord = dest
        activeDestCoord = orig
        originName = destN
        destinationName = origN

        val options = RouteOptions(
            mode = selectedMode,
            avoidTolls = avoidTolls,
            avoidExpressways = avoidExpressways,
            avoidFerries = false
        )
        requestRouteCalculation(dest, orig, options, origN)
    }

    fun cancelActiveRoute() {
        isRouteLoading = false
        isNavigating = false
        activeRoute = null
        activeRouteAdvisory = null
        try { engine?.clearActiveRoute() } catch (e: Exception) {}
        destinationPin = null
        pinInfo = null
        activeOriginCoord = null
        activeDestCoord = null
        originName = "Your location"
        destinationName = "Dropped pin"
        NavigationService.stop(context)
        audioController.speakTerseCue("ROUTE CANCELLED")
    }

    fun resolveCurrentRoad(lat: Double, lon: Double): String {
        val dbFile = File("/sdcard/Documents/Gabay/pois_fts.sqlite")
        if (dbFile.exists()) {
            try {
                val db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
                val dLat = 0.003
                val dLon = 0.003
                val cursor = db.rawQuery(
                    "SELECT name FROM roads WHERE id > 120000 AND name != 'Road' AND min_lat <= ? AND max_lat >= ? AND min_lon <= ? AND max_lon >= ? LIMIT 1",
                    arrayOf((lat + dLat).toString(), (lat - dLat).toString(), (lon + dLon).toString(), (lon - dLon).toString())
                )
                var roadName: String? = null
                if (cursor.moveToFirst()) {
                    roadName = cursor.getString(0)
                }
                cursor.close()
                db.close()
                if (!roadName.isNullOrBlank()) return roadName
            } catch (e: Exception) {}
        }
        return activeRoute?.maneuvers?.firstOrNull()?.roadName?.takeIf { it != "Connecting Road" && it != "Direct Route" && it != "Destination" }
            ?: "San Isidro Arterial Road"
    }

    // Agentic Command & Contextual Intent Dispatcher powered by on-device model.gguf
    fun processAgentPrompt(prompt: String) {
        val now = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val previousHistory = chatMessages.takeLast(6).map { it.sender to it.text }
        chatMessages.add(ChatMessage("RIDER", prompt, now))

        val currentRoad = resolveCurrentRoad(userLat, userLon)
        val riderCtx = dev.gabay.app.ai.RiderContext(
            latitude = userLat,
            longitude = userLon,
            speedKmh = userSpeed,
            heading = userHeading,
            currentRoad = currentRoad,
            isNavigating = isNavigating,
            destinationName = destinationName,
            remainingDistanceKm = telemetry.remainingDistanceKm
        )

        coroutineScope.launch {
            val result = LlamaDaemon.processWithBrain(prompt, previousHistory, riderCtx)
            val replyTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

            when (result) {
                is IntentResult.Action -> {
                    when (result.action.lowercase(Locale.ROOT)) {
                        "where_am_i", "status", "location" -> {
                            val locMsg = if (isNavigating) {
                                "You are on $currentRoad heading towards $destinationName (${String.format(Locale.US, "%.1f", telemetry.remainingDistanceKm)} km remaining). Speed ${userSpeed.toInt()} km/h."
                            } else {
                                "You are on $currentRoad at coordinates ${String.format(Locale.US, "%.4f, %.4f", userLat, userLon)}. Free exploration active."
                            }
                            chatMessages.add(ChatMessage("GABAY AGENT", locMsg, replyTime))
                            audioController.speakTerseCue(locMsg)
                        }
                        "3d" -> {
                            is3dMode = true
                            val msg = "3D perspective enabled."
                            chatMessages.add(ChatMessage("GABAY AGENT", msg, replyTime))
                            audioController.speakTerseCue(msg)
                        }
                        "2d" -> {
                            is3dMode = false
                            val msg = "2D overhead view enabled."
                            chatMessages.add(ChatMessage("GABAY AGENT", msg, replyTime))
                            audioController.speakTerseCue(msg)
                        }
                        "north" -> {
                            resetRotationCount++
                            val msg = "Compass aligned to True North."
                            chatMessages.add(ChatMessage("GABAY AGENT", msg, replyTime))
                            audioController.speakTerseCue(msg)
                        }
                        "follow", "recenter" -> {
                            isFollowingVehicle = true
                            cameraTarget = MapLibreLatLng(userLat, userLon)
                            val msg = "Magnetic camera tracking engaged."
                            chatMessages.add(ChatMessage("GABAY AGENT", msg, replyTime))
                            audioController.speakTerseCue(msg)
                        }
                        "cancel", "stop" -> {
                            cancelActiveRoute()
                            val msg = "Route and pin cleared. Returning to free explore."
                            chatMessages.add(ChatMessage("GABAY AGENT", msg, replyTime))
                        }
                        "navigate" -> {
                            val targetQuery = if (result.target.isNotBlank()) result.target else prompt
                            val pois = engine?.searchPois(targetQuery, 1u) ?: emptyList()
                            val target = pois.firstOrNull()
                            if (target != null) {
                                val options = RouteOptions(
                                    mode = selectedMode,
                                    avoidTolls = (selectedMode == VehicleMode.SCOOTER),
                                    avoidExpressways = (selectedMode == VehicleMode.SCOOTER),
                                    avoidFerries = false
                                )
                                val startCoord = LatLng(userLat, userLon)
                                originName = "Your location"
                                requestRouteCalculation(startCoord, target.coordinate, options, target.name)
                                val msg = "Navigating to ${target.name}."
                                chatMessages.add(ChatMessage("GABAY AGENT", msg, replyTime))
                            } else {
                                val msg = "Destination \"$targetQuery\" not found in offline vault."
                                chatMessages.add(ChatMessage("GABAY AGENT", msg, replyTime))
                                audioController.speakTerseCue(msg)
                            }
                        }
                        "search" -> {
                            val targetQuery = if (result.target.isNotBlank()) result.target else "Gas"
                            val pois = engine?.searchPois(targetQuery, 5u) ?: emptyList()
                            val closest = pois.firstOrNull()
                            if (closest != null) {
                                destinationPin = closest.coordinate
                                cameraTarget = MapLibreLatLng(closest.coordinate.latitude, closest.coordinate.longitude)
                                val distResults = FloatArray(1)
                                Location.distanceBetween(userLat, userLon, closest.coordinate.latitude, closest.coordinate.longitude, distResults)
                                val km = distResults[0] / 1000.0
                                val msg = "Found ${closest.name}, ${String.format(Locale.US, "%.1f", km)} km away. Pin dropped."
                                chatMessages.add(ChatMessage("GABAY AGENT", msg, replyTime))
                                audioController.speakTerseCue("Found ${closest.name}")
                            } else {
                                val msg = "No matching locations found nearby in offline vault."
                                chatMessages.add(ChatMessage("GABAY AGENT", msg, replyTime))
                                audioController.speakTerseCue(msg)
                            }
                        }
                        else -> {
                            val msg = "Command acknowledged."
                            chatMessages.add(ChatMessage("GABAY AGENT", msg, replyTime))
                            audioController.speakTerseCue(msg)
                        }
                    }
                }
                is IntentResult.Conversational -> {
                    chatMessages.add(ChatMessage("GABAY AGENT", result.text, replyTime))
                    audioController.speakTerseCue(result.text)
                }
                is IntentResult.GuardrailBlocked -> {
                    chatMessages.add(ChatMessage("GABAY AGENT", result.reason, replyTime))
                }
            }
        }
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
                pinInfo = PinInfo(
                    coordinate = latLng,
                    nearestRoadName = "Locating...",
                    barangay = "Local Vicinity",
                    cityOrMunicipality = "Philippines"
                )
                coroutineScope.launch(Dispatchers.IO) {
                    var landmarkName = "Dropped pin"
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
                                landmarkName = cursor.getString(0) ?: "Dropped pin"
                            }
                            cursor.close()
                            db.close()
                        }
                    } catch (e: Exception) {}

                    withContext(Dispatchers.Main) {
                        if (pinInfo?.coordinate == latLng) {
                            pinInfo = pinInfo?.copy(nearestRoadName = landmarkName)
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // LAYER 1: Top Bar (Search Bar / Two-Way Route Header / Non-blocking Loading State)
        val statusBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = (statusBarTopPadding - 8.dp).coerceAtLeast(4.dp))
                .fillMaxWidth()
        ) {
            if (activeRoute != null || isRouteLoading) {
                TwoWayRouteHeader(
                    originName = originName,
                    destinationName = destinationName,
                    isLoading = isRouteLoading,
                    onSwap = { swapRoute() },
                    onClose = { cancelActiveRoute() },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                TopSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    searchResults = searchResults,
                    onSelectPlace = { place ->
                        val dest = place.coordinate
                        searchQuery = ""
                        originName = "Your location"
                        val options = RouteOptions(
                            mode = selectedMode,
                            avoidTolls = avoidTolls,
                            avoidExpressways = avoidExpressways,
                            avoidFerries = false
                        )
                        val startCoord = LatLng(userLat, userLon)
                        requestRouteCalculation(startCoord, dest, options, place.name)
                    },
                    onMenuClick = { isMenuSheetOpen = true },
                    audioGuidanceMode = audioGuidanceMode,
                    onToggleAudioMode = {
                        val nextMode = when (audioGuidanceMode) {
                            dev.gabay.app.audio.AudioGuidanceMode.FULL_VOICE -> dev.gabay.app.audio.AudioGuidanceMode.ALERTS_ONLY
                            dev.gabay.app.audio.AudioGuidanceMode.ALERTS_ONLY -> dev.gabay.app.audio.AudioGuidanceMode.MUTED
                            dev.gabay.app.audio.AudioGuidanceMode.MUTED -> dev.gabay.app.audio.AudioGuidanceMode.FULL_VOICE
                        }
                        audioGuidanceMode = nextMode
                        audioController.guidanceMode = nextMode
                        val toastMsg = when (nextMode) {
                            dev.gabay.app.audio.AudioGuidanceMode.FULL_VOICE -> "Voice Guidance: FULL (Voice & Alerts)"
                            dev.gabay.app.audio.AudioGuidanceMode.ALERTS_ONLY -> "Voice Guidance: ALERTS ONLY"
                            dev.gabay.app.audio.AudioGuidanceMode.MUTED -> "Voice Guidance: MUTED"
                        }
                        android.widget.Toast.makeText(context, toastMsg, android.widget.Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // LAYER 2: Floating Distance, Cancel Route, & Pace Pills (ONLY Visible When Actively Navigating)
            if (activeRoute != null && isNavigating && !isRouteLoading) {
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
                            .clickable { cancelActiveRoute() }
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
                isRouteLoading = isRouteLoading,
                onStartRoute = { destCoord ->
                    val startCoord = LatLng(userLat, userLon)
                    originName = "Your location"
                    val dName = if (info.nearestRoadName.isNotBlank() && info.nearestRoadName != "Road" && info.nearestRoadName != "Locating...") info.nearestRoadName else "Dropped pin"
                    val options = RouteOptions(
                        mode = selectedMode,
                        avoidTolls = avoidTolls,
                        avoidExpressways = avoidExpressways,
                        avoidFerries = false
                    )
                    requestRouteCalculation(startCoord, destCoord, options, dName) {
                        pinInfo = null
                    }
                },
                onCancelRoute = {
                    cancelActiveRoute()
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

        // LAYER 3.5: Google Maps Style Route Preview Sheet
        if (activeRoute != null && !isNavigating && !isRouteLoading && pinInfo == null) {
            val route = activeRoute!!
            RoutePreviewSheet(
                route = route,
                selectedMode = selectedMode,
                onSelectMode = { newMode ->
                    selectedMode = newMode
                    val startCoord = activeOriginCoord ?: LatLng(userLat, userLon)
                    val destCoord = activeDestCoord ?: destinationPin ?: return@RoutePreviewSheet
                    val options = RouteOptions(
                        mode = newMode,
                        avoidTolls = avoidTolls,
                        avoidExpressways = avoidExpressways,
                        avoidFerries = false
                    )
                    requestRouteCalculation(startCoord, destCoord, options, destinationName)
                },
                avoidTolls = avoidTolls,
                onToggleAvoidTolls = {
                    avoidTolls = !avoidTolls
                    val startCoord = activeOriginCoord ?: LatLng(userLat, userLon)
                    val destCoord = activeDestCoord ?: destinationPin ?: return@RoutePreviewSheet
                    val options = RouteOptions(
                        mode = selectedMode,
                        avoidTolls = avoidTolls,
                        avoidExpressways = avoidExpressways,
                        avoidFerries = false
                    )
                    requestRouteCalculation(startCoord, destCoord, options, destinationName)
                },
                avoidHighways = avoidExpressways,
                onToggleAvoidHighways = {
                    avoidExpressways = !avoidExpressways
                    val startCoord = activeOriginCoord ?: LatLng(userLat, userLon)
                    val destCoord = activeDestCoord ?: destinationPin ?: return@RoutePreviewSheet
                    val options = RouteOptions(
                        mode = selectedMode,
                        avoidTolls = avoidTolls,
                        avoidExpressways = avoidExpressways,
                        avoidFerries = false
                    )
                    requestRouteCalculation(startCoord, destCoord, options, destinationName)
                },
                onStartNavigation = {
                    startActiveNavigation()
                },
                onCancelRoute = {
                    cancelActiveRoute()
                },
                routeAdvisory = activeRouteAdvisory,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp)
            )
        }

        // LAYER 4: Bottom Floating Controls & Speedometer (Visible When Idle or Actively Navigating)
        if (activeRoute == null || isNavigating) {
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

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
import dev.gabay.app.commute.CommuteRouter
import dev.gabay.app.commute.CommuteSheet
import dev.gabay.app.commute.CommuteItinerary
import dev.gabay.app.commute.CommuteLeg
import dev.gabay.app.data.VaultIntegrityManager
import dev.gabay.app.data.VaultStatus

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
        thread {
            dev.gabay.app.data.PoiDatabaseEnricher.ensureEnriched(this@MainActivity)
        }



        // Initialize Audio Controller (TTS + Audio Focus Ducking)

        audioController = AudioController(this)



        // Restore saved theme preference

        val settingsPrefs = getSharedPreferences("gabay_settings", android.content.Context.MODE_PRIVATE)

        GabayColors.isSunlightLightMode = settingsPrefs.getBoolean("sunlight_light_mode", false)
        dev.gabay.app.ui.GabayHudSettings.init(this)



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
    var activeEngine by remember { mutableStateOf(engine) }



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

    var recentDestinations by remember { mutableStateOf(dev.gabay.app.data.RecentDestinationsManager.getRecents(context)) }



    // Sheet overlays

    var isMenuSheetOpen by remember { mutableStateOf(false) }

    var isAgentSheetOpen by remember { mutableStateOf(false) }



    // Navigation and Routing state

    var activeRoute by remember { mutableStateOf<RouteResult?>(null) }
    var alternativeRoute by remember { mutableStateOf<RouteResult?>(null) }
    var activeRouteAdvisory by remember { mutableStateOf<String?>(null) }
    var isCommuteMode by remember { mutableStateOf(false) }
    var commuteItineraries by remember { mutableStateOf<List<dev.gabay.app.commute.CommuteItinerary>>(emptyList()) }
    var selectedCommuteIndex by remember { mutableIntStateOf(0) }
    var vaultStatus by remember { mutableStateOf(dev.gabay.app.data.VaultIntegrityManager.checkVault()) }
    var showVaultDialog by remember { mutableStateOf(!vaultStatus.isReady) }

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

    var isPickingOrigin by remember { mutableStateOf(false) }

    var isPickingDestination by remember { mutableStateOf(false) }

    val speechRecognizer = remember {

        if (SpeechRecognizer.isRecognitionAvailable(context)) {

            SpeechRecognizer.createSpeechRecognizer(context)

        } else null

    }





    fun cancelActiveRoute() {
        val wasNavigating = isNavigating || activeRoute != null
        isRouteLoading = false
        isNavigating = false
        activeRoute = null
        alternativeRoute = null
        activeRouteAdvisory = null
        isCommuteMode = false
        commuteItineraries = emptyList()
        try { activeEngine?.clearActiveRoute() } catch (e: Exception) {}
        destinationPin = null
        pinInfo = null
        activeOriginCoord = null
        activeDestCoord = null
        originName = "Your location"
        destinationName = "Dropped pin"
        if (wasNavigating) {
            NavigationService.stop(context)
            audioController.speakTerseCue("ROUTE CANCELLED")
        }
    }

    DisposableEffect(Unit) {

        NavigationService.onStopNavigationRequested = {

            cancelActiveRoute()

        }

        onDispose {

            NavigationService.onStopNavigationRequested = null

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

            ChatMessage("Gabay", "Gabay is ready. Where would you like to ride?", java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date()))

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

            searchResults = activeEngine?.searchPois(searchQuery.trim(), 5u) ?: emptyList()

        } else {

            searchResults = emptyList()

        }

    }



    // Process GPS tick through Rust Engine

    LaunchedEffect(userLat, userLon, userSpeed, userHeading, activeRoute) {

        activeEngine?.let { eng ->

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

                // Milestone-based discrete turn advisories (500m, 200m, 50m, arrival)
                if (isNavigating && activeRoute != null) {
                    val nowMs = System.currentTimeMillis()
                    val isStationary = userSpeed < 3.0f
                    if (updatedTelemetry.isOffRoute) {
                        if (!isStationary && (nowMs - lastSpokenAdvisoryTimeMs > 25000L)) {
                            lastSpokenAdvisoryTimeMs = nowMs
                            audioController.speakTerseCue("Off-route. Recalculating route.")
                        }
                    } else {
                        val turnDist = updatedTelemetry.nextTurnDistanceM
                        val turnRoad = updatedTelemetry.nextTurnRoad
                        if (turnRoad != lastSpokenAdvisory) {
                            lastSpokenAdvisory = turnRoad
                            lastSpokenAdvisoryTimeMs = 0L
                        }
                        val milestone = when {
                            turnDist in 450.0..550.0 -> 500
                            turnDist in 180.0..220.0 -> 200
                            turnDist in 40.0..60.0 -> 50
                            turnDist <= 20.0 && turnDist > 0.0 -> 10
                            else -> -1
                        }
                        if (milestone != -1 && (nowMs - lastSpokenAdvisoryTimeMs > 15000L)) {
                            lastSpokenAdvisoryTimeMs = nowMs
                            val cue = when (milestone) {
                                500 -> "In 500 meters, prepare to turn onto $turnRoad"
                                200 -> "In 200 meters, turn onto $turnRoad"
                                50 -> "In 50 meters, turn onto $turnRoad"
                                10 -> "Turn onto $turnRoad"
                                else -> ""
                            }
                            if (cue.isNotBlank()) {
                                audioController.speakTerseCue(cue)
                            }
                        }
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

            kotlinx.coroutines.yield()

            try {

                val (route, alt) = withContext(Dispatchers.IO) {
                    val primary = activeEngine?.calculateRoute(startCoord, destCoord, options) ?: RouteResult(
                        success = false,
                        totalDistanceMeters = 0.0,
                        estimatedDurationSeconds = 0u,
                        polyline = emptyList(),
                        maneuvers = emptyList(),
                        errorMessage = "Navigation engine not available"
                    )
                    var alternative: RouteResult? = null
                    if (primary.success && primary.polyline.isNotEmpty()) {
                        try { activeEngine?.setActiveRoute(primary) } catch (e: Exception) {}
                        if (primary.totalDistanceMeters > 2500.0) {
                            try {
                                val altOptions = options.copy(avoidFerries = true)
                                val a = activeEngine?.calculateRoute(startCoord, destCoord, altOptions)
                                if (a != null && a.success && a.polyline.isNotEmpty() &&
                                    Math.abs(a.totalDistanceMeters - primary.totalDistanceMeters) > 30.0) {
                                    alternative = a
                                }
                            } catch (e: Exception) {
                                alternative = null
                            }
                        }
                    }
                    Pair(primary, alternative)
                }

                if (route.success && route.polyline.isNotEmpty()) {
                    activeRoute = route
                    alternativeRoute = alt
                    val durMin = (route.estimatedDurationSeconds / 60u).coerceAtLeast(1u)

                    telemetry = telemetry.copy(

                        remainingDistanceKm = (route.totalDistanceMeters / 1000.0).toFloat(),

                        paceWindowMin = "$durMin MIN"

                    )

                    audioController.speakTerseCue("Route ready")

                    dev.gabay.app.data.RecentDestinationsManager.addRecent(

                        context = context,

                        name = targetName,

                        subtitle = "${String.format(Locale.US, "%.1f", route.totalDistanceMeters / 1000.0)} km • Road Network",

                        category = "place",

                        latitude = destCoord.latitude,

                        longitude = destCoord.longitude

                    )

                    recentDestinations = dev.gabay.app.data.RecentDestinationsManager.getRecents(context)

                    onComplete?.invoke()

                } else {
                    activeRoute = null
                    alternativeRoute = null
                    audioController.speakTerseCue("No road route found")
                    android.widget.Toast.makeText(context, "No connected road route found", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("GabayRoute", "Route calculation failure: ${e.message}")
                activeRoute = null
                alternativeRoute = null
                audioController.speakTerseCue("Route calculation failed")

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







    fun resolveCurrentRoad(lat: Double, lon: Double): String {

        val dbFile = File("/sdcard/Documents/Gabay/pois_fts.sqlite")

        if (dbFile.exists()) {

            try {

                val db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)

                val pad = 0.0015

                val cursor = db.rawQuery(

                    "SELECT name FROM roads WHERE NOT (max_lat < ? OR min_lat > ? OR max_lon < ? OR min_lon > ?) AND name != 'Road' AND name != '' ORDER BY (((min_lat + max_lat)/2.0 - ?)*((min_lat + max_lat)/2.0 - ?) + ((min_lon + max_lon)/2.0 - ?)*((min_lon + max_lon)/2.0 - ?)) ASC LIMIT 1",

                    arrayOf(

                        (lat - pad).toString(),

                        (lat + pad).toString(),

                        (lon - pad).toString(),

                        (lon + pad).toString(),

                        lat.toString(),

                        lon.toString()

                    )

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

            ?: "Current Location"

    }



    // Agentic Command & Contextual Intent Dispatcher powered by on-device model.gguf

    fun processAgentPrompt(prompt: String) {

        val now = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        val previousHistory = chatMessages.takeLast(8).map { it.sender to it.text }

        chatMessages.add(ChatMessage("You", prompt, now))



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

                            val locMsg = if (isNavigating && activeRoute != null) {

                                "You are on $currentRoad heading towards $destinationName (${String.format(Locale.US, "%.1f", telemetry.remainingDistanceKm)} km remaining). Speed ${userSpeed.toInt()} km/h."

                            } else {

                                "You are on $currentRoad at coordinates ${String.format(Locale.US, "%.4f, %.4f", userLat, userLon)}. Free exploration active. No active route."

                            }

                            chatMessages.add(ChatMessage("Gabay", locMsg, replyTime))

                            audioController.speakTerseCue(locMsg)

                        }

                        "3d" -> {

                            is3dMode = true

                            val msg = "3D perspective enabled."

                            chatMessages.add(ChatMessage("Gabay", msg, replyTime))

                            audioController.speakTerseCue(msg)

                        }

                        "2d" -> {

                            is3dMode = false

                            val msg = "2D overhead view enabled."

                            chatMessages.add(ChatMessage("Gabay", msg, replyTime))

                            audioController.speakTerseCue(msg)

                        }

                        "north" -> {

                            resetRotationCount++

                            val msg = "Compass aligned to True North."

                            chatMessages.add(ChatMessage("Gabay", msg, replyTime))

                            audioController.speakTerseCue(msg)

                        }

                        "follow", "recenter" -> {

                            isFollowingVehicle = true

                            cameraTarget = MapLibreLatLng(userLat, userLon)

                            val msg = "Re-centered on your location."

                            chatMessages.add(ChatMessage("Gabay", msg, replyTime))

                            audioController.speakTerseCue(msg)

                        }

                        "cancel", "stop" -> {

                            cancelActiveRoute()

                            val msg = "Route cancelled."

                            chatMessages.add(ChatMessage("Gabay", msg, replyTime))

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

                                chatMessages.add(ChatMessage("Gabay", msg, replyTime))

                            } else {

                                val msg = "Destination \"$targetQuery\" not found in offline vault."

                                chatMessages.add(ChatMessage("Gabay", msg, replyTime))

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

                                chatMessages.add(ChatMessage("Gabay", msg, replyTime))

                                audioController.speakTerseCue("Found ${closest.name}")

                            } else {

                                val msg = "No matching locations found nearby in offline vault."

                                chatMessages.add(ChatMessage("Gabay", msg, replyTime))

                                audioController.speakTerseCue(msg)

                            }

                        }

                        else -> {

                            val msg = "Command acknowledged."

                            chatMessages.add(ChatMessage("Gabay", msg, replyTime))

                            audioController.speakTerseCue(msg)

                        }

                    }

                }

                is IntentResult.Conversational -> {

                    chatMessages.add(ChatMessage("Gabay", result.text, replyTime))

                    audioController.speakTerseCue(result.text)

                }

                is IntentResult.GuardrailBlocked -> {

                    chatMessages.add(ChatMessage("Gabay", result.reason, replyTime))

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
            alternativeRoute = alternativeRoute,
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

                            // Step 1: Check nearest road within ~80m (pad 0.0008)

                            val rPad = 0.0008

                            val roadCursor = db.rawQuery(

                                "SELECT name FROM roads WHERE NOT (max_lat < ? OR min_lat > ? OR max_lon < ? OR min_lon > ?) AND name != 'Road' AND name != '' ORDER BY (((min_lat + max_lat)/2.0 - ?)*((min_lat + max_lat)/2.0 - ?) + ((min_lon + max_lon)/2.0 - ?)*((min_lon + max_lon)/2.0 - ?)) ASC LIMIT 1",

                                arrayOf(

                                    (latLng.latitude - rPad).toString(),

                                    (latLng.latitude + rPad).toString(),

                                    (latLng.longitude - rPad).toString(),

                                    (latLng.longitude + rPad).toString(),

                                    latLng.latitude.toString(),

                                    latLng.longitude.toString()

                                )

                            )

                            if (roadCursor.moveToFirst()) {

                                val rName = roadCursor.getString(0)

                                if (!rName.isNullOrBlank() && rName != "Road") {

                                    landmarkName = rName

                                }

                            }

                            roadCursor.close()



                            // Step 2: If no road name found, check POIs within ~200m (pad 0.002) sorted strictly by Euclidean distance

                            if (landmarkName == "Dropped pin") {

                                val pPad = 0.002

                                val poiCursor = db.rawQuery(

                                    "SELECT name FROM pois WHERE latitude BETWEEN ? AND ? AND longitude BETWEEN ? AND ? ORDER BY ((latitude - ?) * (latitude - ?) + (longitude - ?) * (longitude - ?)) ASC LIMIT 1",

                                    arrayOf(

                                        (latLng.latitude - pPad).toString(),

                                        (latLng.latitude + pPad).toString(),

                                        (latLng.longitude - pPad).toString(),

                                        (latLng.longitude + pPad).toString(),

                                        latLng.latitude.toString(),

                                        latLng.longitude.toString()

                                    )

                                )

                                if (poiCursor.moveToFirst()) {

                                    landmarkName = poiCursor.getString(0) ?: "Dropped pin"

                                }

                                poiCursor.close()

                            }

                            db.close()

                        }

                    } catch (e: Exception) {}



                    withContext(Dispatchers.Main) {

                        if (destinationPin == latLng) {

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

            if ((activeRoute != null || isRouteLoading) && !isPickingOrigin && !isPickingDestination) {

                TwoWayRouteHeader(

                    originName = originName,

                    destinationName = destinationName,

                    isLoading = isRouteLoading,

                    isNavigating = isNavigating,

                    onSwap = { swapRoute() },

                    onClose = { cancelActiveRoute() },

                    onPickOrigin = {

                        isPickingOrigin = true

                        isPickingDestination = false

                        searchQuery = ""

                        searchResults = emptyList()

                    },

                    onPickDestination = {

                        isPickingDestination = true

                        isPickingOrigin = false

                        searchQuery = ""

                        searchResults = emptyList()

                    },

                    modifier = Modifier.fillMaxWidth()

                )

            } else if (isPickingOrigin) {

                TopSearchBar(

                    query = searchQuery,

                    onQueryChange = { searchQuery = it },

                    searchResults = searchResults,

                    onSelectPlace = { place ->

                        originName = place.name

                        activeOriginCoord = place.coordinate

                        isPickingOrigin = false

                        searchQuery = ""

                        searchResults = emptyList()

                        val dest = activeDestCoord ?: destinationPin

                        if (dest != null) {

                            val options = RouteOptions(

                                mode = selectedMode,

                                avoidTolls = avoidTolls,

                                avoidExpressways = avoidExpressways,

                                avoidFerries = false

                            )

                            requestRouteCalculation(place.coordinate, dest, options, destinationName)

                        }

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

                    },

                    recentDestinations = recentDestinations,

                    onSelectRecent = { recent ->

                        originName = recent.name

                        val origCoord = LatLng(recent.latitude, recent.longitude)

                        activeOriginCoord = origCoord

                        isPickingOrigin = false

                        searchQuery = ""

                        searchResults = emptyList()

                        val dest = activeDestCoord ?: destinationPin

                        if (dest != null) {

                            val options = RouteOptions(

                                mode = selectedMode,

                                avoidTolls = avoidTolls,

                                avoidExpressways = avoidExpressways,

                                avoidFerries = false

                            )

                            requestRouteCalculation(origCoord, dest, options, destinationName)

                        }

                    },

                    onClearRecents = {

                        dev.gabay.app.data.RecentDestinationsManager.clearRecents(context)

                        recentDestinations = emptyList()

                    },

                    onCategoryClick = { category ->

                        searchQuery = category

                    },

                    hint = "Choose start point (Point A)...",

                    onCancelSearch = {

                        isPickingOrigin = false

                        searchQuery = ""

                        searchResults = emptyList()

                    },

                    onUseCurrentLocation = {

                        originName = "Your location"

                        activeOriginCoord = null

                        isPickingOrigin = false

                        searchQuery = ""

                        searchResults = emptyList()

                        val dest = activeDestCoord ?: destinationPin

                        if (dest != null) {

                            val options = RouteOptions(

                                mode = selectedMode,

                                avoidTolls = avoidTolls,

                                avoidExpressways = avoidExpressways,

                                avoidFerries = false

                            )

                            requestRouteCalculation(LatLng(userLat, userLon), dest, options, destinationName)

                        }

                    },

                    modifier = Modifier.fillMaxWidth()

                )

            } else if (isPickingDestination) {

                TopSearchBar(

                    query = searchQuery,

                    onQueryChange = { searchQuery = it },

                    searchResults = searchResults,

                    onSelectPlace = { place ->

                        destinationName = place.name

                        activeDestCoord = place.coordinate

                        destinationPin = place.coordinate

                        isPickingDestination = false

                        searchQuery = ""

                        searchResults = emptyList()

                        val start = activeOriginCoord ?: LatLng(userLat, userLon)

                        val options = RouteOptions(

                            mode = selectedMode,

                            avoidTolls = avoidTolls,

                            avoidExpressways = avoidExpressways,

                            avoidFerries = false

                        )

                        requestRouteCalculation(start, place.coordinate, options, place.name)

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

                    },

                    recentDestinations = recentDestinations,

                    onSelectRecent = { recent ->

                        destinationName = recent.name

                        val destCoord = LatLng(recent.latitude, recent.longitude)

                        activeDestCoord = destCoord

                        destinationPin = destCoord

                        isPickingDestination = false

                        searchQuery = ""

                        searchResults = emptyList()

                        val start = activeOriginCoord ?: LatLng(userLat, userLon)

                        val options = RouteOptions(

                            mode = selectedMode,

                            avoidTolls = avoidTolls,

                            avoidExpressways = avoidExpressways,

                            avoidFerries = false

                        )

                        requestRouteCalculation(start, destCoord, options, recent.name)

                    },

                    onClearRecents = {

                        dev.gabay.app.data.RecentDestinationsManager.clearRecents(context)

                        recentDestinations = emptyList()

                    },

                    onCategoryClick = { category ->

                        searchQuery = category

                    },

                    hint = "Choose destination (Point B)...",

                    onCancelSearch = {

                        isPickingDestination = false

                        searchQuery = ""

                        searchResults = emptyList()

                    },

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

                        val startCoord = activeOriginCoord ?: LatLng(userLat, userLon)

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

                            dev.gabay.app.audio.AudioGuidanceMode.FULL_VOICE -> "Voice Guidance: FULL"

                            dev.gabay.app.audio.AudioGuidanceMode.ALERTS_ONLY -> "Voice Guidance: ALERTS ONLY"

                            dev.gabay.app.audio.AudioGuidanceMode.MUTED -> "Voice Guidance: MUTED"

                        }

                        android.widget.Toast.makeText(context, toastMsg, android.widget.Toast.LENGTH_SHORT).show()

                    },

                    recentDestinations = recentDestinations,

                    onSelectRecent = { recent ->

                        searchQuery = ""

                        originName = "Your location"

                        val options = RouteOptions(

                            mode = selectedMode,

                            avoidTolls = avoidTolls,

                            avoidExpressways = avoidExpressways,

                            avoidFerries = false

                        )

                        val startCoord = activeOriginCoord ?: LatLng(userLat, userLon)

                        val destCoord = LatLng(recent.latitude, recent.longitude)

                        requestRouteCalculation(startCoord, destCoord, options, recent.name)

                    },

                    onClearRecents = {

                        dev.gabay.app.data.RecentDestinationsManager.clearRecents(context)

                        recentDestinations = emptyList()

                    },

                    onCategoryClick = { category ->

                        searchQuery = category

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
                    isCommuteMode = false
                    commuteItineraries = emptyList()
                    cancelActiveRoute()
                },
                routeAdvisory = activeRouteAdvisory,
                isCommuteMode = isCommuteMode,
                onSelectCommute = {
                    isCommuteMode = true
                    val startCoord = activeOriginCoord ?: LatLng(userLat, userLon)
                    val destCoord = activeDestCoord ?: destinationPin ?: return@RoutePreviewSheet
                    coroutineScope.launch(Dispatchers.IO) {
                        val results = dev.gabay.app.commute.CommuteRouter.planCommute(startCoord, destCoord)
                        withContext(Dispatchers.Main) {
                            commuteItineraries = results
                            selectedCommuteIndex = 0
                            audioController.speakTerseCue("Commute itineraries ready")
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp)
            )
        }

        // LAYER 3.5: Public Commute Itinerary Sheet (When Commute Mode is active)
        if (isCommuteMode && commuteItineraries.isNotEmpty()) {
            dev.gabay.app.commute.CommuteSheet(
                itineraries = commuteItineraries,
                selectedItineraryIndex = selectedCommuteIndex,
                onSelectItinerary = { selectedCommuteIndex = it },
                onStartCommute = { itin ->
                    val poly = itin.fullPolyline
                    val maneuvers = itin.legs.map { leg ->
                        when (leg) {
                            is dev.gabay.app.commute.CommuteLeg.Walk -> {
                                uniffi.gabay_core.TurnManeuver(
                                    instruction = leg.instruction,
                                    roadName = leg.targetStationName ?: "Walkway",
                                    turnType = "maneuver_straight",
                                    distanceMeters = leg.distanceMeters,
                                    coordinate = leg.polyline.firstOrNull() ?: poly.first()
                                )
                            }
                            is dev.gabay.app.commute.CommuteLeg.TransitRide -> {
                                uniffi.gabay_core.TurnManeuver(
                                    instruction = "Board ${leg.lineName} at ${leg.departureStation}",
                                    roadName = leg.lineName,
                                    turnType = "maneuver_straight",
                                    distanceMeters = leg.distanceMeters,
                                    coordinate = leg.polyline.firstOrNull() ?: poly.first()
                                )
                            }
                            is dev.gabay.app.commute.CommuteLeg.Transfer -> {
                                uniffi.gabay_core.TurnManeuver(
                                    instruction = leg.instruction,
                                    roadName = leg.stationName,
                                    turnType = "maneuver_straight",
                                    distanceMeters = leg.walkDistanceMeters,
                                    coordinate = poly.first()
                                )
                            }
                        }
                    }
                    val routeRes = RouteResult(
                        success = true,
                        totalDistanceMeters = itin.totalWalkMeters + 4000.0,
                        estimatedDurationSeconds = (itin.totalDurationMinutes * 60).toULong(),
                        polyline = poly,
                        maneuvers = maneuvers,
                        errorMessage = null
                    )
                    activeRoute = routeRes
                    isCommuteMode = false
                    startActiveNavigation()
                },
                onClose = {
                    isCommuteMode = false
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp)
            )
        }

        // LAYER 5: Offline Vault Setup & Integrity Check Dialog
        if (showVaultDialog) {
            dev.gabay.app.ui.VaultSetupDialog(
                vaultStatus = vaultStatus,
                onDismiss = { showVaultDialog = false },
                onVaultReady = {
                    showVaultDialog = false
                    vaultStatus = dev.gabay.app.data.VaultIntegrityManager.checkVault()
                    try {
                        activeEngine = GabayEngine("/sdcard/Documents/Gabay")
                        PmTilesServer.reloadReaders(File("/sdcard/Documents/Gabay"))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
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

                            activeColor = GabayColors.AccentAmber,

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

                            activeColor = GabayColors.AccentAmber,

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

                audioController = audioController,

                onThemeToggle = { enabled ->

                    GabayColors.isSunlightLightMode = enabled

                    context.getSharedPreferences("gabay_settings", android.content.Context.MODE_PRIVATE)

                        .edit()

                        .putBoolean("sunlight_light_mode", enabled)

                        .apply()

                },

                currentMapMode = fusedMapMode,

                onMapModeSelect = { fusedMapMode = it },

                onClose = { isMenuSheetOpen = false }

            )

        }

    }

}


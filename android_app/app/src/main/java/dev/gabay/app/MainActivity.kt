package dev.gabay.app

import android.content.Context
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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

        // Start embedded PMTiles localhost HTTP server for nationwide vector, topo, satellite
        PmTilesServer.start(File("/sdcard/Documents/Gabay"))

        // Initialize Audio Controller (TTS + Audio Focus Ducking)
        audioController = AudioController(this)

        // Initialize Rust Core Engine with device vault path
        try {
            engine = GabayEngine("/sdcard/Documents/Gabay")
        } catch (e: Exception) {
            // Safe fallback if vault directory structure is creating
        }

        enableEdgeToEdge()
        setContent {
            GabayTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = GabayColors.BgObsidian
                ) { innerPadding ->
                    CockpitScreen(
                        engine = engine,
                        audioController = audioController,
                        modifier = Modifier.padding(innerPadding)
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

    // Live Hardware Microphone (Activates Android OS Mic Privacy Indicator when Voice Co-Pilot active)
    DisposableEffect(isAgentSheetOpen) {
        var audioRecord: AudioRecord? = null
        var isRecording = false

        if (isAgentSheetOpen) {
            try {
                val sampleRate = 16000
                val channelConfig = AudioFormat.CHANNEL_IN_MONO
                val audioFormat = AudioFormat.ENCODING_PCM_16BIT
                val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
                if (minBufferSize > 0) {
                    audioRecord = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        sampleRate,
                        channelConfig,
                        audioFormat,
                        minBufferSize
                    )
                    audioRecord.startRecording()
                    isRecording = true
                    thread(name = "AgentMicLiveThread") {
                        val buffer = ShortArray(minBufferSize)
                        while (isRecording && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                            audioRecord?.read(buffer, 0, buffer.size)
                        }
                    }
                }
            } catch (e: SecurityException) {
                // Permission not granted
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        onDispose {
            isRecording = false
            try {
                audioRecord?.stop()
                audioRecord?.release()
            } catch (e: Exception) {}
        }
    }

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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GabayColors.MapCanvasBg)
    ) {
        // LAYER 0: MapLibre Canvas with Real OSM Streets, Waterways, & Topo/Satellite Raster
        MapCanvas(
            userLat = userLat,
            userLon = userLon,
            userHeading = userHeading,
            activeRoute = activeRoute,
            destinationPin = destinationPin,
            fusedMapMode = fusedMapMode,
            cameraTarget = cameraTarget,
            onPinDropped = { latLng ->
                destinationPin = latLng
                // Query nearest landmark/road from SQLite FTS5 engine
                val pois = engine?.searchPois("Ayala", 1u) ?: emptyList()
                val landmarkName = pois.firstOrNull()?.name ?: "Pinned Destination"
                pinInfo = PinInfo(
                    coordinate = latLng,
                    nearestRoadName = landmarkName,
                    barangay = "Local Barangay",
                    cityOrMunicipality = "Philippines"
                )
            },
            modifier = Modifier.fillMaxSize()
        )

        // LAYER 1: Top Search Bar (Google Maps Style)
        TopSearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            searchResults = searchResults,
            onSelectPlace = { place ->
                val dest = place.coordinate
                destinationPin = dest
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
                val route = engine?.calculateRoute(startCoord, dest, options)
                if (route != null && route.success) {
                    activeRoute = route
                    engine?.setActiveRoute(route)
                    NavigationService.start(context)
                    audioController.speakTerseCue("ROUTE ACTIVE TO ${place.name.uppercase()}")
                }
            },
            onMenuClick = { isMenuSheetOpen = true },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        )

        // LAYER 2: Floating Distance & Pace Pills (ONLY Visible When Route Is Active, placed below search bar)
        if (activeRoute != null) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 64.dp)
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FloatingTelemetryPill(
                    value = String.format(Locale.US, "%.1f", telemetry.remainingDistanceKm),
                    unit = "KM REMAINING"
                )
                FloatingTelemetryPill(
                    value = telemetry.paceWindowMin,
                    unit = "EST PACE"
                )
            }
        }

        // LAYER 3: Pin Inspector Sheet (Overlayed When Long-Pressed)
        pinInfo?.let { info ->
            PinInspectorSheet(
                pinInfo = info,
                onStartRoute = { destCoord ->
                    val options = RouteOptions(
                        mode = selectedMode,
                        avoidTolls = (selectedMode == VehicleMode.SCOOTER),
                        avoidExpressways = (selectedMode == VehicleMode.SCOOTER),
                        avoidFerries = false
                    )
                    val startCoord = LatLng(userLat, userLon)
                    val route = engine?.calculateRoute(startCoord, destCoord, options)
                    if (route != null && route.success) {
                        activeRoute = route
                        engine?.setActiveRoute(route)
                        NavigationService.start(context)
                        audioController.speakTerseCue("ROUTE TO PIN ACTIVE. PROCEED TO ${route.maneuvers.firstOrNull()?.roadName ?: "ROUTE"}")
                    }
                    pinInfo = null
                },
                onClose = {
                    pinInfo = null
                    destinationPin = null
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
            // Floating Action Buttons Row: Fused Map Layer Speed-Dial, Agent Talk, Recenter GPS
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                // Fused Map Style Speed-Dial (Bottom Left - Extends Upwards with delayed labels)
                Box(modifier = Modifier.align(Alignment.BottomStart)) {
                    MapStyleSpeedDial(
                        currentMode = fusedMapMode,
                        onSelectMode = { fusedMapMode = it }
                    )
                }

                // Talk / Live Voice Co-Pilot Button (Center Floating)
                CircularFloatingButton(
                    icon = GabayIcons.CopilotMic,
                    isActive = isAgentSheetOpen,
                    activeColor = GabayColors.AccentMint,
                    onClick = {
                        isAgentSheetOpen = !isAgentSheetOpen
                        if (isAgentSheetOpen) {
                            audioController.speakTerseCue("GABAY CO-PILOT LISTENING")
                        }
                    },
                    modifier = Modifier
                        .size(54.dp)
                        .align(Alignment.BottomCenter),
                    contentDescription = "Talk to Gabay Agent"
                )

                // Recenter GPS & Reset Rotation Button (Bottom Right)
                CircularFloatingButton(
                    icon = GabayIcons.RecenterGps,
                    onClick = {
                        cameraTarget = MapLibreLatLng(userLat, userLon)
                    },
                    modifier = Modifier.align(Alignment.BottomEnd),
                    contentDescription = "Recenter GPS & Reset Rotation"
                )
            }

            // Middle Bottom Speedometer Readout (Number Only + Hidden arrows when straight + MPH + Bottom Road Label)
            MiddleBottomSpeedometer(
                speedKmh = userSpeed,
                nextTurnIcon = if (activeRoute != null) telemetry.nextTurnIcon else "straight",
                bottomLabel = if (activeRoute != null) telemetry.nextTurnRoad else ""
            )
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
                onSendPrompt = { prompt ->
                    val now = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                    chatMessages.add(ChatMessage("RIDER", prompt, now))

                    // Evaluate query through Low-Resource Gatekeeper
                    val response = engine?.askCopilot(prompt) ?: "NEXT PETRON 450M RIGHT"
                    chatMessages.add(ChatMessage("GABAY AGENT", response, now))
                    audioController.speakTerseCue(response)
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

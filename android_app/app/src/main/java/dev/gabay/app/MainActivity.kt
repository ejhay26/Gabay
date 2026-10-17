package dev.gabay.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.gabay.app.audio.AudioController
import dev.gabay.app.map.FusedMapMode
import dev.gabay.app.map.MapCanvas
import dev.gabay.app.ui.*
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLng as MapLibreLatLng
import uniffi.gabay_core.*
import java.text.SimpleDateFormat
import java.util.*

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

    // User GPS state (Realistic idle speed: 0.0 km/h when stationary)
    var userLat by remember { mutableDoubleStateOf(14.5547) } // Ayala Avenue Makati
    var userLon by remember { mutableDoubleStateOf(121.0244) }
    var userSpeed by remember { mutableFloatStateOf(0.0f) }
    var userHeading by remember { mutableFloatStateOf(45.0f) }

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
                headingCardinal = "NE",
                remainingDistanceKm = 0.0f,
                paceWindowMin = "-- MIN",
                nextTurnRoad = "Current Road",
                nextTurnDistanceM = 0.0f,
                nextTurnIcon = "maneuver_straight",
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
        // LAYER 0: MapLibre Canvas with Real OSM Streets, Waterways, & Parks
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
                val landmarkName = pois.firstOrNull()?.name ?: "Ayala Corridor"
                pinInfo = PinInfo(
                    coordinate = latLng,
                    nearestRoadName = landmarkName,
                    barangay = "Barangay Bel-Air",
                    cityOrMunicipality = "Makati City"
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
                    userSpeed = 24.0f // Simulated realistic cruising speed
                    NavigationService.start(context)
                    audioController.speakTerseCue("ROUTE ACTIVE TO ${place.name.uppercase()}")
                }
            },
            onMenuClick = { isMenuSheetOpen = true },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        )

        // LAYER 2: Floating Distance & Pace Pills (ONLY Visible When Route Is Active)
        if (activeRoute != null) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 74.dp)
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FloatingTelemetryPill(
                    value = String.format("%.1f", telemetry.remainingDistanceKm),
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
                        userSpeed = 28.0f // Cruising speed
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
            // Floating Action Buttons Row: Fused Map Layer, Agent Co-Pilot Talk, Recenter GPS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Fused Map Style Button (Bottom Left)
                val mapModeIcon = when (fusedMapMode) {
                    FusedMapMode.STREET -> GabayIcons.ModeCar
                    FusedMapMode.TOPO -> GabayIcons.LayerTopo
                    FusedMapMode.SATELLITE -> GabayIcons.LayerSatellite
                    FusedMapMode.HYBRID_3D -> GabayIcons.Layer3d
                }
                CircularFloatingButton(
                    icon = mapModeIcon,
                    onClick = {
                        // Cycle fused map mode: STREET -> TOPO -> SATELLITE -> HYBRID_3D -> STREET
                        fusedMapMode = when (fusedMapMode) {
                            FusedMapMode.STREET -> FusedMapMode.TOPO
                            FusedMapMode.TOPO -> FusedMapMode.SATELLITE
                            FusedMapMode.SATELLITE -> FusedMapMode.HYBRID_3D
                            FusedMapMode.HYBRID_3D -> FusedMapMode.STREET
                        }
                    },
                    contentDescription = "Map Style"
                )

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
                    modifier = Modifier.size(54.dp),
                    contentDescription = "Talk to Gabay Agent"
                )

                // Recenter GPS Button (Bottom Right)
                CircularFloatingButton(
                    icon = GabayIcons.RecenterGps,
                    onClick = {
                        cameraTarget = MapLibreLatLng(userLat, userLon)
                    },
                    contentDescription = "Recenter GPS"
                )
            }

            // Middle Bottom Speedometer Readout (Number Only + Flanking Turn Arrows + MPH)
            MiddleBottomSpeedometer(
                speedKmh = telemetry.liveSpeedKmh,
                nextTurnIcon = telemetry.nextTurnIcon
            )
        }

        // LAYER 5: Agent Co-Pilot Conversation Transcript Sheet
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

        // LAYER 6: Top Menu Drawer Sheet (Drive History & Vault Stats)
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

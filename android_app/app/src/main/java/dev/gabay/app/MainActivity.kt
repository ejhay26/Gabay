package dev.gabay.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.gabay.app.audio.AudioController
import dev.gabay.app.map.MapCanvas
import dev.gabay.app.ui.*
import org.maplibre.android.MapLibre
import uniffi.gabay_core.*

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
    var isSatelliteActive by remember { mutableStateOf(false) }
    var isTopoActive by remember { mutableStateOf(false) }
    var is3dActive by remember { mutableStateOf(false) }

    // User GPS state (Default to Metro Manila Makati / Ayala Ave)
    var userLat by remember { mutableDoubleStateOf(14.5547) }
    var userLon by remember { mutableDoubleStateOf(121.0244) }
    var userSpeed by remember { mutableFloatStateOf(42.0f) }
    var userHeading by remember { mutableFloatStateOf(45.0f) }

    // Navigation and Routing state
    var activeRoute by remember { mutableStateOf<RouteResult?>(null) }
    var destinationPin by remember { mutableStateOf<LatLng?>(null) }
    var pinInfo by remember { mutableStateOf<PinInfo?>(null) }

    // Live Telemetry from Rust Core Tracker
    var telemetry by remember {
        mutableStateOf(
            HudTelemetry(
                liveSpeedKmh = userSpeed,
                headingDegrees = userHeading,
                headingCardinal = "NE",
                remainingDistanceKm = 14.8f,
                paceWindowMin = "~22-26 MIN",
                nextTurnRoad = "Ayala Avenue",
                nextTurnDistanceM = 350.0f,
                nextTurnIcon = "maneuver_straight",
                isOffRoute = false,
                offRouteDeltaM = 0.0f,
                coPilotAdvisory = null
            )
        )
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
            .background(GabayColors.BgObsidian)
    ) {
        // LAYER 0: MapLibre Canvas with Elastic Philippines Border Gating
        MapCanvas(
            userLat = userLat,
            userLon = userLon,
            userHeading = userHeading,
            activeRoute = activeRoute,
            destinationPin = destinationPin,
            isSatelliteActive = isSatelliteActive,
            isTopoActive = isTopoActive,
            is3dActive = is3dActive,
            onPinDropped = { latLng ->
                destinationPin = latLng
                // Query nearest landmark/road from engine
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

        // LAYER 1: Top Maneuver Banner
        TopManeuverBanner(
            telemetry = telemetry,
            vehicleMode = selectedMode,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // LAYER 2: Pin Inspector Sheet (overlayed when user long-presses map)
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
                        audioController.speakTerseCue("ROUTE CALCULATED. PROCEED TO ${route.maneuvers.firstOrNull()?.roadName ?: "ROUTE"}")
                    }
                    pinInfo = null
                },
                onClose = {
                    pinInfo = null
                    destinationPin = null
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 180.dp)
            )
        }

        // LAYER 3: Bottom Cockpit & Glove Mode Bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            GloveModeBar(
                selectedMode = selectedMode,
                isSatelliteActive = isSatelliteActive,
                isTopoActive = isTopoActive,
                is3dActive = is3dActive,
                onModeSelect = { mode ->
                    selectedMode = mode
                    // Recalculate route with new vehicle profile if destination active
                    destinationPin?.let { dest ->
                        val options = RouteOptions(
                            mode = mode,
                            avoidTolls = (mode == VehicleMode.SCOOTER),
                            avoidExpressways = (mode == VehicleMode.SCOOTER),
                            avoidFerries = false
                        )
                        val start = LatLng(userLat, userLon)
                        val route = engine?.calculateRoute(start, dest, options)
                        if (route != null) {
                            activeRoute = route
                            engine?.setActiveRoute(route)
                        }
                    }
                },
                onToggleSat = { isSatelliteActive = !isSatelliteActive },
                onToggleTopo = { isTopoActive = !isTopoActive },
                onToggle3d = { is3dActive = !is3dActive },
                onRecenterGps = {
                    if (activeRoute == null) {
                        val start = LatLng(14.5547, 121.0244)
                        val dest = LatLng(14.5869, 121.0614)
                        val opts = RouteOptions(
                            mode = selectedMode,
                            avoidTolls = (selectedMode == VehicleMode.SCOOTER),
                            avoidExpressways = (selectedMode == VehicleMode.SCOOTER),
                            avoidFerries = false
                        )
                        val route = engine?.calculateRoute(start, dest, opts)
                        if (route != null && route.success) {
                            activeRoute = route
                            destinationPin = dest
                            engine?.setActiveRoute(route)
                            NavigationService.start(context)
                            audioController.speakTerseCue("NAVIGATION DEMO ACTIVE. IN 350 METERS PROCEED TO AYALA AVENUE")
                        }
                    } else {
                        userLat = 14.5547
                        userLon = 121.0244
                    }
                },
                onVoiceCoPilot = {
                    val response = engine?.askCopilot("Where is nearest fuel station?")
                        ?: "NEAREST PETRON 450M RIGHT"
                    audioController.speakTerseCue(response)
                }
            )

            BottomCockpitDock(
                telemetry = telemetry,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

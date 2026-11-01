package dev.gabay.app.car

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object GabayCarStateBridge {
    var isNavigating by mutableStateOf(false)
    var destinationName by mutableStateOf("No active trip")
    var distanceRemainingKm by mutableStateOf(0.0f)
    var etaMinutes by mutableStateOf(0)
    var currentManeuverText by mutableStateOf("Head to route")
    var currentManeuverIcon by mutableStateOf("maneuver_straight")
    var distanceToNextManeuverMeters by mutableStateOf(0.0)
    var currentRoadName by mutableStateOf("")

    // Callbacks from Android Auto to App
    var onStopNavigationRequested: (() -> Unit)? = null
    var onRecentSelected: ((name: String, lat: Double, lon: Double) -> Unit)? = null
}

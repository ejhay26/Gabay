package dev.gabay.app.viewmodel

import android.app.Application
import android.location.Location
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.gabay.app.core.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import uniffi.gabay_core.*

data class MapUiState(
    val permissions: AppPermissionsState,
    val vaultStatus: VaultStatus,
    val isEngineReady: Boolean = false,
    val userLocation: Location? = null,
    val searchQuery: String = "",
    val searchResults: List<PoiSearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val selectedPoi: PoiSearchResult? = null,
    val isRouting: Boolean = false,
    val activeRoute: RouteResult? = null,
    val isNavigating: Boolean = false,
    val telemetry: HudTelemetry? = null,
    val isDarkTheme: Boolean = false
)

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val tag = "MapViewModel"
    val engineBridge = EngineBridge()
    val ttsManager = TtsManager(application)

    private val _uiState = MutableStateFlow(
        MapUiState(
            permissions = PermissionManager.checkAllPermissions(application),
            vaultStatus = VaultManager.checkVault(
                application,
                PermissionManager.hasStoragePermission(application)
            )
        )
    )
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var lastSpokenInstruction: String? = null

    init {
        refreshPermissionsAndVault()
    }

    fun refreshPermissionsAndVault() {
        val app = getApplication<Application>()
        val perms = PermissionManager.checkAllPermissions(app)
        val vault = VaultManager.checkVault(app, perms.hasStoragePermission)

        _uiState.update { it.copy(permissions = perms, vaultStatus = vault) }

        if (vault.isReady && !engineBridge.isReady) {
            viewModelScope.launch {
                val ok = engineBridge.init(vault.directory.absolutePath)
                _uiState.update { it.copy(isEngineReady = ok) }
                if (ok) {
                    ttsManager.speak("Gabay offline navigation ready.")
                }
            }
        }
    }

    fun onLocationChanged(location: Location) {
        _uiState.update { it.copy(userLocation = location) }

        if (_uiState.value.isNavigating && engineBridge.isReady) {
            viewModelScope.launch {
                val telem = engineBridge.processGpsTick(
                    location.latitude,
                    location.longitude,
                    location.speed * 3.6f, // m/s to km/h
                    location.bearing
                )
                if (telem != null) {
                    _uiState.update { it.copy(telemetry = telem) }

                    // Trigger Voice TTS on turn cue change
                    val cue = telem.nextTurnRoad
                    if (cue.isNotBlank() && cue != lastSpokenInstruction && telem.nextTurnDistanceM < 150.0) {
                        lastSpokenInstruction = cue
                        val turnVoice = "In ${telem.nextTurnDistanceM.toInt()} meters, ${telem.nextTurnIcon.replace('_', ' ')} on $cue"
                        ttsManager.speak(turnVoice, flush = false)
                    }

                    telem.coPilotAdvisory?.let { adv ->
                        if (adv.isNotBlank()) {
                            ttsManager.speak(adv, flush = false)
                        }
                    }
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()

        if (query.trim().length < 2) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(250) // debounce typing
            _uiState.update { it.copy(isSearching = true) }
            val results = engineBridge.searchPois(query.trim())
            _uiState.update { it.copy(searchResults = results, isSearching = false) }
        }
    }

    fun selectPoi(poi: PoiSearchResult?) {
        _uiState.update { it.copy(selectedPoi = poi, searchResults = emptyList()) }
        if (poi != null) {
            calculateRouteTo(poi)
        } else {
            _uiState.update { it.copy(activeRoute = null) }
        }
    }

    fun calculateRouteTo(poi: PoiSearchResult) {
        val userLoc = _uiState.value.userLocation
        val start = if (userLoc != null) {
            LatLng(userLoc.latitude, userLoc.longitude)
        } else {
            LatLng(14.5995, 120.9842) // Manila fallback
        }
        val end = LatLng(poi.lat, poi.lon)

        viewModelScope.launch {
            _uiState.update { it.copy(isRouting = true) }
            val route = engineBridge.calculateRoute(start, end)
            _uiState.update { it.copy(activeRoute = route, isRouting = false) }

            if (route != null) {
                val km = String.format("%.1f", route.totalDistanceKm)
                val mins = route.estimatedDurationMin
                ttsManager.speak("Route found: $km kilometers, about $mins minutes.")
            }
        }
    }

    fun startNavigation() {
        val route = _uiState.value.activeRoute ?: return
        viewModelScope.launch {
            engineBridge.setActiveRoute(route)
            _uiState.update { it.copy(isNavigating = true) }
            ttsManager.speak("Starting navigation. Drive safely.")
        }
    }

    fun stopNavigation() {
        viewModelScope.launch {
            engineBridge.clearActiveRoute()
            _uiState.update { it.copy(isNavigating = false, telemetry = null) }
            lastSpokenInstruction = null
            ttsManager.speak("Navigation stopped.")
        }
    }

    fun toggleTheme() {
        _uiState.update { it.copy(isDarkTheme = !it.isDarkTheme) }
    }

    fun askAssistant(prompt: String, onReply: (String) -> Unit) {
        viewModelScope.launch {
            val reply = engineBridge.askAssistant(prompt)
            ttsManager.speak(reply, flush = true)
            onReply(reply)
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }
}

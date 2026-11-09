package dev.gabay.app.core

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uniffi.gabay_core.*

class EngineBridge {

    private val tag = "EngineBridge"
    private var engine: GabayEngine? = null

    suspend fun init(vaultPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(tag, "Initializing GabayEngine with vault path: $vaultPath")
            engine = GabayEngine(vaultPath)
            Log.i(tag, "GabayEngine initialized successfully.")
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize GabayEngine: ${e.message}", e)
            false
        }
    }

    val isReady: Boolean
        get() = engine != null

    suspend fun searchPois(query: String, limit: Int = 20): List<PoiSearchResult> = withContext(Dispatchers.IO) {
        val eng = engine ?: return@withContext emptyList()
        try {
            eng.searchPois(query, limit.toUInt())
        } catch (e: Exception) {
            Log.e(tag, "POI search failed for query '$query': ${e.message}")
            emptyList()
        }
    }

    suspend fun calculateRoute(
        start: LatLng,
        end: LatLng,
        options: RouteOptions = RouteOptions(
            avoidTolls = false,
            avoidHighways = false,
            preferScenic = false,
            vehicleType = VehicleType.CAR
        )
    ): RouteResult? = withContext(Dispatchers.IO) {
        val eng = engine ?: return@withContext null
        try {
            val result = eng.calculateRoute(start, end, options)
            if (result.success) result else null
        } catch (e: Exception) {
            Log.e(tag, "Route calculation failed: ${e.message}")
            null
        }
    }

    suspend fun setActiveRoute(route: RouteResult) = withContext(Dispatchers.IO) {
        engine?.setActiveRoute(route)
    }

    suspend fun clearActiveRoute() = withContext(Dispatchers.IO) {
        engine?.clearActiveRoute()
    }

    suspend fun processGpsTick(
        latitude: Double,
        longitude: Double,
        speedKmh: Float,
        headingDeg: Float
    ): HudTelemetry? = withContext(Dispatchers.IO) {
        val eng = engine ?: return@withContext null
        try {
            eng.processGpsTick(latitude, longitude, speedKmh, headingDeg)
        } catch (e: Exception) {
            Log.e(tag, "processGpsTick error: ${e.message}")
            null
        }
    }

    suspend fun askAssistant(query: String): String = withContext(Dispatchers.IO) {
        val eng = engine ?: return@withContext "Engine offline"
        try {
            eng.askCopilot(query)
        } catch (e: Exception) {
            Log.e(tag, "askAssistant error: ${e.message}")
            "Assistant temporarily unavailable"
        }
    }
}

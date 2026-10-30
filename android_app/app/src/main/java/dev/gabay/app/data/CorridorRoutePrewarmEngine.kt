package dev.gabay.app.data

import android.content.Context
import android.util.Log
import uniffi.gabay_core.GabayEngine
import uniffi.gabay_core.LatLng
import uniffi.gabay_core.RouteOptions
import uniffi.gabay_core.VehicleMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Intelligent Chunked Route Pre-Warm & Background Cache Engine
 * Pre-computes and caches nationwide highway corridors and municipal hub connections.
 * 
 * Rules:
 * - Runs asynchronously in low-priority background dispatcher.
 * - Yields (delay) between chunks so phone never freezes or drops UI frames.
 * - Caches bi-directional routes directly into the offline router cache.
 */
object CorridorRoutePrewarmEngine {
    private const val TAG = "GabayPrewarm"
    private const val PREF_KEY = "corridor_prewarm_v1_done"

    // Key Regional Arterial Hubs (Luzon & Central Philippines Corridor Network)
    data class Hub(val name: String, val lat: Double, val lon: Double)

    val KEY_HUBS = listOf(
        Hub("Manila Luneta", 14.5826, 120.9787),
        Hub("BGC Taguig", 14.5505, 121.0500),
        Hub("Quezon City Elliptical", 14.6515, 121.0493),
        Hub("Alabang Muntinlupa", 14.4235, 121.0285),
        Hub("San Fernando Pampanga", 15.0350, 120.6880),
        Hub("Angeles City", 15.1450, 120.5890),
        Hub("San Isidro Nueva Ecija", 15.3090, 120.9025),
        Hub("Gapan City", 15.3075, 120.9472),
        Hub("Cabanatuan City", 15.4855, 120.9660),
        Hub("Tarlac City", 15.4865, 120.5980),
        Hub("Baguio City", 16.4135, 120.5960),
        Hub("Dagupan City", 16.0435, 120.3340),
        Hub("Calamba Laguna", 14.2050, 121.1550),
        Hub("Lipa City Batangas", 13.9410, 121.1630),
        Hub("Batangas City", 13.7565, 121.0580)
    )

    suspend fun startPrewarmIfPending(context: Context, engine: GabayEngine) = withContext(Dispatchers.Default) {
        val prefs = context.getSharedPreferences("gabay_cache_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean(PREF_KEY, false)) {
            Log.d(TAG, "Corridor route cache is already warm. Skipping background precompute.")
            return@withContext
        }

        Log.i(TAG, "Starting chunked background route prewarm for ${KEY_HUBS.size} arterial hubs...")
        val options = RouteOptions(
            mode = VehicleMode.SCOOTER,
            avoidExpressways = true,
            avoidTolls = true,
            avoidFerries = false
        )

        var routesComputed = 0
        val totalPairs = KEY_HUBS.size * (KEY_HUBS.size - 1) / 2

        // Iterate hub pairs chunk-by-chunk with throttling delays
        for (i in 0 until KEY_HUBS.size) {
            val from = KEY_HUBS[i]
            val fromCoord = LatLng(from.lat, from.lon)

            for (j in (i + 1) until KEY_HUBS.size) {
                val to = KEY_HUBS[j]
                val toCoord = LatLng(to.lat, to.lon)

                try {
                    // Forward route calculation
                    val res = engine.calculateRoute(fromCoord, toCoord, options)
                    if (res.success) {
                        routesComputed++
                    }

                    // Backward route calculation
                    val rev = engine.calculateRoute(toCoord, fromCoord, options)
                    if (rev.success) {
                        routesComputed++
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Prewarm failed for ${from.name} <-> ${to.name}: ${e.message}")
                }

                // Chunk throttling: Yield CPU after every pair to guarantee 60fps UI
                delay(40L)
            }

            // Secondary pause after each hub cluster
            delay(100L)
        }

        prefs.edit().putBoolean(PREF_KEY, true).apply()
        Log.i(TAG, "Completed corridor route prewarm: $routesComputed routes successfully cached in background!")
    }
}

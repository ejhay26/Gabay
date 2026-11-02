package dev.gabay.app.ai

import android.content.Context
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.Socket
import java.net.URL
import java.util.Locale

data class RiderContext(
    val latitude: Double = 15.3089,
    val longitude: Double = 120.9065,
    val speedKmh: Float = 0f,
    val heading: Float = 0f,
    val currentRoad: String = "San Isidro Arterial Road",
    val isNavigating: Boolean = false,
    val destinationName: String = "",
    val remainingDistanceKm: Float = 0f
)

sealed class IntentResult {
    data class Action(val action: String, val target: String = "", val rawText: String = "") : IntentResult()
    data class Conversational(val text: String) : IntentResult()
    data class GuardrailBlocked(val reason: String) : IntentResult()
}

object LlamaDaemon {
    private const val TAG = "GabayLlama"
    private const val SERVER_PORT = 8766
    private const val SERVER_URL = "http://127.0.0.1:$SERVER_PORT/v1/chat/completions"

    // Hardcoded Guardrails (Rate Limiting & Thermal CPU Protection)
    private const val COOLDOWN_MS = 4000L           // Minimum 4s between heavy model calls
    private const val REPEAT_WINDOW_MS = 12000L     // Repetition debounce window
    private var lastInferenceTime = 0L
    private var lastPrompt = ""
    private var lastResult: IntentResult? = null

    private var serverProcess: Process? = null

    /**
     * Ensures llama-server is active on loopback port 8766.
     * Uses pre-installed native arm64 binary at /data/local/tmp/llama/llama-server
     * and model at /sdcard/Documents/Gabay/model.gguf.
     */
    fun ensureServerRunning(context: Context) {
        Thread {
            try {
                if (isPortOpen(SERVER_PORT)) {
                    Log.d(TAG, "llama-server already running on port $SERVER_PORT")
                    return@Thread
                }

                val binPath = "/data/local/tmp/llama/llama-server"
                val modelPath = "/sdcard/Documents/Gabay/model.gguf"
                val binFile = File(binPath)
                val modelFile = File(modelPath)

                if (!binFile.exists() || !modelFile.exists()) {
                    Log.w(TAG, "Bin or model not found: bin=${binFile.exists()}, model=${modelFile.exists()}")
                    return@Thread
                }

                Log.d(TAG, "Starting llama-server...")
                val pb = ProcessBuilder(
                    binPath,
                    "-m", modelPath,
                    "-c", "512",
                    "-t", "4",
                    "--port", SERVER_PORT.toString(),
                    "--host", "127.0.0.1"
                )
                pb.environment()["LD_LIBRARY_PATH"] = "/data/local/tmp/llama"
                pb.redirectErrorStream(true)
                serverProcess = pb.start()

                // Wait up to 10 seconds for server to bind
                for (i in 1..20) {
                    Thread.sleep(500)
                    if (isPortOpen(SERVER_PORT)) {
                        Log.d(TAG, "llama-server successfully bound to port $SERVER_PORT")
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start llama-server", e)
            }
        }.start()
    }

    private fun isPortOpen(port: Int): Boolean {
        return try {
            Socket("127.0.0.1", port).use { true }
        } catch (e: Exception) {
            false
        }
    }

    // Standalone Modular Semantic Dispatcher (reusable across any app)
    private val dispatcher = ModularSemanticDispatcher(
        endpointUrl = SERVER_URL,
        systemRole = "You are Gabay, an offline motorcycle navigation co-pilot in the Philippines.\n" +
            "You provide concise, clear, and direct vehicular voice guidance.\n" +
            "Never use generic AI filler, marketing fluff, or robotic sci-fi phrases."
    ).apply {
        registerAction(
            name = "navigate",
            description = "Route to a destination",
            targetDescription = "Destination name (e.g. Neust San Isidro, Gapan City, SM Cabanatuan)",
            examples = listOf("can you navigate to neust san isidro campus", "take me to Gapan City", "dalhin mo ako sa Cabanatuan"),
            keywords = listOf("navigate", "take me to", "route to", "go to", "drive to", "dalhin mo ako")
        )
        registerAction(
            name = "search",
            description = "Find nearby POIs, emergency services, food, or amenities",
            targetDescription = "POI category or name (e.g. Gas, Hospital, Food, School, Pharmacy, Bank)",
            examples = listOf("take me to the nearest gas station", "emergency hospital nearby", "find food nearby", "saan may gasolinahan"),
            keywords = listOf("gas", "hospital", "food", "school", "pharmacy", "bank", "restaurant", "police")
        )
        registerAction(
            name = "where_am_i",
            description = "Check current location, road name, or navigation progress",
            examples = listOf("where am I right now?", "what road is this?", "nasaan ako?", "anong kalsada ito"),
            keywords = listOf("where am i", "nasaan", "location", "what road", "status")
        )
        registerAction(
            name = "street",
            description = "Switch map layer to vector dark street view",
            examples = listOf("switch to street view", "show street map"),
            keywords = listOf("street view", "street map")
        )
        registerAction(
            name = "satellite",
            description = "Switch map layer to satellite imagery",
            examples = listOf("switch to satellite mode", "ipakita ang satellite"),
            keywords = listOf("satellite", "sat view")
        )
        registerAction(
            name = "topo",
            description = "Switch map layer to contour topography",
            examples = listOf("show me topography", "switch to topo view"),
            keywords = listOf("topo", "topography", "contour")
        )
        registerAction(
            name = "3d",
            description = "Enable 3D driver perspective view",
            examples = listOf("switch to 3D", "driver view"),
            keywords = listOf("3d", "perspective", "driver view")
        )
        registerAction(
            name = "2d",
            description = "Enable 2D flat overhead map",
            examples = listOf("switch to 2D", "flat view", "overhead"),
            keywords = listOf("2d", "flat", "overhead")
        )
        registerAction(
            name = "follow",
            description = "Recenter camera and magnetically lock to vehicle",
            examples = listOf("recenter camera", "lock camera", "magnet view"),
            keywords = listOf("recenter", "follow", "magnet", "lock")
        )
        registerAction(
            name = "north",
            description = "Align compass rotation facing True North",
            examples = listOf("align north", "face north", "reset rotation"),
            keywords = listOf("north", "compass", "reset rotation")
        )
        registerAction(
            name = "cancel",
            description = "Cancel active route or end navigation",
            examples = listOf("cancel route", "stop navigation", "end route", "itigil ang ruta"),
            keywords = listOf("cancel", "stop navigation", "end route", "itigil")
        )
    }

    /**
     * Executes the AI Dual-Intent Router & Brain.
     * Guardrails execute first to protect Android hardware from CPU runaway.
     */
    suspend fun processWithBrain(
        prompt: String,
        recentHistory: List<Pair<String, String>> = emptyList(),
        riderContext: RiderContext = RiderContext()
    ): IntentResult = withContext(Dispatchers.IO) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) {
            return@withContext IntentResult.Conversational("Standby, awaiting your command.")
        }

        val now = SystemClock.elapsedRealtime()

        // Guardrail 1: Repetition Rule - Don't fry the CPU if rider repeats same prompt within 12s
        if (trimmed.equals(lastPrompt, ignoreCase = true) && (now - lastInferenceTime) < REPEAT_WINDOW_MS) {
            val cached = lastResult
            if (cached != null) {
                Log.d(TAG, "Guardrail hit: Repetition debounce (<12s). Returning cached result.")
                return@withContext cached
            }
        }

        // Guardrail 2: Thermal Cooldown Rule - Minimum 4s between heavy LLM inferences
        if ((now - lastInferenceTime) < COOLDOWN_MS) {
            Log.d(TAG, "Guardrail hit: Cooldown active (<4s). Rejecting concurrent burn.")
            return@withContext IntentResult.GuardrailBlocked("Co-pilot thinking, please allow a moment.")
        }

        lastPrompt = trimmed
        lastInferenceTime = now

        val navStatus = if (riderContext.isNavigating) {
            "En route to ${riderContext.destinationName} (${String.format(Locale.US, "%.1f", riderContext.remainingDistanceKm)} km remaining)"
        } else {
            "Free exploration (no active navigation route)"
        }

        val contextMap = mapOf(
            "Current Road" to riderContext.currentRoad,
            "Coordinates" to "Lat ${String.format(Locale.US, "%.4f", riderContext.latitude)}, Lon ${String.format(Locale.US, "%.4f", riderContext.longitude)}",
            "Speed" to "${riderContext.speedKmh.toInt()} km/h",
            "Heading" to "${riderContext.heading.toInt()}°",
            "Navigation Status" to navStatus
        )

        val dispatchResult = dispatcher.dispatch(trimmed, contextMap, recentHistory)
        val finalResult = when (dispatchResult) {
            is ModularSemanticDispatcher.DispatchResult.Action -> {
                IntentResult.Action(dispatchResult.action, dispatchResult.target, dispatchResult.rawResponse)
            }
            is ModularSemanticDispatcher.DispatchResult.Conversation -> {
                IntentResult.Conversational(dispatchResult.text)
            }
            is ModularSemanticDispatcher.DispatchResult.Error -> {
                fastHeuristic(trimmed, riderContext)
            }
        }

        lastResult = finalResult
        return@withContext finalResult
    }

    private fun fastHeuristic(prompt: String, riderContext: RiderContext = RiderContext()): IntentResult {
        var cleaned = prompt.lowercase(Locale.ROOT).trim()

        // Strip conversational prefixes and polite wrappers
        val prefixes = listOf(
            "can you please ", "could you please ", "can you kindly ", "can you ", "could you ",
            "please ", "kindly ", "paki ", "pakisuyo ", "hey gabay ", "gabay "
        )
        for (prefix in prefixes) {
            if (cleaned.startsWith(prefix)) {
                cleaned = cleaned.removePrefix(prefix).trim()
            }
        }

        // Strip conversational suffixes
        val suffixes = listOf(" right now", " now", " please", " for me", " asap", " po")
        for (suffix in suffixes) {
            if (cleaned.endsWith(suffix)) {
                cleaned = cleaned.removeSuffix(suffix).trim()
            }
        }

        // Autocorrect common abbreviations/typos
        cleaned = cleaned.replace("nust", "neust")

        // Handle Campus selection conversationally
        if (cleaned.contains("campus") && (cleaned.contains("change") || cleaned.contains("switch") || cleaned.contains("set") || cleaned == "campus")) {
            return IntentResult.Conversational("Active campus is NEUST - San Isidro Campus. Available campuses: San Isidro, Sumacab, General Tinio (Main), Atate, Peñaranda, Fort Magsaysay. Say \"route to [campus]\" to navigate.")
        }

        return when {
            cleaned.contains("where am i") || cleaned.contains("what road") ||
                cleaned.contains("where are we") || cleaned.contains("current location") ||
                cleaned.contains("saan ako") || cleaned.contains("nasaan tayo") || cleaned.contains("anong kalsada") -> {
                IntentResult.Action("where_am_i")
            }
            cleaned.contains("disengage") || cleaned.contains("cancel") || cleaned.contains("stop") ||
                cleaned.contains("end route") || cleaned.contains("clear route") || cleaned.contains("turn off navigation") -> IntentResult.Action("cancel")
            cleaned.contains("satellite") || cleaned.contains("sat view") || cleaned.contains("sat mode") -> IntentResult.Action("satellite")
            cleaned.contains("street view") || cleaned.contains("street mode") || cleaned.contains("street") || cleaned.contains("vector") -> IntentResult.Action("street")
            cleaned.contains("topo") || cleaned.contains("terrain") || cleaned.contains("contour") -> IntentResult.Action("topo")
            cleaned.contains("3d") || cleaned.contains("tilt") || cleaned.contains("perspective") || cleaned.contains("driving view") -> IntentResult.Action("3d")
            cleaned.contains("2d") || cleaned.contains("flat") || cleaned.contains("overhead") || cleaned.contains("top down") -> IntentResult.Action("2d")
            cleaned.contains("north") -> IntentResult.Action("north")
            cleaned.contains("follow") || cleaned.contains("recenter") || cleaned.contains("magnet") || cleaned.contains("track me") -> IntentResult.Action("follow")

            // Navigation commands with dynamic destination extraction
            cleaned.contains("navigate to ") || cleaned.contains("go to ") || cleaned.contains("take me to ") ||
                cleaned.contains("head to ") || cleaned.contains("drive to ") || cleaned.contains("route to ") ||
                cleaned.contains("directions to ") || cleaned.contains("lead me to ") ||
                cleaned.contains("bring me to ") || cleaned.contains("pumunta sa ") || cleaned.contains("dalhin mo ako sa ") -> {
                val verbs = listOf("navigate to ", "go to ", "take me to ", "head to ", "drive to ", "route to ", "directions to ", "lead me to ", "bring me to ", "pumunta sa ", "dalhin mo ako sa ")
                var dest = cleaned
                for (v in verbs) {
                    val idx = dest.indexOf(v)
                    if (idx != -1) {
                        dest = dest.substring(idx + v.length).trim()
                        break
                    }
                }
                dest = dest.removeSuffix(" right now").removeSuffix(" now").removeSuffix(" please").trim()
                IntentResult.Action("navigate", dest)
            }

            cleaned.contains("neust") -> IntentResult.Action("navigate", cleaned)
            cleaned.contains("gas") || cleaned.contains("fuel") || cleaned.contains("petron") || cleaned.contains("shell") || cleaned.contains("caltex") -> IntentResult.Action("search", "Gas")
            cleaned.contains("restaurant") || cleaned.contains("food") || cleaned.contains("jollibee") || cleaned.contains("kain") || cleaned.contains("eat") -> IntentResult.Action("search", "Restaurant")
            cleaned.contains("hospital") || cleaned.contains("emergency") || cleaned.contains("clinic") || cleaned.contains("pharmacy") -> IntentResult.Action("search", "Hospital")
            cleaned.contains("school") || cleaned.contains("university") || cleaned.contains("college") -> IntentResult.Action("search", cleaned)

            else -> {
                // If it looks like a place query (e.g. "neust san isidro"), route to it directly
                if (cleaned.length >= 3 && !cleaned.contains("hello") && !cleaned.contains("hi") && !cleaned.contains("how") && !cleaned.contains("who")) {
                    IntentResult.Action("navigate", cleaned)
                } else {
                    IntentResult.Conversational("Gabay offline co-pilot ready. Speak a destination, NEUST campus, map view command, or ask \"where am I\".")
                }
            }
        }
    }
}

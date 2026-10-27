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

        // Check if server is reachable
        if (!isPortOpen(SERVER_PORT)) {
            Log.w(TAG, "llama-server not ready on port $SERVER_PORT, using fast heuristic")
            val fallback = fastHeuristic(trimmed, riderContext)
            lastResult = fallback
            return@withContext fallback
        }

        // Call on-device Qwen2.5-0.5B-Instruct model with calibrated few-shot context
        try {
            val navStatus = if (riderContext.isNavigating) {
                "En route to ${riderContext.destinationName} (${String.format(Locale.US, "%.1f", riderContext.remainingDistanceKm)} km remaining)"
            } else {
                "Free exploration (no active navigation route)"
            }

            val systemPrompt = "You are Gabay, an offline motorcycle navigation co-pilot in the Philippines.\n" +
                "You provide concise, clear, and direct vehicular voice guidance.\n" +
                "Never use generic AI filler, marketing fluff, or robotic sci-fi phrases.\n\n" +
                "LIVE RIDER CONTEXT:\n" +
                "- Current Road: ${riderContext.currentRoad}\n" +
                "- Coordinates: Lat ${String.format(Locale.US, "%.4f", riderContext.latitude)}, Lon ${String.format(Locale.US, "%.4f", riderContext.longitude)}\n" +
                "- Speed: ${riderContext.speedKmh.toInt()} km/h | Heading: ${riderContext.heading.toInt()}°\n" +
                "- Navigation Status: $navStatus\n\n" +
                "INSTRUCTIONS:\n" +
                "1. If the rider commands an action or navigation task, respond with a JSON object ONLY: {\"action\": \"...\", \"target\": \"...\"}.\n" +
                "   Supported actions: \"where_am_i\", \"navigate\", \"search\", \"3d\", \"2d\", \"cancel\", \"follow\", \"north\".\n" +
                "   Examples:\n" +
                "   - \"Where am I?\" / \"What road is this?\" -> {\"action\": \"where_am_i\"}\n" +
                "   - \"Take me to Gapan City\" -> {\"action\": \"navigate\", \"target\": \"Gapan City\"}\n" +
                "   - \"Find gas station\" -> {\"action\": \"search\", \"target\": \"Gas\"}\n" +
                "   - \"Switch to 3D\" -> {\"action\": \"3d\"}\n" +
                "   - \"Cancel route\" -> {\"action\": \"cancel\"}\n" +
                "2. For ANY other question, conversation, motorcycle advice, tire pressure, or road rules, reply in 1 to 2 clear, natural sentences.\n" +
                "3. NEVER output JSON for conversational questions."

            val messagesArray = org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })

                // Inject recent conversational history for multi-turn context
                for (turn in recentHistory) {
                    val content = turn.second.trim()
                    if (content.isNotEmpty()) {
                        put(JSONObject().apply {
                            val role = if (turn.first.equals("RIDER", ignoreCase = true) || turn.first.equals("You", ignoreCase = true)) "user" else "assistant"
                            put("role", role)
                            put("content", content)
                        })
                    }
                }

                put(JSONObject().apply {
                    put("role", "user")
                    put("content", trimmed)
                })
            }

            val requestJson = JSONObject().apply {
                put("messages", messagesArray)
                put("temperature", 0.2)
                put("max_tokens", 128)
            }

            val conn = (URL(SERVER_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 4000
                readTimeout = 15000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(requestJson.toString())
                writer.flush()
            }

            if (conn.responseCode == 200) {
                val responseText = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val root = JSONObject(responseText)
                val content = root.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim()

                Log.d(TAG, "LLM Response: $content")

                // Check if model returned a JSON action
                val jsonStart = content.indexOf('{')
                val jsonEnd = content.lastIndexOf('}')
                if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                    try {
                        val parsed = JSONObject(content.substring(jsonStart, jsonEnd + 1))
                        val action = parsed.optString("action", "")
                        val target = parsed.optString("target", "")
                        if (action.isNotEmpty()) {
                            val actionResult = IntentResult.Action(action, target, content)
                            lastResult = actionResult
                            return@withContext actionResult
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "JSON parse fallback: $content")
                    }
                }

                // Otherwise, general conversational answer from the model
                val convResult = IntentResult.Conversational(content)
                lastResult = convResult
                return@withContext convResult
            } else {
                Log.w(TAG, "Server error code: ${conn.responseCode}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Inference exception", e)
        }

        val fallback = fastHeuristic(trimmed, riderContext)
        lastResult = fallback
        return@withContext fallback
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
                cleaned.contains("bring me to ") || cleaned.contains("pumunta sa ") -> {
                val verbs = listOf("navigate to ", "go to ", "take me to ", "head to ", "drive to ", "route to ", "bring me to ", "pumunta sa ")
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

            cleaned.contains("gas") || cleaned.contains("fuel") || cleaned.contains("petron") || cleaned.contains("shell") || cleaned.contains("caltex") -> IntentResult.Action("search", "Gas")
            cleaned.contains("restaurant") || cleaned.contains("food") || cleaned.contains("jollibee") || cleaned.contains("kain") || cleaned.contains("eat") -> IntentResult.Action("search", "Restaurant")
            cleaned.contains("hospital") || cleaned.contains("emergency") || cleaned.contains("clinic") || cleaned.contains("pharmacy") -> IntentResult.Action("search", "Hospital")
            cleaned.contains("school") || cleaned.contains("university") || cleaned.contains("college") || cleaned.contains("campus") -> IntentResult.Action("search", cleaned)

            else -> {
                // If it looks like a place query (e.g. "neust san isidro"), route to it directly
                if (cleaned.length >= 3 && !cleaned.contains("hello") && !cleaned.contains("hi") && !cleaned.contains("how")) {
                    IntentResult.Action("navigate", cleaned)
                } else {
                    IntentResult.Conversational("Co-pilot ready. Speak a destination, map command (e.g. street view, 3D), or search.")
                }
            }
        }
    }
}

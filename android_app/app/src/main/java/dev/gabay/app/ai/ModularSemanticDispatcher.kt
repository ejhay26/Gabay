package dev.gabay.app.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * ModularSemanticDispatcher: A standalone, reusable AI Intent & Context Dispatcher.
 *
 * Instead of hardcoding text substring matches (e.g. "if query contains 'gas'"),
 * this engine instructs an LLM to semantically understand rider/user context and
 * map queries to registered actions or natural conversation.
 *
 * It is completely modular:
 * - Can be used in Gabay or copied into any future Android/Kotlin application.
 * - Works with local on-device models, desktop llama.cpp servers, or cloud OpenAI-compatible endpoints.
 * - Built-in fuzzy semantic heuristic fallback if the LLM is unreachable.
 */
class ModularSemanticDispatcher(
    private var endpointUrl: String = "http://127.0.0.1:8080/v1/chat/completions",
    private var systemRole: String = "You are a terse, responsive voice assistant.",
    private val connectTimeoutMs: Int = 3000,
    private val readTimeoutMs: Int = 12000
) {
    companion object {
        private const val TAG = "SemanticDispatcher"
    }

    data class ActionDefinition(
        val name: String,
        val description: String,
        val targetDescription: String? = null,
        val examples: List<String> = emptyList(),
        val keywords: List<String> = emptyList()
    )

    sealed class DispatchResult {
        data class Action(
            val action: String,
            val target: String = "",
            val rawResponse: String = ""
        ) : DispatchResult()

        data class Conversation(
            val text: String
        ) : DispatchResult()

        data class Error(
            val message: String,
            val fallbackText: String = ""
        ) : DispatchResult()
    }

    private val actions = mutableMapOf<String, ActionDefinition>()

    /**
     * Register a new action dynamically without hardcoding logic.
     */
    fun registerAction(
        name: String,
        description: String,
        targetDescription: String? = null,
        examples: List<String> = emptyList(),
        keywords: List<String> = emptyList()
    ): ModularSemanticDispatcher {
        actions[name.lowercase(Locale.ROOT)] = ActionDefinition(
            name = name,
            description = description,
            targetDescription = targetDescription,
            examples = examples,
            keywords = keywords
        )
        return this
    }

    fun setEndpoint(url: String) {
        endpointUrl = url
    }

    fun setSystemRole(role: String) {
        systemRole = role
    }

    /**
     * Dispatches user query using LLM reasoning against registered actions and context.
     */
    suspend fun dispatch(
        query: String,
        contextState: Map<String, Any> = emptyMap(),
        recentHistory: List<Pair<String, String>> = emptyList()
    ): DispatchResult = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return@withContext DispatchResult.Conversation("Ready. What would you like to do?")
        }

        // Try LLM Semantic Inference first
        try {
            val systemPrompt = buildSystemPrompt(contextState)
            val requestJson = buildRequestJson(systemPrompt, trimmed, recentHistory)

            val conn = (URL(endpointUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
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

                return@withContext parseModelResponse(content, trimmed)
            } else {
                Log.w(TAG, "Server responded with code ${conn.responseCode}, falling back to fuzzy semantic match")
            }
        } catch (e: Exception) {
            Log.d(TAG, "LLM endpoint unreachable (${e.message}), executing semantic heuristic fallback")
        }

        // Offline / Fast Heuristic Fallback based on registered actions & keywords
        return@withContext executeFuzzyHeuristic(trimmed)
    }

    private fun buildSystemPrompt(contextState: Map<String, Any>): String {
        val sb = StringBuilder()
        sb.append(systemRole).append("\n\n")

        if (contextState.isNotEmpty()) {
            sb.append("CURRENT RUNTIME CONTEXT:\n")
            for ((k, v) in contextState) {
                sb.append("- ").append(k).append(": ").append(v).append("\n")
            }
            sb.append("\n")
        }

        sb.append("COMMAND INSTRUCTIONS:\n")
        sb.append("1. If the user command matches or implies any registered action, respond with JSON ONLY: {\"action\": \"...\", \"target\": \"...\"}\n")
        sb.append("   Registered Actions:\n")

        for (act in actions.values) {
            sb.append("   - \"").append(act.name).append("\": ").append(act.description)
            if (act.targetDescription != null) {
                sb.append(" (Target: ").append(act.targetDescription).append(")")
            }
            sb.append("\n")
        }

        sb.append("\n   Action Examples:\n")
        for (act in actions.values) {
            for (ex in act.examples.take(2)) {
                sb.append("   - \"").append(ex).append("\" -> {\"action\": \"").append(act.name).append("\"")
                if (act.targetDescription != null) {
                    sb.append(", \"target\": \"...\"")
                }
                sb.append("}\n")
            }
        }

        sb.append("\n2. If the user asks a conversational question, advice, or chit-chat, respond naturally in 1-2 concise sentences without JSON.\n")
        sb.append("3. Output clean JSON without markdown code fences for commands.")

        return sb.toString()
    }

    private fun buildRequestJson(
        systemPrompt: String,
        query: String,
        history: List<Pair<String, String>>
    ): JSONObject {
        val messages = JSONArray()
        messages.put(JSONObject().apply {
            put("role", "system")
            put("content", systemPrompt)
        })

        for (turn in history) {
            val text = turn.second.trim()
            if (text.isNotEmpty()) {
                val role = if (turn.first.equals("user", ignoreCase = true) || turn.first.equals("rider", ignoreCase = true)) "user" else "assistant"
                messages.put(JSONObject().apply {
                    put("role", role)
                    put("content", text)
                })
            }
        }

        messages.put(JSONObject().apply {
            put("role", "user")
            put("content", query)
        })

        return JSONObject().apply {
            put("messages", messages)
            put("temperature", 0.2)
            put("max_tokens", 160)
        }
    }

    private fun parseModelResponse(content: String, query: String): DispatchResult {
        val clean = content
            .replace("<tool_call>", "")
            .replace("</tool_call>", "")
            .replace("```json", "")
            .replace("```", "")
            .trim()

        val jsonStart = clean.indexOf('{')
        val jsonEnd = clean.lastIndexOf('}')

        if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
            try {
                val jsonStr = clean.substring(jsonStart, jsonEnd + 1)
                val json = JSONObject(jsonStr)

                // Read action or tool name
                var actionName = json.optString("action", "")
                    .ifEmpty { json.optString("name", "") }
                    .ifEmpty { json.optString("function", "") }
                    .trim()
                    .lowercase(Locale.ROOT)

                // Read target parameter
                var target = json.optString("target", "").trim()
                if (target.isEmpty()) {
                    val args = json.optJSONObject("arguments")
                    if (args != null) {
                        target = args.optString("target", "")
                            .ifEmpty { args.optString("destination", "") }
                            .ifEmpty { args.optString("query", "") }
                            .ifEmpty { args.optString("location", "") }
                            .trim()
                    }
                }

                // Normalize action aliases common in LLM tool calling
                actionName = when (actionName) {
                    "get_info", "info", "status", "location", "my_location", "check_status" -> "where_am_i"
                    "route", "directions", "drive", "go_to", "start_navigation" -> "navigate"
                    "find", "find_poi", "nearby", "search_poi", "search_place" -> "search"
                    "cancel_route", "stop_route", "end_navigation", "abort" -> "cancel"
                    "re_center", "center", "recenter_camera" -> "recenter"
                    "street_view", "vector_view" -> "street"
                    "sat_view", "satellite_view" -> "satellite"
                    "topo_view", "topography", "elevation" -> "topo"
                    else -> actionName
                }

                if (actions.containsKey(actionName)) {
                    return DispatchResult.Action(action = actionName, target = target, rawResponse = clean)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse JSON candidate: $clean", e)
            }

            // If it looked like raw JSON tool call but couldn't be mapped, NEVER leak JSON to chat
            return executeFuzzyHeuristic(query)
        }

        // If the model output still looks like raw unparsed JSON or tool tags, fall back
        if (clean.startsWith("{") || clean.startsWith("<")) {
            return executeFuzzyHeuristic(query)
        }

        // Clean conversational natural language
        return DispatchResult.Conversation(clean)
    }

    /**
     * Fuzzy matching when LLM is unavailable: checks registered keywords and sample phrases.
     */
    private fun executeFuzzyHeuristic(query: String): DispatchResult {
        val qLower = query.lowercase(Locale.ROOT)

        var bestAction: String? = null
        var bestScore = 0

        for ((name, act) in actions) {
            // Check keyword matches
            for (kw in act.keywords) {
                if (qLower.contains(kw.lowercase(Locale.ROOT))) {
                    bestAction = name
                    bestScore += 2
                }
            }

            // Check example matches
            for (ex in act.examples) {
                val words = ex.lowercase(Locale.ROOT).split(" ").filter { it.length > 3 }
                val matchCount = words.count { qLower.contains(it) }
                if (matchCount > 0 && matchCount * 2 > bestScore) {
                    bestAction = name
                    bestScore = matchCount * 2
                }
            }
        }

        if (bestAction != null) {
            return DispatchResult.Action(action = bestAction, target = query)
        }

        return DispatchResult.Conversation("I heard: \"$query\". Say an action or ask for directions.")
    }
}

package dev.gabay.app.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class RecentDestination(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val subtitle: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis()
)

object RecentDestinationsManager {
    private const val PREFS_NAME = "gabay_recents_pref"
    private const val KEY_RECENTS = "recent_destinations_json"
    private const val MAX_RECENTS = 12

    fun getRecents(context: Context): List<RecentDestination> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_RECENTS, null)
        if (jsonStr.isNullOrBlank()) {
            return getDefaultRecents()
        }
        val list = mutableListOf<RecentDestination>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    RecentDestination(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = obj.getString("name"),
                        subtitle = obj.optString("subtitle", "Location"),
                        category = obj.optString("category", "place"),
                        latitude = obj.getDouble("latitude"),
                        longitude = obj.getDouble("longitude"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            return getDefaultRecents()
        }
        return if (list.isEmpty()) getDefaultRecents() else list
    }

    fun addRecent(
        context: Context,
        name: String,
        subtitle: String,
        category: String,
        latitude: Double,
        longitude: Double
    ) {
        val current = getRecents(context).toMutableList()
        // Deduplicate by name or coordinates within ~50m
        current.removeAll { 
            it.name.equals(name, ignoreCase = true) || 
            (Math.abs(it.latitude - latitude) < 0.0005 && Math.abs(it.longitude - longitude) < 0.0005)
        }
        val newItem = RecentDestination(
            name = name,
            subtitle = subtitle,
            category = category,
            latitude = latitude,
            longitude = longitude
        )
        current.add(0, newItem)
        val trimmed = current.take(MAX_RECENTS)
        saveList(context, trimmed)
    }

    fun clearRecents(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_RECENTS).apply()
    }

    fun removeRecent(context: Context, id: String) {
        val current = getRecents(context).filter { it.id != id }
        saveList(context, current)
    }

    private fun saveList(context: Context, list: List<RecentDestination>) {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("subtitle", item.subtitle)
                put("category", item.category)
                put("latitude", item.latitude)
                put("longitude", item.longitude)
                put("timestamp", item.timestamp)
            }
            array.put(obj)
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_RECENTS, array.toString()).apply()
    }

    private fun getDefaultRecents(): List<RecentDestination> {
        return listOf(
            RecentDestination(
                name = "San Isidro Town Plaza",
                subtitle = "Poblacion, San Isidro • Park & Landmark",
                category = "park",
                latitude = 15.3113,
                longitude = 120.9032
            ),
            RecentDestination(
                name = "San Isidro Municipal Hall",
                subtitle = "Poblacion, San Isidro • Government Office",
                category = "government",
                latitude = 15.3110,
                longitude = 120.9035
            ),
            RecentDestination(
                name = "Gapan City Public Market",
                subtitle = "Bayan, Gapan City • Commercial & Food",
                category = "market",
                latitude = 15.2968,
                longitude = 120.9525
            ),
            RecentDestination(
                name = "Dr. Gloria D. Lacson Hospital",
                subtitle = "Castellano, San Leonardo • Emergency Medical",
                category = "hospital",
                latitude = 15.3645,
                longitude = 120.9632
            ),
            RecentDestination(
                name = "NEUST - San Isidro Campus",
                subtitle = "Poblacion, San Isidro • State University",
                category = "school",
                latitude = 15.3101,
                longitude = 120.9038
            )
        )
    }
}

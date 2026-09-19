package com.aura.defense.assistant

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Persistent, redacted timeline of real AURA events. */
class AssistantTimelineStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun entries(): List<AssistantTimelineEntry> = runCatching {
        val json = JSONArray(preferences.getString(KEY, "[]"))
        (0 until json.length()).mapNotNull { index -> json.optJSONObject(index)?.toEntry() }.takeLast(MAX_ENTRIES)
    }.getOrDefault(emptyList())

    @Synchronized
    fun record(kind: String, source: String, evidence: String, result: String, timestamp: Long = System.currentTimeMillis()): Boolean = runCatching {
        val updated = (entries() + AssistantTimelineEntry(timestamp, kind, source, evidence.take(240), result.take(240))).takeLast(MAX_ENTRIES)
        preferences.edit().putString(KEY, JSONArray().apply { updated.forEach { put(it.toJson()) } }.toString()).commit()
    }.getOrDefault(false)

    @Synchronized
    fun clear(): Boolean = preferences.edit().remove(KEY).commit()

    @Synchronized
    fun clearKind(kind: String): Boolean {
        val remaining = entries().filterNot { it.kind == kind }
        return preferences.edit().putString(KEY, JSONArray().apply { remaining.forEach { put(it.toJson()) } }.toString()).commit()
    }

    private fun JSONObject.toEntry() = AssistantTimelineEntry(optLong("timestamp"), optString("kind"), optString("source"), optString("evidence"), optString("result"))
    private fun AssistantTimelineEntry.toJson() = JSONObject().apply {
        put("timestamp", timestamp)
        put("kind", kind)
        put("source", source)
        put("evidence", evidence)
        put("result", result)
    }

    private companion object {
        const val NAME = "aura_assistant_timeline"
        const val KEY = "entries"
        const val MAX_ENTRIES = 200
    }
}

data class AssistantTimelineEntry(
    val timestamp: Long,
    val kind: String,
    val source: String,
    val evidence: String,
    val result: String
)

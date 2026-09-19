package com.aura.defense.assistant

import android.content.Context
import com.aura.defense.data.SecurePrefs
import org.json.JSONArray
import org.json.JSONObject

/** Persistent, redacted timeline of real AURA events. */
class AssistantTimelineStore(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = SecurePrefs.encryptedOrNull(appContext)

    init {
        migrateLegacy()
    }

    @Synchronized
    fun entries(): List<AssistantTimelineEntry> = runCatching {
        val store = preferences ?: return emptyList()
        val json = JSONArray(store.getString(TIMELINE_KEY, "[]"))
        (0 until json.length()).mapNotNull { index -> json.optJSONObject(index)?.toEntry() }.takeLast(MAX_ENTRIES)
    }.getOrDefault(emptyList())

    @Synchronized
    fun record(kind: String, source: String, evidence: String, result: String, timestamp: Long = System.currentTimeMillis()): Boolean = runCatching {
        val updated = (entries() + AssistantTimelineEntry(timestamp, kind, source, evidence.take(240), result.take(240))).takeLast(MAX_ENTRIES)
        val store = preferences ?: return@runCatching false
        store.edit().putString(TIMELINE_KEY, JSONArray().apply { updated.forEach { put(it.toJson()) } }.toString()).commit()
    }.getOrDefault(false)

    @Synchronized
    fun clear(): Boolean = preferences?.edit()?.remove(TIMELINE_KEY)?.commit() ?: false

    @Synchronized
    fun clearKind(kind: String): Boolean {
        val remaining = entries().filterNot { it.kind == kind }
        val store = preferences ?: return false
        return store.edit().putString(TIMELINE_KEY, JSONArray().apply { remaining.forEach { put(it.toJson()) } }.toString()).commit()
    }

    fun isEncryptedAvailable(): Boolean = preferences != null

    private fun migrateLegacy() {
        val secure = preferences ?: return
        if (secure.contains(TIMELINE_KEY)) return
        val legacy = appContext.getSharedPreferences(NAME, Context.MODE_PRIVATE).getString(KEY, null) ?: return
        if (secure.edit().putString(TIMELINE_KEY, legacy).commit()) {
            appContext.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().remove(KEY).commit()
        }
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
        const val TIMELINE_KEY = "assistant_timeline_entries"
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

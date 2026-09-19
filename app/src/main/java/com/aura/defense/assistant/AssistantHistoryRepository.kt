package com.aura.defense.assistant

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class AssistantHistoryRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("aura_assistant", Context.MODE_PRIVATE)

    fun entries(): List<AssistantHistoryEntry> = runCatching {
        val array = JSONArray(preferences.getString(KEY, "[]"))
        (0 until array.length()).mapNotNull { index -> array.optJSONObject(index)?.toEntry() }.takeLast(MAX_ENTRIES)
    }.getOrDefault(emptyList())

    fun add(entry: AssistantHistoryEntry): Boolean = runCatching {
        val updated = (entries() + entry).takeLast(MAX_ENTRIES)
        preferences.edit().putString(KEY, JSONArray().apply { updated.forEach { put(it.toJson()) } }.toString()).commit()
    }.getOrDefault(false)

    fun clear(): Boolean = preferences.edit().remove(KEY).commit()

    private fun JSONObject.toEntry() = AssistantHistoryEntry(optLong("timestamp"), optString("kind"), optString("summary"), optString("result"))
    private fun AssistantHistoryEntry.toJson() = JSONObject().apply {
        put("timestamp", timestamp); put("kind", kind); put("summary", summary.take(160)); put("result", result.take(160))
    }

    private companion object { const val KEY = "history"; const val MAX_ENTRIES = 100 }
}
package com.aura.defense.assistant

import android.content.Context
import com.aura.defense.data.SecurePrefs
import org.json.JSONArray
import org.json.JSONObject

class AssistantHistoryRepository(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = SecurePrefs.encryptedOrNull(appContext)

    init {
        migrateLegacy()
    }

    fun entries(): List<AssistantHistoryEntry> = runCatching {
        val store = preferences ?: return emptyList()
        val array = JSONArray(store.getString(KEY, "[]"))
        (0 until array.length()).mapNotNull { index -> array.optJSONObject(index)?.toEntry() }.takeLast(MAX_ENTRIES)
    }.getOrDefault(emptyList())

    fun add(entry: AssistantHistoryEntry): Boolean = runCatching {
        val updated = (entries() + entry).takeLast(MAX_ENTRIES)
        val store = preferences ?: return@runCatching false
        store.edit().putString(KEY, JSONArray().apply { updated.forEach { put(it.toJson()) } }.toString()).commit()
    }.getOrDefault(false)

    fun clear(): Boolean = preferences?.edit()?.remove(KEY)?.commit() ?: false

    fun isEncryptedAvailable(): Boolean = preferences != null

    private fun migrateLegacy() {
        val secure = preferences ?: return
        if (secure.contains(KEY)) return
        val legacy = appContext.getSharedPreferences("aura_assistant", Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return
        if (secure.edit().putString(KEY, legacy).commit()) {
            appContext.getSharedPreferences("aura_assistant", Context.MODE_PRIVATE).edit().remove(KEY).commit()
        }
    }

    private fun JSONObject.toEntry() = AssistantHistoryEntry(optLong("timestamp"), optString("kind"), optString("summary"), optString("result"))
    private fun AssistantHistoryEntry.toJson() = JSONObject().apply {
        put("timestamp", timestamp); put("kind", kind); put("summary", summary.take(160)); put("result", result.take(160))
    }

    private companion object { const val KEY = "history"; const val MAX_ENTRIES = 100 }
}
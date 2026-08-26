package com.aura.defense.history

import android.content.Context
import com.aura.defense.vault.AuraVault
import com.aura.defense.vault.AuraVaultReadStatus
import org.json.JSONArray
import org.json.JSONObject

class AuraHistoryStore(context: Context) {
    private val preferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    private val vault = AuraVault(context)

    fun baseline(): AuraBaseline? = runCatching {
        preferences.getString(BASELINE_KEY, null)?.let(::baselineFromJson)
    }.getOrNull()

    fun baselineTimestamp(): String = baseline()?.timestamp ?: "No disponible"

    fun saveBaseline(value: AuraBaseline): Boolean = runCatching {
        preferences.edit().putString(BASELINE_KEY, baselineToJson(value).toString()).commit()
    }.getOrDefault(false)

    fun getEntries(): List<AuraHistoryEntry> = runCatching {
        val encrypted = vault.readSummaryResult()
        val encryptedEntries = if (encrypted.status == AuraVaultReadStatus.CONTENT) {
            encrypted.content.orEmpty().lineSequence().mapNotNull(AuraHistoryEntry::fromJson).toList()
        } else {
            emptyList()
        }
        val entries = encryptedEntries.ifEmpty {
            preferences.getString(HISTORY_KEY, "").orEmpty()
                .lineSequence().mapNotNull(AuraHistoryEntry::fromJson).toList()
        }
        entries.takeLast(MAX_ENTRIES)
    }.getOrDefault(emptyList())

    fun addEntries(entries: List<AuraHistoryEntry>): Boolean = runCatching {
        if (entries.isEmpty()) return@runCatching true
        val all = (getEntries() + entries).takeLast(MAX_ENTRIES)
        val serialized = all.joinToString("\n") { it.toJson() }
        if (vault.isAvailable() && vault.replaceSummary(serialized)) {
            preferences.edit().remove(HISTORY_KEY).apply()
        } else {
            preferences.edit().putString(HISTORY_KEY, serialized).commit()
        }
        true
    }.getOrDefault(false)

    fun clear(): Boolean = runCatching {
        vault.clear()
        preferences.edit().remove(HISTORY_KEY).remove(BASELINE_KEY).commit()
    }.getOrDefault(false)

    private fun baselineToJson(value: AuraBaseline) = JSONObject().apply {
        put("timestamp", value.timestamp)
        put("score", value.score)
        put("vpnActive", value.vpnActive)
        put("privateDnsStatus", value.privateDnsStatus)
        put("apps", JSONArray().also { array -> value.apps.forEach { (packageName, app) ->
            array.put(JSONObject().apply {
                put("packageName", packageName); put("versionName", app.versionName); put("targetSdk", app.targetSdk)
                put("permissions", JSONArray(app.permissions)); put("installer", app.installer); put("findings", JSONArray(app.findings))
            })
        } })
    }

    private fun baselineFromJson(raw: String): AuraBaseline {
        val json = JSONObject(raw)
        val appsJson = json.getJSONArray("apps")
        val apps = (0 until appsJson.length()).associate {
            val item = appsJson.getJSONObject(it)
            item.getString("packageName") to AppSnapshot(
                item.getString("versionName"), item.getInt("targetSdk"), item.getJSONArray("permissions").strings(),
                item.getString("installer"), item.getJSONArray("findings").strings()
            )
        }
        return AuraBaseline(json.getString("timestamp"), json.getInt("score"), json.getBoolean("vpnActive"), json.getString("privateDnsStatus"), apps)
    }

    private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }

    private companion object { const val NAME = "aura_history"; const val BASELINE_KEY = "baseline"; const val HISTORY_KEY = "entries"; const val MAX_ENTRIES = 100 }
}

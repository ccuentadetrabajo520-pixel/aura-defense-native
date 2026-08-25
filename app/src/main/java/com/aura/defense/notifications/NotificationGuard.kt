package com.aura.defense.notifications

import android.content.Context
import com.aura.defense.tools.LinkAnalysis
import com.aura.defense.tools.LinkRisk
import org.json.JSONArray
import org.json.JSONObject

data class NotificationAlert(
    val packageName: String,
    val timestamp: Long,
    val url: String,
    val analysis: LinkAnalysis
)

class NotificationAlertStore(context: Context) {
    private val preferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun add(alert: NotificationAlert) {
        val alerts = getAll().filterNot { it.packageName == alert.packageName && it.url == alert.url && alert.timestamp - it.timestamp < DEDUPE_WINDOW_MS }
            .takeLast(MAX_ALERTS - 1) + alert
        val json = JSONArray()
        alerts.forEach { item ->
            json.put(JSONObject().apply {
                put("paquete", item.packageName)
                put("fecha", item.timestamp)
                put("url", item.url)
                put("riesgo", item.analysis.risk.name)
                put("razones", JSONArray(item.analysis.reasons))
            })
        }
        preferences.edit().putString(KEY, json.toString()).apply()
    }

    @Synchronized
    fun getAll(): List<NotificationAlert> = runCatching {
        val json = JSONArray(preferences.getString(KEY, "[]"))
        (0 until json.length()).mapNotNull { index ->
            runCatching {
                val item = json.getJSONObject(index)
                val reasons = item.getJSONArray("razones").let { values -> (0 until values.length()).map(values::getString) }
                NotificationAlert(
                    packageName = item.getString("paquete"),
                    timestamp = item.getLong("fecha"),
                    url = item.getString("url"),
                    analysis = LinkAnalysis(item.getString("url"), LinkRisk.valueOf(item.getString("riesgo")), reasons)
                )
            }.getOrNull()
        }
    }.getOrDefault(emptyList())

    private companion object {
        const val NAME = "aura_notification_guard"
        const val KEY = "alertas"
        const val MAX_ALERTS = 50
        const val DEDUPE_WINDOW_MS = 10_000L
    }
}
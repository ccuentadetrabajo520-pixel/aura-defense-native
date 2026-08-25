package com.aura.defense.history

import org.json.JSONObject

data class AuraHistoryEntry(
    val timestamp: String,
    val eventType: String,
    val severity: String,
    val titleEs: String,
    val descriptionEs: String,
    val evidenceEs: String,
    val recommendedActionEs: String,
    val source: String
) {
    fun toJson(): String = JSONObject().apply {
        put("timestamp", timestamp)
        put("eventType", eventType)
        put("severity", severity)
        put("titleEs", titleEs)
        put("descriptionEs", descriptionEs)
        put("evidenceEs", evidenceEs)
        put("recommendedActionEs", recommendedActionEs)
        put("source", source)
    }.toString()

    companion object {
        fun fromJson(value: String): AuraHistoryEntry? = runCatching {
            val json = JSONObject(value)
            AuraHistoryEntry(
                json.getString("timestamp"), json.getString("eventType"), json.getString("severity"),
                json.getString("titleEs"), json.getString("descriptionEs"), json.getString("evidenceEs"),
                json.getString("recommendedActionEs"), json.getString("source")
            )
        }.getOrNull()
    }
}

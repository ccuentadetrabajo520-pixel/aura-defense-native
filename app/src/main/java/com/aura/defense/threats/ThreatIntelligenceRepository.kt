package com.aura.defense.threats

import android.content.Context
import org.json.JSONArray

class ThreatIntelligenceRepository(private val context: Context) {
    fun load(): List<ThreatIndicator> = runCatching {
        val content = context.assets.open(FILE_NAME).bufferedReader().use { it.readText() }
        val entries = JSONArray(content)
        (0 until entries.length()).mapNotNull { index ->
            runCatching { ThreatIndicator.fromJson(entries.getJSONObject(index)) }.getOrNull()
        }
    }.getOrDefault(emptyList())

    private companion object {
        const val FILE_NAME = "threats.json"
    }
}

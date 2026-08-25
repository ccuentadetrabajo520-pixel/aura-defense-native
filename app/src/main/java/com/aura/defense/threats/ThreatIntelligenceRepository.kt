package com.aura.defense.threats

import android.content.Context
import java.security.MessageDigest
import org.json.JSONObject
import org.json.JSONArray

data class ThreatIntelligenceSnapshot(
    val indicators: List<ThreatIndicator>,
    val version: String,
    val updatedAt: String,
    val source: String,
    val checksum: String?,
    val isUpdated: Boolean,
    val lastUpdateStatus: String
) {
    val indicatorCount: Int get() = indicators.size
    val isBundled: Boolean get() = !isUpdated
}

class ThreatIntelligenceRepository(private val context: Context) {
    private var snapshot: ThreatIntelligenceSnapshot = loadSnapshot()

    fun load(): List<ThreatIndicator> = snapshot.indicators
    fun current(): ThreatIntelligenceSnapshot = snapshot

    fun refresh(): ThreatIntelligenceSnapshot {
        if (THREAT_FEED_URL == null) {
            snapshot = loadSnapshot(lastUpdateStatus = "Actualización remota no configurada todavía.")
            return snapshot
        }
        val content = ThreatFeedService().download(THREAT_FEED_URL)
        val validated = content?.let(::validate)
        if (validated == null) {
            snapshot = loadSnapshot(lastUpdateStatus = INVALID_MESSAGE)
            return snapshot
        }
        val saved = runCatching {
            context.openFileOutput(CACHE_FILE, Context.MODE_PRIVATE).bufferedWriter().use { it.write(content) }
        }.isSuccess
        snapshot = if (saved) {
            validated.copy(isUpdated = true, lastUpdateStatus = "Base actualizada validada y guardada.")
        } else {
            loadSnapshot(lastUpdateStatus = "No se pudo guardar la actualización. Aura usará la base local incluida.")
        }
        return snapshot
    }

    fun restoreBundled(): ThreatIntelligenceSnapshot {
        runCatching { context.deleteFile(CACHE_FILE) }
        snapshot = loadBundled("Base local restaurada.")
        return snapshot
    }

    fun importUpdatedJson(content: String): ThreatIntelligenceSnapshot {
        val validated = validate(content)
        val bundled = loadBundled("Base incluida activa.")
        if (validated == null || validated.updatedAt < bundled.updatedAt) {
            snapshot = loadSnapshot(INVALID_MESSAGE)
            return snapshot
        }
        runCatching {
            context.openFileOutput(CACHE_FILE, Context.MODE_PRIVATE).bufferedWriter().use { it.write(content) }
        }.onFailure {
            snapshot = loadSnapshot("No se pudo guardar la actualización. Aura usará la base local incluida.")
            return snapshot
        }
        snapshot = validated.copy(isUpdated = true, lastUpdateStatus = "Base actualizada validada y guardada.")
        return snapshot
    }

    private fun loadSnapshot(lastUpdateStatus: String? = null): ThreatIntelligenceSnapshot {
        val bundled = loadBundled("Base incluida activa.")
        val cached = runCatching { context.openFileInput(CACHE_FILE).bufferedReader().use { it.readText() } }.getOrNull()
        if (cached != null) {
            validate(cached)?.let { metadata ->
                if (metadata.updatedAt >= bundled.updatedAt) {
                    return metadata.copy(isUpdated = true, lastUpdateStatus = lastUpdateStatus ?: "Base actualizada validada.")
                }
            }
        }
        return bundled.copy(lastUpdateStatus = lastUpdateStatus ?: if (cached == null) "Base incluida activa." else INVALID_MESSAGE)
    }

    private fun loadBundled(status: String): ThreatIntelligenceSnapshot {
        val bundled = runCatching { context.assets.open(FILE_NAME).bufferedReader().use { it.readText() } }.getOrNull()
        return bundled?.let { validate(it) }?.copy(isUpdated = false, lastUpdateStatus = status)
            ?: emptySnapshot(status)
    }

    private fun validate(content: String): ThreatIntelligenceSnapshot? = runCatching {
        if (content.toByteArray(Charsets.UTF_8).size > MAX_BYTES) return null
        val root = content.trim()
        val objectRoot = if (root.startsWith("{")) JSONObject(root) else null
        val entries = objectRoot?.optJSONArray("indicators")
            ?: objectRoot?.optJSONArray("threats")
            ?: JSONArray(root)
        if (entries.length() == 0) return null
        val indicators = (0 until entries.length()).map { index ->
            val value = entries.getJSONObject(index)
            val domain = value.optString("domain").trim().ifBlank { value.optString("indicator").trim() }
            if (domain.isBlank() || value.optString("category").trim().isBlank() ||
                value.optString("severity").trim().isBlank() || value.optString("source").trim().isBlank()) {
                error("Indicador incompleto")
            }
            val normalized = JSONObject(value.toString()).apply {
                put("indicator", domain.lowercase())
                put("indicatorType", optString("indicatorType", "DOMAIN").uppercase())
                put("category", optString("category").uppercase())
                put("severity", optString("severity").uppercase())
                put("id", optString("id", "threat-${domain.lowercase()}"))
                put("descriptionEs", optString("descriptionEs", "Indicador de inteligencia de amenazas."))
                put("updatedAt", optString("updatedAt", objectRoot?.optString("updatedAt", "") ?: ""))
            }
            ThreatIndicator.fromJson(normalized)
        }.distinctBy { it.indicator.lowercase() }
        if (indicators.isEmpty()) return null
        val checksum = objectRoot?.optString("checksum")?.takeIf { it.isNotBlank() }
        if (checksum != null && checksum != sha256(entries.toString())) return null
        ThreatIntelligenceSnapshot(
            indicators = indicators,
            version = objectRoot?.optString("version")?.takeIf { it.isNotBlank() } ?: "local-compatible",
            updatedAt = objectRoot?.optString("updatedAt")?.takeIf { it.isNotBlank() } ?: indicators.maxOf { it.updatedAt },
            source = objectRoot?.optString("source")?.takeIf { it.isNotBlank() } ?: indicators.first().source,
            checksum = checksum,
            isUpdated = false,
            lastUpdateStatus = "Base validada."
        )
    }.getOrNull()

    private fun emptySnapshot(status: String) = ThreatIntelligenceSnapshot(emptyList(), "No disponible", "No disponible", "No disponible", null, false, status)

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

    private companion object {
        const val FILE_NAME = "threats.json"
        const val CACHE_FILE = "threat_cache.json"
        const val MAX_BYTES = 2 * 1024 * 1024
        val THREAT_FEED_URL: String? = null
        const val INVALID_MESSAGE = "No se pudo validar la inteligencia de amenazas. Aura usará la base local incluida."
    }
}

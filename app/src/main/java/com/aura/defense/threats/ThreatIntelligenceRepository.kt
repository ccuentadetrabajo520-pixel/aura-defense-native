package com.aura.defense.threats

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

enum class ThreatRepositoryState {
    CURRENT,
    STALE,
    EXPIRED,
    UPDATING,
    FAILED,
    ROLLED_BACK
}

data class ThreatRepositorySnapshot(
    val state: ThreatRepositoryState,
    val feed: SignedThreatFeed?,
    val reason: String,
    val lastValidFeed: SignedThreatFeed? = null,
    val version: String? = feed?.version
)

data class ThreatIntelligenceSnapshot(
    val version: String = "local-compatible",
    val source: String = "Inteligencia local incluida",
    val updatedAt: String = "No disponible",
    val isUpdated: Boolean = false,
    val lastUpdateStatus: String = "Sin actualización",
    val isBundled: Boolean = true,
    val indicatorCount: Int = 0,
    val indicators: List<ThreatIndicator> = emptyList(),
    val current: List<ThreatIndicator> = indicators,
    val feed: SignedThreatFeed? = null
)

data class NetworkFetchResult(
    val responseCode: Int,
    val body: String,
    val etag: String? = null,
    val lastModified: String? = null
)

interface ThreatNetworkClient {
    fun fetch(url: String, etag: String? = null, lastModified: String? = null): NetworkFetchResult?
}

class ThreatIntelligenceRepository(
    private val context: Context? = null,
    private val publicKeyBase64: String = System.getenv("AURA_THREAT_PUBLIC_KEY") ?: "",
    private val manifestUrl: String = System.getenv("AURA_THREAT_MANIFEST_URL") ?: "",
    private val networkClient: ThreatNetworkClient = ThreatFeedService()
) {
    private val prefs = context?.getSharedPreferences("aura_threat_repository", Context.MODE_PRIVATE)
    private val tempDir by lazy { context?.let { File(it.filesDir, "threat_feed_tmp") } }
    private var currentState: ThreatRepositoryState = loadPersistedState()
    private var activeFeed: SignedThreatFeed? = loadPersistedFeed(ACTIVE_KEY)
    private var lastValidFeed: SignedThreatFeed? = loadPersistedFeed(LAST_VALID_KEY)

    fun activeFeed(): SignedThreatFeed? = activeFeed?.takeIf { it.expiresAt > System.currentTimeMillis() }
        ?: lastValidFeed?.takeIf { it.expiresAt > System.currentTimeMillis() }

    fun current(): ThreatIntelligenceSnapshot = snapshotForState(activeFeed(), currentState)

    fun restoreBundled(): ThreatIntelligenceSnapshot = snapshotForState(lastValidFeed ?: activeFeed(), currentState)

    fun state(): ThreatRepositoryState = currentState

    fun refresh(): ThreatRepositorySnapshot {
        currentState = ThreatRepositoryState.UPDATING
        persistState(currentState)

        if (publicKeyBase64.isBlank()) {
            currentState = ThreatRepositoryState.FAILED
            persistState(currentState)
            return snapshot(currentState, activeFeed(), "Clave pública Ed25519 no configurada.")
        }
        if (manifestUrl.isBlank()) {
            currentState = ThreatRepositoryState.FAILED
            persistState(currentState)
            return snapshot(currentState, activeFeed(), "URL del manifiesto no configurada.")
        }

        val previous = activeFeed() ?: lastValidFeed
        val result = networkClient.fetch(manifestUrl, prefs?.getString("last_etag", null), prefs?.getString("last_last_modified", null))
            ?: return snapshot(
                if (previous != null) ThreatRepositoryState.STALE else ThreatRepositoryState.FAILED,
                previous,
                if (previous != null) "No se pudo refrescar; se conserva el último feed válido." else "Descarga fallida y no hay último feed válido."
            )

        if (result.responseCode == 304) {
            currentState = if (previous != null && previous.expiresAt > System.currentTimeMillis()) ThreatRepositoryState.STALE else ThreatRepositoryState.EXPIRED
            persistState(currentState)
            return snapshot(currentState, previous, "Manifesto sin cambios.")
        }

        val validated = parseAndValidate(result.body)
        if (validated == null) {
            val fallback = activeFeed() ?: lastValidFeed
            currentState = if (fallback != null) ThreatRepositoryState.STALE else ThreatRepositoryState.FAILED
            persistState(currentState)
            return snapshot(currentState, fallback, "Feed remoto inválido; se conserva el último válido.")
        }

        if (previous != null && compareVersions(validated.version, previous.version) < 0) {
            currentState = ThreatRepositoryState.ROLLED_BACK
            persistState(currentState)
            return snapshot(currentState, previous, "Se rechaza un downgrade del feed válido actual.")
        }

        if (previous != null && previous.signature == validated.signature && previous.version == validated.version) {
            currentState = ThreatRepositoryState.ROLLED_BACK
            persistState(currentState)
            return snapshot(currentState, previous, "Se detecta replay tras reinicio o repetición del mismo feed.")
        }

        val committed = installValidatedFeed(validated)
        currentState = if (committed.expiresAt <= System.currentTimeMillis()) ThreatRepositoryState.EXPIRED else ThreatRepositoryState.CURRENT
        persistState(currentState)
        return snapshot(currentState, committed, if (currentState == ThreatRepositoryState.CURRENT) "Feed actualizado y activado." else "Feed actualizado pero expirado.")
    }

    fun updateFromSignedManifest(rawJson: String): ThreatRepositorySnapshot {
        val validated = parseAndValidate(rawJson)
            ?: return snapshot(
                if (activeFeed() != null) ThreatRepositoryState.STALE else ThreatRepositoryState.FAILED,
                activeFeed(),
                "Manifesto firmado inválido."
            )

        val previous = activeFeed() ?: lastValidFeed
        if (previous != null && compareVersions(validated.version, previous.version) < 0) {
            currentState = ThreatRepositoryState.ROLLED_BACK
            persistState(currentState)
            return snapshot(currentState, previous, "Se rechaza downgrade del feed.")
        }
        if (previous != null && previous.signature == validated.signature && previous.version == validated.version) {
            currentState = ThreatRepositoryState.ROLLED_BACK
            persistState(currentState)
            return snapshot(currentState, previous, "Replay detectado: mismo feed ya procesado.")
        }

        val committed = installValidatedFeed(validated)
        currentState = if (committed.expiresAt <= System.currentTimeMillis()) ThreatRepositoryState.EXPIRED else ThreatRepositoryState.CURRENT
        persistState(currentState)
        return snapshot(currentState, committed, if (currentState == ThreatRepositoryState.CURRENT) "Feed almacenado y activado." else "Feed expirado tras almacenamiento.")
    }

    fun restoreLastValid(): ThreatRepositorySnapshot {
        val fallback = lastValidFeed ?: activeFeed()
        if (fallback == null) {
            currentState = ThreatRepositoryState.FAILED
            persistState(currentState)
            return snapshot(currentState, null, "No hay feed válido previo para restaurar.")
        }
        val committed = installValidatedFeed(fallback)
        currentState = if (committed.expiresAt <= System.currentTimeMillis()) ThreatRepositoryState.EXPIRED else ThreatRepositoryState.CURRENT
        persistState(currentState)
        return snapshot(currentState, committed, "Se restaura el último feed válido.")
    }

    fun lastValidFeed(): SignedThreatFeed? = lastValidFeed ?: activeFeed()

    private fun installValidatedFeed(feed: SignedThreatFeed): SignedThreatFeed {
        tempDir?.mkdirs()
        val target = context?.let { File(it.filesDir, "threat_feed_active.json") }
        val atomicFile = context?.let { File(it.filesDir, "threat_feed_active.json.tmp") }
        val payload = jsonFor(feed)
        atomicFile?.writeText(payload)
        if (atomicFile != null && target != null) {
            atomicFile.renameTo(target)
        }

        activeFeed = feed
        lastValidFeed = feed
        prefs?.edit()
            ?.putString(ACTIVE_KEY, payload)
            ?.putString(LAST_VALID_KEY, payload)
            ?.putString("last_signature", feed.signature)
            ?.putString("last_version", feed.version)
            ?.putString("last_source", feed.source)
            ?.putString("last_etag", prefs?.getString("last_etag", null) ?: "")
            ?.putString("last_last_modified", prefs?.getString("last_last_modified", null) ?: "")
            ?.apply()
        return feed
    }

    private fun parseAndValidate(rawJson: String): SignedThreatFeed? {
        if (publicKeyBase64.isBlank()) return null
        if (rawJson.isBlank()) return null
        val objectJson = runCatching { JSONObject(rawJson.trim()) }.getOrNull() ?: return null
        val root = if (objectJson.has("feed")) objectJson.getJSONObject("feed") else objectJson

        val ruleId = root.optString("ruleId", "").trim()
        val source = root.optString("source", "").trim()
        val version = root.optString("version", "").trim()
        val evidence = root.optString("evidence", "").trim()
        val expiresAt = root.optLong("expiresAt", 0L)
        val checksum = root.optString("checksum", "").trim()
        val size = root.optLong("size", -1L)
        val signature = root.optString("signature", "").trim()
        val publicKeyId = root.optString("publicKeyId", "aura-ed25519-feed-v1").trim()

        if (ruleId.isBlank() || source.isBlank() || version.isBlank() || evidence.isBlank() || checksum.isBlank() || signature.isBlank()) return null
        if (expiresAt <= 0L || size <= 0L) return null

        val payload = listOf(
            ruleId,
            source,
            version,
            evidence,
            expiresAt.toString(),
            size.toString()
        ).joinToString("|")
        if (sha256Hex(payload) != checksum) return null

        val feed = SignedThreatFeed(
            ruleId = ruleId,
            source = source,
            version = version,
            evidence = evidence,
            expiresAt = expiresAt,
            checksum = checksum,
            size = size,
            signature = signature,
            publicKeyId = publicKeyId,
            feedTimestamp = System.currentTimeMillis()
        )

        val validation = SignedThreatFeedValidator(publicKeyBase64).validate(feed)
        if (!validation.valid) return null
        return feed
    }

    private fun loadPersistedFeed(key: String): SignedThreatFeed? = prefs?.getString(key, null)?.let(::parsePersistedFeed)

    private fun parsePersistedFeed(raw: String): SignedThreatFeed? = runCatching {
        val json = JSONObject(raw)
        SignedThreatFeed(
            ruleId = json.optString("ruleId", ""),
            source = json.optString("source", ""),
            version = json.optString("version", ""),
            evidence = json.optString("evidence", ""),
            expiresAt = json.optLong("expiresAt", 0L),
            checksum = json.optString("checksum", ""),
            size = json.optLong("size", 0L),
            signature = json.optString("signature", ""),
            publicKeyId = json.optString("publicKeyId", "aura-ed25519-feed-v1"),
            feedTimestamp = json.optLong("feedTimestamp", System.currentTimeMillis())
        )
    }.getOrNull()?.takeIf { it.ruleId.isNotBlank() }

    private fun snapshot(state: ThreatRepositoryState, feed: SignedThreatFeed?, reason: String): ThreatRepositorySnapshot =
        ThreatRepositorySnapshot(state = state, feed = feed, reason = reason, lastValidFeed = lastValidFeed ?: feed, version = feed?.version)

    private fun snapshotForState(feed: SignedThreatFeed?, state: ThreatRepositoryState): ThreatIntelligenceSnapshot {
        val active = feed ?: lastValidFeed
        val status = when (state) {
            ThreatRepositoryState.CURRENT -> "Feed activo y vigente"
            ThreatRepositoryState.STALE -> "Último feed válido conservado"
            ThreatRepositoryState.EXPIRED -> "Feed expirado"
            ThreatRepositoryState.UPDATING -> "Actualizando feed"
            ThreatRepositoryState.FAILED -> "Sin feed activo disponible"
            ThreatRepositoryState.ROLLED_BACK -> "Se rechazó el feed recibido"
        }
        val source = active?.source ?: "Inteligencia local incluida"
        val version = active?.version ?: "local-compatible"
        val updatedAt = active?.feedTimestamp?.let { java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date(it)) } ?: "No disponible"
        return ThreatIntelligenceSnapshot(
            version = version,
            source = source,
            updatedAt = updatedAt,
            isUpdated = active != null && active.expiresAt > System.currentTimeMillis(),
            lastUpdateStatus = status,
            isBundled = active == null,
            indicatorCount = 0,
            indicators = emptyList(),
            current = emptyList(),
            feed = active
        )
    }

    private fun loadPersistedState(): ThreatRepositoryState = when (prefs?.getString("state", null)) {
        "CURRENT" -> ThreatRepositoryState.CURRENT
        "STALE" -> ThreatRepositoryState.STALE
        "EXPIRED" -> ThreatRepositoryState.EXPIRED
        "UPDATING" -> ThreatRepositoryState.UPDATING
        "FAILED" -> ThreatRepositoryState.FAILED
        "ROLLED_BACK" -> ThreatRepositoryState.ROLLED_BACK
        else -> ThreatRepositoryState.FAILED
    }

    private fun persistState(state: ThreatRepositoryState) {
        prefs?.edit()?.putString("state", state.name)?.apply()
    }

    private fun jsonFor(feed: SignedThreatFeed): String = JSONObject().apply {
        put("ruleId", feed.ruleId)
        put("source", feed.source)
        put("version", feed.version)
        put("evidence", feed.evidence)
        put("expiresAt", feed.expiresAt)
        put("checksum", feed.checksum)
        put("size", feed.size)
        put("signature", feed.signature)
        put("publicKeyId", feed.publicKeyId)
        put("feedTimestamp", feed.feedTimestamp)
    }.toString()

    private fun compareVersions(left: String, right: String): Int {
        val leftParts = left.split("[.-]".toRegex()).filter { it.isNotBlank() }.mapNotNull { it.toIntOrNull() }
        val rightParts = right.split("[.-]".toRegex()).filter { it.isNotBlank() }.mapNotNull { it.toIntOrNull() }
        val max = maxOf(leftParts.size, rightParts.size)
        for (index in 0 until max) {
            val lv = leftParts.getOrNull(index) ?: 0
            val rv = rightParts.getOrNull(index) ?: 0
            if (lv != rv) return lv.compareTo(rv)
        }
        return 0
    }

    private fun sha256Hex(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private companion object {
        const val ACTIVE_KEY = "active_feed"
        const val LAST_VALID_KEY = "last_valid_feed"
    }
}

fun ThreatRepositorySnapshot.toThreatIntelligenceSnapshot(): ThreatIntelligenceSnapshot =
    ThreatIntelligenceSnapshot(
        version = feed?.version ?: "local-compatible",
        source = feed?.source ?: "Inteligencia local incluida",
        updatedAt = feed?.feedTimestamp?.let { java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date(it)) } ?: "No disponible",
        isUpdated = feed != null && feed.expiresAt > System.currentTimeMillis(),
        lastUpdateStatus = when (state) {
            ThreatRepositoryState.CURRENT -> "Feed activo y vigente"
            ThreatRepositoryState.STALE -> "Último feed válido conservado"
            ThreatRepositoryState.EXPIRED -> "Feed expirado"
            ThreatRepositoryState.UPDATING -> "Actualizando feed"
            ThreatRepositoryState.FAILED -> "Sin feed activo disponible"
            ThreatRepositoryState.ROLLED_BACK -> "Se rechazó el feed recibido"
        },
        isBundled = feed == null,
        indicatorCount = 0,
        indicators = emptyList(),
        current = emptyList(),
        feed = feed
    )

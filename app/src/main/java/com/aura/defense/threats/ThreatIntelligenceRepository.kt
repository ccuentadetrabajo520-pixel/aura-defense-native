package com.aura.defense.threats

import android.content.Context
import com.aura.defense.BuildConfig
import org.json.JSONArray
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

data class ThreatConfigResolution(
    val publicKeyBase64: String,
    val manifestUrl: String,
    val errors: List<String> = emptyList()
) {
    val isValid: Boolean get() = publicKeyBase64.isNotBlank() && manifestUrl.isNotBlank() && errors.isEmpty()
}

object ThreatConfigResolver {
    fun resolve(publicKey: String? = null, manifestUrl: String? = null): ThreatConfigResolution {
        val resolvedPublicKey = (publicKey ?: BuildConfig.AURA_THREAT_PUBLIC_KEY).orEmpty().trim()
        val resolvedManifestUrl = (manifestUrl ?: BuildConfig.AURA_THREAT_MANIFEST_URL).orEmpty().trim()
        val errors = buildList {
            if (resolvedPublicKey.isBlank()) add("AURA_THREAT_PUBLIC_KEY no configurada")
            if (resolvedManifestUrl.isBlank()) add("AURA_THREAT_MANIFEST_URL no configurada")
            else {
                val uri = runCatching { java.net.URI(resolvedManifestUrl) }.getOrNull()
                if (uri == null || uri.scheme != "https" || uri.host.isNullOrBlank()) {
                    add("AURA_THREAT_MANIFEST_URL debe ser HTTPS y contener host válido")
                }
            }
            if (resolvedPublicKey.isNotBlank()) {
                val base64 = runCatching { java.util.Base64.getDecoder().decode(resolvedPublicKey) }.getOrNull()
                if (base64 == null || base64.size < 32) add("AURA_THREAT_PUBLIC_KEY no es una clave pública Ed25519 Base64 válida")
            }
        }
        return ThreatConfigResolution(resolvedPublicKey, resolvedManifestUrl, errors)
    }
}

class ThreatIntelligenceRepository(
    private val context: Context? = null,
    private val publicKeyBase64: String = BuildConfig.AURA_THREAT_PUBLIC_KEY,
    private val manifestUrl: String = BuildConfig.AURA_THREAT_MANIFEST_URL,
    private val networkClient: ThreatNetworkClient = ThreatFeedService()
) {
    private val prefs = context?.getSharedPreferences("aura_threat_repository", Context.MODE_PRIVATE)
    private val tempDir by lazy { context?.let { File(it.filesDir, "threat_feed_tmp") } }
    private val config = ThreatConfigResolver.resolve(publicKeyBase64, manifestUrl)
    private var currentState: ThreatRepositoryState = loadPersistedState()
    private var activeFeed: SignedThreatFeed? = loadPersistedFeed(ACTIVE_KEY)
    private var lastValidFeed: SignedThreatFeed? = loadPersistedFeed(LAST_VALID_KEY)
    private var lastPersistedEtag: String? = prefs?.getString("last_etag", null)
    private var lastPersistedLastModified: String? = prefs?.getString("last_last_modified", null)
    private var atomicReplaceFails: Boolean = false

    fun activeFeed(): SignedThreatFeed? = activeFeed?.takeIf { it.expiresAt > System.currentTimeMillis() }
        ?: lastValidFeed?.takeIf { it.expiresAt > System.currentTimeMillis() }

    fun current(): ThreatIntelligenceSnapshot = snapshotForState(activeFeed(), currentState)

    fun restoreBundled(): ThreatIntelligenceSnapshot = snapshotForState(lastValidFeed ?: activeFeed(), currentState)

    fun state(): ThreatRepositoryState = currentState

    fun lastHttpEtag(): String? = lastPersistedEtag

    fun lastHttpLastModified(): String? = lastPersistedLastModified

    fun copyForTesting(atomicReplaceFails: Boolean = false): ThreatIntelligenceRepository {
        return ThreatIntelligenceRepository(
            context = context,
            publicKeyBase64 = publicKeyBase64,
            manifestUrl = manifestUrl,
            networkClient = networkClient
        ).apply {
            this.currentState = this@ThreatIntelligenceRepository.currentState
            this.activeFeed = this@ThreatIntelligenceRepository.activeFeed
            this.lastValidFeed = this@ThreatIntelligenceRepository.lastValidFeed
            this.lastPersistedEtag = this@ThreatIntelligenceRepository.lastPersistedEtag
            this.lastPersistedLastModified = this@ThreatIntelligenceRepository.lastPersistedLastModified
            this.atomicReplaceFails = atomicReplaceFails
        }
    }

    fun refresh(): ThreatRepositorySnapshot {
        currentState = ThreatRepositoryState.UPDATING
        persistState(currentState)

        if (!config.isValid) {
            currentState = ThreatRepositoryState.FAILED
            persistState(currentState)
            return snapshot(currentState, activeFeed(), config.errors.joinToString("; "))
        }

        val previous = activeFeed() ?: lastValidFeed
        val result = networkClient.fetch(manifestUrl, lastPersistedEtag, lastPersistedLastModified)
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

        val committed = installValidatedFeed(validated, result.etag, result.lastModified)
        if (currentState == ThreatRepositoryState.STALE) {
            persistState(currentState)
            return snapshot(currentState, committed, "No se pudo reemplazar el feed activo; se conserva el último válido.")
        }
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
        if (currentState == ThreatRepositoryState.STALE) {
            persistState(currentState)
            return snapshot(currentState, committed, "No se pudo reemplazar el feed activo; se conserva el último válido.")
        }
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

    private fun installValidatedFeed(feed: SignedThreatFeed, etag: String? = null, lastModified: String? = null): SignedThreatFeed {
        if (atomicReplaceFails) {
            currentState = ThreatRepositoryState.STALE
            persistState(currentState)
            if (lastValidFeed != null) {
                activeFeed = lastValidFeed
            }
            return lastValidFeed ?: feed
        }

        if (context != null) {
            val target = File(context.filesDir, "threat_feed_active.json")
            val atomicFile = File(context.filesDir, "threat_feed_active.json.tmp")
            val payload = jsonFor(feed)
            atomicFile.writeText(payload)
            val replaced = atomicFile.renameTo(target)
            if (!replaced) {
                currentState = ThreatRepositoryState.STALE
                persistState(currentState)
                return lastValidFeed ?: feed
            }
        }

        activeFeed = feed
        lastValidFeed = feed
        lastPersistedEtag = etag ?: lastPersistedEtag
        lastPersistedLastModified = lastModified ?: lastPersistedLastModified
        prefs?.edit()
            ?.putString(ACTIVE_KEY, jsonFor(feed))
            ?.putString(LAST_VALID_KEY, jsonFor(feed))
            ?.putString("last_signature", feed.signature)
            ?.putString("last_version", feed.version)
            ?.putString("last_source", feed.source)
            ?.putString("last_etag", lastPersistedEtag ?: "")
            ?.putString("last_last_modified", lastPersistedLastModified ?: "")
            ?.apply()
        return feed
    }

    private fun parseAndValidate(rawJson: String): SignedThreatFeed? {
        if (!config.isValid) return null
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

        val indicatorList = parseIndicators(root)
        val payload = SignedThreatFeed(
            ruleId = ruleId,
            source = source,
            version = version,
            evidence = evidence,
            expiresAt = expiresAt,
            checksum = checksum,
            size = size,
            signature = signature,
            publicKeyId = publicKeyId,
            indicators = indicatorList,
            feedTimestamp = System.currentTimeMillis()
        )

        val canonical = payload.canonicalPayloadString()
        if (sha256Hex(canonical) != checksum) return null
        if (size != canonical.toByteArray(Charsets.UTF_8).size.toLong()) return null
        if (payload.indicators.isNotEmpty()) {
            val indicatorIds = payload.indicators.map { it.id }
            if (indicatorIds.distinct().size != indicatorIds.size) return null
            if (payload.indicators.any { it.indicator.isBlank() }) return null
            val invalidType = payload.indicators.any { runCatching { ThreatIndicatorType.valueOf(it.indicatorType.name) }.isFailure }
            if (invalidType) return null
            val invalidDate = payload.indicators.any { it.updatedAt.isBlank() || runCatching { java.time.Instant.parse(it.updatedAt) }.isFailure }
            if (invalidDate) return null
        }

        val validation = SignedThreatFeedValidator(config.publicKeyBase64).validate(payload)
        if (!validation.valid) return null
        return payload
    }

    private fun parseIndicators(root: JSONObject): List<ThreatIndicator> {
        val items = root.optJSONArray("indicators")
        if (items != null) return parseIndicatorJsonArray(items)
        val indicatorContainer = root.optJSONObject("indicators")
        val nestedItems = indicatorContainer?.optJSONArray("items")
        if (nestedItems != null) return parseIndicatorJsonArray(nestedItems)
        return emptyList()
    }

    private fun parseIndicatorJsonArray(array: JSONArray): List<ThreatIndicator> = buildList {
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            val value = item.optString("indicator", "").trim()
            if (value.isBlank()) continue
            add(
                ThreatIndicator(
                    id = item.optString("id", "indicator-$index"),
                    indicator = value,
                    indicatorType = runCatching { ThreatIndicatorType.valueOf(item.optString("indicatorType", ThreatIndicatorType.DOMAIN.name)) }.getOrDefault(ThreatIndicatorType.DOMAIN),
                    category = runCatching { ThreatCategory.valueOf(item.optString("category", ThreatCategory.MALWARE.name)) }.getOrDefault(ThreatCategory.MALWARE),
                    severity = runCatching { ThreatSeverity.valueOf(item.optString("severity", ThreatSeverity.HIGH.name)) }.getOrDefault(ThreatSeverity.HIGH),
                    descriptionEs = item.optString("descriptionEs", "Señal del feed"),
                    source = item.optString("source", "aurafeed"),
                    updatedAt = item.optString("updatedAt", "1970-01-01T00:00:00Z")
                )
            )
        }
    }

    private fun loadPersistedFeed(key: String): SignedThreatFeed? = prefs?.getString(key, null)?.let { raw ->
        runCatching { parseAndValidate(raw) }.getOrNull()
    }

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
            indicators = parseIndicatorJsonArray(json.optJSONArray("indicators") ?: JSONArray()),
            feedTimestamp = json.optLong("feedTimestamp", System.currentTimeMillis())
        )
    }.getOrNull()?.takeIf { feed ->
        val validated = SignedThreatFeedValidator(config.publicKeyBase64).validate(feed)
        validated.valid && feed.ruleId.isNotBlank() && feed.signature.isNotBlank()
    }

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
            indicatorCount = active?.indicators?.size ?: 0,
            indicators = active?.indicators ?: emptyList(),
            current = active?.indicators ?: emptyList(),
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
        put("indicatorCount", feed.indicators.size)
        put("indicators", JSONArray().apply {
            feed.indicators.forEach { indicator ->
                put(JSONObject().apply {
                    put("id", indicator.id)
                    put("indicator", indicator.indicator)
                    put("indicatorType", indicator.indicatorType.name)
                    put("category", indicator.category.name)
                    put("severity", indicator.severity.name)
                    put("descriptionEs", indicator.descriptionEs)
                    put("source", indicator.source)
                    put("updatedAt", indicator.updatedAt)
                })
            }
        })
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
        indicatorCount = feed?.indicators?.size ?: 0,
        indicators = feed?.indicators ?: emptyList(),
        current = feed?.indicators ?: emptyList(),
        feed = feed
    )

package com.aura.defense.vpn

import android.content.Context
import com.aura.defense.data.SecurePrefs
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

enum class DnsFirewallProfile(val label: String, val categories: Set<String>) {
    EQUILIBRADO("Equilibrado", setOf("PHISHING", "MALWARE", "TRACKING", "FAKENEWS")),
    ESTRICTO("Estricto", setOf("PHISHING", "MALWARE", "TRACKING", "ADS", "CRYPTO_SCAM", "FAKENEWS")),
    PERMITIR_TODO("Permitir todo", emptySet());

    companion object {
        fun fromStored(value: String?): DnsFirewallProfile = entries.firstOrNull { it.name == value } ?: EQUILIBRADO
    }
}

data class DnsBlockedEvent(
    val domain: String,
    val category: String,
    val severity: String,
    val timestamp: Long,
    val reason: String = "threat_rule",
    val source: String = "UNKNOWN",
    val feedVersion: String = "UNKNOWN"
)

class DnsFirewallStore(context: Context) {
    private val preferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    private val securePreferences = SecurePrefs.get(context)

    fun profile(): DnsFirewallProfile = DnsFirewallProfile.fromStored(preferences.getString(PROFILE_KEY, null))

    fun saveProfile(profile: DnsFirewallProfile) {
        preferences.edit().putString(PROFILE_KEY, profile.name).apply()
    }

    fun allowlist(): List<String> = readAllowlist()

    fun blocklist(): List<String> = readBlocklist()

    fun addAllowlistedDomain(domain: String): Boolean {
        val normalized = normalizeDomain(domain) ?: return false
        val updated = (readAllowlist() + normalized).distinct().sorted()
        preferences.edit().putStringSet(ALLOWLIST_KEY, updated.toSet()).apply()
        return true
    }

    fun removeAllowlistedDomain(domain: String) {
        normalizeDomain(domain)?.let { normalized ->
            preferences.edit().putStringSet(ALLOWLIST_KEY, readAllowlist().filterNot { it == normalized }.toSet()).apply()
        }
    }

    fun addBlockedDomain(domain: String): Boolean {
        val normalized = normalizeDomain(domain) ?: return false
        val updated = (readBlocklist() + normalized).distinct().sorted()
        preferences.edit().putStringSet(BLOCKLIST_KEY, updated.toSet()).apply()
        return true
    }

    fun removeBlockedDomain(domain: String) {
        normalizeDomain(domain)?.let { normalized ->
            preferences.edit().putStringSet(BLOCKLIST_KEY, readBlocklist().filterNot { it == normalized }.toSet()).apply()
        }
    }

    fun clearSession() {
        securePreferences.edit().remove(EVENTS_KEY).remove(BLOCKED_COUNT_KEY).apply()
    }

    fun setServiceActive(active: Boolean) {
        preferences.edit().putBoolean(SERVICE_ACTIVE_KEY, active).apply()
    }

    fun wasServiceActive(): Boolean = preferences.getBoolean(SERVICE_ACTIVE_KEY, false)

    fun blockedEvents(): List<DnsBlockedEvent> = runCatching {
        val events = JSONArray(securePreferences.getString(EVENTS_KEY, "[]"))
        (0 until events.length()).mapNotNull { index ->
            events.optJSONObject(index)?.let {
                DnsBlockedEvent(
                    domain = it.optString("domain"),
                    category = it.optString("category"),
                    severity = it.optString("severity"),
                    timestamp = it.optLong("timestamp"),
                    reason = it.optString("reason", "threat_rule"),
                    source = it.optString("source", "UNKNOWN"),
                    feedVersion = it.optString("feedVersion", "UNKNOWN")
                )
            }
        }.takeLast(MAX_EVENTS)
    }.getOrDefault(emptyList())

    fun blockedCount(): Int = securePreferences.getInt(BLOCKED_COUNT_KEY, blockedEvents().size)

    fun recordBlocked(event: DnsBlockedEvent) {
        val cutoff = System.currentTimeMillis() - retentionHours() * 60L * 60L * 1000L
        val events = blockedEvents().filter { it.timestamp >= cutoff }.toMutableList()
        events.add(event)
        val storedEvents = events.takeLast(MAX_EVENTS)
        val json = JSONArray().apply {
            storedEvents.forEach {
                put(JSONObject().apply {
                    put("domain", it.domain)
                    put("category", it.category)
                    put("severity", it.severity)
                    put("timestamp", it.timestamp)
                    put("reason", it.reason)
                    put("source", it.source)
                    put("feedVersion", it.feedVersion)
                })
            }
        }
        securePreferences.edit()
            .putString(EVENTS_KEY, json.toString())
            .putInt(BLOCKED_COUNT_KEY, storedEvents.size)
            .apply()
    }

    fun retentionHours(): Long = preferences.getLong(RETENTION_HOURS_KEY, DEFAULT_RETENTION_HOURS)

    fun setRetentionHours(hours: Long) {
        preferences.edit().putLong(RETENTION_HOURS_KEY, hours.coerceIn(1L, MAX_RETENTION_HOURS)).apply()
    }

    fun clearActivity() {
        securePreferences.edit().remove(EVENTS_KEY).remove(BLOCKED_COUNT_KEY).apply()
    }

    fun allowTemporarily(domain: String, durationMillis: Long, explanation: String): Boolean {
        val normalized = normalizeDomain(domain) ?: return false
        val active = temporaryExceptions().filter { it.expiresAt > System.currentTimeMillis() && it.domain != normalized }
        val updated = active + DnsTemporaryException(normalized, System.currentTimeMillis() + durationMillis.coerceAtLeast(1L), explanation.take(120))
        securePreferences.edit().putString(TEMPORARY_EXCEPTIONS_KEY, temporaryExceptionsJson(updated)).apply()
        return true
    }

    fun temporaryExceptions(): List<DnsTemporaryException> = runCatching {
        val json = JSONArray(securePreferences.getString(TEMPORARY_EXCEPTIONS_KEY, "[]"))
        (0 until json.length()).mapNotNull { index ->
            json.optJSONObject(index)?.let {
                val domain = normalizeDomain(it.optString("domain")) ?: return@let null
                DnsTemporaryException(domain, it.optLong("expiresAt"), it.optString("explanation", "Excepción temporal"))
            }
        }.filter { it.expiresAt > System.currentTimeMillis() }
    }.getOrDefault(emptyList())

    fun clearExpiredExceptions() {
        securePreferences.edit().putString(TEMPORARY_EXCEPTIONS_KEY, temporaryExceptionsJson(temporaryExceptions())).apply()
    }

    fun isAllowed(domain: String): Boolean {
        val normalized = normalizeDomain(domain) ?: return false
        return readAllowlist().any { normalized == it || normalized.endsWith(".$it") } ||
            temporaryExceptions().any { normalized == it.domain || normalized.endsWith(".${it.domain}") }
    }

    fun isBlocked(domain: String): Boolean {
        val normalized = normalizeDomain(domain) ?: return false
        return readBlocklist().any { normalized == it || normalized.endsWith(".$it") }
    }

    private fun readAllowlist(): List<String> = preferences.getStringSet(ALLOWLIST_KEY, emptySet()).orEmpty().mapNotNull(::normalizeDomain).distinct().sorted()
    private fun readBlocklist(): List<String> = preferences.getStringSet(BLOCKLIST_KEY, emptySet()).orEmpty().mapNotNull(::normalizeDomain).distinct().sorted()

    private fun normalizeDomain(value: String): String? = DnsDecisionEngine.normalize(
        value.trim().lowercase(Locale.ROOT).removePrefix("https://").removePrefix("http://").substringBefore('/').substringBefore(':')
    )

    private fun temporaryExceptionsJson(exceptions: List<DnsTemporaryException>) = JSONArray().apply {
        exceptions.forEach { exception ->
            put(JSONObject().apply {
                put("domain", exception.domain)
                put("expiresAt", exception.expiresAt)
                put("explanation", exception.explanation)
            })
        }
    }.toString()

    private companion object {
        const val NAME = "aura_dns_firewall"
        const val PROFILE_KEY = "profile"
        const val ALLOWLIST_KEY = "allowlist"
        const val BLOCKLIST_KEY = "blocklist"
        const val EVENTS_KEY = "blocked_events"
        const val BLOCKED_COUNT_KEY = "blocked_count"
        const val SERVICE_ACTIVE_KEY = "service_active"
        const val RETENTION_HOURS_KEY = "retention_hours"
        const val TEMPORARY_EXCEPTIONS_KEY = "temporary_exceptions"
        const val DEFAULT_RETENTION_HOURS = 24L
        const val MAX_RETENTION_HOURS = 24L * 30L
        const val MAX_EVENTS = 50
    }
}

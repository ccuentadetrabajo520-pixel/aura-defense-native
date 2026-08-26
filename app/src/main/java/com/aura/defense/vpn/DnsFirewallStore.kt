package com.aura.defense.vpn

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

enum class DnsFirewallProfile(val label: String, val categories: Set<String>) {
    EQUILIBRADO("Equilibrado", setOf("PHISHING", "MALWARE", "TRACKING")),
    ESTRICTO("Estricto", setOf("PHISHING", "MALWARE", "TRACKING", "ADS", "CRYPTO_SCAM")),
    PERMITIR_TODO("Permitir todo", emptySet());

    companion object {
        fun fromStored(value: String?): DnsFirewallProfile = entries.firstOrNull { it.name == value } ?: EQUILIBRADO
    }
}

data class DnsBlockedEvent(
    val domain: String,
    val category: String,
    val severity: String,
    val timestamp: Long
)

class DnsFirewallStore(context: Context) {
    private val preferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun profile(): DnsFirewallProfile = DnsFirewallProfile.fromStored(preferences.getString(PROFILE_KEY, null))

    fun saveProfile(profile: DnsFirewallProfile) {
        preferences.edit().putString(PROFILE_KEY, profile.name).apply()
    }

    fun allowlist(): List<String> = readAllowlist()

    fun addAllowlistedDomain(domain: String): Boolean {
        val normalized = normalizeDomain(domain) ?: return false
        val updated = (readAllowlist() + normalized).distinct().sorted()
        preferences.edit().putStringSet(ALLOWLIST_KEY, updated.toSet()).apply()
        return true
    }

    fun removeAllowlistedDomain(domain: String) {
        preferences.edit().putStringSet(ALLOWLIST_KEY, readAllowlist().filterNot { it == domain }.toSet()).apply()
    }

    fun clearSession() {
        preferences.edit().remove(EVENTS_KEY).remove(BLOCKED_COUNT_KEY).apply()
    }

    fun setServiceActive(active: Boolean) {
        preferences.edit().putBoolean(SERVICE_ACTIVE_KEY, active).apply()
    }

    fun wasServiceActive(): Boolean = preferences.getBoolean(SERVICE_ACTIVE_KEY, false)

    fun blockedEvents(): List<DnsBlockedEvent> = runCatching {
        val events = JSONArray(preferences.getString(EVENTS_KEY, "[]"))
        (0 until events.length()).mapNotNull { index ->
            events.optJSONObject(index)?.let {
                DnsBlockedEvent(
                    domain = it.optString("domain"),
                    category = it.optString("category"),
                    severity = it.optString("severity"),
                    timestamp = it.optLong("timestamp")
                )
            }
        }.takeLast(MAX_EVENTS)
    }.getOrDefault(emptyList())

    fun blockedCount(): Int = preferences.getInt(BLOCKED_COUNT_KEY, blockedEvents().size)

    fun recordBlocked(event: DnsBlockedEvent) {
        val events = blockedEvents().toMutableList()
        events.add(event)
        val json = JSONArray().apply {
            events.takeLast(MAX_EVENTS).forEach {
                put(JSONObject().apply {
                    put("domain", it.domain)
                    put("category", it.category)
                    put("severity", it.severity)
                    put("timestamp", it.timestamp)
                })
            }
        }
        preferences.edit()
            .putString(EVENTS_KEY, json.toString())
            .putInt(BLOCKED_COUNT_KEY, blockedCount() + 1)
            .apply()
    }

    fun isAllowed(domain: String): Boolean {
        val normalized = normalizeDomain(domain) ?: return false
        return readAllowlist().any { normalized == it || normalized.endsWith(".$it") }
    }

    private fun readAllowlist(): List<String> = preferences.getStringSet(ALLOWLIST_KEY, emptySet()).orEmpty().toList().sorted()

    private fun normalizeDomain(value: String): String? = value.trim().lowercase(Locale.US)
        .removePrefix("https://")
        .removePrefix("http://")
        .substringBefore('/').substringBefore(':')
        .trimEnd('.')
        .takeIf { it.isNotBlank() && it.length <= 253 && it.all { character -> character.isLetterOrDigit() || character == '.' || character == '-' } }

    private companion object {
        const val NAME = "aura_dns_firewall"
        const val PROFILE_KEY = "profile"
        const val ALLOWLIST_KEY = "allowlist"
        const val EVENTS_KEY = "blocked_events"
        const val BLOCKED_COUNT_KEY = "blocked_count"
        const val SERVICE_ACTIVE_KEY = "service_active"
        const val MAX_EVENTS = 50
    }
}

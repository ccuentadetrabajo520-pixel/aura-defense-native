package com.aura.defense.data

import android.content.Context
import org.json.JSONObject
import timber.log.Timber
import java.util.Locale
import java.util.UUID

class AuraPreferences(context: Context) {
    private val preferences by lazy {
        runCatching { context.getSharedPreferences(NAME, Context.MODE_PRIVATE) }
            .onFailure { Timber.e(it, "No se pudo abrir el almacenamiento local") }
            .getOrNull()
    }

    fun getAuraId(): String = runCatching {
        preferences?.getString(AURA_ID_KEY, null)
            ?: createAuraId().also { saveAuraId(it) }
    }.onFailure { Timber.e(it, "No se pudo leer el Aura ID") }
        .getOrElse { "AURA-LOCAL" }

    fun saveAuraId(value: String) {
        runCatching { preferences?.edit()?.putString(AURA_ID_KEY, value.trim())?.apply() }
            .onFailure { Timber.e(it, "No se pudo guardar el Aura ID") }
    }

    fun hasCompletedOnboarding(): Boolean = preferences?.getBoolean("onboarding_completed", false) ?: false

    fun setOnboardingCompleted() { preferences?.edit()?.putBoolean("onboarding_completed", true)?.apply() }

    fun exportBackup(context: Context): String = runCatching {
        val prefs = context.getSharedPreferences("aura_defense_preferences", Context.MODE_PRIVATE)
        val dnsPrefs = context.getSharedPreferences("aura_dns_firewall", Context.MODE_PRIVATE)
        val schedulePrefs = context.getSharedPreferences("aura_schedule", Context.MODE_PRIVATE)
        val backup = JSONObject()
        backup.put("aura_id", getAuraId())
        backup.put("onboarding_completed", hasCompletedOnboarding())
        backup.put("preferences", prefs.all.toJson())
        backup.put("dns_firewall", dnsPrefs.all.toJson())
        backup.put("schedule", schedulePrefs.all.toJson())
        backup.put("timestamp", java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date()))
        backup.toString()
    }.getOrElse { "{}" }

    fun importBackup(context: Context, json: String): Boolean = runCatching {
        val backup = JSONObject(json)
        backup.getString("aura_id").let { if (it.isNotBlank()) saveAuraId(it) }
        backup.optJSONObject("preferences")?.let { obj ->
            val prefs = context.getSharedPreferences("aura_defense_preferences", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
            obj.keys().forEach { key -> prefs.edit().putString(key, obj.getString(key)).apply() }
        }
        backup.optJSONObject("dns_firewall")?.let { obj ->
            val prefs = context.getSharedPreferences("aura_dns_firewall", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
            obj.keys().forEach { key -> prefs.edit().putString(key, obj.getString(key)).apply() }
        }
        true
    }.getOrElse { false }

    private fun Map<String, *>.toJson(): JSONObject = JSONObject().also { obj -> forEach { (k, v) -> obj.put(k, v.toString()) } }

    private companion object {
        const val NAME = "aura_defense_preferences"
        const val AURA_ID_KEY = "aura_id"

        fun createAuraId(): String =
            "AURA-${UUID.randomUUID().toString().replace("-", "").take(6).uppercase(Locale.US)}"
    }
}

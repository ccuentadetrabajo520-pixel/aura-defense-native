package com.aura.defense.data

import android.content.Context
import android.util.Log
import java.util.Locale
import java.util.UUID

class AuraPreferences(context: Context) {
    private val preferences by lazy {
        runCatching { context.getSharedPreferences(NAME, Context.MODE_PRIVATE) }
            .onFailure { Log.e("AuraDefense", "No se pudo abrir el almacenamiento local", it) }
            .getOrNull()
    }

    fun getAuraId(): String = runCatching {
        preferences?.getString(AURA_ID_KEY, null)
            ?: createAuraId().also { saveAuraId(it) }
    }.onFailure { Log.e("AuraDefense", "No se pudo leer el Aura ID", it) }
        .getOrElse { "AURA-LOCAL" }

    fun saveAuraId(value: String) {
        runCatching { preferences?.edit()?.putString(AURA_ID_KEY, value.trim())?.apply() }
            .onFailure { Log.e("AuraDefense", "No se pudo guardar el Aura ID", it) }
    }

    fun hasCompletedOnboarding(): Boolean = prefs.getBoolean("onboarding_completed", false)

    fun setOnboardingCompleted() = prefs.edit().putBoolean("onboarding_completed", true).apply()

    private companion object {
        const val NAME = "aura_defense_preferences"
        const val AURA_ID_KEY = "aura_id"

        fun createAuraId(): String =
            "AURA-${UUID.randomUUID().toString().replace("-", "").take(6).uppercase(Locale.US)}"
    }
}

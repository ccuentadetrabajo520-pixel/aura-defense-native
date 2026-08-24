package com.aura.defense.data

import android.content.Context
import java.util.Locale
import java.util.UUID

class AuraPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun getAuraId(): String = preferences.getString(AURA_ID_KEY, null)
        ?: createAuraId().also { saveAuraId(it) }

    fun saveAuraId(value: String) {
        preferences.edit().putString(AURA_ID_KEY, value.trim()).apply()
    }

    private companion object {
        const val NAME = "aura_defense_preferences"
        const val AURA_ID_KEY = "aura_id"

        fun createAuraId(): String =
            "AURA-${UUID.randomUUID().toString().replace("-", "").take(6).uppercase(Locale.US)}"
    }
}

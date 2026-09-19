package com.aura.defense.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecurePrefs {
    @Volatile
    private var instance: SharedPreferences? = null
    @Volatile
    private var encryptedInstance: SharedPreferences? = null

    fun get(context: Context): SharedPreferences =
        instance ?: synchronized(this) {
            instance ?: create(context.applicationContext).also { instance = it }
        }

    fun encryptedOrNull(context: Context): SharedPreferences? =
        encryptedInstance ?: synchronized(this) {
            encryptedInstance ?: runCatching {
                val appContext = context.applicationContext
                val masterKey = MasterKey.Builder(appContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    appContext,
                    "aura_secure_assistant",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            }.getOrNull()?.also { encryptedInstance = it }
        }

    private fun create(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "aura_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback HONESTO: Keystore roto en este dispositivo.
            // Documentado: cifrado NO activo aquí, nunca fingir lo contrario.
            android.util.Log.e("SecurePrefs", "Keystore no disponible: ${e.message}")
            context.getSharedPreferences("aura_secure_prefs_fallback", Context.MODE_PRIVATE)
        }
    }
}
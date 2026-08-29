package com.aura.defense.vault

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.UserNotAuthenticatedException
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

object AuraVault {
    private const val ALIAS = "AURA_VAULT_KEY"
    private const val PREFS_NAME = "aura_vault_prefs"
    private const val KEY_DATA = "encrypted_vault_data"

    private fun getOrCreateKey() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        if (!keyStore.containsAlias(ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            keyGenerator.init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(true)
                .setUserAuthenticationValidityDurationSeconds(300)
                .build())
            keyGenerator.generateKey()
        }
    }

    fun isAvailable(): Boolean = runCatching {
        getOrCreateKey()
        true
    }.getOrDefault(false)

    fun saveReport(context: Context, plainText: String): String {
        return try {
            getOrCreateKey()
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, keyStore.getKey(ALIAS, null))
            val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
                Base64.encodeToString(encrypted, Base64.NO_WRAP)
            check(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putString(KEY_DATA, combined).commit())
            "Successfully saved"
        } catch (e: UserNotAuthenticatedException) {
            "Autenticación requerida. Desbloquea tu dispositivo."
        } catch (e: Exception) {
            "Error saving: ${e.message}"
        }
    }

    fun readReport(context: Context): String {
        return try {
            getOrCreateKey()
            val combined = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_DATA, null) ?: return "No saved history."
            val parts = combined.split(":", limit = 2)
            if (parts.size != 2) return "Error: Corrupted data"
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, keyStore.getKey(ALIAS, null), GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)))
            String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), Charsets.UTF_8)
        } catch (e: UserNotAuthenticatedException) {
            "Autenticación requerida. Desbloquea tu dispositivo."
        } catch (e: Exception) {
            "Error reading: ${e.message}"
        }
    }

    fun clearHistory(context: Context): String {
        return try {
            check(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit())
            "History cleared"
        } catch (e: Exception) {
            "Error clearing: ${e.message}"
        }
    }
}

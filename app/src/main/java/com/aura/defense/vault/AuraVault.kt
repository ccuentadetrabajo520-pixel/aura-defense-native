package com.aura.defense.vault

import android.content.Context
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AuraVault(context: Context) {
    private val preferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun isAvailable(): Boolean = runCatching { getKey(); true }.getOrDefault(false)

    fun saveSummary(summary: String): Boolean = runCatching {
        val encrypted = encrypt(summary)
        preferences.edit().putString(KEY, encrypted).apply()
        true
    }.getOrDefault(false)

    fun readSummary(): String? = runCatching { preferences.getString(KEY, null)?.let(::decrypt) }.getOrNull()

    fun clear(): Boolean = runCatching { preferences.edit().remove(KEY).apply(); true }.getOrDefault(false)

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getKey())
        val encrypted = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String {
        val parts = value.split(":", limit = 2)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getKey(), GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)))
        return String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), StandardCharsets.UTF_8)
    }

    private fun getKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance("AES", "AndroidKeyStore").apply {
            init(256)
        }.generateKey()
    }

    private companion object { const val NAME = "aura_vault"; const val KEY = "summary"; const val KEY_ALIAS = "aura_vault_key" }
}

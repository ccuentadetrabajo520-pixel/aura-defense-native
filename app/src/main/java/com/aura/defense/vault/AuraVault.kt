package com.aura.defense.vault

import android.content.Context
import android.util.Base64
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AuraVault(context: Context) {
    private val preferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun isAvailable(): Boolean = runCatching {
        encrypt("comprobación local")
        true
    }.getOrDefault(false)

    fun saveSummary(summary: String): Boolean = runCatching {
        val entries = readEntries().toMutableList()
        entries.add(summary)
        val encrypted = encrypt(entries.takeLast(MAX_ENTRIES).joinToString("\n"))
        check(preferences.edit().putString(KEY, encrypted).commit())
        true
    }.getOrDefault(false)

    fun replaceSummary(summary: String): Boolean = runCatching {
        val encrypted = encrypt(summary)
        check(preferences.edit().putString(KEY, encrypted).commit())
        true
    }.getOrDefault(false)

    fun readSummary(): String? = runCatching {
        preferences.getString(KEY, null)?.let(::decrypt)?.takeIf { it.isNotBlank() }
    }.getOrNull()

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

    private fun readEntries(): List<String> = preferences.getString(KEY, null)?.let(::decrypt)
        ?.lineSequence()?.filter { it.isNotBlank() }?.toList().orEmpty()

    private fun getKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { existing ->
            runCatching {
                Cipher.getInstance("AES/GCM/NoPadding").init(Cipher.ENCRYPT_MODE, existing)
                return existing
            }
            runCatching { store.deleteEntry(KEY_ALIAS) }
        }
        return generateKey()
    }

    private fun generateKey(): SecretKey {
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(128)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            .apply { init(spec) }
            .generateKey()
    }

    private companion object {
        const val NAME = "aura_vault"
        const val KEY = "summary"
        const val KEY_ALIAS = "aura_vault_key"
        const val MAX_ENTRIES = 50
    }
}

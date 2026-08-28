package com.aura.defense.vault

import android.content.Context

enum class AuraVaultReadStatus { CONTENT, EMPTY, UNAVAILABLE, FAILED }

data class AuraVaultReadResult(val status: AuraVaultReadStatus, val content: String? = null)

class AuraVault(context: Context) {
    private val preferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    private val cryptoHelper = CryptoHelper()

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

    fun readSummaryResult(): AuraVaultReadResult = runCatching {
        val stored = preferences.getString(KEY, null)
            ?: return AuraVaultReadResult(AuraVaultReadStatus.EMPTY)
        val content = decrypt(stored).takeIf { it.isNotBlank() }
        if (content == null) AuraVaultReadResult(AuraVaultReadStatus.EMPTY)
        else AuraVaultReadResult(AuraVaultReadStatus.CONTENT, content)
    }.getOrElse {
        AuraVaultReadResult(
            if (isAvailable()) AuraVaultReadStatus.FAILED else AuraVaultReadStatus.UNAVAILABLE
        )
    }

    fun clear(): Boolean = runCatching { preferences.edit().remove(KEY).commit() }.getOrDefault(false)

    private fun encrypt(value: String): String {
        return cryptoHelper.encryptText(value)
    }

    private fun decrypt(value: String): String {
        return cryptoHelper.decryptText(value)
    }

    private fun readEntries(): List<String> = preferences.getString(KEY, null)?.let(::decrypt)
        ?.lineSequence()?.filter { it.isNotBlank() }?.toList().orEmpty()

    private companion object {
        const val NAME = "aura_vault"
        const val KEY = "vault_data"
        const val MAX_ENTRIES = 50
    }
}

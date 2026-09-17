package com.aura.defense.vpn

import kotlinx.coroutines.flow.MutableStateFlow

object VpnDebugger {
    val logs = MutableStateFlow<List<String>>(emptyList())

    fun log(msg: String) {
        logs.value = (logs.value + redact(msg)).takeLast(MAX_LOGS)
    }

    private fun redact(value: String): String = value
        .replace(Regex("https?://[^\\s]+", RegexOption.IGNORE_CASE), "[url-redactada]")
        .replace(Regex("\\b(?:[a-z0-9-]+\\.)+[a-z]{2,}\\b", RegexOption.IGNORE_CASE), "[dominio-redactado]")
        .replace(Regex("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b"), "[ip-redactada]")

    private const val MAX_LOGS = 50
}
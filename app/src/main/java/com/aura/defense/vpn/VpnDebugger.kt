package com.aura.defense.vpn

import kotlinx.coroutines.flow.MutableStateFlow

object VpnDebugger {
    val logs = MutableStateFlow<List<String>>(emptyList())

    fun log(msg: String) {
        logs.value = (logs.value + msg).takeLast(MAX_LOGS)
    }

    private const val MAX_LOGS = 50
}
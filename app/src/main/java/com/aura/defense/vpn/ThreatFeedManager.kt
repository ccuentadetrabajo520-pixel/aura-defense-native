package com.aura.defense.vpn

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object ThreatFeedManager {
    @Volatile
    private var blockedMap: Map<String, String> = emptyMap()
    private var coroutineJob: Job? = null

    fun init(context: Context) {
        blockedMap = emptyMap()
        coroutineJob?.cancel()
        coroutineJob = null
        VpnDebugger.log("ThreatFeedManager desactivado: sin inteligencia no firmada activa.")
    }

    fun refresh(context: Context) {
        blockedMap = emptyMap()
        VpnDebugger.log("ThreatFeedManager: no se usa para inteligencia activa; se requiere feed firmado.")
    }

    fun isBlocked(domain: String): Boolean = false

    fun categoryOf(domain: String): String? = null

    fun matchDomain(map: Map<String, String>, domain: String): String? = null

    fun size(): Int = 0
    fun loadedCategories(): Set<String> = emptySet()
    fun getLoadedDomains(): Set<String> = emptySet()

    fun shutdown() {
        coroutineJob?.cancel()
        coroutineJob = null
    }
}

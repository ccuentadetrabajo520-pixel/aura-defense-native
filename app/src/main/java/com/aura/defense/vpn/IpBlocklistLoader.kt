package com.aura.defense.vpn

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object IpBlocklistLoader {
    private const val FEED_URL = "https://urlhaus.abuse.ch/downloads/ipblocklist/"
    private const val CACHE_FILE = "ipblocklist.txt"
    private const val USER_AGENT = "AuraDefense/2.0"

    @Volatile
    private var ipSet: Set<String> = emptySet()

    fun init(context: Context) {
        val cache = File(context.filesDir, CACHE_FILE)
        if (cache.exists()) {
            runCatching {
                ipSet = cache.readLines()
                    .map { it.trim() }
                    .filter { it.isNotBlank() && !it.startsWith("#") }
                    .toHashSet()
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val con = URL(FEED_URL).openConnection() as HttpURLConnection
                try {
                    con.connectTimeout = 10_000
                    con.readTimeout = 30_000
                    con.setRequestProperty("User-Agent", USER_AGENT)
                    if (con.responseCode == HttpURLConnection.HTTP_OK) {
                        val body = con.inputStream.bufferedReader().use { it.readText() }
                        val tmp = File(context.filesDir, CACHE_FILE + ".tmp")
                        tmp.writeText(body)
                        if (cache.exists()) cache.delete()
                        tmp.renameTo(cache)
                        ipSet = body.lines()
                            .map { it.trim() }
                            .filter { it.isNotEmpty() && !it.startsWith("#") }
                            .toHashSet()
                        VpnDebugger.log("IP blocklist cargada: ${ipSet.size} IPs")
                    }
                } finally {
                    con.disconnect()
                }
            }.onFailure {
                VpnDebugger.log("IP blocklist no actualizada (${it.message}); se usa caché")
            }
        }
    }

    fun isSuspicious(ip: String): Boolean = ipSet.contains(ip)
    fun size(): Int = ipSet.size
}

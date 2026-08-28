package com.aura.defense.vpn

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

object ThreatFeedManager {
    private const val PREFS_NAME = "threat_feed_prefs"
    private const val KEY_LIST = "blocked_domains_list"

    private val blockedSet = mutableSetOf<String>()
    private var coroutineJob: Job? = null

    private val FEED_URLS = listOf(
        "https://raw.githubusercontent.com/StevenBlack/hosts/master/alternates/phishing/hosts",
        "https://raw.githubusercontent.com/StevenBlack/hosts/master/alternates/fakenews/hosts",
        "https://raw.githubusercontent.com/bigdargon/hostsVN/master/hosts"
    )

    fun init(context: Context) {
        try {
            val cached = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LIST, null)
            if (cached != null) {
                synchronized(blockedSet) {
                    blockedSet.clear()
                    blockedSet.addAll(cached.lines().filter { it.isNotBlank() })
                }
                VpnDebugger.log("Loaded ${blockedSet.size} threats from local cache.")
            }
        } catch (e: Exception) {
            VpnDebugger.log("Error reading cache: ${e.message}")
        }

        coroutineJob?.cancel()
        coroutineJob = CoroutineScope(Dispatchers.IO).launch {
            val newDomains = mutableSetOf<String>()
            for (url in FEED_URLS) {
                try {
                    val con = URL(url).openConnection() as HttpURLConnection
                    con.connectTimeout = 8000
                    con.readTimeout = 8000
                    val text = con.inputStream.bufferedReader().readText()
                    for (line in text.lines()) {
                        val trimmed = line.trim()
                        if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("::")) continue
                        val parts = trimmed.split("\\s+".toRegex())
                        if (parts.size >= 2) {
                            val domain = parts[1].lowercase()
                            if (domain.contains(".") && !domain.startsWith("0.0")) {
                                newDomains.add(domain)
                            }
                        }
                    }
                    VpnDebugger.log("Downloaded feed: $url")
                } catch (e: Exception) {
                    VpnDebugger.log("Error downloading feed: ${e.message}")
                }
            }
            synchronized(blockedSet) {
                blockedSet.clear()
                blockedSet.addAll(newDomains)
            }
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putString(KEY_LIST, newDomains.joinToString("\n")).apply()
            VpnDebugger.log("INTELLIGENCE ACTIVE: ${blockedSet.size} threats loaded into memory.")
        }
    }

    @Synchronized
    fun isBlocked(domain: String): Boolean {
        val d = domain.lowercase().trimEnd('.')
        return blockedSet.any { blocked ->
            val b = blocked.lowercase().trimEnd('.')
            d == b || d.endsWith(".$b")
        }
    }

    @Synchronized
    fun getLoadedDomains(): Set<String> = blockedSet.toSet()

    fun shutdown() {
        coroutineJob?.cancel()
        coroutineJob = null
    }
}

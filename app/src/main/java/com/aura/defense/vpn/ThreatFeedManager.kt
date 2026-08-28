package com.aura.defense.vpn
import android.content.Context
import java.net.HttpURLConnection
import java.net.URL

object ThreatFeedManager {
        private val blockedSet = mutableSetOf<String>()
            private const val PREFS_NAME = "threat_feed_prefs"
                private const val KEY_LIST = "blocked_domains_list"

                    private val FEED_URLS = listOf(
                                "https://raw.githubusercontent.com/StevenBlack/hosts/master/alternates/phishing/hosts",
                                        "https://raw.githubusercontent.com/StevenBlack/hosts/master/alternates/fakenews/hosts",
                                                "https://raw.githubusercontent.com/bigdargon/hostsVN/master/hosts"
                    )

                        fun init(context: Context) {
                                    try {
                                                    val cached = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_LIST, null)
                                                                if (cached != null) {
                                                                                    blockedSet.clear()
                                                                                                    blockedSet.addAll(cached.lines().filter { it.isNotBlank() })
                                                                                                                    VpnDebugger.log("🛡️ Loaded ${blockedSet.size} threats from local cache.")
                                                                }
                                    } catch (e: Exception) { VpnDebugger.log("Error reading cache: ${e.message}") }

                                            Thread {
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
                                                                                                                                                                                                                        VpnDebugger.log("✅ Downloaded list: $url")
                                                                                            } catch (e: Exception) {
                                                                                                                    VpnDebugger.log("⚠️ Error downloading feed: ${e.message}")
                                                                                            }
                                                                        }
                                                                                    synchronized(blockedSet) {
                                                                                                        blockedSet.clear()
                                                                                                                        blockedSet.addAll(newDomains)
                                                                                    }
                                                                                                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                                                                                                                .putString(KEY_LIST, newDomains.joinToString("\n")).apply()
                                                                                                                            VpnDebugger.log("🔥 INTELLIGENCE ACTIVE: ${blockedSet.size} threats loaded into memory.")
                                            }.start()
                        }

                            fun isBlocked(domain: String): Boolean {
                                        return blockedSet.any { domain.contains(it) || it.contains(domain) }
                            }
}

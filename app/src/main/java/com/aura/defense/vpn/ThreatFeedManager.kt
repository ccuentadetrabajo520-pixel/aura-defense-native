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
    private const val USER_AGENT = "AuraDefense/2.0 (Android; local DNS firewall)"
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 30_000

    private data class Feed(val url: String, val category: String, val cacheFile: String)

    private val FEEDS = listOf(
        Feed("https://raw.githubusercontent.com/StevenBlack/hosts/master/alternates/phishing/hosts", "PHISHING", "feed_phishing.txt"),
        Feed("https://raw.githubusercontent.com/StevenBlack/hosts/master/alternates/fakenews/hosts", "FAKENEWS", "feed_fakenews.txt"),
        Feed("https://urlhaus.abuse.ch/downloads/hostfile/", "MALWARE", "feed_malware.txt")
    )

    @Volatile
    private var blockedMap: Map<String, String> = emptyMap()
    private var coroutineJob: Job? = null

    fun init(context: Context) {
        blockedMap = rebuildFromCaches(context)
        VpnDebugger.log("ThreatFeedManager: ${blockedMap.size} dominios desde caché local.")
        com.aura.defense.monitor.AuraProcessLog.log("Inteligencia cargada desde caché local: ${blockedMap.size} dominios", "FEED")
        coroutineJob?.cancel()
        coroutineJob = CoroutineScope(Dispatchers.IO).launch { refresh(context) }
    }

    fun refresh(context: Context) {
        val newMap = HashMap<String, String>(180_000)
        val etags = readEtags(context).toMutableMap()
        val freshEtags = HashMap<String, String>()
        for (feed in FEEDS) {
            val cache = File(context.filesDir, feed.cacheFile)
            var downloaded = false
            try {
                val con = URL(feed.url).openConnection() as HttpURLConnection
                try {
                    con.connectTimeout = CONNECT_TIMEOUT_MS
                    con.readTimeout = READ_TIMEOUT_MS
                    con.setRequestProperty("User-Agent", USER_AGENT)
                    etags[feed.url]?.let { con.setRequestProperty("If-None-Match", it) }
                    when (con.responseCode) {
                        HttpURLConnection.HTTP_OK -> {
                            val body = con.inputStream.bufferedReader().use { it.readText() }
                            runCatching {
                                val tmp = File(context.filesDir, feed.cacheFile + ".tmp")
                                tmp.writeText(body)
                                if (cache.exists()) cache.delete()
                                tmp.renameTo(cache)
                            }
                            parseHosts(body, feed.category, newMap)
                            con.getHeaderField("ETag")?.let { freshEtags[feed.url] = it }
                            downloaded = true
                            VpnDebugger.log("Feed actualizado [${feed.category}]")
                        }
                        HttpURLConnection.HTTP_NOT_MODIFIED -> Unit
                        else -> VpnDebugger.log("Feed ${feed.category}: HTTP ${con.responseCode}; se usa caché")
                    }
                } finally {
                    con.disconnect()
                }
            } catch (e: Exception) {
                VpnDebugger.log("Feed ${feed.category} falló (${e.message}); se usa caché local")
            }
            if (!downloaded && cache.exists()) {
                runCatching { parseHosts(cache.readText(), feed.category, newMap) }
            }
        }
        if (newMap.isNotEmpty()) {
            blockedMap = newMap
            etags.putAll(freshEtags)
            saveEtags(context, etags)
            VpnDebugger.log("INTELIGENCIA ACTIVA: ${blockedMap.size} dominios en memoria.")
            com.aura.defense.monitor.AuraProcessLog.log("Feeds actualizados: ${blockedMap.size} dominios maliciosos en memoria", "FEED")
        } else {
            VpnDebugger.log("Sin fuentes ni caché: se conserva la blocklist previa (fail-closed).")
        }
    }

    fun isBlocked(domain: String): Boolean = categoryOf(domain) != null

    fun categoryOf(domain: String): String? {
        return matchDomain(blockedMap, domain)
    }

    fun matchDomain(map: Map<String, String>, domain: String): String? {
        val normalized = domain.lowercase().trim().trimEnd('.')
        return map.entries.firstOrNull { (candidate, _) ->
            val key = candidate.lowercase().trim().trimEnd('.')
            normalized == key || normalized.endsWith(".$key")
        }?.value
    }

    fun size(): Int = blockedMap.size
    fun loadedCategories(): Set<String> = blockedMap.values.toSet()
    fun getLoadedDomains(): Set<String> = blockedMap.keys

    fun shutdown() {
        coroutineJob?.cancel()
        coroutineJob = null
    }

    private fun rebuildFromCaches(context: Context): Map<String, String> {
        val map = HashMap<String, String>(180_000)
        for (feed in FEEDS) {
            val cache = File(context.filesDir, feed.cacheFile)
            if (cache.exists()) runCatching { parseHosts(cache.readText(), feed.category, map) }
        }
        if (map.isEmpty()) runCatching {
            context.getSharedPreferences("threat_feed_prefs", Context.MODE_PRIVATE)
                .getString("blocked_domains_list", null)?.lines()?.forEach { line ->
                    val d = line.trim().lowercase()
                    if (d.contains('.')) map[d] = "MIGRATED"
                }
        }
        return map
    }

    private fun parseHosts(text: String, category: String, into: MutableMap<String, String>) {
        for (raw in text.lineSequence()) {
            val t = raw.trim()
            if (t.isEmpty() || t.startsWith("#") || t.startsWith("::")) continue
            val space = t.indexOfAny(charArrayOf(' ', '\t'))
            if (space <= 0) continue
            val domain = t.substring(space + 1).trim().lowercase().trimEnd('.')
            if (domain.contains('.') && !domain.startsWith("0.0") && domain !in into) into[domain] = category
        }
    }

    private fun readEtags(context: Context): Map<String, String> = runCatching {
        val f = File(context.filesDir, "feed_etags.txt")
        if (!f.exists()) emptyMap()
        else f.readLines().mapNotNull { line ->
            val i = line.indexOf('\t')
            if (i > 0) line.substring(0, i) to line.substring(i + 1) else null
        }.toMap()
    }.getOrDefault(emptyMap())

    private fun saveEtags(context: Context, etags: Map<String, String>) {
        runCatching {
            File(context.filesDir, "feed_etags.txt")
                .writeText(etags.entries.joinToString("\n") { "${it.key}\t${it.value}" })
        }
    }
}

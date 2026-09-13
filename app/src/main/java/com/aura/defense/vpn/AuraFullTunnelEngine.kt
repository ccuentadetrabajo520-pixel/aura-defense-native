package com.aura.defense.vpn

import android.content.Context
import android.net.VpnService
import java.util.concurrent.atomic.AtomicLong

data class ConnectionLogEntry(
        val destIp: String,
            val destPort: Int,
                val protocol: String,
                    val action: String,
                        val timestamp: String
)

class AuraFullTunnelEngine(private val context: Context) {
        companion object {
                    const val MODE_DNS_ONLY = "dns_only"
                            const val MODE_FULL_TUNNEL = "full_tunnel"
                                    private const val PREFS_NAME = "aura_full_tunnel"
                                            private const val KEY_MODE = "tunnel_mode"
                                                    private const val MAX_LOG_ENTRIES = 200
        }

            private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                private val blockedCount = AtomicLong(0L)
                    private val allowedCount = AtomicLong(0L)
                        private val logQueue = ArrayDeque<ConnectionLogEntry>()
                            private val suspiciousIpCache: MutableSet<String> = HashSet()

                                fun getTunnelMode(): String = prefs.getString(KEY_MODE, MODE_DNS_ONLY) ?: MODE_DNS_ONLY

                                    fun setTunnelMode(mode: String) {
                                                prefs.edit().putString(KEY_MODE, mode).apply()
                                    }

                                        fun configureBuilder(builder: VpnService.Builder): VpnService.Builder {
                                                    val mode = getTunnelMode()
                                                            if (mode == MODE_FULL_TUNNEL) {
                                                                            builder.addRoute("0.0.0.0", 0)
                                                                                        builder.addDnsServer("1.1.1.1")
                                                                                                    builder.addDnsServer("8.8.8.8")
                                                                                                                VpnDebugger.log("Modo de túnel completo configurado; requiere reenvío activo")
                                                            } else {
                                                                            builder.addRoute("10.0.0.1", 32)
                                                                                        VpnDebugger.log("Modo DNS local configurado: solo UDP/53")
                                                            }
                                                                    return builder
                                        }

                                            fun logConnection(destIp: String, destPort: Int, protocol: String, blocked: Boolean) {
                                                        synchronized(logQueue) {
                                                                        logQueue.addLast(
                                                                                            ConnectionLogEntry(
                                                                                                                    destIp = destIp,
                                                                                                                                        destPort = destPort,
                                                                                                                                                            protocol = protocol,
                                                                                                                                                                                action = if (blocked) "BLOQUEADA" else "PERMITIDA",
                                                                                                                                                                                                    timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
                                                                                                                                                                                                                            .format(java.util.Date())
                                                                                            )
                                                                        )
                                                                                    while (logQueue.size > MAX_LOG_ENTRIES) logQueue.removeFirst()
                                                        }
                                                                if (blocked) blockedCount.incrementAndGet() else allowedCount.incrementAndGet()
                                            }

                                                fun getRecentLogs(limit: Int = 50): List<ConnectionLogEntry> =
                                                        synchronized(logQueue) { logQueue.takeLast(limit.coerceAtMost(MAX_LOG_ENTRIES)) }

                                                            fun getBlockedCount(): Long = blockedCount.get()

                                                                fun getAllowedCount(): Long = allowedCount.get()

                                                                    fun getTotalConnections(): Long = blockedCount.get() + allowedCount.get()

                                                                        fun loadSuspiciousIps(lines: List<String>) {
                                                                                    synchronized(suspiciousIpCache) {
                                                                                                    suspiciousIpCache.clear()
                                                                                                                suspiciousIpCache.addAll(lines.map { it.trim() }.filter { it.isNotEmpty() })
                                                                                    }
                                                                        }

                                                                            fun isSuspiciousIp(ip: String): Boolean =
                                                                                    synchronized(suspiciousIpCache) { suspiciousIpCache.contains(ip) }

                                                                                        fun clearLogs() {
                                                                                                    synchronized(logQueue) { logQueue.clear() }
                                                                                                            blockedCount.set(0L)
                                                                                                                    allowedCount.set(0L)
                                                                                        }
}

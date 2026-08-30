package com.aura.defense.vpn

import android.content.Context
import android.content.SharedPreferences
import android.net.VpnService
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
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
                    private const val PREFS_NAME = "aura_tunnel"
                            const val MODE_DNS_ONLY = "dns_only"
                                    const val MODE_FULL_TUNNEL = "full_tunnel"
                                            private const val MAX_LOG_ENTRIES = 200
        }

            private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                private val logQueue = ConcurrentLinkedQueue<ConnectionLogEntry>()
                    private val blockedCount = AtomicLong(0)
                        private val allowedCount = AtomicLong(0)

                            fun getTunnelMode(): String {
                                        return prefs.getString("tunnel_mode", MODE_DNS_ONLY) ?: MODE_DNS_ONLY
                            }

                                fun setTunnelMode(mode: String) {
                                            prefs.edit().putString("tunnel_mode", mode).apply()
                                }

                                    fun configureBuilder(builder: VpnService.Builder): VpnService.Builder {
                                                val mode = getTunnelMode()
                                                        if (mode == MODE_FULL_TUNNEL) {
                                                                        builder.addRoute("0.0.0.0", 0)
                                                                                    builder.addDnsServer("1.1.1.1")
                                                                                                builder.addDnsServer("8.8.8.8")
                                                                                                            VpnDebugger.log("FULL TUNNEL mode configured - all traffic routed through VPN")
                                                        } else {
                                                                        builder.addRoute("10.0.0.1", 32)
                                                                                    VpnDebugger.log("DNS ONLY mode configured")
                                                        }
                                                                return builder
                                    }

                                        fun logConnection(destIp: String, destPort: Int, protocol: String, blocked: Boolean) {
                                                    if (blocked) blockedCount.incrementAndGet() else allowedCount.incrementAndGet()
                                                            val ts = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                                                                    val entry = ConnectionLogEntry(destIp, destPort, protocol, if (blocked) "BLOQUEADO" else "PERMITIDO", ts)
                                                                            logQueue.add(entry)
                                                                                    while (logQueue.size > MAX_LOG_ENTRIES) { logQueue.poll() }
                                        }

                                            fun getRecentLogs(limit: Int = 50): List<ConnectionLogEntry> {
                                                        return logQueue.toList().takeLast(limit)
                                            }

                                                fun getBlockedCount(): Long = blockedCount.get()
                                                    fun getAllowedCount(): Long = allowedCount.get()
                                                        fun getTotalConnections(): Long = blockedCount.get() + allowedCount.get()

                                                            fun isSuspiciousIp(ip: String): Boolean {
                                                                        if (ip.startsWith("10.") || ip.startsWith("192.168.") || ip.startsWith("172.")) return false
                                                                                if (ip.startsWith("127.") || ip.startsWith("0.") || ip.startsWith("169.254.")) return false
                                                                                        return false
                                                            }

                                                                fun clearLogs() {
                                                                            logQueue.clear()
                                                                                    blockedCount.set(0)
                                                                                            allowedCount.set(0)
                                                                }
}

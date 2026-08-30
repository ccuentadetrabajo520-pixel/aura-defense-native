package com.aura.defense.security

import android.content.Context
import android.net.wifi.WifiManager
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WiFiNetworkInfo(
        val ssid: String,
            val bssid: String,
                val rssi: Int,
                    val signalStrength: String,
                        val frequency: Int,
                            val band: String,
                                val securityType: String,
                                    val isSecure: Boolean,
                                        val isCurrent: Boolean
)

data class WiFiAnalysisResult(
        val currentNetwork: WiFiNetworkInfo?,
            val nearbyNetworks: List<WiFiNetworkInfo>,
                val warnings: List<String>,
                    val timestamp: String
)

class WiFiAnalyzer(private val context: Context) {

        fun analyze(): WiFiAnalysisResult {
                    val ts = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                            val warnings = mutableListOf<String>()
                                    val wifiManager = runCatching {
                                                    context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                                    }.getOrNull() ?: return WiFiAnalysisResult(null, emptyList(), listOf("Wi‑Fi no disponible"), ts)

                                            val connInfo = wifiManager.connectionInfo
                                                    val currentBssid = connInfo?.bssid

                                                            val current = if (connInfo != null && currentBssid != null) {
                                                                            val ssid = connInfo.ssid?.removeSurrounding("\"")?.takeIf { it.isNotBlank() } ?: "Oculta"
                                                                                        val level = WifiManager.calculateSignalLevel(connInfo.rssi, 100)
                                                                                                    val freq = connInfo.frequency
                                                                                                                val sec = if (freq >= 5000) "WPA2 (5 GHz)" else "WPA2 (2.4 GHz)"
                                                                                                                            if (level < 20) warnings.add("Senal Wi‑Fi muy debil.")
                                                                                                                                        WiFiNetworkInfo(ssid, currentBssid, connInfo.rssi, signalLabel(level), freq, if (freq >= 5000) "5 GHz" else "2.4 GHz", sec, true, true)
                                                            } else null

                                                                    val nearby = runCatching {
                                                                                    wifiManager.scanResults.mapNotNull { sr ->
                                                                                                    val ssid = sr.SSID?.removeSurrounding("\"")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                                                                                                                    val caps = sr.capabilities ?: ""
                                                                                                                                    val secure = caps.contains("WPA") || caps.contains("WEP")
                                                                                                                                                    val secType = when {
                                                                                                                                                                            caps.contains("WPA3") -> "WPA3"
                                                                                                                                                                                                caps.contains("WPA2") -> "WPA2"
                                                                                                                                                                                                                    caps.contains("WPA") -> "WPA"
                                                                                                                                                                                                                                        caps.contains("WEP") -> "WEP"
                                                                                                                                                                                                                                                            secure -> "Segura"
                                                                                                                                                                                                                                                                                else -> "Abierta"
                                                                                                                                                    }
                                                                                                                                                                    WiFiNetworkInfo(ssid, sr.BSSID ?: "", sr.level, signalLabel(WifiManager.calculateSignalLevel(sr.level, 100)), sr.frequency, if (sr.frequency >= 5000) "5 GHz" else "2.4 GHz", secType, secure, sr.BSSID == currentBssid)
                                                                                    }.sortedByDescending { it.rssi }
                                                                    }.onFailure { Timber.e(it, "Wi‑Fi scan failed") }.getOrDefault(emptyList())

                                                                            val openCount = nearby.count { !it.isSecure }
                                                                                    if (openCount > 0) warnings.add("$openCount red(es) abierta(s) detectada(s) cerca.")
                                                                                            if (current != null && !current.isSecure) warnings.add("Tu red Wi‑Fi actual no tiene cifrado.")

                                                                                                    return WiFiAnalysisResult(current, nearby, warnings, ts)
        }

            private fun signalLabel(level: Int): String = when {
                        level >= 70 -> "Excelente"
                                level >= 40 -> "Buena"
                                        level >= 20 -> "Debil"
                                                else -> "Muy debil"
            }
}

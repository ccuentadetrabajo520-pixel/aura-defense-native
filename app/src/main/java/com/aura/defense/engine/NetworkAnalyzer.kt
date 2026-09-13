package com.aura.defense.engine

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.aura.defense.data.model.Threat
import com.aura.defense.data.model.ThreatSeverity

class NetworkAnalyzer(context: Context) {
    private val applicationContext = context.applicationContext

    fun analyzeNetworkSecurity(): List<Threat> {
        val connectivityManager = applicationContext.getSystemService(ConnectivityManager::class.java)
            ?: return emptyList()
        val network = connectivityManager.activeNetwork ?: return emptyList()
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return emptyList()

        if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return emptyList()
        }

        val detectedAt = System.currentTimeMillis().toString()
        return buildList {
            if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ENCRYPTED)) {
                add(
                    Threat(
                        id = "network:unencrypted:$detectedAt",
                        name = "Red WiFi no encriptada",
                        severity = ThreatSeverity.HIGH,
                        description = "La red WiFi actual no está encriptada. Los datos pueden ser interceptados.",
                        detectedAt = detectedAt
                    )
                )
            }

            if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) {
                add(
                    Threat(
                        id = "network:metered:$detectedAt",
                        name = "Red WiFi de uso medido",
                        severity = ThreatSeverity.MEDIUM,
                        description = "La red WiFi actual está marcada como medida. Comprueba que sea una red de confianza.",
                        detectedAt = detectedAt
                    )
                )
            }
        }
    }
}

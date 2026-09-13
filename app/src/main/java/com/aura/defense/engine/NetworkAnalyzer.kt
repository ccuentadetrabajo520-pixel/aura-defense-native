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
            // NET_CAPABILITY_NOT_ENCRYPTED fue deprecado/ausente en SDK 34; Android no expone aquí una señal equivalente de cifrado WiFi.
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

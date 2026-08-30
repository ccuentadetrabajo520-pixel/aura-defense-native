package com.aura.defense.security

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class NetConnInfo(
        val type: String,
            val isConnected: Boolean,
                val details: String
)

data class NetworkMonitorResult(
        val currentNetwork: NetConnInfo,
            val vpnActive: Boolean,
                val privateDnsStatus: String,
                    val networkCapabilities: List<String>,
                        val warnings: List<String>,
                            val timestamp: String
)

class NetworkMonitor(private val context: Context) {

        fun analyze(): NetworkMonitorResult {
                    val ts = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                            val warnings = mutableListOf<String>()
                                    val caps = mutableListOf<String>()
                                            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

                                                    val network = if (cm != null) {
                                                                    val active = cm.activeNetwork
                                                                                val netCaps = active?.let { cm.getNetworkCapabilities(it) }
                                                                                            val connected = netCaps != null
                                                                                                        val hasInternet = netCaps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                                                                                                                    val hasWifi = netCaps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
                                                                                                                                val hasCell = netCaps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
                                                                                                                                            val hasVpn = netCaps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
                                                                                                                                                        val metered = cm.isActiveNetworkMetered

                                                                                                                                                                    if (hasInternet) caps.add("Internet")
                                                                                                                                                                                if (hasWifi) caps.add("Wi-Fi")
                                                                                                                                                                                            if (hasCell) caps.add("Datos moviles")
                                                                                                                                                                                                        if (hasVpn) caps.add("VPN")
                                                                                                                                                                                                                    if (netCaps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == true) caps.add("No medido")
                                                                                                                                                                                                                                if (metered) caps.add("Conexion medida")

                                                                                                                                                                                                                                            if (hasCell && !hasWifi) warnings.add("Datos moviles sin Wi-Fi. El consumo puede ser elevado.")

                                                                                                                                                                                                                                                        val type = when {
                                                                                                                                                                                                                                                                            hasVpn -> "VPN"
                                                                                                                                                                                                                                                                                            hasWifi -> "Wi-Fi"
                                                                                                                                                                                                                                                                                                            hasCell -> "Datos moviles"
                                                                                                                                                                                                                                                                                                                            connected -> "Otra red"
                                                                                                                                                                                                                                                                                                                                            else -> "Sin conexion"
                                                                                                                                                                                                                                                        }
                                                                                                                                                                                                                                                                    val details = when {
                                                                                                                                                                                                                                                                                        hasWifi -> "Conectado a Wi-Fi"
                                                                                                                                                                                                                                                                                                        hasCell -> "Conectado a datos moviles"
                                                                                                                                                                                                                                                                                                                        hasVpn -> "Conectado por VPN"
                                                                                                                                                                                                                                                                                                                                        else -> "Sin conexion activa"
                                                                                                                                                                                                                                                                    }
                                                                                                                                                                                                                                                                                NetConnInfo(type, connected, details)
                                                    } else {
                                                                    NetConnInfo("Desconocida", false, "Gestor de conectividad no disponible")
                                                    }

                                                            val vpnActive = network.type == "VPN"
                                                                    val dnsStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                                                                    cm?.getLinkProperties(cm.activeNetwork)?.privateDnsServerName
                                                                                                    ?: if (cm?.getDefaultProxy() != null) "Automatico" else "Desactivado"
                                                                    } else {
                                                                                    "No disponible en esta version"
                                                                    }

                                                                            return NetworkMonitorResult(network, vpnActive, dnsStatus, caps, warnings, ts)
        }
}

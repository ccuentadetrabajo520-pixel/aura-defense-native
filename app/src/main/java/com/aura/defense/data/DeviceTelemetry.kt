package com.aura.defense.data

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.StatFs
import android.provider.Settings
import android.util.Log
import com.aura.defense.vpn.AuraVpnService

data class DeviceTelemetrySnapshot(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val apiLevel: Int,
    val securityPatch: String,
    val batteryLevel: String,
    val ramAvailableBytes: Long,
    val ramTotalBytes: Long,
    val storageAvailableBytes: Long,
    val storageTotalBytes: Long,
    val networkActive: String,
    val vpnActive: Boolean,
    val privateDnsStatus: String
)

class DeviceTelemetryProvider(private val context: Context) {
    fun read(): DeviceTelemetrySnapshot {
        return runCatching { readSnapshot() }
            .onFailure { Log.e("AuraDefense", "No se pudo leer la telemetría del dispositivo", it) }
            .getOrElse { unavailableSnapshot() }
    }

    private fun readSnapshot(): DeviceTelemetrySnapshot {
        val connectivity = runCatching {
            context.getSystemService(ConnectivityManager::class.java)
        }.getOrNull()
        val network = runCatching { connectivity?.activeNetwork }.getOrNull()
        val capabilities = runCatching {
            network?.let { connectivity?.getNetworkCapabilities(it) }
        }.getOrNull()
        val battery = runCatching {
            context.getSystemService(BatteryManager::class.java)
        }.getOrNull()
        val batteryText = runCatching {
            battery?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                ?.takeIf { it in 0..100 }
                ?.let { "$it%" }
        }.getOrNull() ?: "No disponible"
        val memory = runCatching {
            context.getSystemService(ActivityManager::class.java)?.let { manager ->
                ActivityManager.MemoryInfo().also(manager::getMemoryInfo)
            }
        }.getOrNull()
        val storage = runCatching { StatFs(context.filesDir.absolutePath) }.getOrNull()
        val networkText = runCatching {
            when {
                capabilities == null -> "No disponible"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Datos móviles"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                else -> "Activa"
            }
        }.onFailure { Log.e("AuraDefense", "No se pudo identificar la red activa", it) }
            .getOrDefault("No disponible")
        val vpn = runCatching {
            AuraVpnService.isRunning && capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        }.onFailure { Log.e("AuraDefense", "No se pudo comprobar la VPN", it) }.getOrDefault(false)

        return DeviceTelemetrySnapshot(
            manufacturer = readText("fabricante") { Build.MANUFACTURER },
            model = readText("modelo") { Build.MODEL },
            androidVersion = readText("versión de Android") { Build.VERSION.RELEASE },
            apiLevel = runCatching { Build.VERSION.SDK_INT }
                .onFailure { Log.e("AuraDefense", "No se pudo leer la API de Android", it) }
                .getOrDefault(0),
            securityPatch = readText("parche de seguridad") { Build.VERSION.SECURITY_PATCH },
            batteryLevel = batteryText,
            ramAvailableBytes = runCatching { memory?.availMem ?: 0L }.getOrDefault(0L),
            ramTotalBytes = runCatching { memory?.totalMem ?: 0L }.getOrDefault(0L),
            storageAvailableBytes = runCatching { storage?.availableBytes ?: 0L }.getOrDefault(0L),
            storageTotalBytes = runCatching { storage?.totalBytes ?: 0L }.getOrDefault(0L),
            networkActive = networkText,
            vpnActive = vpn,
            privateDnsStatus = readPrivateDns()
        )
    }

    private fun readPrivateDns(): String = runCatching {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return "No disponible"
        val mode = runCatching {
            Settings.Global.getString(context.contentResolver, "private_dns_mode")
        }.getOrNull()
        runCatching {
            Settings.Global.getString(context.contentResolver, "private_dns_specifier")
        }.onFailure { Log.e("AuraDefense", "No se pudo leer el servidor DNS privado", it) }
        when (mode?.takeIf { it.isNotBlank() }) {
            "off" -> "Inactivo"
            "opportunistic" -> "Automático"
            "hostname" -> "Activo"
            else -> if (mode.isNullOrBlank()) "No disponible" else "Activo"
        }
    }.onFailure { Log.e("AuraDefense", "No se pudo leer el DNS privado", it) }
        .getOrDefault("No disponible")

    private fun readText(name: String, reader: () -> String?): String = runCatching {
        reader()?.takeIf { it.isNotBlank() } ?: "No disponible"
    }.onFailure { Log.e("AuraDefense", "No se pudo leer $name", it) }
        .getOrDefault("No disponible")

}

private fun unavailableSnapshot() = DeviceTelemetrySnapshot(
    manufacturer = "No disponible",
    model = "No disponible",
    androidVersion = "No disponible",
    apiLevel = 0,
    securityPatch = "No disponible",
    batteryLevel = "No disponible",
    ramAvailableBytes = 0L,
    ramTotalBytes = 0L,
    storageAvailableBytes = 0L,
    storageTotalBytes = 0L,
    networkActive = "No disponible",
    vpnActive = false,
    privateDnsStatus = "No disponible"
)

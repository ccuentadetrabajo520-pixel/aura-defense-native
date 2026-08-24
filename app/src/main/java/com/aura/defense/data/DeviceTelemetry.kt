package com.aura.defense.data

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.StatFs
import android.provider.Settings
import android.app.KeyguardManager
import java.util.Locale

 data class DeviceTelemetry(
    val fabricante: String,
    val modelo: String,
    val versionAndroid: String,
    val api: Int,
    val parcheSeguridad: String,
    val bateriaPorcentaje: Int?,
    val ramDisponibleBytes: Long?,
    val ramTotalBytes: Long?,
    val almacenamientoDisponibleBytes: Long?,
    val almacenamientoTotalBytes: Long?,
    val redActiva: Boolean,
    val redValidada: Boolean?,
    val vpnActiva: Boolean,
    val dnsPrivado: String,
    val bloqueoSeguro: Boolean,
    val adbActivo: Boolean?,
    val opcionesDesarrollador: Boolean?
)

class DeviceTelemetryProvider(private val context: Context) {
    fun read(): DeviceTelemetry {
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        val network = connectivity?.activeNetwork
        val capabilities = network?.let { connectivity.getNetworkCapabilities(it) }
        val battery = context.getSystemService(BatteryManager::class.java)
        val batteryLevel = battery?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.takeIf { it in 0..100 }
        val memory = context.getSystemService(ActivityManager::class.java)?.let {
            ActivityManager.MemoryInfo().also(it::getMemoryInfo)
        }
        val storage = runCatching { StatFs(context.filesDir.absolutePath) }.getOrNull()
        val security = context.getSystemService(KeyguardManager::class.java)?.isKeyguardSecure ?: false

        return DeviceTelemetry(
            fabricante = Build.MANUFACTURER.orEmpty().ifBlank { "No disponible" }.replaceFirstChar { it.uppercase(Locale.getDefault()) },
            modelo = Build.MODEL.orEmpty().ifBlank { "No disponible" },
            versionAndroid = Build.VERSION.RELEASE.orEmpty().ifBlank { "No disponible" },
            api = Build.VERSION.SDK_INT,
            parcheSeguridad = Build.VERSION.SECURITY_PATCH.orEmpty().ifBlank { "No disponible" },
            bateriaPorcentaje = batteryLevel,
            ramDisponibleBytes = memory?.availMem,
            ramTotalBytes = memory?.totalMem,
            almacenamientoDisponibleBytes = storage?.availableBytes,
            almacenamientoTotalBytes = storage?.totalBytes,
            redActiva = capabilities != null,
            redValidada = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
            vpnActiva = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true,
            dnsPrivado = readPrivateDns(),
            bloqueoSeguro = security,
            adbActivo = readGlobalInt(Settings.Global.ADB_ENABLED),
            opcionesDesarrollador = readGlobalInt(Settings.Global.DEVELOPMENT_SETTINGS_ENABLED)
        )
    }

    private fun readPrivateDns(): String = runCatching {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return "No disponible"
        when (Settings.Global.getString(context.contentResolver, Settings.Global.PRIVATE_DNS_MODE)) {
            "off" -> "Inactivo"
            "opportunistic" -> "Automático"
            "hostname" -> Settings.Global.getString(context.contentResolver, Settings.Global.PRIVATE_DNS_SPECIFIER)
                ?.takeIf { it.isNotBlank() } ?: "Personalizado"
            else -> "No disponible"
        }
    }.getOrDefault("No disponible")

    private fun readGlobalInt(name: String): Boolean? = runCatching {
        Settings.Global.getInt(context.contentResolver, name, 0) == 1
    }.getOrNull()
}

fun formatBytes(bytes: Long?): String {
    if (bytes == null) return "No disponible"
    val units = arrayOf("B", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var index = 0
    while (value >= 1024 && index < units.lastIndex) {
        value /= 1024
        index++
    }
    return String.format(Locale.getDefault(), "%.1f %s", value, units[index])
}

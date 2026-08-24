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
import android.util.Log
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
) {
    companion object {
        fun unavailable() = DeviceTelemetry(
            fabricante = "No disponible",
            modelo = "No disponible",
            versionAndroid = "No disponible",
            api = 0,
            parcheSeguridad = "No disponible",
            bateriaPorcentaje = null,
            ramDisponibleBytes = null,
            ramTotalBytes = null,
            almacenamientoDisponibleBytes = null,
            almacenamientoTotalBytes = null,
            redActiva = false,
            redValidada = null,
            vpnActiva = false,
            dnsPrivado = "No disponible",
            bloqueoSeguro = false,
            adbActivo = null,
            opcionesDesarrollador = null
        )
    }
)

class DeviceTelemetryProvider(private val context: Context) {
    fun read(): DeviceTelemetry {
        return runCatching { readAvailableSignals() }.getOrElse { error ->
            Log.e("AuraDefense", "No se pudo leer la telemetría del dispositivo", error)
            DeviceTelemetry.unavailable()
        }
    }

    private fun readAvailableSignals(): DeviceTelemetry {
        val connectivity = runCatching { context.getSystemService(ConnectivityManager::class.java) }.getOrNull()
        val network = runCatching { connectivity?.activeNetwork }.getOrNull()
        val capabilities = runCatching { network?.let { connectivity?.getNetworkCapabilities(it) } }.getOrNull()
        val battery = runCatching { context.getSystemService(BatteryManager::class.java) }.getOrNull()
        val batteryLevel = runCatching { battery?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) }
            .getOrNull()?.takeIf { it in 0..100 }
        val memory = runCatching {
            context.getSystemService(ActivityManager::class.java)?.let { manager ->
                ActivityManager.MemoryInfo().also(manager::getMemoryInfo)
            }
        }.getOrNull()
        val storage = runCatching { StatFs(context.filesDir.absolutePath) }.getOrNull()
        val security = runCatching { context.getSystemService(KeyguardManager::class.java)?.isKeyguardSecure }.getOrNull() ?: false

        return DeviceTelemetry(
            fabricante = runCatching { Build.MANUFACTURER.orEmpty().ifBlank { "No disponible" }.replaceFirstChar { it.uppercase(Locale.getDefault()) } }.getOrDefault("No disponible"),
            modelo = runCatching { Build.MODEL.orEmpty().ifBlank { "No disponible" } }.getOrDefault("No disponible"),
            versionAndroid = runCatching { Build.VERSION.RELEASE.orEmpty().ifBlank { "No disponible" } }.getOrDefault("No disponible"),
            api = runCatching { Build.VERSION.SDK_INT }.getOrDefault(0),
            parcheSeguridad = runCatching { Build.VERSION.SECURITY_PATCH.orEmpty().ifBlank { "No disponible" } }.getOrDefault("No disponible"),
            bateriaPorcentaje = batteryLevel,
            ramDisponibleBytes = runCatching { memory?.availMem }.getOrNull(),
            ramTotalBytes = runCatching { memory?.totalMem }.getOrNull(),
            almacenamientoDisponibleBytes = runCatching { storage?.availableBytes }.getOrNull(),
            almacenamientoTotalBytes = runCatching { storage?.totalBytes }.getOrNull(),
            redActiva = capabilities != null,
            redValidada = runCatching { capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) }.getOrNull(),
            vpnActiva = runCatching { capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true }.getOrDefault(false),
            dnsPrivado = readPrivateDns(),
            bloqueoSeguro = security,
            adbActivo = readGlobalInt(Settings.Global.ADB_ENABLED),
            opcionesDesarrollador = readGlobalInt(Settings.Global.DEVELOPMENT_SETTINGS_ENABLED)
        )
    }

    private fun readPrivateDns(): String = runCatching {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return "No disponible"
        val privateDnsMode = runCatching {
            Settings.Global.getString(context.contentResolver, "private_dns_mode")
        }.getOrNull()
        val privateDnsSpecifier = runCatching {
            Settings.Global.getString(context.contentResolver, "private_dns_specifier")
        }.getOrNull()

        when (privateDnsMode?.takeIf { it.isNotBlank() }) {
            "off" -> "Inactivo"
            "opportunistic" -> "Automático"
            "hostname" -> privateDnsSpecifier?.takeIf { it.isNotBlank() }
                ?.let { "DNS privado activo: $it" } ?: "DNS privado activo"
            else -> "Activo"
        }
    }.onFailure { Log.e("AuraDefense", "No se pudo leer el DNS privado", it) }.getOrDefault("No disponible")

    private fun readGlobalInt(name: String): Boolean? = runCatching {
        Settings.Global.getInt(context.contentResolver, name, 0) == 1
    }.onFailure { Log.e("AuraDefense", "No se pudo leer un ajuste del sistema", it) }.getOrNull()
}

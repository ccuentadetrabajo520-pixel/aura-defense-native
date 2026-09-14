package com.aura.defense.monitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.aura.defense.R
import com.aura.defense.data.SecurePrefs

object SelfDefenseWatcher {
    private const val CHANNEL = "aura_selfdefense"

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(
                    NotificationChannel(
                        CHANNEL,
                        "Autodefensa AURA",
                        NotificationManager.IMPORTANCE_HIGH
                    )
                )
        }
    }

    private fun alert(context: Context, title: String, text: String) {
        AuraProcessLog.log("⚠ AUTODEFENSA: $text", "SISTEMA")
        runCatching {
            ensureChannel(context)
            val notification: Notification = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.aura_core_foreground)
                .setContentTitle(title)
                .setContentText(text.take(180))
                .setAutoCancel(true)
                .build()
            context.getSystemService(NotificationManager::class.java)
                .notify(title.hashCode(), notification)
        }
    }

    fun selfCheck(context: Context, vpnRunning: Boolean): List<String> {
        val threats = mutableListOf<String>()
        val packageName = context.packageName
        val enabledListeners = runCatching {
            android.provider.Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: ""
        }.getOrDefault("")
        val others = enabledListeners.split(":")
            .mapNotNull { component -> component.substringBefore('/').substringAfterLast('/').trim() }
            .filter { it.isNotBlank() && it != packageName }
        if (others.isNotEmpty()) {
            threats.add(
                "Otra app está leyendo tus notificaciones: ${others.joinToString(", ")}. " +
                    "Así se leen tus SMS y chats. Revísala en Ajustes > Acceso especial."
            )
        }

        val securePrefs = SecurePrefs.get(context)
        val expected = securePrefs.getBoolean("aura_vpn_expected", false)
        if (expected && !vpnRunning) {
            threats.add("El túnel de AURA fue DESACTIVADO o revocado. Tu tráfico ya no está filtrado.")
            securePrefs.edit().putBoolean("aura_vpn_expected", false).apply()
        }

        val selfDefensePrefs = context.getSharedPreferences("aura_selfdefense", Context.MODE_PRIVATE)
        if (enabledListeners.contains(packageName)) {
            selfDefensePrefs.edit().putBoolean("listener_was_on", true).apply()
        } else if (selfDefensePrefs.getBoolean("listener_was_on", false)) {
            threats.add(
                "El acceso de AURA a notificaciones fue DESACTIVADO: ya no analizo enlaces de SMS/chats. " +
                    "Reactívalo para restaurar esa capa."
            )
        }
        return threats
    }

    fun notifyThreats(context: Context, threats: List<String>) {
        threats.forEach { alert(context, "⚠ AURA Autodefensa", it) }
    }
}

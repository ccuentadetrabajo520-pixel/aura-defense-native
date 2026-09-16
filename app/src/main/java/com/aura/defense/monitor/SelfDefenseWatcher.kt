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
    private const val ALERTED_KEY = "alerted"
    private val systemListenerPrefixes = listOf(
        "com.android.systemui", "com.android.settings", "com.google.android.",
        "com.samsung.", "com.miui.", "com.xiaomi.", "com.huawei.", "com.oppo.", "com.vivo."
    )

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
        val selfDefensePrefs = context.getSharedPreferences("aura_selfdefense", Context.MODE_PRIVATE)
        val alerted = selfDefensePrefs.getStringSet(ALERTED_KEY, emptySet()).orEmpty().toMutableSet()
        val packageName = context.packageName
        val enabledListeners = runCatching {
            android.provider.Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: ""
        }.getOrDefault("")
        val others = enabledListeners.split(":")
            .mapNotNull { component -> component.substringBefore('/').substringAfterLast('/').trim() }
            .filter { it.isNotBlank() && it != packageName && systemListenerPrefixes.none(it::startsWith) }
        val externalThreat =
            "Otra app está leyendo tus notificaciones: ${others.joinToString(", ")}. " +
                "Así se leen tus SMS y chats. Revísala en Ajustes > Acceso especial."
        if (others.isNotEmpty()) {
            threats.add(externalThreat)
            selfDefensePrefs.edit().putString("external_last_threat", externalThreat).apply()
        } else {
            selfDefensePrefs.getString("external_last_threat", null)?.let {
                alerted.remove(it.hashCode().toString())
            }
            alerted.remove(externalThreat.hashCode().toString())
        }

        val securePrefs = SecurePrefs.get(context)
        val expected = securePrefs.getBoolean("aura_vpn_expected", false)
        val vpnThreat = "El túnel de AURA fue DESACTIVADO o revocado. Tu tráfico ya no está filtrado."
        if (expected && !vpnRunning) {
            threats.add(vpnThreat)
        } else {
            alerted.remove(vpnThreat.hashCode().toString())
        }

        val ownListenerActive = enabledListeners.contains(packageName)
        if (ownListenerActive) {
            selfDefensePrefs.edit().putBoolean("listener_was_on", true).apply()
            alerted.remove(ownListenerThreat.hashCode().toString())
        } else if (selfDefensePrefs.getBoolean("listener_was_on", false)) {
            threats.add(ownListenerThreat)
        } else {
            alerted.remove(ownListenerThreat.hashCode().toString())
        }
            selfDefensePrefs.edit().putStringSet(ALERTED_KEY, alerted).apply()
        return threats
    }

    fun notifyThreats(context: Context, threats: List<String>) {
            val prefs = context.getSharedPreferences("aura_selfdefense", Context.MODE_PRIVATE)
            val alerted = prefs.getStringSet(ALERTED_KEY, emptySet()).orEmpty().toMutableSet()
            threats.forEach { threat ->
                val key = threat.hashCode().toString()
                if (alerted.add(key)) alert(context, "⚠ AURA Autodefensa", threat)
            }
            prefs.edit().putStringSet(ALERTED_KEY, alerted).apply()
    }

        private val ownListenerThreat =
            "El acceso de AURA a notificaciones fue DESACTIVADO: ya no analizo enlaces de SMS/chats. " +
                "Reactívalo para restaurar esa capa."
}

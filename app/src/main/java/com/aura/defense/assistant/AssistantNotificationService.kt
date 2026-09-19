package com.aura.defense.assistant

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import com.aura.defense.MainActivity
import com.aura.defense.R
import com.aura.defense.vpn.DnsProtectionState
import com.aura.defense.threats.ThreatRepositoryState

class AssistantNotificationService(context: Context) {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(NotificationManager::class.java)

    init {
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Estado de cobertura AURA", NotificationManager.IMPORTANCE_DEFAULT))
    }

    fun notifyCoverage(state: DnsProtectionState, feedState: ThreatRepositoryState) {
        val decision = AssistantNotificationPolicy.decide(state.status, feedState) ?: return
        if (android.os.Build.VERSION.SDK_INT >= 33 && appContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val intent = PendingIntent.getActivity(appContext, 4401, Intent(appContext, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(NOTIFICATION_ID, NotificationCompat.Builder(appContext, CHANNEL)
            .setSmallIcon(R.drawable.aura_core_foreground)
            .setContentTitle(decision.title)
            .setContentText(decision.text)
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build())
    }

    fun notifyConfirmedMatch() {
        if (android.os.Build.VERSION.SDK_INT >= 33 && appContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val intent = PendingIntent.getActivity(appContext, 4402, Intent(appContext, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(CONFIRMED_NOTIFICATION_ID, NotificationCompat.Builder(appContext, CHANNEL)
            .setSmallIcon(R.drawable.aura_core_foreground)
            .setContentTitle("Coincidencia confirmada por feed")
            .setContentText("AURA registró una coincidencia DNS con evidencia local verificable.")
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build())
    }

    companion object {
        private const val CHANNEL = "aura_assistant_coverage"
        private const val NOTIFICATION_ID = 4401
        private const val CONFIRMED_NOTIFICATION_ID = 4402
    }
}

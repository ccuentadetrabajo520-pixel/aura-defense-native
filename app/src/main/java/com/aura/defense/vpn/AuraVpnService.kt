package com.aura.defense.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.aura.defense.R

class AuraVpnService : VpnService() {
    private var tunnel: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopVpn()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, notification())
        establishVpn()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    private fun establishVpn() {
        runCatching {
            tunnel?.close()
            tunnel = Builder()
                .setSession("Aura Defense")
                .addAddress("10.0.0.2", 32)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("1.1.1.1")
                .establish()
                ?: error("No se pudo establecer el túnel VPN")
        }.onSuccess {
            isRunning = true
        }.onFailure {
            isRunning = false
            stopSelf()
        }
    }

    private fun stopVpn() {
        isRunning = false
        runCatching { tunnel?.close() }
        tunnel = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun notification(): Notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.aura_core_foreground)
        .setContentTitle("Protección VPN activa")
        .setContentText("Aura Defense está protegiendo tu conexión")
        .setOngoing(true)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Protección VPN", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    companion object {
        const val ACTION_STOP = "com.aura.defense.vpn.STOP"
        private const val CHANNEL_ID = "aura_vpn"
        private const val NOTIFICATION_ID = 1801

        @Volatile
        var isRunning: Boolean = false
            private set
    }
}
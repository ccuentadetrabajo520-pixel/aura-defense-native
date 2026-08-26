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
import com.aura.defense.threats.ThreatIntelligenceEngine
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicBoolean

class AuraVpnService : VpnService() {
    private var tunnel: ParcelFileDescriptor? = null
    private var packetThread: Thread? = null
    private val stopping = AtomicBoolean(false)

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

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopVpn()
        super.onTaskRemoved(rootIntent)
    }

    private fun establishVpn() {
        runCatching {
            stopping.set(false)
            tunnel?.close()
            tunnel = Builder()
                .setSession("Aura Defense")
                .addAddress("10.0.0.2", 32)
                .addRoute(DNS_VIRTUAL_ADDRESS, 32)
                .addDnsServer(DNS_VIRTUAL_ADDRESS)
                .establish()
                ?: error("No se pudo establecer el túnel VPN")
        }.onSuccess {
            DnsFirewallStore(this).apply {
                clearSession()
                setServiceActive(true)
            }
            isRunning = true
            packetThread = Thread(::runDnsProxy, "AuraDnsFirewall").also { it.start() }
        }.onFailure {
            isRunning = false
            DnsFirewallStore(this).setServiceActive(false)
            stopSelf()
        }
    }

    fun stopVpn() {
        isRunning = false
        DnsFirewallStore(this).setServiceActive(false)
        stopping.set(true)
        runCatching { tunnel?.close() }
        tunnel = null
        packetThread?.interrupt()
        packetThread = null
        stopForeground(true)
        stopSelf()
    }

    private fun runDnsProxy() {
        val activeTunnel = tunnel ?: return
        val store = DnsFirewallStore(this)
        val threatEngine = ThreatIntelligenceEngine(this)
        runCatching {
            FileInputStream(activeTunnel.fileDescriptor).use { input ->
                FileOutputStream(activeTunnel.fileDescriptor).use { output ->
                    val packet = ByteArray(MAX_PACKET_SIZE)
                    while (!stopping.get()) {
                        val length = input.read(packet)
                        if (length <= 0) break
                        val query = DnsPacketCodec.query(packet, length) ?: continue
                        val match = threatEngine.findMatches(query.domain).firstOrNull { indicator ->
                            store.profile().categories.contains(indicator.category.name) && !store.isAllowed(query.domain)
                        }
                        if (match != null) {
                            store.recordBlocked(DnsBlockedEvent(query.domain, match.category.name, match.severity.name, System.currentTimeMillis()))
                            continue
                        }
                        forwardDnsQuery(packet, length, output)
                    }
                }
            }
        }.onFailure {
            if (!stopping.get()) stopVpn()
        }
    }

    private fun forwardDnsQuery(queryPacket: ByteArray, length: Int, output: FileOutputStream) {
        runCatching {
            DatagramSocket().use { socket ->
                protect(socket)
                socket.soTimeout = DNS_TIMEOUT_MS
                val upstream = InetAddress.getByName(UPSTREAM_DNS)
                socket.send(DatagramPacket(queryPacket, length, upstream, DNS_PORT))
                val responseBytes = ByteArray(MAX_DNS_PACKET_SIZE)
                val response = DatagramPacket(responseBytes, responseBytes.size)
                socket.receive(response)
                DnsPacketCodec.response(queryPacket, length, response.data.copyOf(response.length))?.let {
                    output.write(it)
                }
            }
        }
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
        private const val DNS_VIRTUAL_ADDRESS = "10.0.0.1"
        private const val UPSTREAM_DNS = "1.1.1.1"
        private const val DNS_PORT = 53
        private const val DNS_TIMEOUT_MS = 1500
        private const val MAX_PACKET_SIZE = 32767
        private const val MAX_DNS_PACKET_SIZE = 4096
        private const val CHANNEL_ID = "aura_vpn"
        private const val NOTIFICATION_ID = 1801

        @Volatile
        var isRunning: Boolean = false
            private set
    }
}
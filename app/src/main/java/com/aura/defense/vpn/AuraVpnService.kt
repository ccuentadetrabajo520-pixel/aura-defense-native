package com.aura.defense.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import timber.log.Timber
import com.aura.defense.R
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import com.aura.defense.threats.ThreatIntelligenceEngine

class AuraVpnService : VpnService() {
    private var tunnel: ParcelFileDescriptor? = null
    private var packetThread: Thread? = null
    private val stopping = AtomicBoolean(false)
    private var ThreatEngine: ThreatIntelligenceEngine? = null
    private var dnsStore: DnsFirewallStore? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopVpn()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, notification())
        VpnDebugger.log("✅ AURA VPN SERVICE INICIADO")
        ThreatFeedManager.init(this)
        IpBlocklistLoader.init(this)
        ThreatEngine = ThreatIntelligenceEngine(this)
        dnsStore = DnsFirewallStore(this)
        establishVpn()
        return START_STICKY
    }

    override fun onDestroy() {
        val prefs = getSharedPreferences("aura_killswitch", MODE_PRIVATE)
        prefs.edit().putBoolean("vpn_was_running", true).apply()
        stopVpn()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopVpn()
        super.onTaskRemoved(rootIntent)
    }

    override fun onRevoke() {
        VpnDebugger.log("VPN revocada por el sistema o por otra app")
        isRunning = false
        DnsFirewallStore(this).setServiceActive(false)
        stopping.set(true)
        packetThread?.interrupt()
        packetThread = null
        runCatching { tunnel?.close() }
        tunnel = null
        stopForeground(true)
        stopSelf()
    }

    private fun establishVpn() {
        runCatching {
            stopping.set(false)
            tunnel?.close()
            tunnel = Builder()
                .setSession("Aura Defense")
                .addAddress(VPN_ADDRESS, 32)
                .addDnsServer(VPN_DNS)
                .addRoute(VPN_DNS, 32)
                .addAddress(VPN_ADDRESS_V6, 128)
                .addRoute(VPN_DNS_V6, 128)
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
        ThreatFeedManager.shutdown()
        packetThread = null
        stopForeground(true)
        stopSelf()
    }

    private fun handleDnsPacket(packet: ByteArray, length: Int, vpnOutput: FileOutputStream): Boolean {
        try {
            if (ProfileManager.shouldBlockDueToProfile(this)) return false
            val store = dnsStore ?: return false
            val query = DnsPacketCodec.query(packet, length) ?: return false
            val domain = query.domain

            if (store.isAllowed(domain)) return false

            val verdict = ThreatBridge.enrichDomainCheck(domain, ThreatEngine)
            val profile = store.profile()
            val shouldBlock = when {
                store.isBlocked(domain) -> true
                !verdict.blocked -> false
                profile == DnsFirewallProfile.PERMITIR_TODO -> false
                else -> verdict.category != null && verdict.category in profile.categories
            }

            if (shouldBlock) {
                val category = verdict.category ?: "MANUAL"
                val severity = verdict.severity ?: "HIGH"
                VpnDebugger.log("Consulta DNS bloqueada [$category] $domain")
                DnsPacketCodec.blockedResponsePacket(packet, length, query)?.let(vpnOutput::write)
                vpnOutput.flush()
                store.recordBlocked(
                    DnsBlockedEvent(domain, category, severity, System.currentTimeMillis())
                )
                return true
            }
        } catch (e: Exception) {
            VpnDebugger.log("Error controlado al procesar una consulta DNS")
        }
        return false
    }

    private fun runDnsProxy() {
        val activeTunnel = tunnel ?: return
        runCatching {
            FileInputStream(activeTunnel.fileDescriptor).use { input ->
                FileOutputStream(activeTunnel.fileDescriptor).use { output ->
                    val packet = ByteArray(MAX_PACKET_SIZE)
                    while (!stopping.get()) {
                        val length = input.read(packet)
                        if (length > 0) {
                            if (handleDnsPacket(packet, length, output)) {
                                continue
                            } else {
                                try {
                                    DnsPacketCodec.query(packet, length)?.let { query ->
                                        forwardDnsQuery(packet, length, query.dnsOffset, output, "consulta DNS")
                                    }
                                } catch (e: Exception) {
                                    VpnDebugger.log("Error controlado en el procesamiento de red")
                                }
                            }
                        }
                    }
                }
            }
        }.onFailure {
            if (!stopping.get()) stopVpn()
        }
    }

    private fun forwardDnsQuery(
        queryPacket: ByteArray,
        length: Int,
        dnsOffset: Int,
        output: FileOutputStream,
        domain: String
    ) {
        var upstreamResponse: ByteArray? = null
        runCatching {
            DatagramSocket().use { socket ->
                if (!protect(socket)) {
                    Timber.e("No se pudo proteger el socket DNS")
                    return@use
                }
                socket.soTimeout = DNS_TIMEOUT_MS
                val upstream = InetAddress.getByName(UPSTREAM_DNS)
                val dnsPayload = queryPacket.copyOfRange(dnsOffset, length)
                socket.send(DatagramPacket(dnsPayload, dnsPayload.size, upstream, DNS_PORT))
                val responseBytes = ByteArray(MAX_DNS_PACKET_SIZE)
                val response = DatagramPacket(responseBytes, responseBytes.size)
                socket.receive(response)
                upstreamResponse = response.data.copyOf(response.length)
            }
        }.onFailure { error ->
            if (error is SocketTimeoutException) {
                Timber.w("Tiempo de espera agotado para upstream DNS: $domain")
            } else {
                Timber.e(error, "Error de upstream DNS para $domain: ${error.message}")
            }
        }
        val dnsResponse = upstreamResponse ?: return
        DnsPacketCodec.response(queryPacket, length, dnsResponse)?.let {
            output.write(it)
            output.flush()
        }
    }

    private fun notification(): Notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.aura_core_foreground)
        .setContentTitle("Protección VPN activa")
        .setContentText("Protección principal de consultas DNS locales UDP/53")
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
        private const val TAG = "AuraVpnService"
        private const val VPN_DNS = "10.0.0.1"
        private const val VPN_ADDRESS = "10.0.0.2"
        private const val VPN_DNS_V6 = "fd00:0:0:0:0:0:0:1"
        private const val VPN_ADDRESS_V6 = "fd00:0:0:0:0:0:0:2"
        private const val UPSTREAM_DNS = "1.1.1.1"
        private const val DNS_PORT = 53
        private const val UDP_HEADER_SIZE = 8
        private const val DNS_HEADER_SIZE = 12
        private const val DNS_TIMEOUT_MS = 3000
        private const val MAX_PACKET_SIZE = 32767
        private const val MAX_DNS_PACKET_SIZE = 4096
        private const val CHANNEL_ID = "aura_vpn"
        private const val NOTIFICATION_ID = 1801

        @Volatile
        var isRunning: Boolean = false
            private set
    }
}
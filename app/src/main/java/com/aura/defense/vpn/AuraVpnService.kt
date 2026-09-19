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
import com.aura.defense.ThreatIntelligenceRepositoryProvider
import com.aura.defense.data.SecurePrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class AuraVpnService : VpnService() {
    private var tunnel: ParcelFileDescriptor? = null
    private var packetThread: Thread? = null
    private val stopping = AtomicBoolean(false)
    private var ThreatEngine: ThreatIntelligenceEngine? = null
    private var decisionEngine: DnsDecisionEngine? = null
    private var dnsStore: DnsFirewallStore? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopVpn()
            return START_NOT_STICKY
        }

        return try {
            DnsProtectionStateStore.transition(DnsProtectionStatus.STARTING)
            createNotificationChannel()
            startForeground(NOTIFICATION_ID, notification())
            dnsStore = DnsFirewallStore(this)
            IpBlocklistLoader.init(this)
            ThreatEngine = ThreatIntelligenceEngine(this)
            decisionEngine = buildDecisionEngine()
            establishVpn()
            START_STICKY
        } catch (exception: Exception) {
            Timber.e(exception, "CRASH en onStartCommand del servicio VPN")
            DnsProtectionStateStore.transition(DnsProtectionStatus.ERROR, DnsDegradedReason.INTERNAL_FAILURE, "No se pudo iniciar la protección DNS")
            com.aura.defense.monitor.AuraProcessLog.log("⚠ Error al iniciar la protección DNS", "SISTEMA")
            stopSelf()
            START_NOT_STICKY
        }
    }

    override fun onDestroy() {
        val state = DnsProtectionStateStore.current()
        if (state.status != DnsProtectionStatus.ERROR || state.reason != DnsDegradedReason.VPN_REVOKED) {
            stopVpn()
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopVpn()
        super.onTaskRemoved(rootIntent)
    }

    override fun onRevoke() {
        com.aura.defense.data.SecurePrefs.get(this)
            .edit().putBoolean("aura_vpn_expected", false).apply()
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
        DnsProtectionStateStore.transition(DnsProtectionStatus.ERROR, DnsDegradedReason.VPN_REVOKED, "Android revocó la autorización VPN")
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
                .establish()
                ?: error("No se pudo establecer el túnel VPN")
            com.aura.defense.data.SecurePrefs.get(this)
                .edit().putBoolean("aura_vpn_expected", true).apply()
            com.aura.defense.monitor.AuraProcessLog.log("Túnel VPN activado: el tráfico DNS pasa por el cortafuegos", "VPN")
            DnsFirewallStore(this).apply {
                setServiceActive(true)
            }
            isRunning = true
            packetThread = Thread(::runDnsProxy, "AuraDnsFirewall").also { it.start() }
            val intelligenceReady = ThreatEngine?.indicators?.isNotEmpty() == true
            if (intelligenceReady) {
                DnsProtectionStateStore.transition(DnsProtectionStatus.ACTIVE_DNS_ONLY)
                VpnDebugger.log("Protección DNS activa: solo consultas DNS observadas")
            } else {
                DnsProtectionStateStore.transition(
                    DnsProtectionStatus.DEGRADED,
                    DnsDegradedReason.FEED_INVALID,
                    "No hay inteligencia DNS válida cargada"
                )
            }
        }.onFailure { throwable ->
            Timber.e(throwable, "No se pudo establecer el túnel VPN")
            isRunning = false
            runCatching { DnsFirewallStore(this).setServiceActive(false) }
                .onFailure { cleanupError -> Timber.e(cleanupError, "No se pudo limpiar el estado VPN") }
            DnsProtectionStateStore.transition(DnsProtectionStatus.ERROR, DnsDegradedReason.INTERNAL_FAILURE, "No se pudo establecer el descriptor VPN")
            runCatching { stopSelf() }
        }
    }

    fun stopVpn() {
        val currentStatus = DnsProtectionStateStore.current().status
        if (currentStatus in setOf(DnsProtectionStatus.STARTING, DnsProtectionStatus.ACTIVE_DNS_ONLY, DnsProtectionStatus.DEGRADED)) {
            DnsProtectionStateStore.transition(DnsProtectionStatus.STOPPING)
        }
        com.aura.defense.data.SecurePrefs.get(this)
            .edit().putBoolean("aura_vpn_expected", false).apply()
        com.aura.defense.monitor.AuraProcessLog.log("Túnel VPN desactivado", "VPN")
        isRunning = false
        DnsFirewallStore(this).setServiceActive(false)
        stopping.set(true)
        runCatching { tunnel?.close() }
        tunnel = null
        packetThread?.interrupt()
        decisionEngine = null
        ThreatEngine = null
        packetThread = null
        stopForeground(true)
        stopSelf()
        when (DnsProtectionStateStore.current().status) {
            DnsProtectionStatus.STOPPING -> DnsProtectionStateStore.transition(DnsProtectionStatus.OFF)
            DnsProtectionStatus.ERROR -> DnsProtectionStateStore.transition(DnsProtectionStatus.OFF)
            else -> Unit
        }
    }

    private fun handleDnsPacket(packet: ByteArray, length: Int, vpnOutput: FileOutputStream): Boolean {
        try {
            if (ProfileManager.shouldBlockDueToProfile(this)) return false
            val store = dnsStore ?: return false
            val query = DnsPacketCodec.query(packet, length) ?: return false
            val domain = query.domain

            if (store.isAllowed(domain)) return false

            val verdict = decisionEngine?.decide(domain)
                ?: DnsDecisionResult(DnsDecision.ERROR, domain, "decision_engine_unavailable")
            if (verdict.decision == DnsDecision.ERROR) {
                DnsProtectionStateStore.transition(
                    DnsProtectionStatus.DEGRADED,
                    if (verdict.reason == "expired_feed" || verdict.reason == "invalid_rule") DnsDegradedReason.FEED_INVALID else DnsDegradedReason.INTERNAL_FAILURE,
                    "La decisión DNS no está disponible: ${verdict.reason}"
                )
            }
            if (verdict.decision == DnsDecision.BLOCK) {
                val category = verdict.category ?: "MANUAL"
                val severity = verdict.severity ?: "HIGH"
                VpnDebugger.log("Consulta DNS bloqueada [$category]")
                DnsPacketCodec.blockedResponsePacket(packet, length, query)?.let(vpnOutput::write)
                vpnOutput.flush()
                store.recordBlocked(
                    DnsBlockedEvent(domain, category, severity, System.currentTimeMillis(), verdict.reason, verdict.source ?: "UNKNOWN", verdict.feedVersion ?: "UNKNOWN", verdict.ruleId ?: "UNKNOWN")
                )
                com.aura.defense.monitor.AuraProcessLog.log("Dominio DNS bloqueado [$category]", "RED")
                return true
            }
        } catch (e: Exception) {
            VpnDebugger.log("Error controlado al procesar una consulta DNS")
            DnsProtectionStateStore.transition(DnsProtectionStatus.DEGRADED, DnsDegradedReason.DNS_PARSE_FAILURE, "Fallo interno al evaluar una consulta")
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
                                        if (forwardDnsQuery(packet, length, query.dnsOffset, output)) {
                                            if (DnsProtectionStateStore.current().status == DnsProtectionStatus.DEGRADED) {
                                                DnsProtectionStateStore.transition(DnsProtectionStatus.ACTIVE_DNS_ONLY)
                                            }
                                        } else {
                                            DnsProtectionStateStore.transition(DnsProtectionStatus.DEGRADED, DnsDegradedReason.UPSTREAM_UNAVAILABLE, "El resolver upstream no respondió")
                                        }
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
            if (!stopping.get()) {
                DnsProtectionStateStore.transition(DnsProtectionStatus.ERROR, DnsDegradedReason.INTERNAL_FAILURE, "El hilo DNS terminó inesperadamente")
                stopVpn()
            }
        }
    }

    private fun forwardDnsQuery(
        queryPacket: ByteArray,
        length: Int,
        dnsOffset: Int,
        output: FileOutputStream
    ): Boolean {
        var upstreamResponse: ByteArray? = null
        repeat(UPSTREAM_RETRY_COUNT) { attempt ->
            if (upstreamResponse != null) return@repeat
            if (attempt > 0) runCatching { Thread.sleep(UPSTREAM_RETRY_DELAY_MS * attempt) }
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
                if (error is SocketTimeoutException) Timber.w("Tiempo de espera agotado para upstream DNS")
                else Timber.e(error, "Error de upstream DNS")
            }
        }
        val dnsResponse = upstreamResponse ?: return false
        DnsPacketCodec.response(queryPacket, length, dnsResponse)?.let {
            output.write(it)
            output.flush()
        }
        return true
    }

    private fun notification(): Notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.aura_core_foreground)
        .setContentTitle("Protección DNS local")
        .setContentText("Solo consultas DNS observadas por AURA; no es un túnel completo")
        .setOngoing(true)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .build()

    private fun createNotificationChannel() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getSystemService(NotificationManager::class.java)?.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Protección VPN", NotificationManager.IMPORTANCE_LOW)
                )
            }
        }.onFailure { Timber.e(it, "No se pudo crear el canal de notificación VPN") }
    }

    private fun buildDecisionEngine(): DnsDecisionEngine {
        val repository = ThreatIntelligenceRepositoryProvider.get(this)
        val rulesSource = VerifiedDnsRulesSource(repository)
        val store = dnsStore ?: DnsFirewallStore(this)
        return DnsDecisionEngine(
            profile = store.profile(),
            allowlist = store.allowlist().toSet(),
            blocklist = store.blocklist().toSet(),
            rules = emptyList(),
            temporaryExceptions = store.temporaryExceptions(),
            dynamicRuleLookup = null,
            rulesSource = { rulesSource.currentRules() }
        )
    }

    companion object {
        const val ACTION_STOP = "com.aura.defense.vpn.STOP"
        private const val TAG = "AuraVpnService"
        private const val VPN_DNS = "10.0.0.1"
        private const val VPN_ADDRESS = "10.0.0.2"
        private const val UPSTREAM_DNS = "1.1.1.1"
        private const val DNS_PORT = 53
        private const val UDP_HEADER_SIZE = 8
        private const val DNS_HEADER_SIZE = 12
        private const val DNS_TIMEOUT_MS = 3000
        private const val UPSTREAM_RETRY_COUNT = 2
        private const val UPSTREAM_RETRY_DELAY_MS = 200L
        private const val MAX_PACKET_SIZE = 32767
        private const val MAX_DNS_PACKET_SIZE = 4096
        private const val CHANNEL_ID = "aura_vpn"
        private const val NOTIFICATION_ID = 1801

        @Volatile
        var isRunning: Boolean = false
            private set
    }
}
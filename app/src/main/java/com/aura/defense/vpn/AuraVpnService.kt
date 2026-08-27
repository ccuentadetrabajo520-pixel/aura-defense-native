package com.aura.defense.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.aura.defense.R
import com.aura.defense.threats.ThreatIntelligenceEngine
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.Locale
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
                .addAddress(VPN_ADDRESS, 32)
                .addDnsServer(VPN_DNS)
                .addRoute(VPN_DNS, 32)
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
                        try {
                            val length = input.read(packet)
                            VpnDebugger.log("Paquete recibido del TUN, largo: $length")
                            if (length <= 0) break
                            VpnDebugger.log("Analizando si es paquete DNS...")
                            val query = DnsPacketCodec.query(packet, length)
                            if (query == null) continue
                            val domain = query.domain
                            val normalizedDomain = domain.trim().lowercase(Locale.ROOT).removeSuffix(".")
                            Log.d(TAG, "Consulta DNS: $normalizedDomain")
                            val profile = store.profile()
                            val allowlist = store.allowlist()
                            val blocklist = store.blocklist()
                            val domainAllowed = allowlist.any { normalizedDomain == it || normalizedDomain.endsWith(".$it") }
                            val domainBlocked = blocklist.any { normalizedDomain.contains(it) } || domain.contains("neverssl")
                            Log.d(TAG, "Perfil DNS: ${profile.label}, blocklist: ${blocklist.size}, dominio en blocklist: $domainBlocked")
                            val match = if (profile == DnsFirewallProfile.PERMITIR_TODO) {
                                null
                            } else {
                                threatEngine.findMatches(normalizedDomain).firstOrNull { indicator ->
                                    profile.categories.contains(indicator.category.name) && !domainAllowed
                                }
                            }
                            val manuallyBlocked = profile == DnsFirewallProfile.ESTRICTO &&
                                domainBlocked && !domainAllowed
                            val blocked = match != null || manuallyBlocked
                            Log.d(TAG, "Decisión DNS para $normalizedDomain: bloqueado=$blocked")
                            if (blocked) {
                                VpnDebugger.log("¡ALERTA! Dominio $normalizedDomain ESTÁ BLOQUEADO. Preparando respuesta falsa.")
                                Log.i(TAG, "Dominio bloqueado por DNS Firewall: $normalizedDomain")
                                val category = match?.category?.name ?: "MANUAL"
                                val severity = match?.severity?.name ?: "HIGH"
                                val responsePacket = DnsPacketCodec.blockedResponsePacket(packet, length, query)
                                if (responsePacket == null) {
                                    Log.e(TAG, "No se pudo construir la respuesta NXDOMAIN para $normalizedDomain")
                                    continue
                                }
                                output.write(responsePacket)
                                output.flush()
                                store.recordBlocked(DnsBlockedEvent(normalizedDomain, category, severity, System.currentTimeMillis()))
                                Log.d(TAG, "Contador actualizado: ${store.blockedCount()}")
                                continue
                            }
                            VpnDebugger.log("Dominio $normalizedDomain PERMITIDO. Reenviando a upstream...")
                            Log.i(TAG, "Dominio permitido: $normalizedDomain")
                            forwardDnsQuery(packet, length, query, output)
                        } catch (e: Exception) {
                            VpnDebugger.log("¡¡ERROR FATAL EN EL HILO VPN!!: ${e.message}")
                            throw e
                        }
                    }
                }
            }
        }.onFailure {
            if (!stopping.get()) stopVpn()
        }
    }

    private fun forwardDnsQuery(queryPacket: ByteArray, length: Int, query: DnsQuery, output: FileOutputStream) {
        var upstreamResponse: ByteArray? = null
        runCatching {
            DatagramSocket().use { socket ->
                if (!protect(socket)) {
                    Log.e(TAG, "No se pudo proteger el socket DNS")
                    return@use
                }
                socket.soTimeout = DNS_TIMEOUT_MS
                val upstream = InetAddress.getByName(UPSTREAM_DNS)
                val dnsPayload = DnsPacketCodec.dnsPayload(queryPacket, query)
                socket.send(DatagramPacket(dnsPayload, dnsPayload.size, upstream, DNS_PORT))
                val responseBytes = ByteArray(MAX_DNS_PACKET_SIZE)
                val response = DatagramPacket(responseBytes, responseBytes.size)
                socket.receive(response)
                upstreamResponse = response.data.copyOf(response.length)
            }
        }.onFailure { error ->
            if (error is SocketTimeoutException) {
                Log.w(TAG, "Tiempo de espera agotado para upstream DNS: ${query.domain}")
            } else {
                Log.e(TAG, "Error de upstream DNS para ${query.domain}: ${error.message}", error)
            }
        }
        val dnsResponse = upstreamResponse ?: run {
            Log.w(TAG, "SERVFAIL generado para ${query.domain}")
            DnsPacketCodec.servfailResponse(query)
        }
        sendVpnResponse(queryPacket, length, dnsResponse, output, query.domain)
    }

    private fun sendVpnResponse(
        queryPacket: ByteArray,
        length: Int,
        dnsResponse: ByteArray,
        output: FileOutputStream,
        domain: String
    ) {
        val responsePacket = DnsPacketCodec.response(queryPacket, length, dnsResponse)
        if (responsePacket == null) {
            Log.e(TAG, "No se pudo construir la respuesta DNS para $domain")
            return
        }
        output.write(responsePacket)
        output.flush()
        Log.d(TAG, "Respuesta DNS enviada al túnel para $domain (${responsePacket.size} bytes)")
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
        private const val TAG = "AuraVpnService"
        private const val VPN_DNS = "10.0.0.1"
        private const val VPN_ADDRESS = "10.0.0.2"
        private const val UPSTREAM_DNS = "1.1.1.1"
        private const val DNS_PORT = 53
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
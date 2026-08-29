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
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import com.aura.defense.threats.ThreatIntelligenceEngine

class AuraVpnService : VpnService() {
    private var tunnel: ParcelFileDescriptor? = null
    private var packetThread: Thread? = null
    private val stopping = AtomicBoolean(false)
    private var ThreatEngine: ThreatIntelligenceEngine? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopVpn()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, notification())
        VpnDebugger.log("✅ AURA VPN SERVICE INICIADO")
        ThreatFeedManager.init(this)
        ThreatEngine = ThreatIntelligenceEngine(this)
        establishVpn()
        return START_NOT_STICKY
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
        ThreatFeedManager.shutdown()
        packetThread = null
        stopForeground(true)
        stopSelf()
    }

    private fun handleDnsPacket(packet: ByteArray, length: Int, vpnOutput: FileOutputStream): Boolean {
        try {
            if (ProfileManager.shouldBlockDueToProfile(this)) return false
            if (length < 40 || (packet[9].toInt() and 0xFF) != 17) return false
            val ipHdrLen = (packet[0].toInt() and 0x0F) * 4
            val dstPort = ((packet[ipHdrLen + 2].toInt() and 0xFF) shl 8) or
                (packet[ipHdrLen + 3].toInt() and 0xFF)
            if (dstPort != 53) return false

            var offset = ipHdrLen + 20
            val domainBuilder = StringBuilder()
            var qEnd = offset
            while (offset < length) {
                val labelLength = packet[offset++].toInt() and 0xFF
                if (labelLength == 0) break
                if (labelLength > 63 || offset + labelLength > length) return false
                for (index in 0 until labelLength) {
                    domainBuilder.append((packet[offset++].toInt() and 0xFF).toChar())
                }
                domainBuilder.append(".")
            }
            qEnd = offset
            val domain = domainBuilder.toString().lowercase(Locale.ROOT).removeSuffix(".")
            VpnDebugger.log("DNS: $domain")

            val bridgeResult = ThreatBridge.enrichDomainCheck(domain, ThreatEngine)
            if (bridgeResult.blocked) {
                VpnDebugger.log("BLOCKED! domain [${bridgeResult.source}${bridgeResult.category?.let { " / $it" } ?: ""}]")
                val qBytes = packet.copyOfRange(ipHdrLen + 20, qEnd + 4)
                val udpLen = 8 + 12 + qBytes.size
                val newPkt = ByteArray(20 + udpLen)
                for (index in 0 until 20) newPkt[index] = packet[index]
                for (index in 12 until 16) newPkt[index] = packet[index + 4]
                for (index in 16 until 20) newPkt[index] = packet[index - 4]
                for (index in 0 until 2) newPkt[20 + index] = packet[ipHdrLen + 2 + index]
                for (index in 0 until 2) newPkt[22 + index] = packet[ipHdrLen + index]
                newPkt[24] = ((udpLen shr 8) and 0xFF).toByte()
                newPkt[25] = (udpLen and 0xFF).toByte()
                for (index in 0 until 2) newPkt[28 + index] = packet[ipHdrLen + index]
                newPkt[30] = 0x81.toByte()
                newPkt[31] = 0x83.toByte()
                newPkt[32] = 0
                newPkt[33] = 1
                qBytes.copyInto(newPkt, 40)

                newPkt[10] = 0
                newPkt[11] = 0
                var sum = 0L
                for (index in 0 until 20 step 2) {
                    sum += ((newPkt[index].toInt() and 0xFF) shl 8) or
                        (newPkt[index + 1].toInt() and 0xFF)
                }
                while ((sum ushr 16) != 0L) sum = (sum and 0xFFFF) + (sum ushr 16)
                val checksum = sum.inv() and 0xFFFF
                newPkt[10] = (checksum shr 8).toByte()
                newPkt[11] = checksum.toByte()
                vpnOutput.write(newPkt)
                vpnOutput.flush()
                DnsFirewallStore(this).recordBlocked(
                    DnsBlockedEvent(domain, bridgeResult.source ?: "MANUAL", bridgeResult.severity ?: "HIGH", System.currentTimeMillis())
                )
                return true
            }
        } catch (e: Exception) {
            VpnDebugger.log("DNS Error: ${e.message}")
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
                                    if (length > 28 && (packet[9].toInt() and 0xFF) == 17) {
                                        val ipHeaderLength = (packet[0].toInt() and 0x0F) * 4
                                        val destinationPort = readUnsignedShort(packet, ipHeaderLength + 2)
                                        if (destinationPort == DNS_PORT) {
                                            val dnsOffset = ipHeaderLength + UDP_HEADER_SIZE
                                            forwardDnsQuery(packet, length, ipHeaderLength, dnsOffset, output, "consulta DNS")
                                        }
                                    }
                                } catch (e: Exception) {
                                    VpnDebugger.log("FATAL LOOP ERROR: ${e.message}")
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
        ipHeaderLength: Int,
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
        val responsePacket = buildDnsResponsePacket(queryPacket, length, ipHeaderLength, null, dnsResponse)
        output.write(responsePacket)
        output.flush()
    }

    private fun buildDnsResponsePacket(
        queryPacket: ByteArray,
        length: Int,
        ipHeaderLength: Int,
        flags: Int?,
        dnsPayload: ByteArray
    ): ByteArray {
        val sourcePort = readUnsignedShort(queryPacket, ipHeaderLength)
        val destinationPort = readUnsignedShort(queryPacket, ipHeaderLength + 2)
        val packet = ByteArray(ipHeaderLength + UDP_HEADER_SIZE + dnsPayload.size)
        queryPacket.copyInto(packet, 0, 0, ipHeaderLength)
        queryPacket.copyInto(packet, 12, 16, 20)
        queryPacket.copyInto(packet, 16, 12, 16)
        writeUnsignedShort(packet, ipHeaderLength, destinationPort)
        writeUnsignedShort(packet, ipHeaderLength + 2, sourcePort)
        writeUnsignedShort(packet, ipHeaderLength + 4, UDP_HEADER_SIZE + dnsPayload.size)
        writeUnsignedShort(packet, ipHeaderLength + 6, 0)
        dnsPayload.copyInto(packet, ipHeaderLength + UDP_HEADER_SIZE)
        flags?.let { writeUnsignedShort(packet, ipHeaderLength + UDP_HEADER_SIZE + 2, it) }
        writeUnsignedShort(packet, 2, packet.size)
        packet[10] = 0
        packet[11] = 0
        writeUnsignedShort(packet, 10, checksum(packet, 0, ipHeaderLength))
        return packet
    }

    private fun findQuestionEnd(packet: ByteArray, start: Int, length: Int): Int? {
        var offset = start
        repeat(MAX_DNS_LABELS) {
            if (offset >= length) return null
            val labelLength = packet[offset++].toInt() and 0xFF
            if (labelLength == 0) return offset
            if (labelLength > 63 || offset + labelLength > length) return null
            offset += labelLength
        }
        return null
    }

    private fun readUnsignedShort(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xFF) shl 8) or (bytes[offset + 1].toInt() and 0xFF)

    private fun writeUnsignedShort(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = (value ushr 8).toByte()
        bytes[offset + 1] = value.toByte()
    }

    private fun checksum(bytes: ByteArray, offset: Int, length: Int): Int {
        var sum = 0L
        var index = offset
        while (index + 1 < offset + length) {
            sum += readUnsignedShort(bytes, index)
            index += 2
        }
        if (index < offset + length) sum += (bytes[index].toInt() and 0xFF) shl 8
        while (sum ushr 16 != 0L) sum = (sum and 0xFFFF) + (sum ushr 16)
        return sum.inv().toInt() and 0xFFFF
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
        private const val UDP_HEADER_SIZE = 8
        private const val DNS_HEADER_SIZE = 12
        private const val MAX_DNS_LABELS = 128
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
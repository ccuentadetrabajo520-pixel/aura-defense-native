package com.aura.defense.lan

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

class AuraLanDiscovery(private val context: Context) {
    suspend fun discover(
        auraId: String,
        guardianLevel: String,
        visible: Boolean,
        onPeer: (AuraLanPeer) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            DatagramSocket().use { socket ->
                socket.broadcast = true
                socket.soTimeout = RECEIVE_TIMEOUT_MS
                val discovery = JSONObject().apply {
                    put("type", TYPE_DISCOVERY)
                    put("auraId", auraId)
                    put("timestamp", System.currentTimeMillis())
                }.toString().toByteArray(Charsets.UTF_8)
                socket.send(DatagramPacket(discovery, discovery.size, InetAddress.getByName("255.255.255.255"), PORT))
                val deadline = System.currentTimeMillis() + RECEIVE_TIMEOUT_MS
                while (System.currentTimeMillis() < deadline) {
                    val buffer = ByteArray(MAX_PACKET_SIZE)
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        socket.receive(packet)
                    } catch (_: SocketTimeoutException) {
                        break
                    }
                    handlePacket(socket, packet, auraId, guardianLevel, visible, onPeer)
                }
            }
            true
        }.getOrDefault(false)
    }

    private fun handlePacket(
        socket: DatagramSocket,
        packet: DatagramPacket,
        ownAuraId: String,
        guardianLevel: String,
        visible: Boolean,
        onPeer: (AuraLanPeer) -> Unit
    ) {
        runCatching {
            val message = JSONObject(String(packet.data, packet.offset, packet.length, Charsets.UTF_8))
            when (message.optString("type")) {
                TYPE_DISCOVERY -> {
                    if (visible && message.optString("auraId") != ownAuraId) {
                        val response = responsePayload(ownAuraId, guardianLevel).toString().toByteArray(Charsets.UTF_8)
                        socket.send(DatagramPacket(response, response.size, packet.address, packet.port))
                    }
                }
                TYPE_RESPONSE -> {
                    val peerId = message.optString("auraId")
                    if (peerId.isNotBlank() && peerId != ownAuraId) {
                        onPeer(
                            AuraLanPeer(
                                auraId = peerId,
                                name = message.optString("name", "Dispositivo Aura"),
                                guardianLevel = message.optString("guardianLevel", "No disponible"),
                                timestamp = message.optLong("timestamp", 0L),
                                address = packet.address.hostAddress.orEmpty()
                            )
                        )
                    }
                }
            }
        }
    }

    private fun responsePayload(auraId: String, guardianLevel: String) = JSONObject().apply {
        put("type", TYPE_RESPONSE)
        put("auraId", auraId)
        put("name", "Dispositivo Aura")
        put("guardianLevel", guardianLevel)
        put("timestamp", System.currentTimeMillis())
    }

    private companion object {
        const val PORT = 48162
        const val RECEIVE_TIMEOUT_MS = 3000
        const val MAX_PACKET_SIZE = 2048
        const val TYPE_DISCOVERY = "AURA_DISCOVERY"
        const val TYPE_RESPONSE = "AURA_RESPONSE"
    }
}

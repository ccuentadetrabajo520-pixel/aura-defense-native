package com.aura.defense.vpn

internal data class DnsQuery(
    val domain: String,
    val transactionId: Int,
    val flags: Int,
    val questionType: Int,
    val questionClass: Int,
    val questionBytes: ByteArray,
    val dnsOffset: Int,
    val dnsLength: Int
)

internal object DnsPacketCodec {
    private const val IPV4_HEADER_SIZE = 20
    private const val UDP_HEADER_SIZE = 8
    private const val DNS_PORT = 53

    fun query(packet: ByteArray, length: Int): DnsQuery? {
        if (length < IPV4_HEADER_SIZE + UDP_HEADER_SIZE + 12) return null
        val versionAndLength = packet[0].toInt() and 0xff
        if (versionAndLength ushr 4 != 4) return null
        val ipHeaderLength = (versionAndLength and 0x0f) * 4
        if (ipHeaderLength < IPV4_HEADER_SIZE || length < ipHeaderLength + UDP_HEADER_SIZE + 12) return null
        if (packet[9].toInt() and 0xff != 17) return null
        val sourcePort = readUnsignedShort(packet, ipHeaderLength)
        val destinationPort = readUnsignedShort(packet, ipHeaderLength + 2)
        if (sourcePort == DNS_PORT || destinationPort != DNS_PORT) return null
        val dnsOffset = ipHeaderLength + UDP_HEADER_SIZE
        val flags = readUnsignedShort(packet, dnsOffset + 2)
        val questionCount = readUnsignedShort(packet, dnsOffset + 4)
        if ((flags and 0x8000) != 0 || questionCount < 1) return null
        val domain = readName(packet, dnsOffset + 12, length) ?: return null
        val questionEnd = questionEnd(packet, dnsOffset + 12, length) ?: return null
        if (questionEnd + 4 > length) return null
        val questionBytes = packet.copyOfRange(dnsOffset + 12, questionEnd + 4)
        return DnsQuery(
            domain = domain,
            transactionId = readUnsignedShort(packet, dnsOffset),
            flags = flags,
            questionType = readUnsignedShort(packet, questionEnd),
            questionClass = readUnsignedShort(packet, questionEnd + 2),
            questionBytes = questionBytes,
            dnsOffset = dnsOffset,
            dnsLength = length - dnsOffset
        )
    }

    fun dnsPayload(packet: ByteArray, query: DnsQuery): ByteArray =
        packet.copyOfRange(query.dnsOffset, query.dnsOffset + query.dnsLength)

    fun blockedResponse(query: DnsQuery): ByteArray {
        val response = ByteArray(12 + query.questionBytes.size)
        writeUnsignedShort(response, 0, query.transactionId)
        writeUnsignedShort(response, 2, 0x8000 or (query.flags and 0x7800) or (query.flags and 0x0100) or 0x0003)
        writeUnsignedShort(response, 4, 1)
        query.questionBytes.copyInto(response, 12)
        return response
    }

    fun response(queryPacket: ByteArray, length: Int, upstreamResponse: ByteArray): ByteArray? {
        if (length < IPV4_HEADER_SIZE + UDP_HEADER_SIZE || upstreamResponse.size < 12) return null
        val versionAndLength = queryPacket[0].toInt() and 0xff
        val ipHeaderLength = (versionAndLength and 0x0f) * 4
        val sourcePort = readUnsignedShort(queryPacket, ipHeaderLength)
        val destinationPort = readUnsignedShort(queryPacket, ipHeaderLength + 2)
        val sourceAddress = queryPacket.copyOfRange(12, 16)
        val destinationAddress = queryPacket.copyOfRange(16, 20)
        val packet = ByteArray(ipHeaderLength + UDP_HEADER_SIZE + upstreamResponse.size)
        queryPacket.copyInto(packet, 0, 0, ipHeaderLength)
        destinationAddress.copyInto(packet, 12)
        sourceAddress.copyInto(packet, 16)
        packet[8] = 64
        packet[9] = 17
        writeUnsignedShort(packet, ipHeaderLength, destinationPort)
        writeUnsignedShort(packet, ipHeaderLength + 2, sourcePort)
        writeUnsignedShort(packet, ipHeaderLength + 4, UDP_HEADER_SIZE + upstreamResponse.size)
        writeUnsignedShort(packet, ipHeaderLength + 6, 0)
        upstreamResponse.copyInto(packet, ipHeaderLength + UDP_HEADER_SIZE)
        writeUnsignedShort(packet, 2, packet.size)
        writeUnsignedShort(packet, 10, 0)
        writeUnsignedShort(packet, 10, checksum(packet, 0, ipHeaderLength))
        val udpChecksum = pseudoHeaderChecksum(packet, ipHeaderLength, upstreamResponse.size)
        writeUnsignedShort(packet, ipHeaderLength + 6, udpChecksum)
        return packet
    }

    private fun readName(packet: ByteArray, start: Int, length: Int): String? {
        var offset = start
        val labels = mutableListOf<String>()
        repeat(128) {
            if (offset >= length) return null
            val labelLength = packet[offset++].toInt() and 0xff
            if (labelLength == 0) return labels.joinToString(".").lowercase()
            if (labelLength > 63 || offset + labelLength > length) return null
            labels += String(packet, offset, labelLength, Charsets.US_ASCII)
            offset += labelLength
        }
        return null
    }

    private fun questionEnd(packet: ByteArray, start: Int, length: Int): Int? {
        var offset = start
        repeat(128) {
            if (offset >= length) return null
            val labelLength = packet[offset++].toInt() and 0xff
            if (labelLength == 0) return offset - 1
            if (labelLength > 63 || offset + labelLength > length) return null
            offset += labelLength
        }
        return null
    }

    private fun readUnsignedShort(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xff) shl 8) or (bytes[offset + 1].toInt() and 0xff)

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
        if (index < offset + length) sum += (bytes[index].toInt() and 0xff) shl 8
        while (sum ushr 16 != 0L) sum = (sum and 0xffff) + (sum ushr 16)
        return sum.inv().toInt() and 0xffff
    }

    private fun pseudoHeaderChecksum(packet: ByteArray, ipHeaderLength: Int, dnsLength: Int): Int {
        val udpLength = UDP_HEADER_SIZE + dnsLength
        val pseudo = ByteArray(12 + udpLength)
        packet.copyInto(pseudo, 0, 12, 20)
        pseudo[9] = 17
        writeUnsignedShort(pseudo, 10, udpLength)
        packet.copyInto(pseudo, 12, ipHeaderLength, ipHeaderLength + udpLength)
        return checksum(pseudo, 0, pseudo.size)
    }
}

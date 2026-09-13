package com.aura.defense.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DnsPacketCodecTest {
    @Test
    fun `extrae dominio de consulta DNS UDP IPv4`() {
        val packet = byteArrayOf(
            0x45, 0, 0, 0, 0, 0, 0, 0, 64, 17, 0, 0,
            10, 0, 0, 2, 8, 8, 8, 8,
            0x30, 0x39, 0, 53, 0, 28, 0, 0,
            0x12, 0x34, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0,
            3, 'w'.code.toByte(), 'w'.code.toByte(), 'w'.code.toByte(),
            6, 'a'.code.toByte(), 'u'.code.toByte(), 'r'.code.toByte(), 'a'.code.toByte(), 'd'.code.toByte(), 'e'.code.toByte(),
            3, 'c'.code.toByte(), 'o'.code.toByte(), 'm'.code.toByte(), 0,
            0, 1, 0, 1
        )

        val query = DnsPacketCodec.query(packet, packet.size)

        assertNotNull(query)
        assertEquals("www.aurade.com", query?.domain)
    }

    @Test
    fun `rechaza etiqueta DNS truncada`() {
        val packet = byteArrayOf(
            0x45, 0, 0, 0, 0, 0, 0, 0, 64, 17, 0, 0,
            10, 0, 0, 2, 8, 8, 8, 8,
            0x30, 0x39, 0, 53, 0, 16, 0, 0,
            0x12, 0x34, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0,
            5, 'a'.code.toByte()
        )

        assertEquals(null, DnsPacketCodec.query(packet, packet.size))
    }
}
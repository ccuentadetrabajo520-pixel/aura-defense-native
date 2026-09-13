package com.aura.defense.security

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

object DnsHijackDetector {
    data class HijackResult(
        val hijacked: Boolean?,
        val systemIps: List<String>,
        val dohIps: List<String>,
        val detail: String
    )

    suspend fun check(anchor: String = "cloudflare.com"): HijackResult = withContext(Dispatchers.IO) {
        coroutineScope {
            val systemDeferred = async {
                runCatching {
                    InetAddress.getAllByName(anchor)
                        .mapNotNull { it.hostAddress }
                        .filter { it.isNotEmpty() }
                }.getOrDefault(emptyList())
            }
            val dohDeferred = async {
                runCatching {
                    val connection = URL(
                        "https://cloudflare-dns.com/dns-query?name=$anchor&type=A"
                    ).openConnection() as HttpURLConnection
                    connection.connectTimeout = 8000
                    connection.readTimeout = 12000
                    connection.setRequestProperty("accept", "application/dns-json")
                    if (connection.responseCode != 200) {
                        connection.disconnect()
                        return@runCatching emptyList<String>()
                    }
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    connection.disconnect()
                    val answers = JSONObject(body).optJSONArray("Answer")
                    val ips = ArrayList<String>()
                    if (answers != null) {
                        for (index in 0 until answers.length()) {
                            val answer = answers.getJSONObject(index)
                            if (answer.optInt("type") == 1) ips.add(answer.optString("data"))
                        }
                    }
                    ips
                }.getOrDefault(emptyList())
            }
            val systemIps = systemDeferred.await()
            val dohIps = dohDeferred.await()
            when {
                systemIps.isEmpty() || dohIps.isEmpty() -> HijackResult(
                    null,
                    systemIps,
                    dohIps,
                    "No pude completar ambas resoluciones: verificación inconclusa (honestidad antes que un veredicto falso)."
                )
                systemIps.any { it in dohIps } -> HijackResult(
                    false,
                    systemIps,
                    dohIps,
                    "El DNS del sistema y DoH concuerdan para $anchor: sin indicios de secuestro."
                )
                else -> HijackResult(
                    true,
                    systemIps,
                    dohIps,
                    "DISCREPANCIA TOTAL entre el DNS del sistema y DoH para $anchor. Posible hijacking, portal cautivo o proxy corporativo. No introduzcas credenciales en esta red hasta verificar."
                )
            }
        }
    }
}

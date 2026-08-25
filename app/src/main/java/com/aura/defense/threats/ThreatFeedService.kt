package com.aura.defense.threats

import java.net.HttpURLConnection
import java.net.URL

class ThreatFeedService {
    fun download(urlValue: String?): String? = runCatching {
        if (urlValue.isNullOrBlank()) return null
        val url = URL(urlValue)
        require(url.protocol.equals("https", ignoreCase = true))
        (url.openConnection() as HttpURLConnection).run {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = false
            requestMethod = "GET"
            useCaches = false
            connect()
            if (responseCode !in 200..299) return null
            inputStream.use { input ->
                val output = StringBuilder()
                val buffer = ByteArray(8192)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_BYTES) return null
                    output.append(String(buffer, 0, count, Charsets.UTF_8))
                }
                output.toString()
            }
        }
    }.getOrNull()

    private companion object {
        const val TIMEOUT_MS = 15_000
        const val MAX_BYTES = 2 * 1024 * 1024
    }
}
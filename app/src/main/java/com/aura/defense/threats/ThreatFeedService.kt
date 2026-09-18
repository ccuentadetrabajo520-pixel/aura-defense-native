package com.aura.defense.threats

import java.net.HttpURLConnection
import java.net.URL

class ThreatFeedService : ThreatNetworkClient {
    override fun fetch(url: String, etag: String?, lastModified: String?): NetworkFetchResult? = runCatching {
        val urlValue = url.ifBlank { return null }
        val parsed = URL(urlValue)
        require(parsed.protocol.equals("https", ignoreCase = true))

        var currentUrl = parsed
        var redirectCount = 0
        while (redirectCount < MAX_REDIRECTS) {
            val connection = currentUrl.openConnection() as HttpURLConnection
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.instanceFollowRedirects = false
            connection.useCaches = false
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            if (!etag.isNullOrBlank()) connection.setRequestProperty("If-None-Match", etag)
            if (!lastModified.isNullOrBlank()) connection.setRequestProperty("If-Modified-Since", lastModified)
            val code = connection.responseCode
            if (code in 300..399) {
                val location = connection.getHeaderField("Location") ?: return null
                currentUrl = URL(currentUrl, location)
                if (!currentUrl.protocol.equals("https", ignoreCase = true)) return null
                redirectCount += 1
                continue
            }
            if (code == 304) return NetworkFetchResult(responseCode = 304, body = "", etag = connection.getHeaderField("ETag"), lastModified = connection.getHeaderField("Last-Modified"))
            if (code !in 200..299) return null

            val contentLength = connection.contentLengthLong
            if (contentLength in 1..MAX_BYTES && contentLength > MAX_BYTES) return null

            val raw = connection.inputStream.use { input ->
                val output = StringBuilder()
                val buffer = ByteArray(8192)
                var total = 0L
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_BYTES) return null
                    output.append(String(buffer, 0, count, Charsets.UTF_8))
                }
                output.toString()
            }
            return NetworkFetchResult(
                responseCode = code,
                body = raw,
                etag = connection.getHeaderField("ETag"),
                lastModified = connection.getHeaderField("Last-Modified")
            )
        }
        null
    }.getOrNull()

    private companion object {
        const val TIMEOUT_MS = 15_000
        const val MAX_BYTES = 2 * 1024 * 1024
        const val MAX_REDIRECTS = 3
    }
}
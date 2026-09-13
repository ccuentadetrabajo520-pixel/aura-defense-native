package com.aura.defense.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object DomainAgeChecker {
    data class DomainAge(val daysOld: Long?, val registrar: String?, val error: String? = null)

    suspend fun check(domain: String): DomainAge = withContext(Dispatchers.IO) {
        runCatching {
            val connection = URL("https://rdap.org/domain/$domain").openConnection() as HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 12000
            connection.setRequestProperty("Accept", "application/rdap+json")
            connection.instanceFollowRedirects = true
            if (connection.responseCode != 200) {
                val response = connection.responseCode
                connection.disconnect()
                return@runCatching DomainAge(null, null, "RDAP HTTP $response")
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            val json = JSONObject(body)
            val events = json.optJSONArray("events")
            var registrationDate: String? = null
            if (events != null) {
                for (index in 0 until events.length()) {
                    val event = events.getJSONObject(index)
                    if (event.optString("eventAction") == "registration") {
                        registrationDate = event.optString("eventDate")
                        break
                    }
                }
            }
            if (registrationDate == null) {
                return@runCatching DomainAge(null, null, "Sin fecha de registro en RDAP")
            }
            val registrationInstant = Instant.parse(registrationDate)
            val registrationDay = LocalDateTimeCompat.from(registrationInstant)
            val days = ChronoUnit.DAYS.between(registrationDay, LocalDate.now(ZoneId.of("UTC")))
            DomainAge(days, json.optString("handle", null))
        }.getOrElse { DomainAge(null, null, it.message ?: "Error consultando RDAP") }
    }

    private object LocalDateTimeCompat {
        fun from(instant: Instant): LocalDate =
            instant.atZone(ZoneId.of("UTC")).toLocalDate()
    }
}

package com.aura.defense.security

import android.content.Context
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONArray

data class EmailBreachResult(
        val email: String,
            val breachCount: Int,
                val breaches: List<BreachInfo>,
                    val checkedAt: String,
                        val error: String? = null
)

data class BreachInfo(
        val name: String,
            val domain: String,
                val breachDate: String,
                    val dataClasses: List<String>
)

class EmailBreachChecker(private val context: Context) {

        companion object {
                    private const val BASE_URL = "https://haveibeenpwned.com/api/v3"
        }

            fun check(email: String, apiKey: String): EmailBreachResult {
                        val ts = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                                return runCatching {
                                                val encoded = URLEncoder.encode(email, "UTF-8")
                                                            val urlObj = URL("$BASE_URL/breachedaccount/$encoded?truncateResponse=false")
                                                                        val conn = urlObj.openConnection() as HttpURLConnection
                                                                                    conn.connectTimeout = 15000
                                                                                                conn.readTimeout = 15000
                                                                                                            conn.requestMethod = "GET"
                                                                                                                        conn.setRequestProperty("hibp-api-key", apiKey)
                                                                                                                                    conn.setRequestProperty("user-agent", "AuraDefense-Android")

                                                                                                                                                val code = conn.responseCode
                                                                                                                                                            if (code == 404) { conn.disconnect(); return EmailBreachResult(email, 0, emptyList(), ts) }
                                                                                                                                                                        if (code == 401) { conn.disconnect(); return EmailBreachResult(email, -1, emptyList(), ts, "API key invalida. Obten una en haveibeenpwned.com/API/Key") }
                                                                                                                                                                                    if (code == 403) { conn.disconnect(); return EmailBreachResult(email, -1, emptyList(), ts, "Acceso denegado. Verifica tu API key.") }
                                                                                                                                                                                                if (code != 200) { conn.disconnect(); return EmailBreachResult(email, -1, emptyList(), ts, "Error del servidor: $code") }

                                                                                                                                                                                                            val body = conn.inputStream.bufferedReader().readText()
                                                                                                                                                                                                                        conn.disconnect()
                                                                                                                                                                                                                                    val arr = JSONArray(body)
                                                                                                                                                                                                                                                val list = mutableListOf<BreachInfo>()
                                                                                                                                                                                                                                                            for (i in 0 until arr.length()) {
                                                                                                                                                                                                                                                                                val obj = arr.getJSONObject(i)
                                                                                                                                                                                                                                                                                                val classes = obj.optJSONArray("DataClasses")
                                                                                                                                                                                                                                                                                                                val classList = mutableListOf<String>()
                                                                                                                                                                                                                                                                                                                                if (classes != null) {
                                                                                                                                                                                                                                                                                                                                                        for (j in 0 until classes.length()) classList.add(classes.getString(j))
                                                                                                                                                                                                                                                                                                                                }
                                                                                                                                                                                                                                                                                                                                                list.add(BreachInfo(obj.getString("Name"), obj.optString("Domain", ""), obj.getString("BreachDate"), classList))
                                                                                                                                                                                                                                                            }
                                                                                                                                                                                                                                                                        EmailBreachResult(email, list.size, list, ts)
                                }.onFailure { e ->
                                            Timber.e(e, "Email breach check failed")
                                                        EmailBreachResult(email, -1, emptyList(), ts, e.message)
                                                                }.getOrDefault(EmailBreachResult(email, -1, emptyList(), ts, "Error desconocido"))
            }
}

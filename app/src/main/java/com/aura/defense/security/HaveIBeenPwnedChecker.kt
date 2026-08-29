package com.aura.defense.security

import android.content.Context
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BreachCheckResult(
        val passwordMasked: String,
            val breachCount: Long,
                val checkedAt: String,
                    val error: String? = null
) {
        val isBreached: Boolean get() = breachCount > 0
}

class HaveIBeenPwnedChecker(private val context: Context) {

        fun checkPassword(password: String): BreachCheckResult {
                    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                            return try {
                                            val sha1 = sha1Hash(password)
                                                        val prefix = sha1.substring(0, 5).uppercase()
                                                                    val suffix = sha1.substring(5).uppercase()
                                                                                val url = URL("https://api.pwnedpasswords.com/range/$prefix")
                                                                                            val connection = (url.openConnection() as HttpURLConnection).apply {
                                                                                                                connectTimeout = 10_000
                                                                                                                                readTimeout = 10_000
                                                                                                                                                requestMethod = "GET"
                                                                                                                                                                setRequestProperty("Add-Padding", "true")
                                                                                                                                                                                setRequestProperty("User-Agent", "AuraDefense-Android")
                                                                                            }
                                                                                                        val response = connection.inputStream.bufferedReader().readText()
                                                                                                                    connection.disconnect()
                                                                                                                                val count = response.lineSequence().mapNotNull { line ->
                                                                                                                                                val parts = line.split(":", limit = 2)
                                                                                                                                                                if (parts.size == 2 && parts[0].trim() == suffix) parts[1].trim().toLongOrNull() ?: 0L else null
                                                                                                                                                                            }.firstOrNull() ?: 0L
                                                                                                                                                                                        BreachCheckResult(passwordMasked = maskPassword(password), breachCount = count, checkedAt = timestamp)
                            } catch (e: Exception) {
                                            Timber.e(e, "HIBP check failed")
                                                        BreachCheckResult(passwordMasked = maskPassword(password), breachCount = -1, checkedAt = timestamp, error = e.message)
                            }
        }

            private fun sha1Hash(input: String): String {
                        val digest = MessageDigest.getInstance("SHA-1")
                                val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
                                        return hash.joinToString("") { "%02x".format(it) }
            }

                private fun maskPassword(password: String): String =
                        if (password.length <= 2) "***" else password.first() + "*".repeat(password.length - 2) + password.last()
}
            }
                            }
                                                                                            }
                            }
        }
}
}
)

package com.aura.defense.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

data class TotpEntry(
        val id: String,
            val label: String,
                val issuer: String,
                    val encryptedSecret: String,
                        val iv: String,
                            val digits: Int,
                                val period: Int,
                                    val addedAt: String
)

data class TotpCode(
        val code: String,
            val remainingSeconds: Int,
                val label: String
)

class TotpAuthenticator(private val context: Context) {

        companion object {
                    private const val PREFS_NAME = "aura_totp"
                            private const val KEY_ALIAS = "aura_totp_enc_key"
                                    private const val DEFAULT_DIGITS = 6
                                            private const val DEFAULT_PERIOD = 30
                                                    private val BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        }

            private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

                fun generateCode(secret: String, digits: Int = DEFAULT_DIGITS, period: Int = DEFAULT_PERIOD): String {
                            val keyBytes = decodeBase32(secret)
                                    if (keyBytes.isEmpty()) return "ERROR"
                                            val time = System.currentTimeMillis() / 1000
                                                    var counter = time / period
                                                            val counterBytes = ByteArray(8)
                                                                    for (i in 7 downTo 0) {
                                                                                    counterBytes[i] = (counter and 0xFF).toByte()
                                                                                                counter = counter ushr 8
                                                                    }
                                                                            val hmac = javax.crypto.Mac.getInstance("HmacSHA1")
                                                                                    hmac.init(SecretKeySpec(keyBytes, "HmacSHA1"))
                                                                                            val hash = hmac.doFinal(counterBytes)
                                                                                                    val offset = hash[hash.size - 1].toInt() and 0x0F
                                                                                                            val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
                                                                                                                        ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                                                                                                                                    ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                                                                                                                                                (hash[offset + 3].toInt() and 0xFF)
                                                                                                                                                        var mod = 1
                                                                                                                                                                for (i in 0 until digits) { mod *= 10 }
                                                                                                                                                                        val otp = binary % mod
                                                                                                                                                                                return otp.toString().padStart(digits, '0')
                }

                    fun getRemainingSeconds(period: Int = DEFAULT_PERIOD): Int {
                                val time = System.currentTimeMillis() / 1000
                                        return (period - (time % period)).toInt()
                    }

                        fun saveEntry(label: String, issuer: String, secret: String, digits: Int = DEFAULT_DIGITS, period: Int = DEFAULT_PERIOD): String {
                                    val id = "totp_${System.currentTimeMillis()}"
                                            val key = getOrCreateKey()
                                                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                                                            cipher.init(Cipher.ENCRYPT_MODE, key)
                                                                    val ivBytes = cipher.iv
                                                                            val encrypted = cipher.doFinal(secret.toByteArray(Charsets.UTF_8))
                                                                                    val ts = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                                                                                            val json = "{\"id\":\"$id\",\"label\":\"$label\",\"issuer\":\"$issuer\"," +
                                                                                                        "\"enc\":\"${Base64.encodeToString(encrypted, Base64.NO_WRAP)}\"," +
                                                                                                                    "\"iv\":\"${Base64.encodeToString(ivBytes, Base64.NO_WRAP)}\"," +
                                                                                                                                "\"digits\":$digits,\"period\":$period,\"added\":\"$ts\"}"
                                                                                                                                        prefs.edit().putString(id, json).apply()
                                                                                                                                                val ids = getEntryIds().toMutableList()
                                                                                                                                                        ids.add(id)
                                                                                                                                                                prefs.edit().putString("entry_ids", ids.joinToString(",")).apply()
                                                                                                                                                                        return id
                        }

                            fun getEntryIds(): List<String> {
                                        val stored = prefs.getString("entry_ids", "") ?: ""
                                                return if (stored.isBlank()) emptyList() else stored.split(",")
                            }

                                fun getEntries(): List<TotpEntry> {
                                            return getEntryIds().mapNotNull { id ->
                                                        val json = prefs.getString(id, null) ?: return@mapNotNull null
                                                                    parseEntry(json)
                                                                            }
                                }

                                    fun generateCodeForEntry(entry: TotpEntry): String {
                                                val key = getOrCreateKey()
                                                        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                                                                val spec = GCMParameterSpec(128, Base64.decode(entry.iv, Base64.NO_WRAP))
                                                                        cipher.init(Cipher.DECRYPT_MODE, key, spec)
                                                                                val decrypted = runCatching {
                                                                                                cipher.doFinal(Base64.decode(entry.encryptedSecret, Base64.NO_WRAP))
                                                                                }.getOrDefault(byteArrayOf())
                                                                                        val secret = String(decrypted, Charsets.UTF_8)
                                                                                                return generateCode(secret, entry.digits, entry.period)
                                    }

                                        fun deleteEntry(id: String) {
                                                    prefs.edit().remove(id).apply()
                                                            val ids = getEntryIds().toMutableList()
                                                                    ids.remove(id)
                                                                            prefs.edit().putString("entry_ids", ids.joinToString(",")).apply()
                                        }

                                            private fun parseEntry(json: String): TotpEntry? {
                                                        return runCatching {
                                                                        val idVal = json.substringAfter("\"id\":\"").substringBefore("\"")
                                                                                    val labelVal = json.substringAfter("\"label\":\"").substringBefore("\"")
                                                                                                val issuerVal = json.substringAfter("\"issuer\":\"").substringBefore("\"")
                                                                                                            val encVal = json.substringAfter("\"enc\":\"").substringBefore("\"")
                                                                                                                        val ivVal = json.substringAfter("\"iv\":\"").substringBefore("\"")
                                                                                                                                    val digitsVal = json.substringAfter("\"digits\":").substringBefore(",").toInt()
                                                                                                                                                val periodVal = json.substringAfter("\"period\":").substringBefore(",").toInt()
                                                                                                                                                            val addedVal = json.substringAfter("\"added\":\"").substringBefore("\"")
                                                                                                                                                                        TotpEntry(idVal, labelVal, issuerVal, encVal, ivVal, digitsVal, periodVal, addedVal)
                                                        }.getOrNull()
                                            }

                                                private fun getOrCreateKey(): java.security.Key {
                                                            return runCatching {
                                                                            val ks = KeyStore.getInstance("AndroidKeyStore")
                                                                                        ks.load(null)
                                                                                                    if (ks.containsAlias(KEY_ALIAS)) {
                                                                                                                        val entry = ks.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
                                                                                                                                        entry.secretKey
                                                                                                    } else {
                                                                                                                        val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
                                                                                                                                        kg.init(KeyGenParameterSpec.Builder(KEY_ALIAS,
                                                                                                                                                            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                                                                                                                                                                                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                                                                                                                                                                                                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                                                                                                                                                                                                                        .setKeySize(256)
                                                                                                                                                                                                                                            .build())
                                                                                                                                                                                                                                                            kg.generateKey()
                                                                                                    }
                                                            }.getOrDefault(SecretKeySpec(ByteArray(32) { 0 }, "AES"))
                                                }

                                                    private fun decodeBase32(input: String): ByteArray {
                                                                val cleaned = input.uppercase(Locale.ROOT).replace(" ", "").replace("=", "")
                                                                        if (cleaned.isEmpty()) return ByteArray(0)
                                                                                val bits = StringBuilder()
                                                                                        for (c in cleaned) {
                                                                                                        val charIndex = BASE32_CHARS.indexOf(c)
                                                                                                                    if (charIndex < 0) continue
                                                                                                                                bits.append(charIndex.toString(2).padStart(5, '0'))
                                                                                        }
                                                                                                val bytes = mutableListOf<Byte>()
                                                                                                        var i = 0
                                                                                                                while (i + 8 <= bits.length) {
                                                                                                                                bytes.add(bits.substring(i, i + 8).toInt(2).toByte())
                                                                                                                                            i += 8
                                                                                                                }
                                                                                                                        return bytes.toByteArray()
                                                    }
}

package com.aura.defense.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import org.json.JSONArray
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
        }

            private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

                fun generateCode(secret: String, digits: Int = DEFAULT_DIGITS, period: Int = DEFAULT_PERIOD): String {
                            return generateTotpCode(secret, System.currentTimeMillis() / 1000, digits, period)
                }

                    fun getRemainingSeconds(period: Int = DEFAULT_PERIOD): Int {
                                require(period > 0) { "El periodo TOTP debe ser positivo" }
                                val time = System.currentTimeMillis() / 1000
                                        return (period - (time % period)).toInt()
                    }

                        fun saveEntry(label: String, issuer: String, secret: String, digits: Int = DEFAULT_DIGITS, period: Int = DEFAULT_PERIOD): String {
                                                            require(label.isNotBlank()) { "La etiqueta TOTP no puede estar vacía" }
                                                                    require(issuer.isNotBlank()) { "El emisor TOTP no puede estar vacío" }
                                                                            require(digits in 6..8) { "Los códigos TOTP deben tener entre 6 y 8 dígitos" }
                                                                                    require(period > 0) { "El periodo TOTP debe ser positivo" }
                                                                                            require(decodeBase32(secret).isNotEmpty()) { "El secreto TOTP no es válido" }
                                                                                                    val id = "totp_${java.util.UUID.randomUUID()}"
                                            val key = getOrCreateKey()
                                                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                                                            cipher.init(Cipher.ENCRYPT_MODE, key)
                                                                    val ivBytes = cipher.iv
                                                                            val encrypted = cipher.doFinal(secret.toByteArray(Charsets.UTF_8))
                                                                                    val ts = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                                                                                                            val json = JSONObject().apply {
                                                                                                                        put("id", id)
                                                                                                                                    put("label", label)
                                                                                                                                                put("issuer", issuer)
                                                                                                                                                            put("enc", Base64.encodeToString(encrypted, Base64.NO_WRAP))
                                                                                                                                                                        put("iv", Base64.encodeToString(ivBytes, Base64.NO_WRAP))
                                                                                                                                                                                    put("digits", digits)
                                                                                                                                                                                                put("period", period)
                                                                                                                                                                                                            put("added", ts)
                                                                                                            }.toString()
                                                                                                                                        prefs.edit().putString(id, json).apply()
                                                                                                                                                val ids = getEntryIds().toMutableList()
                                                                                                                                                        ids.add(id)
                                                                                                                                                                prefs.edit().putString("entry_ids", encodeEntryIds(ids)).apply()
                                                                                                                                                                        return id
                        }

                            fun getEntryIds(): List<String> {
                                        val stored = prefs.getString("entry_ids", "") ?: ""
                                                if (stored.isBlank()) return emptyList()
                                                        return runCatching {
                                                                        if (stored.trimStart().startsWith("[")) {
                                                                                            val array = JSONArray(stored)
                                                                                                        List(array.length()) { index -> array.getString(index) }
                                                                        } else {
                                                                                            stored.split(",")
                                                                        }
                                                        }.getOrElse { emptyList() }
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
                                                                                }.getOrElse { error ->
                                                                                                throw IllegalStateException("No se pudo descifrar la entrada TOTP", error)
                                                                                }
                                                                                        val secret = String(decrypted, Charsets.UTF_8)
                                                                                                return generateCode(secret, entry.digits, entry.period)
                                    }

                                        fun deleteEntry(id: String) {
                                                    prefs.edit().remove(id).apply()
                                                            val ids = getEntryIds().toMutableList()
                                                                    ids.remove(id)
                                                                            prefs.edit().putString("entry_ids", encodeEntryIds(ids)).apply()
                                        }

                                            private fun parseEntry(json: String): TotpEntry? {
                                                        return runCatching {
                                                                        val objectJson = JSONObject(json)
                                                                                    val idVal = objectJson.getString("id")
                                                                                                val labelVal = objectJson.getString("label")
                                                                                                            val issuerVal = objectJson.getString("issuer")
                                                                                                                        val encVal = objectJson.getString("enc")
                                                                                                                                    val ivVal = objectJson.getString("iv")
                                                                                                                                                val digitsVal = objectJson.getInt("digits")
                                                                                                                                                            val periodVal = objectJson.getInt("period")
                                                                                                                                                                        val addedVal = objectJson.getString("added")
                                                                                                                                                                        TotpEntry(idVal, labelVal, issuerVal, encVal, ivVal, digitsVal, periodVal, addedVal)
                                                        }.getOrNull()
                                            }

                                                private fun encodeEntryIds(ids: List<String>): String = JSONArray().apply {
                                                            ids.forEach(::put)
                                                }.toString()

                                                private fun getOrCreateKey(): java.security.Key {
                                                            return try {
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
                                                            } catch (error: Exception) {
                                                                        throw IllegalStateException("No se pudo acceder al almacenamiento seguro de Android", error)
                                                            }
                                                }

}

internal fun generateTotpCode(secret: String, epochSeconds: Long, digits: Int = 6, period: Int = 30): String {
        require(digits in 6..8) { "Los códigos TOTP deben tener entre 6 y 8 dígitos" }
        require(period > 0) { "El periodo TOTP debe ser positivo" }
        val keyBytes = decodeBase32(secret)
        require(keyBytes.isNotEmpty()) { "El secreto TOTP no es válido" }
        var counter = epochSeconds / period
        val counterBytes = ByteArray(8)
        for (index in 7 downTo 0) {
                counterBytes[index] = (counter and 0xFF).toByte()
                counter = counter ushr 8
        }
        val hmac = javax.crypto.Mac.getInstance("HmacSHA1")
        hmac.init(SecretKeySpec(keyBytes, "HmacSHA1"))
        val hash = hmac.doFinal(counterBytes)
        val offset = hash[hash.lastIndex].toInt() and 0x0F
        val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
                ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                (hash[offset + 3].toInt() and 0xFF)
        val modulus = 10.0.pow(digits).toInt()
        return (binary % modulus).toString().padStart(digits, '0')
}

private const val BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

private fun decodeBase32(input: String): ByteArray {
        val cleaned = input.uppercase(Locale.ROOT).replace(" ", "").replace("=", "")
        if (cleaned.isEmpty()) return ByteArray(0)
        val bits = StringBuilder()
        for (character in cleaned) {
                val charIndex = BASE32_CHARS.indexOf(character)
                if (charIndex < 0) return ByteArray(0)
                bits.append(charIndex.toString(2).padStart(5, '0'))
        }
        val bytes = mutableListOf<Byte>()
        var index = 0
        while (index + 8 <= bits.length) {
                bytes.add(bits.substring(index, index + 8).toInt(2).toByte())
                index += 8
        }
        return bytes.toByteArray()
}

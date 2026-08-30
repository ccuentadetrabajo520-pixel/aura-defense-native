package com.aura.defense.security

import android.content.Context
import android.os.Build
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ClipboardScanResult(
        val hasUrl: Boolean,
            val url: String?,
                val hasSensitivePattern: Boolean,
                    val sensitiveType: String?,
                        val scannedAt: String,
                            val clipLabel: String?
)

class ClipboardMonitor(private val context: Context) {

        companion object {
                    private val URL_PATTERN = Regex("(https?://[\\w\\-._~:/?#@!\\$&'()*+,;=%]+)")
                            private val EMAIL_PATTERN = Regex("[\\w.-]+@[\\w.-]+\\.\\w{2,}")
                                    private val PHONE_PATTERN = Regex("\\+?\\d{7,15}")
                                            private val CRYPTO_WALLET = Regex("\\b(0x[a-fA-F0-9]{40}|[13][a-km-zA-HJ-NP-Z1-9]{25,34}|bc1[a-z0-9]{39,59})\\b")
        }

            fun scanCurrent(): ClipboardScanResult {
                        val ts = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                                return runCatching {
                                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                                                ?: return ClipboardScanResult(false, null, false, null, ts, null)

                                                                            if (!cm.hasPrimaryClip()) return ClipboardScanResult(false, null, false, null, ts, null)
                                                                                        val clip = cm.primaryClip ?: return ClipboardScanResult(false, null, false, null, ts, null)
                                                                                                    val item = clip.getItemAt(0)
                                                                                                                val text = item.text?.toString() ?: item.coerceToText(context).toString()
                                                                                                                            val label = clip.description?.label?.toString()

                                                                                                                                        val urlMatch = URL_PATTERN.find(text)
                                                                                                                                                    var sensitiveType: String? = null
                                                                                                                                                                if (EMAIL_PATTERN.containsMatchIn(text)) sensitiveType = "Email detectado en el portapapeles"
                                                                                                                                                                            if (PHONE_PATTERN.containsMatchIn(text) && sensitiveType == null) sensitiveType = "Numero de telefono detectado"
                                                                                                                                                                                        if (CRYPTO_WALLET.containsMatchIn(text)) sensitiveType = "Posible wallet de criptomonedas detectado"

                                                                                                                                                                                                    ClipboardScanResult(hasUrl = urlMatch != null, url = urlMatch?.value, hasSensitivePattern = sensitiveType != null, sensitiveType = sensitiveType, scannedAt = ts, clipLabel = label)
                                }.onFailure { e ->
                                            Timber.e(e, "Clipboard scan failed")
                                                        ClipboardScanResult(false, null, false, null, ts, null)
                                                                }.getOrDefault(ClipboardScanResult(false, null, false, null, ts, null))
            }
}

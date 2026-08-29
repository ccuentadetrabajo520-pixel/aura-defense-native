package com.aura.defense.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.aura.defense.tools.LinkAnalyzer
import timber.log.Timber
import com.aura.defense.tools.LinkRisk

class AuraNotificationListenerService : NotificationListenerService() {
    private val urlRegex = Regex(
        pattern = """https?://[^\s<>\"']+|www\.[^\s<>\"']+""",
        option = RegexOption.IGNORE_CASE
    )

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        runCatching {
            if (sbn == null) return
            val urls = extractUrls(extractNotificationText(sbn))
            if (urls.isEmpty()) return

            val store = NotificationAlertStore(applicationContext)
            val now = System.currentTimeMillis()
            urls.forEach { url ->
                val analysis = LinkAnalyzer(applicationContext).analyze(url)
                if (analysis.risk != LinkRisk.SEGURO) {
                    store.add(NotificationAlert(sbn.packageName, now, url, analysis))
                }
            }
        }.onFailure {
            Timber.e("Error al procesar notificación")
        }
    }

    private fun extractNotificationText(sbn: StatusBarNotification): String {
        val extras = sbn.notification.extras ?: return ""
        val parts = mutableListOf<String>()

        extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.let { parts.add(it) }
        extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.let { parts.add(it) }
        extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.let { parts.add(it) }
        extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.let { parts.add(it) }
        extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.forEach { line ->
            line?.toString()?.let { parts.add(it) }
        }

        return parts.joinToString(" ")
    }

    private fun extractUrls(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        return urlRegex.findAll(text)
            .map { it.value.trim().trimEnd('.', ',', ';', ':', ')', ']', '}') }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
    }
}
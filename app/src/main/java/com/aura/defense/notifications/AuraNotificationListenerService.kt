package com.aura.defense.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.aura.defense.tools.LinkAnalyzer
import com.aura.defense.tools.LinkRisk

class AuraNotificationListenerService : NotificationListenerService() {
    private val urlPattern = Regex("https?://[^\\s<>\\\"']+")

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        runCatching {
            val notification = sbn.notification
            val extras = notification.extras ?: return
            val visibleText = buildList {
                extras.getCharSequence("android.text")?.toString()?.let(::add)
                extras.getCharSequence("android.bigText")?.toString()?.let(::add)
                extras.getCharSequenceArray("android.textLines")?.forEach { add(it.toString()) }
            }
            val store = NotificationAlertStore(applicationContext)
            val now = System.currentTimeMillis()
            visibleText.flatMap { text -> urlPattern.findAll(text).map { it.value.trimEnd('.', ',', ';', ':', ')', ']', '}') }.distinct().forEach { url ->
                val analysis = LinkAnalyzer().analyze(url)
                if (analysis.risk != LinkRisk.SEGURO) {
                    store.add(NotificationAlert(sbn.packageName, now, url, analysis))
                }
            }
        }
    }
}
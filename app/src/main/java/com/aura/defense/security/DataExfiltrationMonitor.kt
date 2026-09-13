package com.aura.defense.security

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager

data class AppTraffic(val packageName: String, val totalBytes: Long)

object DataExfiltrationMonitor {
    fun last24hPerApp(context: Context): List<AppTraffic> = runCatching {
        val networkStatsManager = context.getSystemService(NetworkStatsManager::class.java)
            ?: return emptyList()
        val now = System.currentTimeMillis()
        val start = now - 24L * 60 * 60 * 1000
        val packageManager = context.packageManager
        val totals = LinkedHashMap<String, Long>()
        for (networkType in intArrayOf(ConnectivityManager.TYPE_WIFI, ConnectivityManager.TYPE_MOBILE)) {
            val bucket: NetworkStats = networkStatsManager.querySummary(
                networkType,
                null,
                start,
                now
            ) ?: continue
            try {
                val item = NetworkStats.Bucket()
                while (bucket.hasNextBucket()) {
                    bucket.getNextBucket(item)
                    if (item.uid <= 10000) continue
                    val packageName = packageManager.getNameForUid(item.uid) ?: continue
                    val bytes = item.rxBytes + item.txBytes
                    totals[packageName] = (totals[packageName] ?: 0L) + bytes
                }
            } finally {
                bucket.close()
            }
        }
        totals.entries
            .map { AppTraffic(it.key, it.value) }
            .sortedByDescending { it.totalBytes }
    }.getOrDefault(emptyList())

    fun topUploader(context: Context): AppTraffic? = last24hPerApp(context).firstOrNull()
}

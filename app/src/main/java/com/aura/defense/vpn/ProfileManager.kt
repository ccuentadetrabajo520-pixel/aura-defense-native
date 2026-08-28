package com.aura.defense.vpn
import android.content.Context
import android.net.ConnectivityManager

object ProfileManager {
        var isFamilyModeEnabled = false
            var blockCellularData = false

                fun shouldBlockDueToProfile(context: Context): Boolean {
                            if (!blockCellularData) return false
                                    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                                            val network = cm.activeNetwork ?: return false
                                                    val caps = network.getCapabilities()
                                                            return !caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)
                }
}

package com.aura.defense.vpn

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

object ProfileManager {
    var isFamilyModeEnabled = false
    var blockCellularData = false

    @android.annotation.SuppressLint("DEPRECATION")
    fun shouldBlockDueToProfile(context: Context): Boolean {
        if (!blockCellularData) return false

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            return !caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        }

        val mobileNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
        return mobileNetwork?.isConnected == false
    }
}

package com.aura.defense.vpn

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

object ProfileManager {
    private const val PREFERENCES_NAME = "aura_profile_prefs"
    private const val FAMILY_MODE_KEY = "is_family_mode_enabled"
    private const val BLOCK_CELLULAR_DATA_KEY = "block_cellular_data"

    private lateinit var appContext: Context

    private val preferences by lazy {
        appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    var isFamilyModeEnabled: Boolean = false
        private set

    var blockCellularData: Boolean = false
        private set

    fun loadProfile(context: Context) {
        appContext = context.applicationContext
        isFamilyModeEnabled = preferences.getBoolean(FAMILY_MODE_KEY, false)
        blockCellularData = preferences.getBoolean(BLOCK_CELLULAR_DATA_KEY, false)
    }

    fun setFamilyModeEnabled(context: Context, value: Boolean) {
        loadProfile(context)
        isFamilyModeEnabled = value
        preferences.edit().putBoolean(FAMILY_MODE_KEY, value).apply()
    }

    fun setCellularDataBlocked(context: Context, value: Boolean) {
        loadProfile(context)
        blockCellularData = value
        preferences.edit().putBoolean(BLOCK_CELLULAR_DATA_KEY, value).apply()
    }

    fun reset(context: Context) {
        loadProfile(context)
        preferences.edit().clear().apply()
        isFamilyModeEnabled = false
        blockCellularData = false
    }

    @android.annotation.SuppressLint("DEPRECATION")
    fun shouldBlockDueToProfile(context: Context): Boolean {
        loadProfile(context)
        if (!blockCellularData) return false

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            return caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        }

        val activeNetwork = cm.activeNetworkInfo ?: return false
        return activeNetwork.isConnected && activeNetwork.type == ConnectivityManager.TYPE_MOBILE
    }
}

package com.aura.defense.vpn

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

object ProfileManager {
    private const val PREFERENCES_NAME = "aura_profile_prefs"
    private const val FAMILY_MODE_KEY = "is_family_mode_enabled"
    private const val BLOCK_CELLULAR_DATA_KEY = "block_cellular_data"

    private var appContext: Context? = null

    private val preferences by lazy {
        requireNotNull(appContext) { "ProfileManager must be initialized with a Context" }
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    var isFamilyModeEnabled: Boolean
        get() = preferences.getBoolean(FAMILY_MODE_KEY, false)
        set(value) {
            preferences.edit().putBoolean(FAMILY_MODE_KEY, value).apply()
        }

    var blockCellularData: Boolean
        get() = preferences.getBoolean(BLOCK_CELLULAR_DATA_KEY, false)
        set(value) {
            preferences.edit().putBoolean(BLOCK_CELLULAR_DATA_KEY, value).apply()
        }

    fun initialize(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
    }

    fun reset() {
        preferences.edit().clear().apply()
    }

    @android.annotation.SuppressLint("DEPRECATION")
    fun shouldBlockDueToProfile(context: Context): Boolean {
        initialize(context)
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

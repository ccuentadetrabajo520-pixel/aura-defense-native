package com.aura.defense.data.model

data class SecurityStatus(
    val score: Int,
    val threatsFound: Int,
    val appsScanned: Int,
    val isVpnActive: Boolean,
    val isDeveloperModeEnabled: Boolean,
    val lastScanTime: String
)
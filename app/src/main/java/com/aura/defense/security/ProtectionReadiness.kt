package com.aura.defense.security

object ProtectionReadiness {
    fun isProtected(
        vpnServiceRunning: Boolean,
        threatFeedEntries: Int,
        threatEngineActive: Boolean
    ): Boolean = vpnServiceRunning && threatFeedEntries > 0 && threatEngineActive
}
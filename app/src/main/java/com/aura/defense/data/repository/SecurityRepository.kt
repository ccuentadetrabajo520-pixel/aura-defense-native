package com.aura.defense.data.repository

import android.content.Context
import com.aura.defense.data.model.SecurityStatus
import com.aura.defense.data.model.Threat
import com.aura.defense.utils.SecurityUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SecurityRepository(private val context: Context) {
    private val _threats = MutableStateFlow<List<Threat>>(emptyList())
    val threats: StateFlow<List<Threat>> = _threats

    private val _securityStatus = MutableStateFlow(createInitialStatus())
    val securityStatus: StateFlow<SecurityStatus> = _securityStatus

    private fun createInitialStatus(): SecurityStatus {
        return SecurityStatus(
            score = calculateScore(),
            threatsFound = 0,
            appsScanned = 0,
            isVpnActive = SecurityUtils.isVpnActive(context),
            isDeveloperModeEnabled = SecurityUtils.isDeveloperModeEnabled(context),
            lastScanTime = getCurrentTime()
        )
    }

    fun refreshStatus() {
        _securityStatus.value = SecurityStatus(
            score = calculateScore(),
            threatsFound = _threats.value.size,
            appsScanned = 127,
            isVpnActive = SecurityUtils.isVpnActive(context),
            isDeveloperModeEnabled = SecurityUtils.isDeveloperModeEnabled(context),
            lastScanTime = getCurrentTime()
        )
    }

    fun addThreat(threat: Threat) {
        _threats.value = _threats.value + threat
        refreshStatus()
    }

    fun clearThreats() {
        _threats.value = emptyList()
        refreshStatus()
    }

    private fun calculateScore(): Int {
        var score = 100
        if (SecurityUtils.isDeveloperModeEnabled(context)) score -= 15
        if (!SecurityUtils.isVpnActive(context)) score -= 10
        score -= _threats.value.size * 5
        return maxOf(score, 0)
    }

    private fun getCurrentTime(): String {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        return format.format(Date())
    }
}
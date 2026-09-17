package com.aura.defense.ui

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aura.defense.vpn.DnsFirewallProfile
import com.aura.defense.vpn.DnsProtectionState
import com.aura.defense.vpn.DnsProtectionStatus
import com.aura.defense.ui.screens.DefenseScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DnsProtectionUiInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun inactiveStateNeverShowsActiveProtection() {
        composeRule.setContent {
            AuraTheme {
                DefenseScreen(
                    vpnStatus = "Protección DNS desactivada",
                    dnsState = DnsProtectionState(DnsProtectionStatus.OFF),
                    vpnRunning = false,
                    feedUpdatedAt = "No disponible",
                    firewallProfile = DnsFirewallProfile.EQUILIBRADO,
                    blockedDomains = emptyList(),
                    blockedDomainCount = 0,
                    allowlistedDomains = emptyList(),
                    blockedManuallyDomains = emptyList(),
                    blockPulse = 0,
                    onProfileChange = {},
                    onAllowlistAdd = {},
                    onAllowlistRemove = {},
                    onBlocklistAdd = {},
                    onBlocklistRemove = {},
                    onAllowTemporary = {},
                    onClearActivity = {},
                    onVpnToggle = {},
                    onModuleDialog = { _, _ -> },
                    onEmergency = {}
                )
            }
        }

        composeRule.onNodeWithText("PROTECCIÓN DNS DESACTIVADA").assertExists()
        composeRule.onNodeWithText("PROTECCIÓN DNS ACTIVA").assertDoesNotExist()
    }
}
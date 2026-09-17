package com.aura.defense.vpn

import android.content.Context
import android.net.VpnService
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DnsProtectionServiceInstrumentedTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun vpnAuthorizationIsControlledByAndroid() {
        val preparation = VpnService.prepare(context)
        assertTrue(preparation == null || preparation.action != null || preparation.component != null)
    }

    @Test
    fun storedActiveFlagDoesNotBecomeRuntimeState() {
        DnsFirewallStore(context).setServiceActive(true)
        assertTrue(DnsFirewallStore(context).wasServiceActive())
        assertFalse(DnsProtectionStateStore.state.value.status == DnsProtectionStatus.ACTIVE_DNS_ONLY)
        DnsFirewallStore(context).setServiceActive(false)
    }
}
package com.aura.defense.ui

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aura.defense.apps.AppScanResult
import com.aura.defense.guardian.AuraGuardianEngine
import com.aura.defense.ui.components.ModuleDialog
import com.aura.defense.ui.screens.AppsScreen
import com.aura.defense.ui.screens.AurasScreen
import com.aura.defense.ui.screens.DefenseScreen
import com.aura.defense.ui.screens.HomeScreen
import com.aura.defense.vpn.DnsFirewallStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun AuraMainShell(
    boot: AuraBootstrap,
    scanningApps: Boolean,
    appScanResult: AppScanResult?,
    isVpnRunning: Boolean,
    onScan: () -> Unit,
    onVpnToggle: () -> Unit,
    onProfileChange: (com.aura.defense.vpn.DnsFirewallProfile) -> Unit,
    sharedText: String?,
    onSharedTextConsumed: () -> Unit,
    sharedFile: Uri?,
    onSharedFileConsumed: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tabIndex by remember { mutableStateOf(0) }
    var moduleDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    val dialogLambda: (String, String) -> Unit = { title, message -> moduleDialog = title to message }
    val guardianAssessment = remember(boot.posture, boot.threatEngine, appScanResult, boot.historyEntries) {
        AuraGuardianEngine(boot.threatEngine).assess(
            boot.posture,
            appScanResult,
            emptyList(),
            boot.notificationAlerts,
            boot.historyEntries,
            boot.blockedDns
        )
    }
    val labels = listOf("Consola", "Defensa", "Apps", "Auras")
    val icons = listOf(Icons.Default.Home, Icons.Default.Security, Icons.Default.Apps, Icons.Default.Public)

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("AURA DEFENS", color = AuraCyan)
                Text(boot.auraId, modifier = Modifier.padding(start = 10.dp), color = AuraMuted)
            }
        },
        bottomBar = {
            NavigationBar {
                labels.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = tabIndex == index,
                        onClick = { tabIndex = index },
                        icon = { Icon(icons[index], contentDescription = label) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            when (tabIndex) {
                0 -> HomeScreen(
                    result = boot.posture,
                    guardianAssessment = guardianAssessment,
                    historyCount = boot.historyEntries.size,
                    onGuardianAnalysis = {},
                    onStartScan = onScan,
                    onModuleDialog = dialogLambda,
                    onEmergency = {},
                    onToolsHub = {}
                )
                1 -> DefenseScreen(
                    vpnStatus = if (isVpnRunning) "Protegido" else "VPN desactivada",
                    vpnRunning = isVpnRunning,
                    firewallProfile = boot.dnsProfile,
                    blockedDomains = boot.blockedDns,
                    blockedDomainCount = boot.blockedDnsCount,
                    allowlistedDomains = boot.allowlistedDomains,
                    blockedManuallyDomains = boot.blockedManuallyDomains,
                    blockPulse = 0,
                    onProfileChange = onProfileChange,
                    onAllowlistAdd = { domain -> scope.launch(Dispatchers.IO) { DnsFirewallStore(context).addAllowlistedDomain(domain) } },
                    onAllowlistRemove = { domain -> scope.launch(Dispatchers.IO) { DnsFirewallStore(context).removeAllowlistedDomain(domain) } },
                    onBlocklistAdd = { domain -> scope.launch(Dispatchers.IO) { DnsFirewallStore(context).addBlockedDomain(domain) } },
                    onBlocklistRemove = { domain -> scope.launch(Dispatchers.IO) { DnsFirewallStore(context).removeBlockedDomain(domain) } },
                    onVpnToggle = onVpnToggle,
                    onModuleDialog = dialogLambda,
                    onEmergency = {}
                )
                2 -> AppsScreen(
                    scanResult = appScanResult,
                    scanning = scanningApps,
                    onScan = onScan,
                    onViewRisks = {},
                    onExport = {},
                    onModuleDialog = dialogLambda
                )
                else -> AurasScreen(
                    locationActive = false,
                    lanSearching = false,
                    lanPeers = emptyList(),
                    lastLanScan = null,
                    historyEntries = boot.historyEntries,
                    visible = true,
                    onActivateLocation = {},
                    onVisibilityToggle = {},
                    onSearchLan = {},
                    onStopLanSearch = {},
                    onModuleDialog = dialogLambda
                )
            }
        }
    }

    if (sharedText != null) {
        androidx.compose.material3.TextButton(onClick = onSharedTextConsumed) {
            Text("Contenido compartido recibido")
        }
    }
    if (sharedFile != null) {
        androidx.compose.material3.TextButton(onClick = onSharedFileConsumed) {
            Text("Archivo compartido recibido")
        }
    }
    moduleDialog?.let { (title, message) -> ModuleDialog(title, message) { moduleDialog = null } }
}

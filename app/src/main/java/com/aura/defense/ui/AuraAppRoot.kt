package com.aura.defense.ui

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aura.defense.MainActivity
import com.aura.defense.apps.AppScanResult
import com.aura.defense.apps.AppScanner
import com.aura.defense.data.AuraPreferences
import com.aura.defense.data.DeviceTelemetryProvider
import com.aura.defense.history.AuraHistoryStore
import com.aura.defense.notifications.NotificationAlertStore
import com.aura.defense.security.SecurityPostureEngine
import com.aura.defense.threats.ThreatIntelligenceEngine
import com.aura.defense.ui.screens.AuraIntroScreen
import com.aura.defense.vpn.DnsFirewallProfile
import com.aura.defense.vpn.DnsFirewallStore
import com.aura.defense.vpn.DnsProtectionStateStore
import com.aura.defense.vpn.DnsProtectionStatus
import com.aura.defense.vpn.DnsDegradedReason
import com.aura.defense.ui.components.ModuleDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import timber.log.Timber
import com.aura.defense.util.Haptics

data class AuraBootstrap(
    val ready: Boolean = false,
    val termsAccepted: Boolean = false,
    val hasCompletedOnboarding: Boolean = false,
    val auraId: String = "",
    val posture: com.aura.defense.security.PostureResult =
        com.aura.defense.security.PostureResult.pending(),
    val dnsProfile: DnsFirewallProfile = DnsFirewallProfile.EQUILIBRADO,
    val blockedDns: List<com.aura.defense.vpn.DnsBlockedEvent> = emptyList(),
    val blockedDnsCount: Int = 0,
    val allowlistedDomains: List<String> = emptyList(),
    val blockedManuallyDomains: List<String> = emptyList(),
    val notificationAlerts: List<com.aura.defense.notifications.NotificationAlert> = emptyList(),
    val historyEntries: List<com.aura.defense.history.AuraHistoryEntry> = emptyList(),
    val threatEngine: ThreatIntelligenceEngine? = null,
    val threatFeedUpdatedAt: String = "No disponible"
)

@Composable
fun AuraAppRoot(
    sharedText: String?,
    onSharedTextConsumed: () -> Unit,
    sharedFile: Uri?,
    onSharedFileConsumed: () -> Unit,
    onRequestVpn: ((() -> Unit)) -> Unit,
    onStopVpn: () -> Unit
) {
    val context = LocalContext.current
    val safeScope = remember {
        CoroutineScope(
            SupervisorJob() + Dispatchers.Main + CoroutineExceptionHandler { _, exception ->
                Timber.e(exception, "Excepción no manejada en UI")
                com.aura.defense.monitor.AuraProcessLog.log(
                    "⚠ Error interno recuperado: ${exception.message}", "SISTEMA"
                )
            }
        )
    }
    var boot by remember { mutableStateOf(AuraBootstrap()) }
    var scanningApps by remember { mutableStateOf(false) }
    var appScanResult by remember { mutableStateOf<AppScanResult?>(null) }
    var isVpnRunning by remember { mutableStateOf(false) }
    var threatFeedEntries by remember { mutableStateOf(0) }
    val dnsState by DnsProtectionStateStore.state.collectAsState()
    var inBackground by remember { mutableStateOf(false) }
    var sharedAnalysis by remember { mutableStateOf<Pair<String, String>?>(null) }
    val appScanner = remember { AppScanner(context) }
    val prefs = remember { AuraPreferences(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> inBackground = true
                Lifecycle.Event.ON_START -> {
                    inBackground = false
                    isVpnRunning = MainActivity.auraVpnActiveStatic(context)
                    if (!isVpnRunning && dnsState.status in setOf(DnsProtectionStatus.ACTIVE_DNS_ONLY, DnsProtectionStatus.DEGRADED)) {
                        DnsProtectionStateStore.transition(
                            DnsProtectionStatus.ERROR,
                            DnsDegradedReason.VPN_REVOKED,
                            "Android ya no observa el túnel DNS activo"
                        )
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        boot = withContext(Dispatchers.IO) {
            Timber.i("BOOT:4 bootstrap IO iniciado")
            com.aura.defense.vpn.ProfileManager.loadProfile(context)
            val dnsStore = DnsFirewallStore(context)
            val alertStore = NotificationAlertStore(context)
            val historyStore = AuraHistoryStore(context)
            val engine = ThreatIntelligenceEngine(context)
            val telemetry = DeviceTelemetryProvider(context).read()
            val posture = SecurityPostureEngine().evaluate(telemetry)
            val integrityFindings = com.aura.defense.security.DeviceIntegrityChecker(context).check().checks
                .filterNot { it.passed }
                .map { check ->
                    val severity = when {
                        check.name.contains("Root", ignoreCase = true) -> com.aura.defense.security.FindingSeverity.CRITICAL
                        check.name.contains("Debug", ignoreCase = true) -> com.aura.defense.security.FindingSeverity.HIGH
                        check.name.contains("Bloqueo", ignoreCase = true) || check.name.contains("Cifrado", ignoreCase = true) -> com.aura.defense.security.FindingSeverity.HIGH
                        check.name.contains("Google", ignoreCase = true) -> com.aura.defense.security.FindingSeverity.LOW
                        else -> com.aura.defense.security.FindingSeverity.MEDIUM
                    }
                    com.aura.defense.security.SecurityFinding(
                        title = check.name,
                        severity = severity,
                        evidence = check.detail,
                        explanation = "Comprobación de integridad del dispositivo.",
                        recommendedAction = "Revisa esta configuración en los ajustes de Android.",
                        settingsAction = if (check.name.contains("Desarrollador", ignoreCase = true)) com.aura.defense.security.SettingsAction.DESARROLLADOR else com.aura.defense.security.SettingsAction.SEGURIDAD
                    )
                }
            val postureWithIntegrity = SecurityPostureEngine().addFindings(posture, integrityFindings)
            com.aura.defense.scheduler.ensureScheduled(context)
            com.aura.defense.security.IntegrityCheck.report(context)
            Timber.i("BOOT:5/6 bootstrap IO completado")
            com.aura.defense.monitor.AuraProcessLog.log(
                "Bootstrap completado: ${postureWithIntegrity.findings.size} hallazgos, score ${postureWithIntegrity.score}",
                "SISTEMA"
            )
            AuraBootstrap(
                ready = true,
                termsAccepted = prefs.hasAcceptedTerms(),
                hasCompletedOnboarding = prefs.hasCompletedOnboarding(),
                auraId = prefs.getAuraId(),
                posture = postureWithIntegrity,
                dnsProfile = dnsStore.profile(),
                blockedDns = dnsStore.blockedEvents(),
                blockedDnsCount = dnsStore.blockedCount(),
                allowlistedDomains = dnsStore.allowlist(),
                blockedManuallyDomains = dnsStore.blocklist(),
                notificationAlerts = alertStore.getAll(),
                historyEntries = historyStore.getEntries(),
                threatEngine = engine,
                threatFeedUpdatedAt = engine.lastUpdatedAt
            )
        }
    }

    LaunchedEffect(Unit) {
        var checks = 0
        while (true) {
            isVpnRunning = MainActivity.auraVpnActiveStatic(context)
            threatFeedEntries = com.aura.defense.vpn.ThreatFeedManager.size()
            if (boot.ready) {
                val dnsStore = DnsFirewallStore(context)
                boot = boot.copy(
                    blockedDns = dnsStore.blockedEvents(),
                    blockedDnsCount = dnsStore.blockedCount()
                )
            }
            if (++checks % 5 == 0) {
                val threats = com.aura.defense.monitor.SelfDefenseWatcher
                    .selfCheck(context, isVpnRunning)
                if (threats.isNotEmpty()) {
                    com.aura.defense.monitor.SelfDefenseWatcher.notifyThreats(context, threats)
                }
            }
            delay(1500)
        }
    }

    LaunchedEffect(sharedText) {
        val text = sharedText ?: return@LaunchedEffect
        val analysis = withContext(Dispatchers.IO) {
            com.aura.defense.tools.LinkAnalyzer(context).analyze(text)
        }
        sharedAnalysis = "Análisis: ${analysis.risk}" to analysis.reasons.joinToString("\n")
        Timber.i("BOOT:6 contenido compartido analizado")
        onSharedTextConsumed()
    }

    when {
        !boot.ready -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("AURA", color = Color(0xFF4DD8E6), style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(12.dp))
                    CircularProgressIndicator(color = Color(0xFF4DD8E6))
                    Spacer(Modifier.height(12.dp))
                    Text("Preparando tu defensa…", color = Color.Gray)
                }
            }
        }
        !boot.termsAccepted || !boot.hasCompletedOnboarding -> {
            AuraIntroScreen(
                preferences = prefs,
                onFinished = {
                safeScope.launch {
                    boot = withContext(Dispatchers.IO) {
                        boot.copy(
                            termsAccepted = prefs.hasAcceptedTerms(),
                            hasCompletedOnboarding = prefs.hasCompletedOnboarding(),
                            auraId = prefs.getAuraId()
                        )
                    }
                }
            })
        }
        else -> {
            AuraMainShell(
                boot = boot,
                scanningApps = scanningApps,
                appScanResult = appScanResult,
                isVpnRunning = isVpnRunning,
                dnsState = dnsState,
                threatFeedEntries = threatFeedEntries,
                inBackground = inBackground,
                onScan = {
                    if (!scanningApps) safeScope.launch {
                        scanningApps = true
                        try {
                            com.aura.defense.monitor.AuraProcessLog.log(
                                "Iniciando escaneo solicitado por el usuario", "SCAN"
                            )
                            appScanResult = withContext(Dispatchers.Default) { appScanner.scan() }
                            appScanResult?.let { result ->
                                if (result.highRiskApps.isEmpty()) Haptics.confirm(context) else Haptics.alert(context)
                            }
                            appScanResult?.let { result ->
                                withContext(Dispatchers.IO) {
                                    com.aura.defense.history.ScoreHistoryStore(context)
                                        .saveScore(boot.posture.score, boot.posture.status)
                                }
                            }
                        } finally {
                            scanningApps = false
                        }
                    }
                },
                onVpnToggle = {
                    if (dnsState.status in setOf(DnsProtectionStatus.STARTING, DnsProtectionStatus.ACTIVE_DNS_ONLY, DnsProtectionStatus.DEGRADED)) onStopVpn()
                    else onRequestVpn { }
                },
                onProfileChange = { profile ->
                    boot = boot.copy(dnsProfile = profile)
                    safeScope.launch(Dispatchers.IO) { DnsFirewallStore(context).saveProfile(profile) }
                },
                sharedText = sharedText,
                onSharedTextConsumed = onSharedTextConsumed,
                sharedFile = sharedFile,
                onSharedFileConsumed = onSharedFileConsumed
            )
        }
    }

    sharedAnalysis?.let { (title, message) ->
        ModuleDialog(title, message) { sharedAnalysis = null }
    }
}

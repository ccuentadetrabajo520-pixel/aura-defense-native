package com.aura.defense

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aura.defense.data.AuraPreferences
import com.aura.defense.ui.AuraBackground
import com.aura.defense.ui.AuraTheme
import com.aura.defense.ui.components.AuraBottomNav
import com.aura.defense.ui.components.AuraCenterDialog
import com.aura.defense.ui.components.AuraIdDialog
import com.aura.defense.ui.components.AuraTopBar
import com.aura.defense.ui.components.ModuleDialog
import com.aura.defense.ui.screens.AppsScreen
import com.aura.defense.ui.screens.AurasScreen
import com.aura.defense.ui.screens.DefenseScreen
import com.aura.defense.ui.screens.HomeScreen

private data class AuraUiState(
    val auraScore: String = "Analyzing...",
    val vpnStatus: String = "Standby",
    val dnsStatus: String = "Ready",
    val riskCount: Int = 0,
    val selectedTab: Int = 0,
    val auraId: String,
    val visibilityVisible: Boolean = true
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = AuraPreferences(this)
        setContent {
            AuraTheme {
                AuraDefenseApp(preferences = preferences)
            }
        }
    }
}

@Composable
private fun AuraDefenseApp(preferences: AuraPreferences) {
    var state by remember { mutableStateOf(AuraUiState(auraId = preferences.getAuraId())) }
    var showAuraCenter by remember { mutableStateOf(false) }
    var showAuraIdDialog by remember { mutableStateOf(false) }
    var moduleDialog by remember { mutableStateOf<Pair<String, String>?>(null) }

    Scaffold(
        containerColor = AuraBackground,
        topBar = {
            AuraTopBar(
                auraId = state.auraId,
                onAuraIdClick = { showAuraIdDialog = true },
                onSettingsClick = { showAuraCenter = true }
            )
        },
        bottomBar = { AuraBottomNav(state.selectedTab) { state = state.copy(selectedTab = it) } }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp, vertical = 18.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (state.selectedTab) {
                0 -> HomeScreen(
                    auraScore = state.auraScore,
                    vpnStatus = state.vpnStatus,
                    dnsStatus = state.dnsStatus,
                    riskCount = state.riskCount,
                    onStartScan = { state = state.copy(auraScore = "Base ready for real diagnosis") },
                    onModuleDialog = { title, message -> moduleDialog = title to message }
                )
                1 -> AurasScreen { title, message -> moduleDialog = title to message }
                2 -> DefenseScreen { title, message -> moduleDialog = title to message }
                else -> AppsScreen { title, message -> moduleDialog = title to message }
            }
        }
    }

    if (showAuraIdDialog) {
        AuraIdDialog(
            currentId = state.auraId,
            onSave = { value ->
                preferences.saveAuraId(value)
                state = state.copy(auraId = value.trim())
            },
            onDismiss = { showAuraIdDialog = false }
        )
    }
    if (showAuraCenter) {
        AuraCenterDialog(
            auraId = state.auraId,
            visibilityVisible = state.visibilityVisible,
            onEditId = { showAuraIdDialog = true },
            onVisibilityToggle = { state = state.copy(visibilityVisible = !state.visibilityVisible) },
            onItemClick = { item -> moduleDialog = item to "This module will be connected to the real Android API in the next implementation phase." },
            onDismiss = { showAuraCenter = false }
        )
    }
    moduleDialog?.let { (title, message) -> ModuleDialog(title, message) { moduleDialog = null } }
}

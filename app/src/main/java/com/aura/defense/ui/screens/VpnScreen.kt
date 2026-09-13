package com.aura.defense.ui.screens

import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.aura.defense.service.AuraVpnService

@Composable
fun VpnScreen() {
    val context = LocalContext.current
    var isVpnActive by remember { mutableStateOf(AuraVpnService.isRunning) }
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (AuraVpnService.isRunning) {
            isVpnActive = true
        }
    }

    fun startVpn() {
        val preparationIntent = VpnService.prepare(context)
        if (preparationIntent != null) {
            vpnPermissionLauncher.launch(preparationIntent)
            return
        }

        ContextCompat.startForegroundService(context, Intent(context, AuraVpnService::class.java))
        isVpnActive = true
    }

    fun stopVpn() {
        context.startService(
            Intent(context, AuraVpnService::class.java).setAction(AuraVpnService.ACTION_STOP)
        )
        isVpnActive = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "VPN en tiempo real",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = if (isVpnActive) "VPN activa" else "VPN desactivada",
            style = MaterialTheme.typography.bodyLarge,
            color = if (isVpnActive) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            }
        )
        Button(
            onClick = if (isVpnActive) ::stopVpn else ::startVpn,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isVpnActive) "Desactivar VPN" else "Activar VPN")
        }
    }
}

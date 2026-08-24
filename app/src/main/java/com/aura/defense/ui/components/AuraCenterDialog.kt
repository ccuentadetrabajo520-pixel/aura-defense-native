package com.aura.defense.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.defense.ui.AuraCyan
import com.aura.defense.ui.AuraMuted
import com.aura.defense.ui.AuraSurfaceRaised

@Composable
fun AuraCenterDialog(
    auraId: String,
    visibilityVisible: Boolean,
    onEditId: () -> Unit,
    onVisibilityToggle: () -> Unit,
    onItemClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val permissions = listOf("VPN permission", "Location permission", "Camera permission", "Notification permission", "Notification access", "Usage access", "Battery optimization")
    val privacy = listOf("Local-first mode", "Do not upload apps", "Do not upload location", "Do not upload URLs", "Clear local history")
    val tools = listOf("Link Analyzer", "QR Anti-Phishing", "Password Auditor", "Share Scanner", "Notification Guard", "Reports", "Encrypted Vault", "Emergency Mode", "Scheduled Checks", "Quick Settings Tile", "Auras LAN")
    val legal = listOf("Terms and Conditions", "User responsibility", "Android no-root limitations")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aura Center") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                CenterSection("IDENTITY") {
                    CenterRow("Editable Aura ID", auraId, onEditId)
                    CenterRow("Visibility mode", if (visibilityVisible) "Visible" else "Invisible", onVisibilityToggle)
                }
                CenterSection("PERMISSIONS") { permissions.forEach { CenterRow(it, "Not connected", { onItemClick(it) }) } }
                CenterSection("PRIVACY") { privacy.forEach { CenterRow(it, "Local setting", { onItemClick(it) }) } }
                CenterSection("TOOLS") { tools.forEach { CenterRow(it, "Module", { onItemClick(it) }) } }
                CenterSection("LEGAL") { legal.forEach { CenterRow(it, "Information", { onItemClick(it) }) } }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun CenterSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, color = AuraCyan, fontSize = 11.sp)
        content()
    }
}

@Composable
private fun CenterRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = AuraMuted, fontSize = 11.sp)
    }
}

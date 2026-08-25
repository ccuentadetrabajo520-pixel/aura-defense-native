package com.aura.defense.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun ReportDialog(onExport: (Boolean) -> Unit, onDismiss: () -> Unit) {
    AuraHudDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reportes locales") },
        text = { Text("Comparte un informe con la telemetría y los resultados disponibles. La contraseña nunca se incluye.") },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                TextButton(onClick = { onExport(false) }) { Text("Compartir informe TXT") }
                TextButton(onClick = { onExport(true) }) { Text("Compartir informe JSON") }
                TextButton(onClick = onDismiss) { Text("Cerrar") }
            }
        }
    )
}

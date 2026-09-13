package com.aura.defense.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.aura.defense.ui.components.AuraButton
import com.aura.defense.utils.ReportGenerator

@Composable
fun ReportsScreen() {
    val context = LocalContext.current
    val reportGenerator = remember { ReportGenerator(context) }
    var report by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Reportes de seguridad",
            style = MaterialTheme.typography.headlineMedium
        )

        if (report.isEmpty()) {
            AuraButton(
                text = "Generar reporte",
                onClick = { report = reportGenerator.generateSecurityReport() }
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .padding(16.dp)
                ) {
                    item {
                        Text(
                            text = report,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AuraButton(
                    text = "Copiar",
                    onClick = { copyReport(context, report) },
                    modifier = Modifier.weight(1f)
                )
                AuraButton(
                    text = "Regenerar",
                    onClick = { report = reportGenerator.generateSecurityReport() },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun copyReport(context: Context, report: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Aura Report", report))
    Toast.makeText(context, "Reporte copiado", Toast.LENGTH_SHORT).show()
}

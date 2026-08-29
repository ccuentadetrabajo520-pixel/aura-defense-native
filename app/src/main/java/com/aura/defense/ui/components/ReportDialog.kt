package com.aura.defense.ui.components

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aura.defense.apps.AppScanResult
import com.aura.defense.guardian.AuraGuardianAssessment
import com.aura.defense.reports.AuraReportBuilder
import com.aura.defense.security.PostureResult
import com.aura.defense.tools.LinkAnalysis
import java.io.File

@Composable
fun ReportDialog(
    auraId: String = "",
    postureResult: PostureResult = PostureResult.pending(),
    appScanResult: AppScanResult? = null,
    linkHistory: List<LinkAnalysis> = emptyList(),
    guardianAssessment: AuraGuardianAssessment? = null,
    onExport: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AuraHudDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reportes locales") },
        text = { Text("Comparte un informe con la telemetría y los resultados disponibles. La contraseña nunca se incluye.") },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                TextButton(onClick = { onExport(false) }) { Text("Compartir informe TXT") }
                TextButton(onClick = { onExport(true) }) { Text("Compartir informe JSON") }
                TextButton(onClick = {
                    val htmlReport = AuraReportBuilder().html(
                        auraId = auraId,
                        posture = postureResult,
                        apps = appScanResult,
                        links = linkHistory,
                        password = null,
                        notifications = emptyList(),
                        indicators = emptyList(),
                        guardian = guardianAssessment,
                        file = null,
                        vaultAvailable = false,
                        lanPeers = emptyList(),
                        lastLanScan = null,
                        history = emptyList(),
                        baselineTimestamp = "No disponible",
                        threatSnapshot = null
                    )
                    val htmlFile = File(context.cacheDir, "aura_report_${System.currentTimeMillis()}.html")
                    htmlFile.writeText(htmlReport)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/html"
                        putExtra(Intent.EXTRA_SUBJECT, "Informe Aura HTML")
                        putExtra(Intent.EXTRA_TEXT, htmlReport)
                        putExtra(Intent.EXTRA_STREAM, android.net.Uri.fromFile(htmlFile))
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Compartir informe HTML"))
                }) { Text("Compartir informe HTML") }
                TextButton(onClick = onDismiss) { Text("Cerrar") }
            }
        }
    )
}

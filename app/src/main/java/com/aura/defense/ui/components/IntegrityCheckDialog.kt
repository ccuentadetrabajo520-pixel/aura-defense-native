package com.aura.defense.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.defense.security.IntegrityResult
import com.aura.defense.ui.AuraCyan
import com.aura.defense.ui.AuraGreen
import com.aura.defense.ui.AuraMuted
import com.aura.defense.ui.AuraRed
import com.aura.defense.ui.AuraSpacing

@Composable
fun IntegrityCheckDialog(result: IntegrityResult, onDismiss: () -> Unit) {
        AuraHudDialog(
                    onDismissRequest = onDismiss,
                            title = { Text("Integridad del Dispositivo") },
                                    text = {
                                                    Column(
                                                                        modifier = Modifier.verticalScroll(rememberScrollState()),
                                                                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                                        if (result.overallPassed) {
                                                                                                Text("Todos los controles pasaron", color = AuraGreen, fontSize = 14.sp)
                                                                        } else {
                                                                                                Text("${result.passedCount} de ${result.totalChecks} controles pasaron", color = AuraRed, fontSize = 14.sp)
                                                                        }
                                                                                        Text("Evaluado: ${result.timestamp}", color = AuraMuted, fontSize = 11.sp)
                                                                                                        Column(verticalArrangement = Arrangement.spacedBy(AuraSpacing.sm)) {
                                                                                                                                result.checks.forEach { check ->
                                                                                                                                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                                                                                                                                                        Text(
                                                                                                                                                                                                                            if (check.passed) "PASS" else "FAIL",
                                                                                                                                                                                                                                                            color = if (check.passed) AuraGreen else AuraRed,
                                                                                                                                                                                                                                                                                            fontSize = 10.sp,
                                                                                                                                                                                                                                                                                                                            modifier = Modifier.width(40.dp)
                                                                                                                                                                                        )
                                                                                                                                                                                                                    Column(modifier = Modifier.weight(1f)) {
                                                                                                                                                                                                                                                        Text(check.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                                                                                                                                                                                                                                                                                        Text(check.detail, color = AuraMuted, fontSize = 10.sp)
                                                                                                                                                                                                                    }
                                                                                                                                                        }
                                                                                                                                }
                                                                                                        }
                                                    }
                                    },
                                            confirmButton = {
                                                            TextButton(onClick = onDismiss) { Text("Cerrar") }
                                            }
        )
}

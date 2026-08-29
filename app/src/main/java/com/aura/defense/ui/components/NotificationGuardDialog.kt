package com.aura.defense.ui.components

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aura.defense.notifications.AuraNotificationListenerService
import com.aura.defense.notifications.NotificationAlert
import com.aura.defense.notifications.NotificationAlertStore
import com.aura.defense.tools.LinkRisk
import com.aura.defense.ui.AuraAmber
import com.aura.defense.ui.AuraGreen
import com.aura.defense.ui.AuraMuted
import com.aura.defense.ui.AuraRed

@Composable
fun NotificationGuardDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val store = remember { NotificationAlertStore(context) }
    var enabled by remember { mutableStateOf(isNotificationAccessEnabled(context)) }
    var alerts by remember { mutableStateOf(store.getAll().asReversed()) }

    fun refresh() {
        enabled = isNotificationAccessEnabled(context)
        alerts = store.getAll().asReversed()
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresh() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AuraHudDialog(
        onDismissRequest = onDismiss,
        title = { Text("Protección de notificaciones") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Aura necesita acceso a notificaciones para detectar enlaces peligrosos y phishing en tiempo real. Actívalo en: Ajustes → Apps especiales → Acceso a notificaciones → Aura Defense", color = AuraMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                Text(if (enabled) "Activo" else "Acceso requerido", color = if (enabled) AuraGreen else AuraAmber, fontSize = 17.sp)
                Text("Aura analiza únicamente el texto visible de las notificaciones cuando concedes acceso. El análisis se realiza localmente y no se sube contenido.", color = AuraMuted, fontSize = 12.sp)
                if (!enabled) Text("Desactivado", color = AuraMuted, fontSize = 12.sp)
                if (alerts.isNotEmpty()) {
                    Text("Notificaciones analizadas", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                    alerts.forEach { NotificationAlertRow(it) }
                }
            }
        },
        confirmButton = {
            if (!enabled) Button(onClick = {
                runCatching { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
            }) { Text("Activar acceso") } else TextButton(onClick = { refresh() }) { Text("Actualizar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

@Composable
private fun NotificationAlertRow(alert: NotificationAlert) {
    val color = when (alert.analysis.risk) {
        LinkRisk.SEGURO -> AuraGreen
        LinkRisk.SOSPECHOSO -> AuraAmber
        LinkRisk.PELIGROSO -> AuraRed
    }
    Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text("Enlace detectado · Resultado: ${alert.analysis.risk.toSpanish()}", color = color, fontSize = 14.sp)
        Text(alert.url, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, maxLines = 2)
        Text("Razones: ${alert.analysis.reasons.joinToString(", ")}", color = AuraMuted, fontSize = 11.sp)
        Text("No abrir automáticamente", color = AuraMuted, fontSize = 11.sp)
    }
}

fun isNotificationAccessEnabled(context: Context): Boolean = runCatching {
    val enabledListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners").orEmpty()
    enabledListeners.split(':').mapNotNull { value -> runCatching { ComponentName.unflattenFromString(value) }.getOrNull() }
        .any { it.packageName == context.packageName && it.className == AuraNotificationListenerService::class.java.name }
}.getOrDefault(false)

private fun LinkRisk.toSpanish() = when (this) {
    LinkRisk.SEGURO -> "Seguro"
    LinkRisk.SOSPECHOSO -> "Sospechoso"
    LinkRisk.PELIGROSO -> "Peligroso"
}
package com.aura.defense.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import com.aura.defense.ui.AuraCyan
import com.aura.defense.ui.AuraCyanGlow
import com.aura.defense.ui.AuraSpacing
import com.aura.defense.ui.AuraSurface
import com.aura.defense.ui.AuraSurfaceRaised

private val AuraHudText = Color(0xFFEAF7F5)
private val AuraHudSecondaryText = Color(0xFFB7CCCA)

@Composable
fun AuraHudDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f).widthIn(max = 520.dp),
            color = AuraSurfaceRaised.copy(alpha = 0.98f),
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, AuraCyan.copy(alpha = 0.25f))
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(AuraCyan.copy(alpha = 0.5f)))
            Column(
                modifier = Modifier.padding(horizontal = AuraSpacing.xl, vertical = AuraSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = AuraSurfaceRaised.copy(alpha = 0.55f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                        CompositionLocalProvider(
                            LocalContentColor provides AuraCyan,
                            LocalTextStyle provides MaterialTheme.typography.headlineMedium
                        ) {
                            title()
                        }
                    }
                }
                CompositionLocalProvider(
                    LocalContentColor provides AuraHudText,
                    LocalTextStyle provides MaterialTheme.typography.bodyMedium
                ) {
                    text()
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides AuraHudSecondaryText,
                        LocalTextStyle provides MaterialTheme.typography.labelLarge
                    ) {
                        dismissButton?.invoke()
                    }
                    CompositionLocalProvider(
                        LocalContentColor provides AuraCyan,
                        LocalTextStyle provides MaterialTheme.typography.labelLarge
                    ) {
                        confirmButton()
                    }
                }
            }
        }
    }
}

@Composable
fun AuraHudSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = AuraCyan, style = MaterialTheme.typography.labelSmall)
        content()
    }
}

@Composable
fun AuraStatusChip(text: String, color: Color = AuraCyan) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(text, color = color, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun AuraHudActionButton(label: String, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick) { Text(label) }
}

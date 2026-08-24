package com.aura.defense

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import java.util.UUID

private const val PREFERENCES_NAME = "aura_defense_preferences"
private const val AURA_ID_KEY = "aura_id"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val savedAuraId = preferences.getString(AURA_ID_KEY, null)
            ?: createAuraId().also { preferences.edit().putString(AURA_ID_KEY, it).apply() }

        setContent {
            AuraDefenseTheme {
                AuraDefenseScreen(
                    initialAuraId = savedAuraId,
                    onAuraIdChanged = { auraId ->
                        preferences.edit().putString(AURA_ID_KEY, auraId).apply()
                    }
                )
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun AuraDefenseTheme(content: @androidx.compose.runtime.Composable () -> Unit) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.darkColorScheme(
            primary = Color(0xFF62E6D5),
            onPrimary = Color(0xFF00201D),
            background = Color(0xFF081317),
            surface = Color(0xFF10242A),
            onBackground = Color(0xFFE5F4F1),
            onSurface = Color(0xFFE5F4F1)
        ),
        content = content
    )
}

@androidx.compose.runtime.Composable
private fun AuraDefenseScreen(
    initialAuraId: String,
    onAuraIdChanged: (String) -> Unit
) {
    var score by remember { mutableStateOf("Analyzing...") }
    var showAuraCenter by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF081317))
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "AURA / DEFENSE",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Aura Defense",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Private and realistic mobile defense",
                    color = Color(0xFFA9C2C0),
                    fontSize = 17.sp
                )

                Spacer(modifier = Modifier.height(18.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("AURA SCORE", color = Color(0xFF8EA9A7), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(score, color = MaterialTheme.colorScheme.onSurface, fontSize = 22.sp)
                    }
                }

                Button(
                    onClick = { score = "Base ready for real diagnosis" },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Start diagnosis", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { showAuraCenter = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Aura Center")
                }
            }

            Text(
                text = "Aura does not use root, does not simulate threats, and only uses Android-permitted features.",
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                color = Color(0xFF78918F),
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
    }

    if (showAuraCenter) {
        AuraCenterDialog(
            initialAuraId = initialAuraId,
            onAuraIdChanged = onAuraIdChanged,
            onDismiss = { showAuraCenter = false }
        )
    }
}

@androidx.compose.runtime.Composable
private fun AuraCenterDialog(
    initialAuraId: String,
    onAuraIdChanged: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var auraId by remember { mutableStateOf(initialAuraId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aura Center") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = auraId,
                    onValueChange = {
                        auraId = it
                        onAuraIdChanged(it)
                    },
                    label = { Text("Aura ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Divider()
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Terms", color = MaterialTheme.colorScheme.onSurface)
                    Text("Privacy", color = MaterialTheme.colorScheme.onSurface)
                    Text("Version 0.1.0", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

private fun createAuraId(): String =
    "AURA-${UUID.randomUUID().toString().replace("-", "").take(6).uppercase(Locale.US)}"

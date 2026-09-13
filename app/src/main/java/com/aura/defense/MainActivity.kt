package com.aura.defense

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import timber.log.Timber

class MainActivity : ComponentActivity() {
    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startAuraVpn(onFailure = { vpnPermissionDeniedCallback?.invoke() })
        } else {
            vpnPermissionDeniedCallback?.invoke()
        }
    }

    private var vpnPermissionDeniedCallback: (() -> Unit)? = null
    private var sharedText by mutableStateOf<String?>(null)
    private var sharedFile by mutableStateOf<Uri?>(null)
    private lateinit var diagTree: com.aura.defense.util.DiagnosticLogTree

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        diagTree = com.aura.defense.util.DiagnosticLogTree(filesDir)
        sharedDiagTree = diagTree
        Timber.plant(diagTree)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                Timber.e(throwable, "CRASH en ${thread.name}")
                diagTree.flushNow()
            }
            previous?.uncaughtException(thread, throwable)
        }
        Timber.i("BOOT:1 onCreate iniciado")
        sharedText = extractSharedText(intent)
        sharedFile = extractSharedFile(intent)
        Timber.i("BOOT:2 antes de setContent")
        setContent {
            com.aura.defense.ui.AuraTheme {
                com.aura.defense.ui.AuraAppRoot(
                    sharedText = sharedText,
                    onSharedTextConsumed = { sharedText = null },
                    sharedFile = sharedFile,
                    onSharedFileConsumed = { sharedFile = null },
                    onRequestVpn = { onDenied -> requestAuraVpn(onDenied) },
                    onStopVpn = { stopAuraVpn() }
                )
            }
        }
        Timber.i("BOOT:3 setContent programado")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        sharedText = extractSharedText(intent)
        sharedFile = extractSharedFile(intent)
    }

    fun requestAuraVpn(onDenied: () -> Unit) {
        vpnPermissionDeniedCallback = onDenied
        runCatching { VpnService.prepare(this) }
            .onSuccess { preparationIntent ->
                if (preparationIntent == null) startAuraVpn(onFailure = onDenied)
                else vpnPermissionLauncher.launch(preparationIntent)
            }
            .onFailure { onDenied() }
    }

    private fun startAuraVpn(onFailure: () -> Unit) {
        runCatching {
            ContextCompat.startForegroundService(
                this,
                Intent(this, com.aura.defense.vpn.AuraVpnService::class.java)
            )
        }.onFailure { onFailure() }
    }

    fun stopAuraVpn() {
        startService(
            Intent(this, com.aura.defense.vpn.AuraVpnService::class.java)
                .setAction(com.aura.defense.vpn.AuraVpnService.ACTION_STOP)
        )
    }

    companion object {
        lateinit var sharedDiagTree: com.aura.defense.util.DiagnosticLogTree

        fun auraVpnActiveStatic(context: Context): Boolean = runCatching {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as? ConnectivityManager
            connectivityManager?.allNetworks?.any { network ->
                connectivityManager.getNetworkCapabilities(network)
                    ?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
            } ?: false
        }.getOrDefault(false)
    }

    private fun extractSharedText(intent: Intent?): String? = runCatching {
        if (intent?.action != Intent.ACTION_SEND || intent.type != "text/plain") return null
        intent.getStringExtra(Intent.EXTRA_TEXT)?.takeIf { it.isNotBlank() }
    }.getOrNull()

    private fun extractSharedFile(intent: Intent?): Uri? = runCatching {
        when (intent?.action) {
            Intent.ACTION_SEND -> @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            Intent.ACTION_VIEW -> intent.data
            else -> null
        }
    }.getOrNull()
}

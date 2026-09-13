package com.aura.defense.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Android no permite iniciar una VPN sin interaccion del usuario tras reiniciar.
            VpnDebugger.log("Dispositivo reiniciado - reactiva el tunel desde AURA")
        }
    }
}

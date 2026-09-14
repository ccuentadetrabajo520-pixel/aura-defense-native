package com.aura.defense.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SelfDestructWatcher : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart
            ?: intent.getStringArrayExtra(Intent.EXTRA_REPLACING)?.firstOrNull()
            ?: return
        if (packageName == context.packageName) {
            AuraProcessLog.log(
                "⚠ AURA ESTÁ SIENDO DESINSTALADA. Si no fuiste tú, alguien está quitando tu defensa.",
                "SISTEMA"
            )
        }
    }
}

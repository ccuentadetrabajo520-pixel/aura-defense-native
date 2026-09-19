package com.aura.defense

import android.app.Application
import android.content.Context
import com.aura.defense.threats.ThreatIntelligenceRepository

class AuraApplication : Application() {
    val threatIntelligenceRepository: ThreatIntelligenceRepository by lazy {
        ThreatIntelligenceRepository(this)
    }
}

object ThreatIntelligenceRepositoryProvider {
    fun get(context: Context): ThreatIntelligenceRepository =
        (context.applicationContext as AuraApplication).threatIntelligenceRepository
}
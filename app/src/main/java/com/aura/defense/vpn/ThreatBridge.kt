package com.aura.defense.vpn

import com.aura.defense.threats.ThreatIntelligenceEngine

object ThreatBridge {

     data class EnrichedResult(
                 val blocked: Boolean,
                         val source: String?,
                                 val category: String?,
                                         val severity: String?
     )

         fun enrichDomainCheck(
                     domain: String,
                             threatEngine: ThreatIntelligenceEngine?
         ): EnrichedResult {
                     if (ThreatFeedManager.isBlocked(domain)) {
                                     return EnrichedResult(
                                                         blocked = true,
                                                                         source = "FEED",
                                                                                         category = null,
                                                                                                         severity = "HIGH"
                                     )
                     }

                             if (threatEngine != null) {
                                             val matches = threatEngine.findMatches(domain)
                                                         if (matches.isNotEmpty()) {
                                                                                             val best = matches.first()
                                                                                                             return EnrichedResult(
                                                                                                                                             blocked = true,
                                                                                                                                                                 source = "LOCAL_INTELLIGENCE",
                                                                                                                                                                                     category = best.category.name,
                                                                                                                                                                                                         severity = best.severity.name
                                                                                                             )
                                                         }
                             }

                                     return EnrichedResult(
                                                     blocked = false,
                                                                 source = null,
                                                                             category = null,
                                                                                         severity = null
                                     )
         }
}

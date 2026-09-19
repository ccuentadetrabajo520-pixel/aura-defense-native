package com.aura.defense.vpn

enum class DnsDecision {
    ALLOW,
    BLOCK,
    UNKNOWN,
    ERROR
}

data class DnsRule(
    val domain: String,
    val category: String,
    val source: String,
    val feedVersion: String,
    val severity: String = "UNKNOWN",
    val validUntil: Long? = null
)

data class DnsDecisionResult(
    val decision: DnsDecision,
    val domain: String,
    val reason: String,
    val category: String? = null,
    val source: String? = null,
    val feedVersion: String? = null,
    val severity: String? = null
)

data class DnsTemporaryException(
    val domain: String,
    val expiresAt: Long,
    val explanation: String
)

class DnsDecisionEngine(
    private val profile: DnsFirewallProfile,
    private val allowlist: Set<String>,
    private val blocklist: Set<String>,
    private val rules: List<DnsRule>,
    private val temporaryExceptions: List<DnsTemporaryException> = emptyList(),
    private val now: () -> Long = { System.currentTimeMillis() },
    private val dynamicRuleLookup: ((String) -> DnsRule?)? = null,
    private val rulesSource: (() -> List<DnsRule>)? = null
) {
    fun decide(rawDomain: String): DnsDecisionResult {
        val domain = normalize(rawDomain)
            ?: return DnsDecisionResult(DnsDecision.ERROR, rawDomain.take(253), "invalid_domain")
        val currentRules = rulesSource?.invoke() ?: rules
        if (currentRules.any { normalize(it.domain) == null }) {
            return DnsDecisionResult(DnsDecision.ERROR, domain, "invalid_rule")
        }
        val activeExceptions = temporaryExceptions.filter { it.expiresAt > now() }
        if (matches(activeExceptions.map { it.domain }, domain)) {
            val exception = activeExceptions.first { matches(setOf(it.domain), domain) }
            return DnsDecisionResult(DnsDecision.ALLOW, domain, "temporary_exception:${exception.explanation}")
        }
        if (matches(allowlist, domain)) return DnsDecisionResult(DnsDecision.ALLOW, domain, "allowlist")
        if (matches(blocklist, domain)) return DnsDecisionResult(DnsDecision.BLOCK, domain, "manual_rule", source = "LOCAL")

        val rule = currentRules.firstOrNull { matches(setOf(it.domain), domain) }
            ?: dynamicRuleLookup?.invoke(domain)
            ?: return DnsDecisionResult(DnsDecision.UNKNOWN, domain, "no_matching_rule")
        if (rule.validUntil != null && rule.validUntil <= now()) {
            return DnsDecisionResult(DnsDecision.ERROR, domain, "expired_feed", rule.category, rule.source, rule.feedVersion, rule.severity)
        }
        if (rule.category !in profile.categories) {
            return DnsDecisionResult(DnsDecision.ALLOW, domain, "category_not_in_profile", rule.category, rule.source, rule.feedVersion, rule.severity)
        }
        return DnsDecisionResult(DnsDecision.BLOCK, domain, "threat_rule", rule.category, rule.source, rule.feedVersion, rule.severity)
    }

    private fun matches(candidates: Collection<String>, domain: String): Boolean = candidates.any { candidate ->
        normalize(candidate)?.let { domain == it || domain.endsWith(".$it") } == true
    }

    companion object {
        fun normalize(value: String): String? = value.trim().lowercase()
            .trimEnd('.')
            .takeIf { it.isNotBlank() && it.length <= 253 && it.all { char -> char.isLetterOrDigit() || char == '.' || char == '-' } }
    }
}

class VerifiedDnsRulesSource(private val repository: com.aura.defense.threats.ThreatIntelligenceRepository) {
    fun currentRules(): List<DnsRule> {
        val feed = repository.activeFeed() ?: repository.lastValidFeed()
            ?: return emptyList()
        return feed.indicators.map { indicator ->
            DnsRule(
                domain = indicator.indicator,
                category = indicator.category.name,
                source = indicator.source,
                feedVersion = feed.version,
                severity = indicator.severity.name,
                validUntil = feed.expiresAt
            )
        }
    }
}
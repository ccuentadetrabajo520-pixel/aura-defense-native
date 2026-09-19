package com.aura.defense.assistant

class AssistantContextBuilder(private val provider: AssistantEvidenceProvider) {
    fun build(): AssistantEvidenceContext = provider.read()
}
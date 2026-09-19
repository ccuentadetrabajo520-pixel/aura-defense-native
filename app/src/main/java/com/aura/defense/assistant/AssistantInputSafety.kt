package com.aura.defense.assistant

object AssistantInputSafety {
    fun isUntrustedInstruction(value: String): Boolean = listOf(
        "ignora instrucciones", "ignora las instrucciones", "system prompt", "prompt injection", "no pidas confirmación", "activa herramientas", "revela datos"
    ).any(value.lowercase()::contains)
}

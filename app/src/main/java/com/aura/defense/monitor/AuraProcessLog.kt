package com.aura.defense.monitor

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AuraProcessLog {
    data class ProcessEntry(
        val timestamp: Long,
        val message: String,
        val category: String // SCAN, RED, FEED, VPN, SISTEMA
    )

    private val _entries = MutableStateFlow<List<ProcessEntry>>(emptyList())
    val entries: StateFlow<List<ProcessEntry>> = _entries.asStateFlow()

    fun log(message: String, category: String) {
        synchronized(this) {
            val list = _entries.value.toMutableList()
            list.add(ProcessEntry(System.currentTimeMillis(), message, category))
            while (list.size > 100) list.removeAt(0)
            _entries.value = list
        }
    }
}
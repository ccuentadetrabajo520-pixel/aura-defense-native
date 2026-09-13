package com.aura.defense.util

import android.util.Log
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DiagnosticLogTree(private val logDir: File) : Timber.Tree() {
    private val buffer = StringBuilder()
    private val dateFormat = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)
    private var lastFlush = 0L

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val line = buildString {
            append(dateFormat.format(Date())).append(" [")
            append(priorityToLabel(priority)).append("] ")
            append(Thread.currentThread().name).append(" ")
            append(tag ?: "AURA").append(": ").append(message)
            t?.let {
                append(" || EXC: ").append(it.javaClass.simpleName).append(": ").append(it.message)
            }
            append("\n")
        }
        synchronized(buffer) {
            buffer.append(line)
            val now = System.currentTimeMillis()
            if (now - lastFlush > 1500 || buffer.length > 2048) flush(now)
        }
    }

    private fun flush(now: Long) {
        if (buffer.isEmpty()) {
            lastFlush = now
            return
        }
        val content = buffer.toString()
        buffer.setLength(0)
        lastFlush = now
        try {
            val file = File(logDir, "aura_diagnostico.log")
            file.appendText(content)
            if (file.length() > 1_000_000) {
                File(logDir, "aura_diagnostico_old.log").delete()
                file.renameTo(File(logDir, "aura_diagnostico_old.log"))
            }
        } catch (_: Exception) {
        }
    }

    fun flushNow() {
        synchronized(buffer) { flush(System.currentTimeMillis()) }
    }

    private fun priorityToLabel(priority: Int) = when (priority) {
        Log.ERROR -> "ERROR"
        Log.WARN -> "WARN"
        Log.INFO -> "INFO"
        Log.DEBUG -> "DEBUG"
        else -> "VERBOSE"
    }
}

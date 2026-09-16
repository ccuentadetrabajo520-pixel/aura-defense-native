package com.aura.defense.ai

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class AuraVoice(context: Context) {
    private var tts: TextToSpeech? = null
    @Volatile private var ready = false
    @Volatile var speaking: Boolean = false
        private set
    var onSpeakingChanged: ((Boolean) -> Unit)? = null

    init {
        tts = TextToSpeech(context) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                runCatching { tts?.language = Locale("es", "VE") }
                tts?.setSpeechRate(1.0f)
            }
        }
    }

    fun speak(text: String) {
        if (!ready) return
        val clean = text.take(280)
        speaking = true
        onSpeakingChanged?.invoke(true)
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) {
                speaking = false
                onSpeakingChanged?.invoke(false)
            }
            override fun onError(utteranceId: String?) {
                speaking = false
                onSpeakingChanged?.invoke(false)
            }
        })
        tts?.speak(clean, TextToSpeech.QUEUE_ADD, null, "aura_${System.currentTimeMillis()}")
    }

    fun stop() {
        tts?.stop()
        speaking = false
        onSpeakingChanged?.invoke(false)
    }

    fun destroy() {
        tts?.shutdown()
        tts = null
        ready = false
    }
}

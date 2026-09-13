package com.aura.defense.ai.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.Normalizer
import java.util.Locale

class VoiceCommandEngine(private val context: Context) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    private val _commandResult = MutableStateFlow<String?>(null)
    val commandResult: StateFlow<String?> = _commandResult

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        initializeSpeechRecognizer()
    }

    private fun initializeSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isProcessing.value = true
                    _error.value = null
                }

                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() {
                    isListening = false
                }

                override fun onError(error: Int) {
                    isListening = false
                    _isProcessing.value = false
                    _error.value = errorMessage(error)
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    _isProcessing.value = false
                    val spokenText = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                    if (spokenText.isNullOrBlank()) {
                        _error.value = "No se entendió ningún comando. Inténtalo de nuevo."
                    } else {
                        _commandResult.value = matchVoiceCommand(spokenText)
                        _error.value = null
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
        }
    }

    private fun matchVoiceCommand(spokenText: String): String {
        val normalized = normalize(spokenText)
        return when {
            normalized.contains("escane") || normalized.contains("analiza") -> "scan"
            normalized.contains("vpn") && (normalized.contains("activa") || normalized.contains("enciende")) -> "vpn_on"
            normalized.contains("vpn") && (normalized.contains("desactiva") || normalized.contains("apaga")) -> "vpn_off"
            normalized.contains("vpn") -> "vpn_status"
            normalized.contains("reporte") || normalized.contains("informe") -> "reports"
            normalized.contains("amenaza") || normalized.contains("peligro") -> "threats"
            normalized.contains("seguro") || normalized.contains("protegido") -> "security_status"
            normalized.contains("proteccion") || normalized.contains("defensa") -> "protection_status"
            normalized.contains("silencio") || normalized.contains("silenciar") -> "silent_mode"
            else -> "unknown"
        }
    }

    private fun normalize(value: String): String = Normalizer
        .normalize(value.lowercase(Locale.getDefault()).trim(), Normalizer.Form.NFD)
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")

    private fun errorMessage(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "No se pudo acceder al micrófono."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Falta permiso para usar el micrófono."
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "El reconocimiento de voz no está disponible ahora."
        SpeechRecognizer.ERROR_NO_MATCH -> "No reconocí un comando válido."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "El reconocimiento de voz está ocupado."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No detecté voz. Inténtalo de nuevo."
        else -> "No se pudo procesar el comando de voz."
    }

    fun startListening() {
        if (isListening || speechRecognizer == null) {
            if (speechRecognizer == null) _error.value = "El reconocimiento de voz no está disponible en este dispositivo."
            return
        }
        _commandResult.value = null
        _error.value = null
        _isProcessing.value = true
        isListening = true
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Di un comando de seguridad")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        runCatching { speechRecognizer?.startListening(intent) }
            .onFailure {
                isListening = false
                _isProcessing.value = false
                _error.value = "No se pudo iniciar el reconocimiento de voz."
            }
    }

    fun stopListening() {
        if (isListening) speechRecognizer?.stopListening()
        isListening = false
        _isProcessing.value = false
    }

    fun clearResult() {
        _commandResult.value = null
        _error.value = null
    }

    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        isListening = false
        _isProcessing.value = false
    }
}
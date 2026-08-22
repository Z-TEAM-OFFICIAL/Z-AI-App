package com.example.data.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

class ZegaWakeWordDetector(
    private val context: Context,
    private val onWakeWordDetected: () -> Unit,
    private val onDetectorStateChanged: (Boolean) -> Unit
) {
    private val acousticEngine = AcousticWakeWordEngine(context) {
        onWakeWordTriggeredInternal()
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isEnabled = false

    val currentConfidence: StateFlow<Float> = acousticEngine.currentConfidence
    val audioLevelRms: StateFlow<Float> = acousticEngine.audioLevelRms
    val selectedEngine: StateFlow<WakeWordEngineType> = acousticEngine.selectedEngine
    val sensitivity: StateFlow<WakeWordSensitivity> = acousticEngine.sensitivity

    init {
        // Register listener with the background service
        WakeWordDetectionService.listener = object : WakeWordDetectionService.Companion.WakeWordServiceListener {
            override fun onWakeWordTriggered() {
                onWakeWordTriggeredInternal()
            }
        }
    }

    fun setEngineType(type: WakeWordEngineType) {
        acousticEngine.setEngineType(type)
    }

    fun setSensitivity(sensitivity: WakeWordSensitivity) {
        acousticEngine.setSensitivity(sensitivity)
    }

    private fun onWakeWordTriggeredInternal() {
        Log.d("ZegaWakeWord", "Wake word trigger invoked!")
        stopListeningForActiveSession()
        onWakeWordDetected()
    }

    fun startListening() {
        isEnabled = true
        onDetectorStateChanged(true)

        // Start lightweight on-device acoustic model
        acousticEngine.start()

        // Also start fallback speech cycle if selected
        if (acousticEngine.selectedEngine.value == WakeWordEngineType.SYSTEM_HYBRID) {
            startSpeechRecognizerCycle()
        }

        // Start background service for persistent listening
        try {
            WakeWordDetectionService.startService(context)
        } catch (e: Exception) {
            Log.e("ZegaWakeWord", "Could not start background service: ${e.message}")
        }
    }

    private fun startSpeechRecognizerCycle() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) { isListening = true }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() { isListening = false }

                    override fun onError(error: Int) {
                        isListening = false
                        if (isEnabled && error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                if (isEnabled && acousticEngine.selectedEngine.value == WakeWordEngineType.SYSTEM_HYBRID) {
                                    startSpeechRecognizerCycle()
                                }
                            }, 1000)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        isListening = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val speech = matches[0].lowercase(Locale.getDefault())
                            if (speech.contains("zega") || speech.contains("sega") || speech.contains("vega") ||
                                speech.contains("z-ai") || speech.contains("z ai") || speech.contains("zi") || speech.contains("z-i")) {
                                onWakeWordTriggeredInternal()
                            } else if (isEnabled) {
                                startSpeechRecognizerCycle()
                            }
                        } else if (isEnabled) {
                            startSpeechRecognizerCycle()
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val speech = matches[0].lowercase(Locale.getDefault())
                            if (speech.contains("hey zega") || speech.contains("hey sega") || speech.contains("hey vega") || speech.contains("wake zega") ||
                                speech.contains("hey z-ai") || speech.contains("hey z ai") || speech.contains("hey zi") || speech.contains("hey z-i")) {
                                onWakeWordTriggeredInternal()
                            }
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("ZegaWakeWord", "Error starting speech recognizer: ${e.message}")
        }
    }

    fun stopListeningForActiveSession() {
        acousticEngine.stop()
        isListening = false
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e("ZegaWakeWord", "Error stopping for active session: ${e.message}")
        }
        speechRecognizer = null
    }

    fun stopListening() {
        isEnabled = false
        isListening = false
        onDetectorStateChanged(false)
        acousticEngine.stop()
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e("ZegaWakeWord", "Error stopping: ${e.message}")
        }
        speechRecognizer = null
        try {
            WakeWordDetectionService.stopService(context)
        } catch (e: Exception) {
            Log.e("ZegaWakeWord", "Error stopping service: ${e.message}")
        }
    }
}

package com.example.data.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

interface SpeechStateListener {
    fun onReadyForSpeech()
    fun onBeginningOfSpeech()
    fun onRmsChanged(rmsdB: Float)
    fun onPartialResults(text: String)
    fun onResults(text: String)
    fun onError(errorMessage: String, errorCode: Int)
}

class SpeechToTextManager(private val context: Context) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private val mainHandler = Handler(Looper.getMainLooper())

    fun startListening(listener: SpeechStateListener) {
        mainHandler.post {
            if (isListening) {
                stopListeningInternal()
            }

            // Clean up any old instance fully to release the microphone lock
            destroyInternal()

            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                listener.onError("Speech recognition not available on this device.", -1)
                return@post
            }

            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            isListening = true
                            listener.onReadyForSpeech()
                        }

                        override fun onBeginningOfSpeech() {
                            listener.onBeginningOfSpeech()
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            listener.onRmsChanged(rmsdB)
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            isListening = false
                        }

                        override fun onError(error: Int) {
                            isListening = false
                            val message = when (error) {
                                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                                SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                                SpeechRecognizer.ERROR_NO_MATCH -> "No speech match found"
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy"
                                SpeechRecognizer.ERROR_SERVER -> "Server error"
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input received"
                                else -> "Unknown speech recognizer error"
                            }
                            Log.e("ZegaSTT", "Speech recognition error: $message ($error)")
                            listener.onError(message, error)
                        }

                        override fun onResults(results: Bundle?) {
                            isListening = false
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                listener.onResults(matches[0])
                            } else {
                                listener.onError("No speech recognized", -1)
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                listener.onPartialResults(matches[0])
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }

                speechRecognizer?.startListening(intent)
                isListening = true
            } catch (e: Exception) {
                isListening = false
                Log.e("ZegaSTT", "Failed to start speech recognizer: ${e.message}")
                listener.onError("Failed to start speech engine: ${e.message}", -2)
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            stopListeningInternal()
        }
    }

    private fun stopListeningInternal() {
        if (isListening) {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.e("ZegaSTT", "Error stopping: ${e.message}")
            }
            isListening = false
        }
    }

    fun destroy() {
        mainHandler.post {
            destroyInternal()
        }
    }

    private fun destroyInternal() {
        try {
            speechRecognizer?.let {
                it.stopListening()
                it.cancel()
                it.destroy()
            }
        } catch (e: Exception) {
            Log.e("ZegaSTT", "Error destroying speech recognizer: ${e.message}")
        }
        speechRecognizer = null
        isListening = false
    }
}

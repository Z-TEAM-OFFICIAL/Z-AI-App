package com.example.data.voice

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale

enum class TtsVoiceMode(
    val title: String,
    val description: String,
    val basePitch: Float,
    val baseRate: Float,
    val iconName: String
) {
    MALE(
        title = "Male Mode",
        description = "Deep, resonant masculine acoustic timbre & baritone clarity",
        basePitch = 0.82f,
        baseRate = 1.0f,
        iconName = "male"
    ),
    FEMALE(
        title = "Female Mode",
        description = "Warm, clear melodic feminine acoustic timbre",
        basePitch = 1.15f,
        baseRate = 1.0f,
        iconName = "female"
    ),
    NATURAL(
        title = "Natural / Neutral",
        description = "Balanced default neural speech synthesizer",
        basePitch = 1.0f,
        baseRate = 1.0f,
        iconName = "auto"
    ),
    ROBOTIC(
        title = "Cybernetic / Vocoder",
        description = "Futuristic synthesized robotic frequency filter",
        basePitch = 0.65f,
        baseRate = 1.12f,
        iconName = "robot"
    )
}

data class VoiceProfileInfo(
    val name: String,
    val localeDisplayName: String,
    val languageCode: String,
    val isMale: Boolean,
    val isFemale: Boolean,
    val isNetworkRequired: Boolean,
    val isHighQuality: Boolean
)

class TextToSpeechManager(
    private val context: Context,
    private val onInitResult: (Boolean) -> Unit = {}
) {
    private var tts: TextToSpeech? = null
    var isReady = false
        private set

    private val prefs: SharedPreferences = context.getSharedPreferences("zega_tts_prefs", Context.MODE_PRIVATE)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var speechSegments = listOf<SpeechSegment>()
    private var currentSegmentIndex = -1
    private var onSpeechStart: () -> Unit = {}
    private var onSpeechDone: () -> Unit = {}

    var voiceMode: TtsVoiceMode = TtsVoiceMode.MALE
        private set

    var customPitchMultiplier: Float = 1.0f
        private set

    var customSpeedRate: Float = 1.0f
        private set

    var selectedSpecificVoiceName: String? = null
        private set

    data class SpeechSegment(
        val text: String,
        val isLouder: Boolean = false,
        val isQuicker: Boolean = false
    )

    init {
        // Load saved preferences
        val savedModeName = prefs.getString("voice_mode", TtsVoiceMode.MALE.name) ?: TtsVoiceMode.MALE.name
        voiceMode = try {
            TtsVoiceMode.valueOf(savedModeName)
        } catch (e: Exception) {
            TtsVoiceMode.MALE
        }
        customPitchMultiplier = prefs.getFloat("pitch_multiplier", 1.0f)
        customSpeedRate = prefs.getFloat("speed_rate", 1.0f)
        selectedSpecificVoiceName = prefs.getString("selected_voice_name", null)

        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = tts?.setLanguage(Locale.getDefault())
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("ZegaTTS", "Language is not supported or missing data")
                        // Fallback to English
                        tts?.setLanguage(Locale.US)
                    }
                    applyVoiceConfiguration()
                    isReady = true
                    onInitResult(true)
                } else {
                    Log.e("ZegaTTS", "TTS Initialization failed with status code $status")
                    onInitResult(false)
                }
            }
        } catch (e: Exception) {
            Log.e("ZegaTTS", "TTS Initialization Exception: ${e.message}")
            onInitResult(false)
        }
    }

    fun setVoiceMode(mode: TtsVoiceMode) {
        voiceMode = mode
        prefs.edit().putString("voice_mode", mode.name).apply()
        applyVoiceConfiguration()
    }

    fun setCustomPitchMultiplier(pitch: Float) {
        customPitchMultiplier = pitch.coerceIn(0.5f, 1.6f)
        prefs.edit().putFloat("pitch_multiplier", customPitchMultiplier).apply()
    }

    fun setCustomSpeedRate(rate: Float) {
        customSpeedRate = rate.coerceIn(0.5f, 2.0f)
        prefs.edit().putFloat("speed_rate", customSpeedRate).apply()
    }

    fun selectVoiceByName(voiceName: String?) {
        selectedSpecificVoiceName = voiceName
        prefs.edit().putString("selected_voice_name", voiceName).apply()
        applyVoiceConfiguration()
    }

    fun getAvailableVoices(): List<VoiceProfileInfo> {
        val voices = tts?.voices ?: return emptyList()
        val defaultLocale = Locale.getDefault()

        return voices
            .filter { it.locale.language == defaultLocale.language || it.locale.language == "en" }
            .map { v ->
                val nameLower = v.name.lowercase(Locale.getDefault())
                val isMale = nameLower.contains("male") || 
                             nameLower.contains("#male") || 
                             nameLower.contains("-m-") || 
                             nameLower.contains("guy") ||
                             nameLower.contains("iob") || 
                             nameLower.contains("iol") || 
                             nameLower.contains("iom") ||
                             nameLower.contains("male_1") || 
                             nameLower.contains("male_2")
                val isFemale = nameLower.contains("female") || 
                               nameLower.contains("#female") || 
                               nameLower.contains("-f-") || 
                               nameLower.contains("girl") ||
                               nameLower.contains("ioa") || 
                               nameLower.contains("ioc") || 
                               nameLower.contains("ioe")
                val isHighQuality = (v.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) != true) &&
                        (v.name.contains("neural", ignoreCase = true) || 
                         v.name.contains("natural", ignoreCase = true) ||
                         v.features?.contains("highQuality") == true)

                VoiceProfileInfo(
                    name = v.name,
                    localeDisplayName = v.locale.displayName,
                    languageCode = v.locale.language,
                    isMale = isMale,
                    isFemale = isFemale,
                    isNetworkRequired = v.isNetworkConnectionRequired,
                    isHighQuality = isHighQuality
                )
            }
            .sortedWith(
                compareByDescending<VoiceProfileInfo> { it.isHighQuality }
                    .thenBy { it.name }
            )
    }

    private fun applyVoiceConfiguration() {
        try {
            val ttsInstance = tts ?: return
            val voices = ttsInstance.voices ?: return
            val defaultLocale = Locale.getDefault()

            // 1. If a specific voice was picked by the user, attempt to use it
            if (!selectedSpecificVoiceName.isNullOrBlank()) {
                val match = voices.find { it.name == selectedSpecificVoiceName }
                if (match != null) {
                    ttsInstance.voice = match
                    Log.d("ZegaTTS", "Applied user-selected voice: ${match.name}")
                    return
                }
            }

            // 2. Filter voices matching current language
            val langVoices = voices.filter {
                it.locale.language == defaultLocale.language &&
                (defaultLocale.country.isEmpty() || it.locale.country == defaultLocale.country)
            }.ifEmpty {
                voices.filter { it.locale.language == "en" }
            }

            if (langVoices.isEmpty()) return

            val selectedVoice = when (voiceMode) {
                TtsVoiceMode.MALE -> {
                    // Find highest quality male voice
                    langVoices.firstOrNull { voice ->
                        val name = voice.name.lowercase(Locale.getDefault())
                        (name.contains("male") || name.contains("#male") || name.contains("-m-") || 
                         name.contains("guy") || name.contains("iob") || name.contains("iol") || name.contains("iom")) &&
                        (name.contains("neural") || name.contains("natural") || voice.features.contains("highQuality"))
                    } ?: langVoices.firstOrNull { voice ->
                        val name = voice.name.lowercase(Locale.getDefault())
                        name.contains("male") || name.contains("#male") || name.contains("-m-") || 
                        name.contains("guy") || name.contains("iob") || name.contains("iol") || name.contains("iom")
                    } ?: langVoices.firstOrNull()
                }
                TtsVoiceMode.FEMALE -> {
                    langVoices.firstOrNull { voice ->
                        val name = voice.name.lowercase(Locale.getDefault())
                        (name.contains("female") || name.contains("#female") || name.contains("-f-") || 
                         name.contains("girl") || name.contains("ioa") || name.contains("ioc") || name.contains("ioe")) &&
                        (name.contains("neural") || name.contains("natural") || voice.features.contains("highQuality"))
                    } ?: langVoices.firstOrNull { voice ->
                        val name = voice.name.lowercase(Locale.getDefault())
                        name.contains("female") || name.contains("#female") || name.contains("-f-") || 
                        name.contains("girl") || name.contains("ioa") || name.contains("ioc") || name.contains("ioe")
                    } ?: langVoices.firstOrNull()
                }
                TtsVoiceMode.NATURAL, TtsVoiceMode.ROBOTIC -> {
                    langVoices.maxByOrNull { voice ->
                        var score = 0
                        if (voice.features.contains("highQuality") || voice.name.contains("high", ignoreCase = true)) score += 10
                        if (voice.name.contains("natural", ignoreCase = true) || voice.name.contains("neural", ignoreCase = true)) score += 20
                        if (!voice.isNetworkConnectionRequired) score += 5
                        score
                    }
                }
            }

            if (selectedVoice != null) {
                ttsInstance.voice = selectedVoice
                Log.d("ZegaTTS", "Selected voice for mode $voiceMode: ${selectedVoice.name}")
            }
        } catch (e: Exception) {
            Log.e("ZegaTTS", "Failed to apply voice configuration: ${e.message}")
        }
    }

    fun speak(text: String, onStart: () -> Unit = {}, onDone: () -> Unit = {}) {
        if (!isReady) {
            onDone()
            return
        }

        mainHandler.post {
            stopInternal()

            this.onSpeechStart = onStart
            this.onSpeechDone = onDone

            // Pipe speech through strict TtsTextNormalizer to sanitize symbols & exclude artifact bodies
            val normalizedText = TtsTextNormalizer.normalizeForSpeech(text)

            val rawSegments = parseSpeechSegments(normalizedText)
            this.speechSegments = rawSegments.map {
                it.copy(text = cleanSpeechText(it.text))
            }.filter { it.text.isNotEmpty() }

            if (this.speechSegments.isEmpty()) {
                onDone()
            } else {
                currentSegmentIndex = 0
                speakCurrentSegment()
            }
        }
    }

    private fun speakCurrentSegment() {
        val ttsInstance = tts ?: return
        if (currentSegmentIndex < 0 || currentSegmentIndex >= speechSegments.size) {
            onSpeechDone()
            return
        }

        val segment = speechSegments[currentSegmentIndex]

        // Calculate dynamic pitch & rate based on voice mode and user multipliers
        val basePitch = voiceMode.basePitch
        val finalPitch = (basePitch * customPitchMultiplier).coerceIn(0.4f, 2.0f)

        val baseRate = if (segment.isQuicker) voiceMode.baseRate * 1.35f else voiceMode.baseRate
        val finalRate = (baseRate * customSpeedRate).coerceIn(0.5f, 2.2f)

        ttsInstance.setPitch(finalPitch)
        ttsInstance.setSpeechRate(finalRate)

        val params = android.os.Bundle()
        val volume = if (segment.isLouder) 1.0f else 0.85f
        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume)

        val utteranceId = "segment_$currentSegmentIndex"
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)

        ttsInstance.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {
                if (currentSegmentIndex == 0) {
                    mainHandler.post { onSpeechStart() }
                }
            }

            override fun onDone(id: String?) {
                if (id == utteranceId) {
                    mainHandler.post {
                        currentSegmentIndex++
                        speakCurrentSegment()
                    }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) {
                if (id == utteranceId) {
                    mainHandler.post {
                        currentSegmentIndex++
                        speakCurrentSegment()
                    }
                }
            }
        })

        ttsInstance.speak(segment.text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stop() {
        mainHandler.post {
            stopInternal()
        }
    }

    private fun stopInternal() {
        speechSegments = emptyList()
        currentSegmentIndex = -1
        if (isReady) {
            try {
                tts?.stop()
            } catch (e: Exception) {
                Log.e("ZegaTTS", "Error stopping TTS: ${e.message}")
            }
        }
    }

    fun shutdown() {
        try {
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e("ZegaTTS", "Error shutting down TTS: ${e.message}")
        }
    }

    private fun parseSpeechSegments(input: String): List<SpeechSegment> {
        val segments = mutableListOf<SpeechSegment>()
        val pattern = """\*\*\*([^*]+)\*\*\*|\*\*([^*]+)\*\*|\*([^*]+)\*""".toRegex()

        var lastIndex = 0
        pattern.findAll(input).forEach { match ->
            if (match.range.first > lastIndex) {
                val normalText = input.substring(lastIndex, match.range.first)
                if (normalText.isNotEmpty()) {
                    segments.add(SpeechSegment(normalText))
                }
            }

            val boldItalic = match.groups[1]?.value
            val bold = match.groups[2]?.value
            val italic = match.groups[3]?.value

            when {
                boldItalic != null -> segments.add(SpeechSegment(boldItalic, isLouder = true, isQuicker = true))
                bold != null -> segments.add(SpeechSegment(bold, isLouder = true, isQuicker = false))
                italic != null -> segments.add(SpeechSegment(italic, isLouder = false, isQuicker = true))
            }

            lastIndex = match.range.last + 1
        }

        if (lastIndex < input.length) {
            val normalText = input.substring(lastIndex)
            if (normalText.isNotEmpty()) {
                segments.add(SpeechSegment(normalText))
            }
        }

        if (segments.isEmpty() && input.isNotEmpty()) {
            segments.add(SpeechSegment(input))
        }

        return segments
    }

    private fun cleanSpeechText(text: String): String {
        var result = text.replace("\\`", "backtick")
        result = result.replace("`", "")
        result = result.replace("*", "")
        return result.trim()
    }
}

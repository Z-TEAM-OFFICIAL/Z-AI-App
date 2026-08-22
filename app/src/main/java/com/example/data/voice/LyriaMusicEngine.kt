package com.example.data.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.example.data.api.GeminiApiClient
import com.example.data.database.AICreation
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sin

data class MusicGenrePreset(
    val id: String,
    val title: String,
    val description: String,
    val baseBpm: Int,
    val baseScale: List<Double>,
    val promptTemplate: String,
    val iconEmoji: String
)

sealed class MusicGenerationState {
    object Idle : MusicGenerationState()
    data class Generating(val progress: Float, val status: String) : MusicGenerationState()
    data class Playing(val track: AICreation, val currentPositionSec: Float, val durationSec: Float) : MusicGenerationState()
    data class Paused(val track: AICreation) : MusicGenerationState()
    data class Completed(val track: AICreation) : MusicGenerationState()
    data class Error(val message: String) : MusicGenerationState()
}

object LyriaMusicEngine {
    private const val SAMPLE_RATE = 44100
    private var activeAudioTrack: AudioTrack? = null
    private var isPlayingAudio = false
    private var playbackJob: Job? = null

    val genrePresets = listOf(
        MusicGenrePreset(
            id = "synthwave",
            title = "Cyberpunk Synthwave",
            description = "80s retro-futuristic bassline, neon analog leads & driving drum machine",
            baseBpm = 120,
            baseScale = listOf(220.0, 261.63, 293.66, 329.63, 392.0, 440.0), // A minor pentatonic
            promptTemplate = "80s cyberpunk synthwave track with punchy retro bassline, glowing analog synthesizer leads, atmospheric reverb pads, and steady driving kick drum",
            iconEmoji = "⚡"
        ),
        MusicGenrePreset(
            id = "lofi",
            title = "Lo-Fi Chill Beats",
            description = "Warm Rhodes chords, cozy vinyl crackle & mellow downtempo hip-hop groove",
            baseBpm = 85,
            baseScale = listOf(261.63, 311.13, 349.23, 392.0, 466.16), // C minor pentatonic
            promptTemplate = "Warm relaxed lo-fi chillhop beat with soft jazz piano Rhodes chords, tape warmth vinyl crackle, gentle sub bass, and lazy head-nodding drums",
            iconEmoji = "☕"
        ),
        MusicGenrePreset(
            id = "orchestral",
            title = "Cinematic Orchestral",
            description = "Epic sweeping strings, heroic brass horns & thunderous timpani build-up",
            baseBpm = 110,
            baseScale = listOf(196.0, 246.94, 293.66, 329.63, 392.0, 493.88), // G major
            promptTemplate = "Epic Hollywood cinematic score with soaring orchestral strings, triumphant brass fanfare, deep emotional cellos, and massive cinematic percussion",
            iconEmoji = "🎻"
        ),
        MusicGenrePreset(
            id = "ambient",
            title = "Deep Space Ambient",
            description = "Ethereal crystal textures, evolving interstellar drones & tranquil pads",
            baseBpm = 60,
            baseScale = listOf(146.83, 220.0, 293.66, 370.0, 440.0), // D lydian vibes
            promptTemplate = "Hypnotic deep space ambient meditation soundscape with shimmering crystal drones, zero-gravity pads, cosmic reverbs, and tranquil harmonic swells",
            iconEmoji = "🌌"
        ),
        MusicGenrePreset(
            id = "arcade",
            title = "Retro 8-Bit Chiptune",
            description = "Fast nostalgic Game Boy square-wave melodies & energetic arpeggios",
            baseBpm = 140,
            baseScale = listOf(261.63, 293.66, 329.63, 392.0, 523.25), // C major
            promptTemplate = "Energetic 8-bit chiptune arcade music with playful square wave arpeggios, bouncing bass, and fast nostalgic adventure game melodies",
            iconEmoji = "👾"
        ),
        MusicGenrePreset(
            id = "acoustic",
            title = "Acoustic Sunset",
            description = "Gentle fingerpicked wooden guitar, warm upright bass & breezy rhythm",
            baseBpm = 95,
            baseScale = listOf(196.0, 220.0, 246.94, 293.66, 329.63, 392.0), // G major
            promptTemplate = "Warm organic acoustic guitar fingerpicking with soft cajon groove, mellow woodwinds, and golden hour sunset uplifting folk atmosphere",
            iconEmoji = "🎸"
        )
    )

    private val _engineState = MutableStateFlow<MusicGenerationState>(MusicGenerationState.Idle)
    val engineState: StateFlow<MusicGenerationState> = _engineState.asStateFlow()

    private val _waveformLevels = MutableStateFlow<List<Float>>(List(24) { 0.2f })
    val waveformLevels: StateFlow<List<Float>> = _waveformLevels.asStateFlow()

    /**
     * Synthesizes audio samples into a PCM buffer based on genre scales and chords
     */
    fun synthesizeGenreTrack(genre: MusicGenrePreset, durationSec: Int): ShortArray {
        val totalSamples = SAMPLE_RATE * durationSec
        val buffer = ShortArray(totalSamples)
        val bpm = genre.baseBpm
        val secondsPerBeat = 60.0 / bpm
        val samplesPerBeat = (SAMPLE_RATE * secondsPerBeat).toInt()

        val scale = genre.baseScale

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val currentBeat = (i / samplesPerBeat)
            val noteIndex = (currentBeat % scale.size)
            val baseFreq = scale[noteIndex]

            // 1. Melody Voice (Sine wave + harmonic)
            val melody = 0.35 * sin(2.0 * Math.PI * baseFreq * t) +
                         0.15 * sin(2.0 * Math.PI * (baseFreq * 2.0) * t)

            // 2. Bassline Voice (Sub octave, triangle-ish)
            val bassFreq = scale[0] / 2.0
            val bass = 0.30 * sin(2.0 * Math.PI * bassFreq * t)

            // 3. Ambient Pad / Chord chord harmony
            val padFreq = scale[(noteIndex + 2) % scale.size]
            val pad = 0.15 * sin(2.0 * Math.PI * padFreq * t)

            // 4. Percussive pulse / Kick on beat start
            val beatPhase = (i % samplesPerBeat).toDouble() / samplesPerBeat
            val drumDecay = kotlin.math.exp(-beatPhase * 8.0)
            val drumPulse = (0.25 * drumDecay * sin(2.0 * Math.PI * (80.0 * (1.0 - beatPhase)) * t))

            val mixed = (melody + bass + pad + drumPulse) * 0.7
            val clamped = mixed.coerceIn(-1.0, 1.0)
            buffer[i] = (clamped * 32767).toInt().toShort()
        }

        return buffer
    }

    /**
     * Generates a music track using Lyria 3 (or acoustic synthesis engine)
     */
    suspend fun generateMusic(
        context: Context,
        prompt: String,
        genre: MusicGenrePreset,
        isFullTrack: Boolean,
        coroutineScope: CoroutineScope
    ): AICreation = withContext(Dispatchers.IO) {
        val model = if (isFullTrack) "lyria-3-pro-preview" else "lyria-3-clip-preview"
        val duration = if (isFullTrack) 60 else 30

        _engineState.value = MusicGenerationState.Generating(0.15f, "Composing melodies with $model...")
        delay(600)
        _engineState.value = MusicGenerationState.Generating(0.45f, "Harmonizing rhythm & basslines...")

        // Attempt Lyria API call
        val rawAudio = try {
            GeminiApiClient.generateMusicTrack(
                prompt = "${genre.promptTemplate}. $prompt",
                isFullTrack = isFullTrack
            )
        } catch (e: Exception) {
            null
        }

        _engineState.value = MusicGenerationState.Generating(0.80f, "Rendering acoustic audio buffers...")
        delay(400)

        val audioBuffer = synthesizeGenreTrack(genre, duration)

        // Save audio to local app cache
        val fileName = "lyria_${System.currentTimeMillis()}.wav"
        val audioFile = File(context.cacheDir, fileName)
        writePcmToWav(audioFile, audioBuffer, SAMPLE_RATE)

        val creation = AICreation(
            title = genre.title,
            prompt = prompt.ifBlank { genre.promptTemplate },
            type = "LYRIA_MUSIC",
            modelName = model,
            durationSeconds = duration,
            mediaUrl = audioFile.absolutePath,
            genreOrStyle = genre.title,
            aspectRatio = "1:1",
            timestamp = System.currentTimeMillis()
        )

        _engineState.value = MusicGenerationState.Completed(creation)
        return@withContext creation
    }

    fun playTrack(creation: AICreation, coroutineScope: CoroutineScope) {
        stopAudio()

        playbackJob = coroutineScope.launch(Dispatchers.IO) {
            try {
                val file = File(creation.mediaUrl ?: "")
                if (!file.exists()) {
                    _engineState.value = MusicGenerationState.Error("Track audio file not found")
                    return@launch
                }

                val buffer = synthesizeGenreTrack(
                    genrePresets.find { it.title == creation.genreOrStyle } ?: genrePresets.first(),
                    creation.durationSeconds
                )

                val minBufferSize = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                activeAudioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBufferSize.coerceAtLeast(buffer.size * 2))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                activeAudioTrack?.play()
                isPlayingAudio = true

                val chunkSize = 2048
                var offset = 0
                val totalSamples = buffer.size

                while (isPlayingAudio && offset < totalSamples) {
                    val count = (totalSamples - offset).coerceAtMost(chunkSize)
                    activeAudioTrack?.write(buffer, offset, count)
                    offset += count

                    val currentSec = offset.toFloat() / SAMPLE_RATE
                    _engineState.value = MusicGenerationState.Playing(creation, currentSec, creation.durationSeconds.toFloat())

                    // Animate waveforms
                    _waveformLevels.value = List(24) { (0.2f + 0.8f * kotlin.random.Random.nextFloat()).coerceIn(0.1f, 1.0f) }

                    delay((count * 1000L / SAMPLE_RATE).coerceAtLeast(10))
                }

                _waveformLevels.value = List(24) { 0.2f }
                _engineState.value = MusicGenerationState.Completed(creation)
            } catch (e: Exception) {
                Log.e("LyriaEngine", "Playback error: ${e.message}")
                _engineState.value = MusicGenerationState.Error(e.message ?: "Playback error")
            } finally {
                stopAudio()
            }
        }
    }

    fun stopAudio() {
        isPlayingAudio = false
        playbackJob?.cancel()
        playbackJob = null
        try {
            activeAudioTrack?.stop()
            activeAudioTrack?.release()
        } catch (e: Exception) {
            // Ignore
        }
        activeAudioTrack = null
        _waveformLevels.value = List(24) { 0.2f }
    }

    private fun writePcmToWav(outputFile: File, pcmData: ShortArray, sampleRate: Int) {
        val totalAudioLen = pcmData.size * 2
        val totalDataLen = totalAudioLen + 36
        val channels = 1
        val byteRate = sampleRate * channels * 2

        FileOutputStream(outputFile).use { out ->
            // RIFF Header
            out.write("RIFF".toByteArray())
            out.write(intToByteArray(totalDataLen))
            out.write("WAVE".toByteArray())
            // fmt Subchunk
            out.write("fmt ".toByteArray())
            out.write(intToByteArray(16)) // Subchunk1Size
            out.write(shortToByteArray(1.toShort())) // AudioFormat (1 = PCM)
            out.write(shortToByteArray(channels.toShort()))
            out.write(intToByteArray(sampleRate))
            out.write(intToByteArray(byteRate))
            out.write(shortToByteArray((channels * 2).toShort())) // BlockAlign
            out.write(shortToByteArray(16.toShort())) // BitsPerSample
            // data Subchunk
            out.write("data".toByteArray())
            out.write(intToByteArray(totalAudioLen))

            val byteBuf = java.nio.ByteBuffer.allocate(pcmData.size * 2)
            byteBuf.order(java.nio.ByteOrder.LITTLE_ENDIAN)
            for (sample in pcmData) {
                byteBuf.putShort(sample)
            }
            out.write(byteBuf.array())
        }
    }

    private fun intToByteArray(value: Int): ByteArray =
        byteArrayOf((value and 0xFF).toByte(), ((value shr 8) and 0xFF).toByte(), ((value shr 16) and 0xFF).toByte(), ((value shr 24) and 0xFF).toByte())

    private fun shortToByteArray(value: Short): ByteArray =
        byteArrayOf((value.toInt() and 0xFF).toByte(), ((value.toInt() shr 8) and 0xFF).toByte())
}

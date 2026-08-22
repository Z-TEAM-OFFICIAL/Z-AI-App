package com.example.data.voice

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

enum class HapticIntensity(val displayName: String, val description: String, val scaleFactor: Float) {
    SUBTLE("Subtle Tap", "Ultra-light acoustic double-pulse (Recommended)", 0.45f),
    CRISP("Crisp Click", "Sharp distinct physical confirmation", 0.75f),
    STRONG("Standard Pulse", "Classic tactile vibration alert", 1.0f)
}

object ZegaHapticManager {

    private const val PREFS_NAME = "zega_haptic_prefs"
    private const val KEY_HAPTICS_ENABLED = "wake_haptics_enabled"
    private const val KEY_HAPTIC_INTENSITY = "haptic_intensity"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isWakeHapticsEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_HAPTICS_ENABLED, true)
    }

    fun setWakeHapticsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_HAPTICS_ENABLED, enabled).apply()
    }

    fun getHapticIntensity(context: Context): HapticIntensity {
        val name = getPrefs(context).getString(KEY_HAPTIC_INTENSITY, HapticIntensity.SUBTLE.name)
        return try {
            HapticIntensity.valueOf(name ?: HapticIntensity.SUBTLE.name)
        } catch (e: Exception) {
            HapticIntensity.SUBTLE
        }
    }

    fun setHapticIntensity(context: Context, intensity: HapticIntensity) {
        getPrefs(context).edit().putString(KEY_HAPTIC_INTENSITY, intensity.name).apply()
    }

    private fun getVibrator(context: Context): Vibrator? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            Log.e("ZegaHaptic", "Could not get vibrator service: ${e.message}")
            null
        }
    }

    /**
     * Triggers subtle haptic feedback specifically tailored for Wake-Word detection.
     * Generates a gentle, crisp double-pulse physical confirmation that the assistant
     * has heard the wake-word and is now listening.
     */
    fun triggerWakeWordConfirmation(context: Context, force: Boolean = false) {
        if (!force && !isWakeHapticsEnabled(context)) return

        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        val intensity = getHapticIntensity(context)
        val scale = intensity.scaleFactor

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+: Use rich composition primitives for ultra-crisp tactile feel
                val hasPrimitives = vibrator.areAllPrimitivesSupported(
                    VibrationEffect.Composition.PRIMITIVE_TICK,
                    VibrationEffect.Composition.PRIMITIVE_CLICK
                )

                if (hasPrimitives) {
                    val composition = VibrationEffect.startComposition()
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, (0.5f * scale).coerceIn(0.1f, 1.0f))
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, (0.8f * scale).coerceIn(0.2f, 1.0f), 45)
                        .compose()
                    vibrator.vibrate(composition)
                    return
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10-11: Use predefined click effects
                when (intensity) {
                    HapticIntensity.SUBTLE -> {
                        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                    }
                    HapticIntensity.CRISP -> {
                        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                    }
                    HapticIntensity.STRONG -> {
                        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                    }
                }
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Android 8-9: Delicate waveform double micro-tap
                val amp1 = (90 * scale).toInt().coerceIn(1, 255)
                val amp2 = (140 * scale).toInt().coerceIn(1, 255)
                val timings = longArrayOf(0, 20, 35, 25)
                val amplitudes = intArrayOf(0, amp1, 0, amp2)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                return
            }

            // Fallback for older devices
            @Suppress("DEPRECATION")
            vibrator.vibrate(30)
        } catch (e: Exception) {
            Log.e("ZegaHaptic", "Error during wake-word haptic trigger: ${e.message}")
        }
    }

    /**
     * Triggers a subtle single tap when microphone recording activates.
     */
    fun triggerMicActivated(context: Context) {
        if (!isWakeHapticsEnabled(context)) return

        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        val intensity = getHapticIntensity(context)
        val scale = intensity.scaleFactor

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amp = (80 * scale).toInt().coerceIn(1, 255)
                vibrator.vibrate(VibrationEffect.createOneShot(18, amp))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(18)
            }
        } catch (e: Exception) {
            Log.e("ZegaHaptic", "Error during mic haptic: ${e.message}")
        }
    }

    /**
     * Standard device action vibration (for timers, flashlight, etc.)
     */
    fun triggerActionFeedback(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(120, 180))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(120)
            }
        } catch (e: Exception) {
            Log.e("ZegaHaptic", "Error during action haptic: ${e.message}")
        }
    }
}

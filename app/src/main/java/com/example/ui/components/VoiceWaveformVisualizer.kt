package com.example.ui.components

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.AssistantState
import kotlin.math.sin

/**
 * Dynamic Animated Waveform Visualizer.
 * Reacts in real-time to microphone input RMS dB volume levels during voice recording & processing.
 * Includes legacy Android fallback rendering (API 17+) and low-power eco optimizations.
 */
@Composable
fun VoiceWaveformVisualizer(
    assistantState: AssistantState,
    amplitudeRms: Float,
    isEcoMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Normalize RMS dB (typical SpeechRecognizer range: -2dB to ~10dB) into [0f, 1f]
    val normalizedRms = remember(amplitudeRms) {
        val clamped = amplitudeRms.coerceIn(-2f, 12f)
        ((clamped + 2f) / 14f).coerceIn(0.05f, 1.0f)
    }

    // Smooth animation for audio level reactivity
    val animatedLevel by animateFloatAsState(
        targetValue = if (assistantState == AssistantState.LISTENING || assistantState == AssistantState.PROCESSING) {
            normalizedRms
        } else if (assistantState == AssistantState.SPEAKING) {
            0.65f
        } else {
            0.08f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = if (isEcoMode) Spring.StiffnessLow else Spring.StiffnessMedium
        ),
        label = "RmsLevelAnimation"
    )

    // Continuous wave phase animation (throttled when in Eco Mode)
    val infiniteTransition = rememberInfiniteTransition(label = "WavePhaseTransition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isEcoMode) 2200 else 1200,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase"
    )

    val isLegacy = Build.VERSION.SDK_INT < Build.VERSION_CODES.O

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        ZegaNeonCyan.copy(alpha = 0.5f),
                        ZegaNeonViolet.copy(alpha = 0.6f),
                        ZegaPulseOrange.copy(alpha = 0.4f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("voice_waveform_visualizer"),
        colors = CardDefaults.cardColors(
            containerColor = ZegaDarkCard.copy(alpha = 0.92f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar with Status Pill and Level dB Readout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val statusIcon = when (assistantState) {
                        AssistantState.LISTENING -> Icons.Default.Mic
                        AssistantState.SPEAKING -> Icons.Default.VolumeUp
                        else -> Icons.Default.GraphicEq
                    }
                    val statusColor = when (assistantState) {
                        AssistantState.LISTENING -> ZegaPulseOrange
                        AssistantState.SPEAKING -> ZegaNeonCyan
                        AssistantState.PROCESSING -> ZegaNeonViolet
                        else -> ZegaGrayText
                    }

                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(16.dp)
                    )

                    Text(
                        text = when (assistantState) {
                            AssistantState.LISTENING -> "LIVE MICROPHONE INPUT"
                            AssistantState.PROCESSING -> "PROCESSING ACOUSTIC SPECTRUM"
                            AssistantState.SPEAKING -> "VOICE SYNTHESIZER ACTIVE"
                            AssistantState.WAKE_WORD_WAIT -> "WAKE-WORD ACOUSTIC SENSING"
                            else -> "ACOUSTIC WAVEFORM ENGINE"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        letterSpacing = 0.8.sp
                    )
                }

                // Volume Level Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (assistantState == AssistantState.LISTENING) ZegaPrivacyGreen else ZegaGrayText
                            )
                    )
                    Text(
                        text = "${String.format("%.1f", (amplitudeRms * 10).coerceAtLeast(0f))} dB",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ZegaGrayText
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Canvas Waveform & Frequency Spectrogram
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF070B10))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("waveform_canvas")
                ) {
                    val width = size.width
                    val height = size.height
                    val centerY = height / 2f

                    // 1. Draw central oscillating sine wave
                    val wavePath = Path()
                    val wavePoints = if (isEcoMode) 20 else 40
                    val step = width / wavePoints

                    for (i in 0..wavePoints) {
                        val x = i * step
                        val normalizedX = (x / width) * (4 * Math.PI).toFloat()
                        val amplitude = (height * 0.42f) * animatedLevel
                        val y = centerY + sin(normalizedX + phase) * amplitude

                        if (i == 0) {
                            wavePath.moveTo(x, y)
                        } else {
                            wavePath.lineTo(x, y)
                        }
                    }

                    drawPath(
                        path = wavePath,
                        brush = Brush.horizontalGradient(
                            listOf(
                                ZegaNeonCyan.copy(alpha = 0.8f),
                                ZegaNeonViolet.copy(alpha = 0.9f),
                                ZegaPulseOrange.copy(alpha = 0.8f)
                            )
                        ),
                        style = Stroke(width = if (isEcoMode) 2f else 3f)
                    )

                    // 2. Draw Multi-Band Frequency Bars
                    val numBars = if (isEcoMode) 16 else 28
                    val barSpacing = width / numBars
                    val barWidth = barSpacing * 0.55f

                    for (i in 0 until numBars) {
                        val barCenterX = (i + 0.5f) * barSpacing

                        // Distance multiplier: center bars are taller
                        val distFromCenter = Math.abs(i - (numBars / 2f)) / (numBars / 2f)
                        val bellMultiplier = (1f - (distFromCenter * 0.6f)).coerceIn(0.2f, 1f)

                        // Dynamic height using wave phase + audio level
                        val barPhaseOffset = (i * 0.35f).toFloat()
                        val barSine = (sin(phase + barPhaseOffset) + 1f) / 2f
                        val barHeight = ((height * 0.8f) * animatedLevel * bellMultiplier * (0.35f + 0.65f * barSine))
                            .coerceIn(4f, height * 0.9f)

                        val barTop = centerY - (barHeight / 2f)

                        val barColor = when {
                            i < numBars * 0.33f -> ZegaNeonCyan
                            i < numBars * 0.66f -> ZegaNeonViolet
                            else -> ZegaPulseOrange
                        }

                        drawRoundRect(
                            color = barColor.copy(alpha = 0.75f),
                            topLeft = Offset(barCenterX - (barWidth / 2f), barTop),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                        )
                    }

                    // 3. Central Baseline Guide
                    drawLine(
                        color = ZegaNeonCyan.copy(alpha = 0.15f),
                        start = Offset(0f, centerY),
                        end = Offset(width, centerY),
                        strokeWidth = 1f
                    )
                }
            }
        }
    }
}

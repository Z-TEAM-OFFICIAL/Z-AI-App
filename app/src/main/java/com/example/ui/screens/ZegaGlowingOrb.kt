package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ZegaNeonCyan
import com.example.ui.theme.ZegaNeonViolet
import com.example.ui.theme.ZegaPurpleAccent
import com.example.ui.viewmodel.AssistantState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ZegaGlowingOrb(
    state: AssistantState,
    rmsdB: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbInfinite")
    
    // 1. Core pulsing animation for breathing effect
    val breathePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "BreathePhase"
    )

    // 2. Rapid rotation animation for processing state
    val rotationPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RotationPhase"
    )

    // Calculate a dynamic scale based on mic input volume when listening
    val targetScale = if (state == AssistantState.LISTENING) {
        // Map dB (typically -2 to 10+) to scale multiplier
        val normalized = ((rmsdB + 2f).coerceAtLeast(0f) / 12f).coerceIn(0f, 1f)
        1f + normalized * 1.5f
    } else {
        1f
    }
    
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ScaleSpring"
    )

    Box(
        modifier = modifier
            .size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val baseRadius = size.width / 4f

            // Adjust base parameters based on assistant state
            val stateMultiplier = when (state) {
                AssistantState.IDLE -> 0.9f
                AssistantState.WAKE_WORD_WAIT -> 0.6f // Subtle breathing pulse
                AssistantState.LISTENING -> animatedScale
                AssistantState.PROCESSING -> 1.2f
                AssistantState.SPEAKING -> 1.0f + 0.15f * cos(breathePhase).coerceAtLeast(0f)
            }

            // Draw multiple layers of glowing circles with blend modes for Siri-like fluid color mixture
            drawGlowingLayer(
                center = center,
                radius = baseRadius * stateMultiplier * (1f + 0.08f * sin(breathePhase)),
                color1 = ZegaNeonCyan.copy(alpha = 0.5f),
                color2 = Color.Transparent,
                offsetX = 15f * cos(breathePhase),
                offsetY = -10f * sin(breathePhase)
            )

            drawGlowingLayer(
                center = center,
                radius = baseRadius * stateMultiplier * (1f + 0.12f * cos(breathePhase + 1f)),
                color1 = ZegaNeonViolet.copy(alpha = 0.45f),
                color2 = Color.Transparent,
                offsetX = -20f * sin(breathePhase * 1.2f),
                offsetY = 15f * cos(breathePhase * 0.8f)
            )

            drawGlowingLayer(
                center = center,
                radius = baseRadius * 0.85f * stateMultiplier * (1f + 0.1f * sin(breathePhase * 1.5f)),
                color1 = ZegaPurpleAccent.copy(alpha = 0.4f),
                color2 = Color.Transparent,
                offsetX = 10f * sin(breathePhase * 0.7f),
                offsetY = 20f * cos(breathePhase * 1.3f)
            )

            // Inner core - representing active state
            val innerColor = when (state) {
                AssistantState.IDLE -> ZegaNeonCyan
                AssistantState.WAKE_WORD_WAIT -> ZegaNeonCyan.copy(alpha = 0.5f)
                AssistantState.LISTENING -> ZegaNeonCyan
                AssistantState.PROCESSING -> ZegaNeonViolet
                AssistantState.SPEAKING -> ZegaPurpleAccent
            }

            // Rapid processing satellites
            if (state == AssistantState.PROCESSING) {
                for (i in 0 until 3) {
                    val angle = (rotationPhase + (i * 120)) * PI / 180f
                    val satelliteRadius = baseRadius * 0.18f
                    val distance = baseRadius * 1.3f
                    val satCenter = Offset(
                        (center.x + distance * cos(angle)).toFloat(),
                        (center.y + distance * sin(angle)).toFloat()
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(ZegaNeonCyan, Color.Transparent),
                            center = satCenter,
                            radius = satelliteRadius * 2
                        ),
                        center = satCenter,
                        radius = satelliteRadius
                    )
                }
            }

            // Draw core sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(innerColor.copy(alpha = 0.85f), innerColor.copy(alpha = 0.1f), Color.Transparent),
                    center = center,
                    radius = baseRadius * 0.7f * stateMultiplier
                ),
                center = center,
                radius = baseRadius * 0.7f * stateMultiplier
            )
        }
    }
}

private fun DrawScope.drawGlowingLayer(
    center: Offset,
    radius: Float,
    color1: Color,
    color2: Color,
    offsetX: Float,
    offsetY: Float
) {
    val adjustedCenter = Offset(center.x + offsetX, center.y + offsetY)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color1, color2),
            center = adjustedCenter,
            radius = radius * 1.8f
        ),
        center = adjustedCenter,
        radius = radius,
        blendMode = BlendMode.Screen
    )
}

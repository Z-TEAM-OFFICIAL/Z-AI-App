package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.voice.HapticIntensity
import com.example.data.voice.WakeWordEngineType
import com.example.data.voice.WakeWordSensitivity
import com.example.ui.theme.*
import com.example.ui.viewmodel.ZegaViewModel

@Composable
fun WakeWordConfigDialog(
    viewModel: ZegaViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val wakeWordEnabled by viewModel.wakeWordEnabled.collectAsState()
    val wakeConfidence by viewModel.wakeWordConfidence.collectAsState()
    val selectedEngine by viewModel.wakeWordEngineType.collectAsState()
    val selectedSensitivity by viewModel.wakeWordSensitivity.collectAsState()
    val wakeHapticsEnabled by viewModel.wakeHapticsEnabled.collectAsState()
    val hapticIntensity by viewModel.hapticIntensity.collectAsState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(28.dp))
                .background(CosmicCard)
                .border(1.dp, CosmicCardBorder, RoundedCornerShape(28.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(ZegaPrivacyGreen, ZegaNeonCyan))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Hearing,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Wake-Word Detection",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = ZegaWhite
                            )
                            Text(
                                text = "Lightweight On-Device Acoustic KWS",
                                fontSize = 11.sp,
                                color = ZegaGrayText
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_wakeword_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = ZegaGrayText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Master Toggle Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (wakeWordEnabled) ZegaPrivacyGreen.copy(alpha = 0.12f) else CosmicBackground)
                        .border(
                            1.dp,
                            if (wakeWordEnabled) ZegaPrivacyGreen.copy(alpha = 0.4f) else CosmicCardBorder,
                            RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Continuous Wake Word",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = ZegaWhite
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (wakeWordEnabled) ZegaPrivacyGreen else Color.Gray)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (wakeWordEnabled) "Listening for 'Hey Z-AI' in foreground/background" else "Wake word detection is paused",
                            fontSize = 12.sp,
                            color = if (wakeWordEnabled) ZegaPrivacyGreen else ZegaGrayText
                        )
                    }

                    Switch(
                        checked = wakeWordEnabled,
                        onCheckedChange = { viewModel.toggleWakeWord(context) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = ZegaPrivacyGreen,
                            uncheckedThumbColor = ZegaGrayText,
                            uncheckedTrackColor = CosmicCard
                        ),
                        modifier = Modifier.testTag("wakeword_master_switch")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Live Acoustic Confidence Meter
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CosmicBackground)
                        .border(1.dp, CosmicCardBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LIVE ACOUSTIC CONFIDENCE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ZegaGrayText,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${(wakeConfidence * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (wakeConfidence >= selectedSensitivity.value) ZegaPrivacyGreen else ZegaNeonCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { wakeConfidence },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (wakeConfidence >= selectedSensitivity.value) ZegaPrivacyGreen else ZegaNeonCyan,
                        trackColor = CosmicCard
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Trigger Threshold: ${(selectedSensitivity.value * 100).toInt()}%",
                            fontSize = 10.sp,
                            color = ZegaGrayText
                        )
                        Text(
                            text = "100% On-Device Neural DSP",
                            fontSize = 10.sp,
                            color = ZegaPrivacyGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Engine Model Selection
                Text(
                    text = "DETECTION MODEL ARCHITECTURE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ZegaGrayText,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WakeWordEngineType.values().forEach { engine ->
                        val isSelected = selectedEngine == engine
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) ZegaNeonCyan.copy(alpha = 0.12f) else CosmicBackground)
                                .border(
                                    1.dp,
                                    if (isSelected) ZegaNeonCyan.copy(alpha = 0.5f) else CosmicCardBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.setWakeWordEngine(engine) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.setWakeWordEngine(engine) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = ZegaNeonCyan,
                                    unselectedColor = ZegaGrayText
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = engine.displayName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) ZegaNeonCyan else ZegaWhite
                                )
                                Text(
                                    text = engine.description,
                                    fontSize = 11.sp,
                                    color = ZegaGrayText
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sensitivity Selection
                Text(
                    text = "ACOUSTIC SENSITIVITY",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ZegaGrayText,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WakeWordSensitivity.values().forEach { sens ->
                        val isSelected = selectedSensitivity == sens
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) ZegaNeonViolet.copy(alpha = 0.25f) else CosmicBackground)
                                .border(
                                    1.dp,
                                    if (isSelected) ZegaNeonViolet.copy(alpha = 0.6f) else CosmicCardBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { viewModel.setWakeWordSensitivity(sens) }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = sens.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) ZegaNeonViolet else ZegaWhite
                                )
                                Text(
                                    text = "${(sens.value * 100).toInt()}%",
                                    fontSize = 9.sp,
                                    color = ZegaGrayText
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Haptic Confirmation Settings
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CosmicBackground)
                        .border(
                            1.dp,
                            if (wakeHapticsEnabled) ZegaNeonCyan.copy(alpha = 0.35f) else CosmicCardBorder,
                            RoundedCornerShape(16.dp)
                        )
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Vibration,
                                contentDescription = null,
                                tint = if (wakeHapticsEnabled) ZegaNeonCyan else ZegaGrayText,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Wake-Word Haptic Feedback",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ZegaWhite
                                )
                                Text(
                                    text = "Subtle tactile double-tap confirmation",
                                    fontSize = 10.sp,
                                    color = ZegaGrayText
                                )
                            }
                        }

                        Switch(
                            checked = wakeHapticsEnabled,
                            onCheckedChange = { viewModel.setWakeHapticsEnabled(context, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = ZegaNeonCyan,
                                uncheckedThumbColor = ZegaGrayText,
                                uncheckedTrackColor = CosmicCard
                            ),
                            modifier = Modifier.testTag("wake_haptics_switch")
                        )
                    }

                    if (wakeHapticsEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "HAPTIC TIMBRE & INTENSITY",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = ZegaGrayText,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            HapticIntensity.values().forEach { intensity ->
                                val isSelected = hapticIntensity == intensity
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) ZegaNeonCyan.copy(alpha = 0.2f) else CosmicCard)
                                        .border(
                                            1.dp,
                                            if (isSelected) ZegaNeonCyan else CosmicCardBorder,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            viewModel.setHapticIntensity(context, intensity)
                                            viewModel.triggerWakeWordHaptic(context, force = true)
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = intensity.displayName,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) ZegaNeonCyan else ZegaWhite,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = hapticIntensity.description,
                                fontSize = 10.sp,
                                color = ZegaGrayText,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = { viewModel.triggerWakeWordHaptic(context, force = true) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TouchApp,
                                    contentDescription = null,
                                    tint = ZegaNeonCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "TEST PULSE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ZegaNeonCyan
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Test Trigger Button (Great for testing wake word on emulator or quiet environment)
                OutlinedButton(
                    onClick = {
                        viewModel.simulateWakeWordTrigger(context)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ZegaNeonCyan.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = ZegaNeonCyan.copy(alpha = 0.08f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("test_wakeword_trigger_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = ZegaNeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Simulate 'Hey Z-AI' Wake Trigger",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ZegaNeonCyan
                    )
                }
            }
        }
    }
}

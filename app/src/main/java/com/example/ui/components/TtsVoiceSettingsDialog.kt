package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.voice.TtsVoiceMode
import com.example.data.voice.VoiceProfileInfo
import com.example.ui.theme.CosmicBackground
import com.example.ui.theme.CosmicCard
import com.example.ui.theme.CosmicCardBorder
import com.example.ui.theme.ZegaDeepIndigo
import com.example.ui.theme.ZegaGrayText
import com.example.ui.theme.ZegaLightGrayText
import com.example.ui.theme.ZegaNeonCyan
import com.example.ui.theme.ZegaNeonViolet
import com.example.ui.theme.ZegaPrivacyGreen
import com.example.ui.theme.ZegaWhite

@Composable
fun TtsVoiceSettingsDialog(
    currentMode: TtsVoiceMode,
    pitchMultiplier: Float,
    speedRate: Float,
    availableVoices: List<VoiceProfileInfo>,
    selectedVoiceName: String?,
    onSelectMode: (TtsVoiceMode) -> Unit,
    onPitchChange: (Float) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onSelectSpecificVoice: (String?) -> Unit,
    onTestVoice: () -> Unit,
    onDismiss: () -> Unit
) {
    var showVoicesInspector by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .testTag("tts_settings_dialog"),
            shape = RoundedCornerShape(16.dp),
            color = CosmicCard,
            border = BorderStroke(1.5.dp, ZegaNeonCyan)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(18.dp)
            ) {
                // Header Row
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
                                .size(36.dp)
                                .background(ZegaNeonCyan.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = "TTS Voice Settings",
                                tint = ZegaNeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "TTS VOICE & TIMBRE",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = ZegaWhite,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Acoustic frequency & offline vocal synthesis",
                                fontSize = 10.sp,
                                color = ZegaGrayText
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = ZegaWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(
                    color = CosmicCardBorder.copy(alpha = 0.3f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Voice Mode Selection Grid / List
                    item {
                        Text(
                            text = "SELECT VOICE MODE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ZegaNeonCyan,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            VoiceModeCard(
                                mode = TtsVoiceMode.MALE,
                                isSelected = currentMode == TtsVoiceMode.MALE,
                                icon = Icons.Default.Male,
                                badgeText = "POPULAR / DEEP",
                                onSelect = { onSelectMode(TtsVoiceMode.MALE) }
                            )

                            VoiceModeCard(
                                mode = TtsVoiceMode.FEMALE,
                                isSelected = currentMode == TtsVoiceMode.FEMALE,
                                icon = Icons.Default.Female,
                                badgeText = "MELODIC",
                                onSelect = { onSelectMode(TtsVoiceMode.FEMALE) }
                            )

                            VoiceModeCard(
                                mode = TtsVoiceMode.NATURAL,
                                isSelected = currentMode == TtsVoiceMode.NATURAL,
                                icon = Icons.Default.AutoAwesome,
                                badgeText = "NEURAL AUTO",
                                onSelect = { onSelectMode(TtsVoiceMode.NATURAL) }
                            )

                            VoiceModeCard(
                                mode = TtsVoiceMode.ROBOTIC,
                                isSelected = currentMode == TtsVoiceMode.ROBOTIC,
                                icon = Icons.Default.PrecisionManufacturing,
                                badgeText = "CYBERNETIC",
                                onSelect = { onSelectMode(TtsVoiceMode.ROBOTIC) }
                            )
                        }
                    }

                    // 2. Fine-Tuning Pitch and Speed Sliders
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CosmicBackground),
                            border = BorderStroke(1.dp, CosmicCardBorder.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "ACOUSTIC FINE-TUNING",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ZegaNeonCyan
                                    )
                                    Text(
                                        text = "Base Pitch: ${(currentMode.basePitch * pitchMultiplier * 100).toInt()}%",
                                        fontSize = 10.sp,
                                        color = ZegaGrayText
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Pitch Slider
                                Text(
                                    text = "Pitch / Frequency Multiplier: ${String.format("%.2f", pitchMultiplier)}x",
                                    fontSize = 11.sp,
                                    color = ZegaWhite
                                )
                                Slider(
                                    value = pitchMultiplier,
                                    onValueChange = onPitchChange,
                                    valueRange = 0.6f..1.5f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = ZegaNeonCyan,
                                        activeTrackColor = ZegaNeonCyan,
                                        inactiveTrackColor = ZegaNeonViolet
                                    ),
                                    modifier = Modifier.testTag("pitch_slider")
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    PresetPill(text = "Deep Baritone (0.82x)", isSelected = pitchMultiplier in 0.80f..0.85f) {
                                        onPitchChange(0.82f)
                                    }
                                    PresetPill(text = "Standard (1.0x)", isSelected = pitchMultiplier in 0.98f..1.02f) {
                                        onPitchChange(1.0f)
                                    }
                                    PresetPill(text = "High (1.20x)", isSelected = pitchMultiplier in 1.18f..1.22f) {
                                        onPitchChange(1.20f)
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Speed Rate Slider
                                Text(
                                    text = "Speech Rate Multiplier: ${String.format("%.2f", speedRate)}x",
                                    fontSize = 11.sp,
                                    color = ZegaWhite
                                )
                                Slider(
                                    value = speedRate,
                                    onValueChange = onSpeedChange,
                                    valueRange = 0.6f..1.6f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = ZegaNeonCyan,
                                        activeTrackColor = ZegaNeonCyan,
                                        inactiveTrackColor = ZegaNeonViolet
                                    ),
                                    modifier = Modifier.testTag("speed_slider")
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    PresetPill(text = "0.85x", isSelected = speedRate in 0.83f..0.87f) {
                                        onSpeedChange(0.85f)
                                    }
                                    PresetPill(text = "1.0x (Normal)", isSelected = speedRate in 0.98f..1.02f) {
                                        onSpeedChange(1.0f)
                                    }
                                    PresetPill(text = "1.25x (Fast)", isSelected = speedRate in 1.23f..1.27f) {
                                        onSpeedChange(1.25f)
                                    }
                                }
                            }
                        }
                    }

                    // 3. Detected Voice Profiles Inspector (Optional manual override)
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CosmicBackground),
                            border = BorderStroke(1.dp, CosmicCardBorder.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showVoicesInspector = !showVoicesInspector },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "SYSTEM ENGINE VOICE PACKAGES",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ZegaWhite
                                        )
                                        Text(
                                            text = "${availableVoices.size} local packages detected",
                                            fontSize = 10.sp,
                                            color = ZegaGrayText
                                        )
                                    }
                                    Icon(
                                        imageVector = if (showVoicesInspector) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Expand",
                                        tint = ZegaNeonCyan
                                    )
                                }

                                AnimatedVisibility(visible = showVoicesInspector) {
                                    Column(modifier = Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onSelectSpecificVoice(null) }
                                                .background(
                                                    if (selectedVoiceName == null) ZegaDeepIndigo else Color.Transparent,
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .border(
                                                    BorderStroke(1.dp, if (selectedVoiceName == null) ZegaNeonCyan else CosmicCardBorder.copy(alpha = 0.2f)),
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text("Automatic Mode Selection (Recommended)", fontSize = 11.sp, color = ZegaWhite, fontWeight = FontWeight.Bold)
                                                Text("Optimizes based on the selected mode above", fontSize = 9.sp, color = ZegaGrayText)
                                            }
                                            if (selectedVoiceName == null) {
                                                Icon(Icons.Default.CheckCircle, "Selected", tint = ZegaNeonCyan, modifier = Modifier.size(16.dp))
                                            }
                                        }

                                        availableVoices.take(15).forEach { voice ->
                                            val isCurrent = selectedVoiceName == voice.name
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { onSelectSpecificVoice(voice.name) }
                                                    .background(
                                                        if (isCurrent) ZegaDeepIndigo else Color.Transparent,
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                    .border(
                                                        BorderStroke(1.dp, if (isCurrent) ZegaNeonCyan else CosmicCardBorder.copy(alpha = 0.2f)),
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                    .padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = voice.name,
                                                        fontSize = 10.sp,
                                                        color = ZegaWhite,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Text(voice.localeDisplayName, fontSize = 9.sp, color = ZegaGrayText)
                                                        if (voice.isMale) Text("• Male", fontSize = 9.sp, color = ZegaNeonCyan)
                                                        if (voice.isFemale) Text("• Female", fontSize = 9.sp, color = ZegaNeonCyan)
                                                        if (voice.isHighQuality) Text("• Neural", fontSize = 9.sp, color = ZegaPrivacyGreen)
                                                    }
                                                }
                                                if (isCurrent) {
                                                    Icon(Icons.Default.CheckCircle, "Selected", tint = ZegaNeonCyan, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    color = CosmicCardBorder.copy(alpha = 0.3f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Bottom Action Row: Test Voice Button & Done
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onTestVoice,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ZegaNeonCyan,
                            contentColor = CosmicBackground
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("test_voice_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Test Voice",
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "TEST VOICE OUTPUT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, CosmicCardBorder),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("close_tts_settings_button")
                    ) {
                        Text(
                            text = "DONE",
                            color = ZegaWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceModeCard(
    mode: TtsVoiceMode,
    isSelected: Boolean,
    icon: ImageVector,
    badgeText: String,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .testTag("voice_mode_${mode.name.lowercase()}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) ZegaDeepIndigo else CosmicBackground
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) ZegaNeonCyan else CosmicCardBorder.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            if (isSelected) ZegaNeonCyan.copy(alpha = 0.2f) else CosmicCard,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = mode.title,
                        tint = if (isSelected) ZegaNeonCyan else ZegaWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = mode.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) ZegaNeonCyan else ZegaWhite
                        )
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) ZegaNeonCyan.copy(alpha = 0.15f) else ZegaNeonViolet.copy(alpha = 0.3f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = badgeText,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) ZegaNeonCyan else ZegaGrayText
                            )
                        }
                    }
                    Text(
                        text = mode.description,
                        fontSize = 10.sp,
                        color = ZegaGrayText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Active",
                    tint = ZegaNeonCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun PresetPill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(
                if (isSelected) ZegaNeonCyan.copy(alpha = 0.2f) else CosmicCard,
                RoundedCornerShape(6.dp)
            )
            .border(
                BorderStroke(
                    1.dp,
                    if (isSelected) ZegaNeonCyan else CosmicCardBorder.copy(alpha = 0.3f)
                ),
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) ZegaNeonCyan else ZegaLightGrayText
        )
    }
}

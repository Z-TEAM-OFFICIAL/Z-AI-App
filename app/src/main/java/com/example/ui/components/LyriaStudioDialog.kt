package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.database.AICreation
import com.example.data.voice.LyriaMusicEngine
import com.example.data.voice.MusicGenrePreset
import com.example.data.voice.MusicGenerationState
import com.example.ui.theme.*
import com.example.ui.viewmodel.ZegaViewModel
import kotlinx.coroutines.launch

@Composable
fun LyriaStudioDialog(
    viewModel: ZegaViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val musicState by LyriaMusicEngine.engineState.collectAsState()
    val waveformLevels by LyriaMusicEngine.waveformLevels.collectAsState()
    val creations by viewModel.allCreations.collectAsState(initial = emptyList())
    val musicCreations = creations.filter { it.type == "LYRIA_MUSIC" }

    var isFullTrack by remember { mutableStateOf(false) } // false = lyria-3-clip-preview (up to 30s), true = lyria-3-pro-preview
    var selectedGenre by remember { mutableStateOf(LyriaMusicEngine.genrePresets.first()) }
    var customPrompt by remember { mutableStateOf(selectedGenre.promptTemplate) }

    Dialog(
        onDismissRequest = {
            LyriaMusicEngine.stopAudio()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(28.dp))
                .background(CosmicCard)
                .border(1.dp, CosmicCardBorder, RoundedCornerShape(28.dp))
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
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
                                    Brush.linearGradient(
                                        listOf(ZegaNeonCyan, Color(0xFFFFB703))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = "Lyria Music",
                                tint = Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Lyria 3 Music Studio",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ZegaWhite
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(ZegaNeonCyan.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isFullTrack) "lyria-3-pro" else "lyria-3-clip",
                                        fontSize = 9.sp,
                                        color = ZegaNeonCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "Generate audio tracks and multi-instrumental beats",
                                fontSize = 11.sp,
                                color = ZegaGrayText
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            LyriaMusicEngine.stopAudio()
                            onDismiss()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = ZegaWhite)
                    }
                }

                HorizontalDivider(
                    color = CosmicCardBorder,
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // 1. Model & Length Selector
                Text(
                    text = "GENERATION LENGTH & MODEL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ZegaGrayText,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Short Clip (lyria-3-clip-preview)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (!isFullTrack) ZegaNeonCyan.copy(alpha = 0.2f) else CosmicBackground)
                            .border(1.dp, if (!isFullTrack) ZegaNeonCyan else CosmicCardBorder, RoundedCornerShape(12.dp))
                            .clickable { isFullTrack = false }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Short Clip (30s)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!isFullTrack) ZegaNeonCyan else ZegaWhite
                            )
                            Text(
                                text = "lyria-3-clip-preview",
                                fontSize = 9.sp,
                                color = ZegaGrayText
                            )
                        }
                    }

                    // Full Track (lyria-3-pro-preview)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isFullTrack) ZegaNeonCyan.copy(alpha = 0.2f) else CosmicBackground)
                            .border(1.dp, if (isFullTrack) ZegaNeonCyan else CosmicCardBorder, RoundedCornerShape(12.dp))
                            .clickable { isFullTrack = true }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Full Track (60s+)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isFullTrack) ZegaNeonCyan else ZegaWhite
                            )
                            Text(
                                text = "lyria-3-pro-preview",
                                fontSize = 9.sp,
                                color = ZegaGrayText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Genre Presets
                Text(
                    text = "GENRE & MOOD PRESETS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ZegaGrayText,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(LyriaMusicEngine.genrePresets) { genre ->
                        val isSelected = selectedGenre.id == genre.id
                        Card(
                            onClick = {
                                selectedGenre = genre
                                customPrompt = genre.promptTemplate
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) ZegaNeonCyan.copy(alpha = 0.15f) else CosmicBackground
                            ),
                            border = BorderStroke(1.dp, if (isSelected) ZegaNeonCyan else CosmicCardBorder),
                            modifier = Modifier.width(155.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(genre.iconEmoji, fontSize = 16.sp)
                                    Text(
                                        text = genre.title,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) ZegaNeonCyan else ZegaWhite,
                                        maxLines = 1
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${genre.baseBpm} BPM • ${genre.description}",
                                    fontSize = 9.sp,
                                    color = ZegaGrayText,
                                    maxLines = 2,
                                    lineHeight = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Prompt Input
                Text(
                    text = "MUSIC DESCRIPTION PROMPT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ZegaGrayText,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = customPrompt,
                    onValueChange = { customPrompt = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lyria_prompt_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ZegaNeonCyan,
                        unfocusedBorderColor = CosmicCardBorder,
                        focusedTextColor = ZegaWhite,
                        unfocusedTextColor = ZegaWhite,
                        cursorColor = ZegaNeonCyan
                    ),
                    maxLines = 3,
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Waveform Audio Visualizer Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = CosmicBackground),
                    border = BorderStroke(1.dp, CosmicCardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "ACOUSTIC WAVEFORM MONITOR",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = ZegaGrayText,
                                letterSpacing = 1.sp
                            )
                            if (musicState is MusicGenerationState.Playing) {
                                val playing = musicState as MusicGenerationState.Playing
                                Text(
                                    text = "${playing.currentPositionSec.toInt()}s / ${playing.durationSec.toInt()}s",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ZegaNeonCyan
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Waveform Bars
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            waveformLevels.forEach { level ->
                                Box(
                                    modifier = Modifier
                                        .width(6.dp)
                                        .fillMaxHeight(level.coerceIn(0.15f, 1.0f))
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(ZegaNeonCyan, ZegaDeepIndigo)
                                            )
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Player Control row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            when (val state = musicState) {
                                is MusicGenerationState.Playing -> {
                                    IconButton(
                                        onClick = { LyriaMusicEngine.stopAudio() },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(ZegaNeonCyan)
                                    ) {
                                        Icon(Icons.Default.Pause, contentDescription = "Pause", tint = Color.Black)
                                    }
                                }
                                is MusicGenerationState.Completed -> {
                                    IconButton(
                                        onClick = {
                                            LyriaMusicEngine.playTrack(state.track, coroutineScope)
                                        },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(ZegaNeonCyan)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.Black)
                                    }
                                }
                                else -> {
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                val creation = LyriaMusicEngine.generateMusic(
                                                    context = context,
                                                    prompt = customPrompt,
                                                    genre = selectedGenre,
                                                    isFullTrack = isFullTrack,
                                                    coroutineScope = coroutineScope
                                                )
                                                viewModel.saveAICreation(creation)
                                                LyriaMusicEngine.playTrack(creation, coroutineScope)
                                            }
                                        },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(ZegaNeonCyan)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.Black)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Status or Generation Progress
                when (val state = musicState) {
                    is MusicGenerationState.Generating -> {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = CosmicBackground),
                            border = BorderStroke(1.dp, ZegaNeonCyan.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = state.status,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ZegaNeonCyan
                                    )
                                    Text(
                                        text = "${(state.progress * 100).toInt()}%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ZegaWhite
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { state.progress },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = ZegaNeonCyan,
                                    trackColor = CosmicCardBorder
                                )
                            }
                        }
                    }
                    else -> {}
                }

                // Generate Button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val creation = LyriaMusicEngine.generateMusic(
                                    context = context,
                                    prompt = customPrompt,
                                    genre = selectedGenre,
                                    isFullTrack = isFullTrack,
                                    coroutineScope = coroutineScope
                                )
                                viewModel.saveAICreation(creation)
                                LyriaMusicEngine.playTrack(creation, coroutineScope)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    },
                    enabled = musicState !is MusicGenerationState.Generating,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ZegaNeonCyan,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("generate_lyria_music_button")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (musicState is MusicGenerationState.Generating) "Generating Track..." else "Generate Track with Lyria 3",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Saved Music Library
                if (musicCreations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "SAVED MUSIC LIBRARY (${musicCreations.size})",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ZegaGrayText,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    musicCreations.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(CosmicBackground)
                                .border(1.dp, CosmicCardBorder, RoundedCornerShape(10.dp))
                                .padding(10.dp)
                                .clickable {
                                    LyriaMusicEngine.playTrack(item, coroutineScope)
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(ZegaNeonCyan.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play Track",
                                        tint = ZegaNeonCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = item.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ZegaWhite
                                    )
                                    Text(
                                        text = "${item.modelName} • ${item.durationSeconds}s",
                                        fontSize = 10.sp,
                                        color = ZegaGrayText
                                    )
                                }
                            }

                            IconButton(
                                onClick = { viewModel.deleteCreationById(item.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ZegaWarningRed, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.database.AICreation
import com.example.data.voice.VeoAnimationPreset
import com.example.data.voice.VeoGenerationState
import com.example.data.voice.VeoVideoEngine
import com.example.ui.theme.*
import com.example.ui.viewmodel.ZegaViewModel
import kotlinx.coroutines.launch

@Composable
fun VeoStudioDialog(
    viewModel: ZegaViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val videoState by VeoVideoEngine.videoState.collectAsState()
    val creations by viewModel.allCreations.collectAsState(initial = emptyList())
    val videoCreations = creations.filter { it.type == "VEO_VIDEO" }

    var selectedAspectRatio by remember { mutableStateOf("16:9") }
    var selectedPreset by remember { mutableStateOf(VeoVideoEngine.animationPresets.first()) }
    var customPrompt by remember { mutableStateOf(selectedPreset.prompt) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isPlayingPreview by remember { mutableStateOf(true) }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    selectedBitmap = BitmapFactory.decodeStream(stream)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Infinite breathing/zoom animation for simulated Veo preview
    val infiniteTransition = rememberInfiniteTransition(label = "VeoVideoPreview")
    val zoomScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ZoomAnim"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAnim"
    )

    Dialog(
        onDismissRequest = {
            VeoVideoEngine.resetState()
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
                                        listOf(ZegaNeonCyan, ZegaNeonViolet)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = "Veo Video",
                                tint = Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Veo 3.1 Video Studio",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ZegaWhite
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(ZegaNeonViolet.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("veo-3.1-fast", fontSize = 9.sp, color = ZegaNeonViolet, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                text = "Animate photos into cinematic video generations",
                                fontSize = 11.sp,
                                color = ZegaGrayText
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            VeoVideoEngine.resetState()
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

                // 1. Aspect Ratio Selector (16:9 vs 9:16 mandatory)
                Text(
                    text = "TARGET ASPECT RATIO",
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
                    listOf("16:9" to "Landscape (16:9)", "9:16" to "Portrait (9:16)").forEach { (ratio, label) ->
                        val isSelected = selectedAspectRatio == ratio
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) ZegaNeonCyan.copy(alpha = 0.2f) else CosmicBackground)
                                .border(
                                    1.dp,
                                    if (isSelected) ZegaNeonCyan else CosmicCardBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedAspectRatio = ratio }
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (ratio == "16:9") Icons.Default.AspectRatio else Icons.Default.StayCurrentPortrait,
                                    contentDescription = null,
                                    tint = if (isSelected) ZegaNeonCyan else ZegaGrayText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) ZegaNeonCyan else ZegaWhite
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Photo Upload / Source Selection
                Text(
                    text = "INPUT PHOTO OR SOURCE FRAME",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ZegaGrayText,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (selectedAspectRatio == "16:9") 160.dp else 220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CosmicBackground)
                        .border(1.dp, if (selectedBitmap != null) ZegaNeonCyan.copy(alpha = 0.5f) else CosmicCardBorder, RoundedCornerShape(16.dp))
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedBitmap != null) {
                        Image(
                            bitmap = selectedBitmap!!.asImageBitmap(),
                            contentDescription = "Selected Photo",
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(if (isPlayingPreview) zoomScale else 1.0f)
                                .clip(RoundedCornerShape(16.dp))
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.7f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Change Photo", fontSize = 10.sp, color = ZegaNeonCyan, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Upload photo",
                                tint = ZegaNeonCyan,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Tap to upload or pick photo to animate",
                                fontSize = 12.sp,
                                color = ZegaWhite,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Supports JPG, PNG • Aspect ratio will scale to $selectedAspectRatio",
                                fontSize = 10.sp,
                                color = ZegaGrayText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Animation Presets
                Text(
                    text = "VEO MOTION STYLES",
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
                    items(VeoVideoEngine.animationPresets) { preset ->
                        val isSelected = selectedPreset.id == preset.id
                        Card(
                            onClick = {
                                selectedPreset = preset
                                customPrompt = preset.prompt
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) ZegaNeonCyan.copy(alpha = 0.15f) else CosmicBackground
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) ZegaNeonCyan else CosmicCardBorder
                            ),
                            modifier = Modifier.width(160.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(preset.iconEmoji, fontSize = 16.sp)
                                    Text(
                                        text = preset.title,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) ZegaNeonCyan else ZegaWhite,
                                        maxLines = 1
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = preset.description,
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

                // 4. Custom Motion Prompt
                Text(
                    text = "MOTION PROMPT",
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
                        .testTag("veo_prompt_input"),
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

                // Status or Generation Progress
                when (val state = videoState) {
                    is VeoGenerationState.Generating -> {
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
                    is VeoGenerationState.Completed -> {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = ZegaPrivacyGreen.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, ZegaPrivacyGreen),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ZegaPrivacyGreen)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Video Animation Ready!", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ZegaWhite)
                                    Text("Generated with veo-3.1-fast • Synced to Cloud & Local", fontSize = 10.sp, color = ZegaGrayText)
                                }
                            }
                        }
                    }
                    is VeoGenerationState.Error -> {
                        Text(
                            text = "Error: ${state.message}",
                            fontSize = 11.sp,
                            color = ZegaWarningRed,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    else -> {}
                }

                // Generate Button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val creation = VeoVideoEngine.generateVideo(
                                    context = context,
                                    prompt = customPrompt,
                                    sourceBitmap = selectedBitmap,
                                    aspectRatio = selectedAspectRatio,
                                    presetTitle = selectedPreset.title
                                )
                                viewModel.saveAICreation(creation)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    },
                    enabled = videoState !is VeoGenerationState.Generating,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ZegaNeonCyan,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("generate_veo_video_button")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (videoState is VeoGenerationState.Generating) "Generating Video..." else "Generate Video with Veo 3.1",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Gallery of previous generations
                if (videoCreations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "SAVED VEO CREATIONS (${videoCreations.size})",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ZegaGrayText,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(videoCreations) { item ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = CosmicBackground),
                                border = BorderStroke(1.dp, CosmicCardBorder),
                                modifier = Modifier.width(130.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(70.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(CosmicCard),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayCircleFilled,
                                            contentDescription = "Play",
                                            tint = ZegaNeonCyan,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.title,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ZegaWhite,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${item.aspectRatio} • ${item.durationSeconds}s",
                                        fontSize = 9.sp,
                                        color = ZegaGrayText
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

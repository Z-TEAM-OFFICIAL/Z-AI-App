package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.database.ChatMessage
import com.example.data.voice.TtsVoiceMode
import com.example.ui.components.TtsVoiceSettingsDialog
import com.example.ui.components.VeoStudioDialog
import com.example.ui.components.LyriaStudioDialog
import com.example.ui.components.VoiceWaveformVisualizer
import com.example.ui.components.VfsExplorerDialog
import com.example.ui.components.AiSearchToolDialog
import com.example.ui.viewmodel.PendingToolPermission
import com.example.ui.theme.*
import com.example.ui.viewmodel.AssistantState
import com.example.ui.viewmodel.ZegaViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ZegaHomeScreen(
    viewModel: ZegaViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Collect Viewmodel States
    val assistantState by viewModel.assistantState.collectAsStateWithLifecycle()
    val wakeWordEnabled by viewModel.wakeWordEnabled.collectAsStateWithLifecycle()
    val partialSpeechInput by viewModel.partialSpeechInput.collectAsStateWithLifecycle()
    val amplitudeRms by viewModel.amplitudeRms.collectAsStateWithLifecycle()
    val timerRemaining by viewModel.timerSecondsRemaining.collectAsStateWithLifecycle()
    val timerTotal by viewModel.timerTotalSeconds.collectAsStateWithLifecycle()
    val timerActive by viewModel.timerActive.collectAsStateWithLifecycle()
    val batteryLevel by viewModel.batteryLevel.collectAsStateWithLifecycle()
    val flashlightEnabled by viewModel.flashlightEnabled.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val localFiles by viewModel.localFiles.collectAsStateWithLifecycle()
    val canvasRequestTrigger by viewModel.canvasRequestTrigger.collectAsStateWithLifecycle()
    val lastWrittenFileName by viewModel.lastWrittenFileName.collectAsStateWithLifecycle()
    val requestSystemSpeech by viewModel.requestSystemSpeech.collectAsStateWithLifecycle()
    val wakeWordDetected by viewModel.wakeWordDetectedTrigger.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val ttsVoiceMode by viewModel.ttsVoiceMode.collectAsStateWithLifecycle()
    val ttsPitchMultiplier by viewModel.ttsPitchMultiplier.collectAsStateWithLifecycle()
    val ttsSpeechRate by viewModel.ttsSpeechRate.collectAsStateWithLifecycle()
    val availableVoices by viewModel.availableVoices.collectAsStateWithLifecycle()
    val selectedVoiceName by viewModel.selectedVoiceName.collectAsStateWithLifecycle()

    val pendingPermission by viewModel.pendingToolPermission.collectAsStateWithLifecycle()
    val browserUrl by viewModel.inAppBrowserUrl.collectAsStateWithLifecycle()
    val vfsOpen by viewModel.vfsDialogOpen.collectAsStateWithLifecycle()
    val isAiSearchOpen by viewModel.isAiSearchDialogOpen.collectAsStateWithLifecycle()
    val currentModelTier by viewModel.currentModelTier.collectAsStateWithLifecycle()
    val ecoModeActive by viewModel.ecoModeActive.collectAsStateWithLifecycle()

    var showAuthDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showWakeWordConfigDialog by remember { mutableStateOf(false) }
    var showTtsSettingsDialog by remember { mutableStateOf(false) }
    var showVeoStudio by remember { mutableStateOf(false) }
    var showLyriaStudio by remember { mutableStateOf(false) }
    val searchGroundingEnabled by viewModel.searchGroundingEnabled.collectAsStateWithLifecycle()

    // Infinite pulse animation for wake word feedback
    val wakeWordPulseTransition = rememberInfiniteTransition(label = "WakeWordPulse")
    val wakePulseProgress by if (wakeWordDetected) {
        wakeWordPulseTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "WakePulse"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    var keyboardInputText by remember { mutableStateOf("") }
    var showNotesPanel by remember { mutableStateOf(false) }
    var currentTab by remember { mutableStateOf(0) }
    var webUrlInput by remember { mutableStateOf("https://www.google.com") }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var manualFileName by remember { mutableStateOf("") }
    var manualFileContent by remember { mutableStateOf("") }
    var selectedFileNameForView by remember { mutableStateOf<String?>(null) }
    var fileViewContent by remember { mutableStateOf("") }
    var isPreviewMode by remember { mutableStateOf(false) }


    // Initialize TTS and Managers on screen enter
    LaunchedEffect(Unit) {
        viewModel.initManagers(context)
    }

    LaunchedEffect(showNotesPanel) {
        if (showNotesPanel) {
            viewModel.refreshLocalFiles(context)
        }
    }

    // Auto-scroll chat list when a new message is appended
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(canvasRequestTrigger) {
        val trigger = canvasRequestTrigger
        if (trigger != null) {
            currentTab = 1
            showNotesPanel = true
            val fileToOpen = lastWrittenFileName ?: localFiles.find {
                it.endsWith(".html") || it.endsWith(".js") || it.endsWith(".ts")
            }
            if (fileToOpen != null) {
                selectedFileNameForView = fileToOpen
                fileViewContent = viewModel.readLocalFile(context, fileToOpen)
                isPreviewMode = true
            }
            viewModel.consumeCanvasRequest()
        }
    }

    // Permission launcher for Microphone Recording
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasMicPermission = granted
            if (granted) {
                viewModel.onMicButtonClicked(context)
            }
        }
    )

    // Fallback System Voice Dialog Launcher (works 100% on all Android devices & emulators)
    val systemSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
                if (!spokenText.isNullOrEmpty()) {
                    viewModel.processUserSpeech(spokenText, context)
                }
            }
        }
    )

    // Trigger standard system Google voice popup as a reliable fallback
    LaunchedEffect(requestSystemSpeech) {
        if (requestSystemSpeech) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Z-AI Assistant...")
            }
            try {
                systemSpeechLauncher.launch(intent)
            } catch (e: Exception) {
                android.util.Log.e("ZegaHomeScreen", "Failed to launch system voice overlay: ${e.message}")
            }
            viewModel.consumeSystemSpeechRequest()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(CosmicBackground),
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Z-AI Assistant",
                                fontSize = 26.sp,
                                fontFamily = FontFamily.Serif,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Bold,
                                color = ZegaNeonCyan,
                                modifier = Modifier.testTag("app_title")
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(ZegaNeonViolet.copy(alpha = 0.25f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "OFFLINE CORE",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ZegaNeonCyan,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                        Text(
                            text = "PRIVACY-FIRST INTELLIGENCE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = ZegaGrayText,
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                },
                actions = {
                    // Continuous Wake-Word Detection button (Click toggles, Long click or button configures)
                    IconButton(
                        onClick = {
                            if (hasMicPermission) {
                                viewModel.toggleWakeWord(context)
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier.testTag("toggle_wakeword_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (wakeWordEnabled) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(ZegaPrivacyGreen)
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (wakeWordEnabled) Icons.Default.Hearing else Icons.Default.HearingDisabled,
                                contentDescription = "Toggle Wake Word",
                                tint = if (wakeWordEnabled) ZegaPrivacyGreen else ZegaGrayText
                            )
                        }
                    }

                    // Power Saver Eco-Mode Toggle
                    IconButton(
                        onClick = { viewModel.toggleEcoMode(context) },
                        modifier = Modifier.testTag("toggle_eco_mode_button")
                    ) {
                        BadgedBox(
                            badge = {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (ecoModeActive) Color(0xFFFFB703) else ZegaPrivacyGreen)
                                        .padding(horizontal = 3.dp, vertical = 0.5.dp)
                                ) {
                                    Text(
                                        text = if (ecoModeActive) "ECO" else "MAX",
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.Black
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (ecoModeActive) Icons.Default.BatterySaver else Icons.Default.ElectricBolt,
                                contentDescription = "Toggle Power Saver Eco Mode",
                                tint = if (ecoModeActive) Color(0xFFFFB703) else ZegaPrivacyGreen
                            )
                        }
                    }

                    // VFS Virtual File System Explorer button
                    IconButton(
                        onClick = { viewModel.setVfsDialogOpen(true) },
                        modifier = Modifier.testTag("open_vfs_explorer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderSpecial,
                            contentDescription = "Virtual File System Explorer",
                            tint = ZegaNeonCyan
                        )
                    }

                    // TTS Voice Mode & Acoustic Config
                    IconButton(
                        onClick = {
                            viewModel.refreshAvailableVoices()
                            showTtsSettingsDialog = true
                        },
                        modifier = Modifier.testTag("tts_voice_config_button")
                    ) {
                        BadgedBox(
                            badge = {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(ZegaNeonCyan.copy(alpha = 0.25f))
                                        .padding(horizontal = 3.dp, vertical = 0.5.dp)
                                ) {
                                    Text(
                                        text = if (ttsVoiceMode == TtsVoiceMode.MALE) "MALE" else if (ttsVoiceMode == TtsVoiceMode.FEMALE) "FEMALE" else "AUTO",
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ZegaNeonCyan
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = "TTS Voice Mode Settings",
                                tint = ZegaNeonCyan
                            )
                        }
                    }

                    // AI Web Search Interactive Tool button
                    IconButton(
                        onClick = { viewModel.setAiSearchDialogOpen(true) },
                        modifier = Modifier.testTag("open_ai_search_tool_button")
                    ) {
                        BadgedBox(
                            badge = {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(ZegaNeonCyan.copy(alpha = 0.25f))
                                        .padding(horizontal = 3.dp, vertical = 0.5.dp)
                                ) {
                                    Text(
                                        text = "SEARCH",
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ZegaNeonCyan
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "AI Web Search Tool",
                                tint = ZegaNeonCyan
                            )
                        }
                    }

                    // Google Search Grounding Toggle
                    IconButton(
                        onClick = { viewModel.toggleSearchGrounding() },
                        modifier = Modifier.testTag("toggle_search_grounding_button")
                    ) {
                        BadgedBox(
                            badge = {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (searchGroundingEnabled) ZegaPrivacyGreen else ZegaGrayText)
                                        .padding(horizontal = 3.dp, vertical = 0.5.dp)
                                ) {
                                    Text(
                                        text = if (searchGroundingEnabled) "ON" else "OFF",
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.Black
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.TravelExplore,
                                contentDescription = "Toggle Google Search Grounding",
                                tint = if (searchGroundingEnabled) ZegaPrivacyGreen else ZegaGrayText
                            )
                        }
                    }

                    // Veo 3.1 Video Studio button
                    IconButton(
                        onClick = { showVeoStudio = true },
                        modifier = Modifier.testTag("open_veo_studio_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = "Veo 3.1 Video Studio",
                            tint = ZegaNeonCyan
                        )
                    }

                    // Lyria 3 Music Studio button
                    IconButton(
                        onClick = { showLyriaStudio = true },
                        modifier = Modifier.testTag("open_lyria_studio_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Lyria 3 Music Studio",
                            tint = Color(0xFFFFB703)
                        )
                    }

                    // Wake-Word Settings & Acoustic Engine Config
                    IconButton(
                        onClick = { showWakeWordConfigDialog = true },
                        modifier = Modifier.testTag("wakeword_config_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Wake Word Engine Settings",
                            tint = ZegaNeonCyan
                        )
                    }

                    // View Saved Voice Notes DB button
                    IconButton(
                        onClick = { showNotesPanel = !showNotesPanel },
                        modifier = Modifier.testTag("notes_panel_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (notes.isNotEmpty()) {
                                    Badge(
                                        containerColor = ZegaNeonViolet,
                                        contentColor = Color.White
                                    ) {
                                        Text(notes.size.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Note,
                                contentDescription = "Saved Notes",
                                tint = if (showNotesPanel) ZegaNeonViolet else ZegaGrayText
                            )
                        }
                    }

                    // User Account Profile / Sign In
                    IconButton(
                        onClick = {
                            if (currentUser != null) {
                                showProfileDialog = true
                            } else {
                                showAuthDialog = true
                            }
                        },
                        modifier = Modifier.testTag("user_account_button")
                    ) {
                        if (currentUser != null) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(listOf(ZegaNeonCyan, ZegaNeonViolet))
                                    )
                                    .padding(1.5.dp)
                                    .clip(CircleShape)
                                    .background(CosmicBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentUser!!.displayName.take(1).uppercase(Locale.getDefault()),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ZegaNeonCyan
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Sign In / Account",
                                tint = ZegaNeonCyan
                            )
                        }
                    }

                    // Clear Chat logs
                    IconButton(
                        onClick = { viewModel.clearChatHistory() },
                        modifier = Modifier.testTag("clear_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear Chat Log",
                            tint = ZegaGrayText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CosmicBackground,
                    titleContentColor = ZegaWhite
                )
            )
        },
        bottomBar = {
            // Screen safe area bottom padding with Keyboard / Mic trigger interface
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .fillMaxWidth()
                    .background(CosmicBackground)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Interactive manual typing bar for quiet environments or emulator simulation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(CosmicCard)
                        .border(1.dp, CosmicCardBorder, RoundedCornerShape(24.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.setAiSearchDialogOpen(true) },
                        modifier = Modifier.testTag("quick_search_tool_icon_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Open AI Web Search",
                            tint = ZegaNeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    TextField(
                        value = keyboardInputText,
                        onValueChange = { keyboardInputText = it },
                        placeholder = {
                            Text(
                                text = "Ask Z-AI anything offline or search...",
                                color = ZegaGrayText,
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("keyboard_text_input"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = ZegaWhite,
                            unfocusedTextColor = ZegaWhite
                        ),
                        singleLine = true
                    )

                    IconButton(
                        onClick = {
                            if (keyboardInputText.isNotBlank()) {
                                keyboardController?.hide()
                                viewModel.processUserSpeech(keyboardInputText, context)
                                keyboardInputText = ""
                            }
                        },
                        enabled = keyboardInputText.isNotBlank(),
                        modifier = Modifier.testTag("send_text_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send offline query",
                            tint = if (keyboardInputText.isNotBlank()) ZegaNeonCyan else ZegaGrayText
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CosmicBackground)
                .padding(innerPadding)
        ) {
            // Primary Body layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // 1. Privacy Guard visual trust banner
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 12.dp)
                        .testTag("privacy_banner"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CosmicCard),
                    border = BorderStroke(1.dp, ZegaPrivacyGreen.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ZegaPrivacyGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Secure Shield",
                                tint = ZegaPrivacyGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "100% PRIVATE & OFFLINE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ZegaPrivacyGreen,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Speech parsed locally. 0 bytes sent to servers.",
                                fontSize = 12.sp,
                                color = ZegaGrayText
                            )
                        }

                        // Passive state listening indicator
                        if (assistantState == AssistantState.WAKE_WORD_WAIT) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ZegaPrivacyGreen.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(ZegaPrivacyGreen)
                                    )
                                    Text(
                                        text = "HEARING",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ZegaPrivacyGreen
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Chat history / Empty-state welcome instructions
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (messages.isEmpty()) {
                        // Empty State: Welcome panel with clickable local simulation prompts
                        val emptyStateScrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(emptyStateScrollState)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "“Hey Z-AI, what's my\nschedule look like today?”",
                                fontSize = 22.sp,
                                fontFamily = FontFamily.Serif,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Light,
                                color = ZegaWhite,
                                textAlign = TextAlign.Center,
                                lineHeight = 28.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            // Inline flex badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(CosmicCard.copy(alpha = 0.5f))
                                    .border(BorderStroke(1.dp, CosmicCardBorder.copy(alpha = 0.5f)), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(ZegaPrivacyGreen)
                                )
                                Text(
                                    text = "OFFLINE CORE ACTIVE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ZegaLightGrayText,
                                    letterSpacing = 1.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Recent Conversation simulated banner / info block
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = CosmicCard),
                                border = BorderStroke(1.dp, CosmicCardBorder)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "RECENT CONVERSATION",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ZegaGrayText,
                                        letterSpacing = 1.sp,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                    Text(
                                        text = "\"Your local files show a briefing at 2 PM. Processing summary on-device now...\"",
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Serif,
                                        fontStyle = FontStyle.Italic,
                                        color = ZegaLightGrayText,
                                        lineHeight = 18.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "Tap any suggestion to speak with me:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ZegaNeonCyan,
                                modifier = Modifier.padding(bottom = 12.dp),
                                letterSpacing = 0.5.sp
                            )

                            // Quick click prompt chips for seamless on-device offline exploration
                            val chips = listOf(
                                "What time is it?",
                                "Take a note: Buy apples",
                                "Set a timer for 10 seconds",
                                "Tell me a funny joke",
                                "What is 25 times 4?",
                                "Turn on flashlight"
                            )

                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                              ) {
                                chips.chunked(2).forEach { rowChips ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        rowChips.forEach { chipText ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(CosmicCard)
                                                    .border(1.dp, CosmicCardBorder, RoundedCornerShape(16.dp))
                                                    .clickable {
                                                        viewModel.processUserSpeech(chipText, context)
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = chipText,
                                                    fontSize = 12.sp,
                                                    color = ZegaLightGrayText,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Scrolling chat log
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 200.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(messages) { message ->
                                ChatMessageRow(message = message)
                            }
                        }
                    }
                }
            }

            // 3. Floating Interactive Timer overlay (Floating Circle)
            if (timerActive) {
                Box(
                    modifier = Modifier
                        .padding(20.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CosmicCard),
                        border = BorderStroke(1.dp, CosmicCardBorder),
                        modifier = Modifier
                            .shadow(12.dp, RoundedCornerShape(20.dp))
                            .testTag("timer_overlay_card")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = {
                                        if (timerTotal > 0) timerRemaining.toFloat() / timerTotal.toFloat() else 0f
                                    },
                                    modifier = Modifier.size(28.dp),
                                    color = ZegaNeonCyan,
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = timerRemaining.toString(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ZegaWhite
                                )
                            }
                            Column {
                                Text("TIMER", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ZegaNeonCyan)
                                Text("Offline Alarm", fontSize = 10.sp, color = ZegaGrayText)
                            }
                            IconButton(
                                onClick = { viewModel.stopLocalTimer() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Stop timer",
                                    tint = ZegaWarningRed,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Glowing Siri Orb Container (Overlay on top of chat list, positioned at bottom center)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .offset(y = 30.dp) // Floating organic feeling overlapping elements
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, CosmicBackground.copy(alpha = 0.95f), CosmicBackground),
                            startY = 0f
                        )
                    )
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Real-time transcribed text being spoken/heard
                AnimatedVisibility(
                    visible = (partialSpeechInput.isNotEmpty() || assistantState != AssistantState.IDLE) && !wakeWordDetected,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 24.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CosmicCard.copy(alpha = 0.85f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (partialSpeechInput.isNotEmpty()) partialSpeechInput else when (assistantState) {
                                AssistantState.LISTENING -> "Listening... Speak now"
                                AssistantState.PROCESSING -> "Thinking entirely locally..."
                                AssistantState.SPEAKING -> "Speaking Response..."
                                AssistantState.WAKE_WORD_WAIT -> "Waiting for 'Hey Z-AI'..."
                                else -> ""
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (partialSpeechInput.startsWith("Error:") || partialSpeechInput.contains("didn't catch")) ZegaWarningRed else ZegaNeonCyan,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("speech_transcript_text")
                        )
                    }
                }

                // Elegant high-intensity wake word detection banner
                AnimatedVisibility(
                    visible = wakeWordDetected,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 24.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(CosmicCard)
                            .border(1.5.dp, ZegaNeonCyan, RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Wake Word Detected",
                            tint = ZegaNeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "✨ HEY Z-AI DETECTED ✨",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ZegaNeonCyan,
                            letterSpacing = 1.2.sp
                        )
                    }
                }

                // Dynamic Animated Waveform Visualizer reacting in real-time to microphone input volume
                VoiceWaveformVisualizer(
                    assistantState = assistantState,
                    amplitudeRms = amplitudeRms,
                    isEcoMode = ecoModeActive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 2.dp)
                )

                // Interactive Glowing Canvas Orb
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .testTag("glowing_orb_mic_container")
                        .combinedClickable(
                            onClick = {
                                if (hasMicPermission) {
                                    viewModel.onMicButtonClicked(context)
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            onLongClick = {
                                // Simulate Voice Input command for emulators with no microphone
                                viewModel.processUserSpeech("What time is it?", context)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (wakeWordDetected) {
                        // Multi-stage high-intensity energy ripples
                        // Ripple 1
                        val size1 = 120.dp + (140.dp * wakePulseProgress)
                        val alpha1 = 0.8f * (1f - wakePulseProgress)
                        Box(
                            modifier = Modifier
                                .size(size1)
                                .clip(CircleShape)
                                .background(ZegaNeonCyan.copy(alpha = alpha1))
                        )
                        
                        // Ripple 2 (Staggered by 50%)
                        val progress2 = (wakePulseProgress + 0.5f) % 1f
                        val size2 = 120.dp + (140.dp * progress2)
                        val alpha2 = 0.8f * (1f - progress2)
                        Box(
                            modifier = Modifier
                                .size(size2)
                                .clip(CircleShape)
                                .background(ZegaNeonViolet.copy(alpha = alpha2))
                        )
                    }

                    ZegaGlowingOrb(
                        state = assistantState,
                        rmsdB = amplitudeRms
                    )

                    // Standard microphone floating symbol in middle
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                when (assistantState) {
                                    AssistantState.LISTENING -> ZegaNeonCyan.copy(alpha = 0.3f)
                                    AssistantState.PROCESSING -> ZegaNeonViolet.copy(alpha = 0.3f)
                                    AssistantState.SPEAKING -> ZegaPurpleAccent.copy(alpha = 0.3f)
                                    else -> CosmicCard
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (assistantState) {
                                AssistantState.LISTENING -> Icons.Default.Mic
                                AssistantState.PROCESSING -> Icons.Default.Sync
                                AssistantState.SPEAKING -> Icons.Default.VolumeUp
                                else -> Icons.Default.Mic
                            },
                            contentDescription = "Tap to speak with Z-AI Assistant",
                            tint = when (assistantState) {
                                AssistantState.LISTENING -> ZegaNeonCyan
                                AssistantState.PROCESSING -> ZegaNeonViolet
                                AssistantState.SPEAKING -> ZegaPurpleAccent
                                else -> ZegaWhite
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Interaction Hint Subtext
                Text(
                    text = when (assistantState) {
                        AssistantState.LISTENING -> "Speaking... Tap to finish"
                        AssistantState.PROCESSING -> "Processing offline query"
                        AssistantState.SPEAKING -> "Tap orb to mute voice"
                        AssistantState.WAKE_WORD_WAIT -> "Waiting... or say 'Hey Z-AI'"
                        AssistantState.IDLE -> "Tap Orb to Speak (Long-tap to simulate)"
                    },
                    fontSize = 11.sp,
                    color = ZegaGrayText,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // 5. Expandable Local Voice Notes, Web Browser & System Tools Deck
            if (showNotesPanel) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(490.dp)
                        .align(Alignment.BottomCenter)
                        .shadow(24.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .testTag("notes_drawer_panel"),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = CosmicCard),
                    border = BorderStroke(1.dp, CosmicCardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                    ) {
                        // Drawer Tab Selector Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { currentTab = 0 },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = if (currentTab == 0) ZegaNeonCyan else ZegaGrayText
                                    ),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "NOTES (${notes.size})",
                                        fontSize = 11.sp,
                                        fontWeight = if (currentTab == 0) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                                TextButton(
                                    onClick = { currentTab = 1 },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = if (currentTab == 1) ZegaNeonCyan else ZegaGrayText
                                    ),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "FILES (${localFiles.size})",
                                        fontSize = 11.sp,
                                        fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                                TextButton(
                                    onClick = { currentTab = 2 },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = if (currentTab == 2) ZegaNeonCyan else ZegaGrayText
                                    ),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "WEB BROWSER",
                                        fontSize = 11.sp,
                                        fontWeight = if (currentTab == 2) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                                TextButton(
                                    onClick = { currentTab = 3 },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = if (currentTab == 3) ZegaNeonCyan else ZegaGrayText
                                    ),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "SYS TOOLS",
                                        fontSize = 11.sp,
                                        fontWeight = if (currentTab == 3) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                                TextButton(
                                    onClick = { showVeoStudio = true },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = ZegaNeonCyan
                                    ),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "🎬 VEO",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                TextButton(
                                    onClick = { showLyriaStudio = true },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = Color(0xFFFFB703)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "🎵 LYRIA",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (currentTab == 0 && notes.isNotEmpty()) {
                                    TextButton(
                                        onClick = { viewModel.clearAllNotes() },
                                        contentPadding = PaddingValues(horizontal = 6.dp)
                                    ) {
                                        Text("Clear All", color = ZegaWarningRed, fontSize = 11.sp)
                                    }
                                }
                                IconButton(
                                    onClick = { showNotesPanel = false },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close panel", tint = ZegaWhite, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        HorizontalDivider(color = CosmicCardBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                        when (currentTab) {
                            0 -> {
                                // Notes List View
                                Column(modifier = Modifier.weight(1f)) {
                                    // Manual entry to make notes system even better
                                    var manualNoteText by remember { mutableStateOf("") }
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        TextField(
                                            value = manualNoteText,
                                            onValueChange = { manualNoteText = it },
                                            placeholder = { Text("Write a quick note manually...", fontSize = 11.sp, color = ZegaGrayText) },
                                            singleLine = true,
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = CosmicBackground,
                                                unfocusedContainerColor = CosmicBackground,
                                                focusedTextColor = ZegaWhite,
                                                unfocusedTextColor = ZegaWhite,
                                                focusedIndicatorColor = ZegaNeonCyan,
                                                unfocusedIndicatorColor = Color.Transparent
                                            ),
                                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                            modifier = Modifier.weight(1f).height(40.dp).testTag("manual_note_input")
                                        )
                                        Button(
                                            onClick = {
                                                if (manualNoteText.trim().isNotEmpty()) {
                                                    viewModel.insertNoteManual(manualNoteText.trim())
                                                    manualNoteText = ""
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = ZegaDeepIndigo),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp),
                                            modifier = Modifier.height(40.dp)
                                        ) {
                                            Text("Save", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ZegaWhite)
                                        }
                                    }

                                    if (notes.isEmpty()) {
                                        Column(
                                            modifier = Modifier.weight(1f).fillMaxWidth(),
                                            verticalArrangement = Arrangement.Center,
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.EditNote,
                                                contentDescription = "Notes Empty Icon",
                                                tint = ZegaGrayText.copy(alpha = 0.4f),
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Text(
                                                text = "No saved voice or text notes yet.",
                                                fontSize = 13.sp,
                                                color = ZegaGrayText
                                            )
                                            Text(
                                                text = "Try manual entry above, or speaking: 'Take a note: Buy milk'",
                                                fontSize = 11.sp,
                                                color = ZegaGrayText,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(notes, key = { it.id }) { note ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(CosmicBackground)
                                                        .padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = note.content,
                                                            fontSize = 13.sp,
                                                            color = ZegaWhite
                                                        )
                                                        Text(
                                                            text = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date(note.timestamp)),
                                                            fontSize = 10.sp,
                                                            color = ZegaGrayText,
                                                            modifier = Modifier.padding(top = 4.dp)
                                                        )
                                                    }

                                                    IconButton(onClick = { viewModel.deleteNoteById(note.id) }) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Delete note",
                                                            tint = ZegaWarningRed.copy(alpha = 0.8f),
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            1 -> {
                                // Brand new Local Files Manager
                                Column(modifier = Modifier.weight(1f)) {

                                    // File Creation Row/Form
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        colors = CardDefaults.cardColors(containerColor = CosmicBackground),
                                        border = BorderStroke(1.dp, CosmicCardBorder)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("CREATE NEW FILE", fontSize = 10.sp, color = ZegaNeonCyan, fontWeight = FontWeight.Bold)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                TextField(
                                                    value = manualFileName,
                                                    onValueChange = { manualFileName = it },
                                                    placeholder = { Text("filename.txt", fontSize = 10.sp, color = ZegaGrayText) },
                                                    singleLine = true,
                                                    colors = TextFieldDefaults.colors(
                                                        focusedContainerColor = CosmicCard,
                                                        unfocusedContainerColor = CosmicCard,
                                                        focusedTextColor = ZegaWhite,
                                                        unfocusedTextColor = ZegaWhite,
                                                        focusedIndicatorColor = ZegaNeonCyan,
                                                        unfocusedIndicatorColor = Color.Transparent
                                                    ),
                                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                                                    modifier = Modifier.weight(0.4f).height(38.dp)
                                                )
                                                TextField(
                                                    value = manualFileContent,
                                                    onValueChange = { manualFileContent = it },
                                                    placeholder = { Text("File contents...", fontSize = 10.sp, color = ZegaGrayText) },
                                                    singleLine = true,
                                                    colors = TextFieldDefaults.colors(
                                                        focusedContainerColor = CosmicCard,
                                                        unfocusedContainerColor = CosmicCard,
                                                        focusedTextColor = ZegaWhite,
                                                        unfocusedTextColor = ZegaWhite,
                                                        focusedIndicatorColor = ZegaNeonCyan,
                                                        unfocusedIndicatorColor = Color.Transparent
                                                    ),
                                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                                                    modifier = Modifier.weight(0.6f).height(38.dp)
                                                )
                                                Button(
                                                    onClick = {
                                                        if (manualFileName.isNotBlank()) {
                                                            viewModel.writeLocalFile(context, manualFileName.trim(), manualFileContent)
                                                            manualFileName = ""
                                                            manualFileContent = ""
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = ZegaNeonCyan),
                                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                                    modifier = Modifier.height(38.dp),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("Save", color = CosmicBackground, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }

                                    // List of Files
                                    if (localFiles.isEmpty()) {
                                        Box(
                                            modifier = Modifier.weight(1f).fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "No files created yet.\nAsk Z-AI to \"write file logs.txt with contents hello\"!",
                                                color = ZegaGrayText,
                                                fontSize = 12.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    } else {
                                        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            // Left Pane: File list
                                            LazyColumn(
                                                modifier = Modifier.weight(0.5f).fillMaxHeight(),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                items(localFiles) { filename ->
                                                    val isSelected = selectedFileNameForView == filename
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(
                                                                if (isSelected) ZegaDeepIndigo else CosmicBackground,
                                                                RoundedCornerShape(8.dp)
                                                            )
                                                            .border(
                                                                BorderStroke(1.dp, if (isSelected) ZegaNeonCyan else CosmicCardBorder),
                                                                RoundedCornerShape(8.dp)
                                                            )
                                                            .clickable {
                                                                selectedFileNameForView = filename
                                                                fileViewContent = viewModel.readLocalFile(context, filename)
                                                            }
                                                            .padding(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Info,
                                                                contentDescription = "File",
                                                                tint = if (isSelected) ZegaNeonCyan else ZegaGrayText,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                            Text(
                                                                text = filename,
                                                                color = ZegaWhite,
                                                                fontSize = 11.sp,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                viewModel.deleteLocalFile(context, filename)
                                                                if (selectedFileNameForView == filename) {
                                                                    selectedFileNameForView = null
                                                                    fileViewContent = ""
                                                                }
                                                            },
                                                            modifier = Modifier.size(22.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Delete,
                                                                contentDescription = "Delete",
                                                                tint = ZegaWarningRed,
                                                                modifier = Modifier.size(12.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            // Right Pane: Content Preview / Editor
                                            Card(
                                                modifier = Modifier.weight(0.5f).fillMaxHeight(),
                                                colors = CardDefaults.cardColors(containerColor = CosmicCard),
                                                border = BorderStroke(1.dp, CosmicCardBorder)
                                            ) {
                                                Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                                                    if (selectedFileNameForView != null) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = selectedFileNameForView!!,
                                                                color = ZegaNeonCyan,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                            ) {
                                                                val isHtmlOrCode = selectedFileNameForView?.let { name ->
                                                                    name.endsWith(".html") || name.endsWith(".htm") || name.endsWith(".js") || name.endsWith(".ts") || name.endsWith(".txt")
                                                                } ?: false
                                                                
                                                                if (isHtmlOrCode) {
                                                                    Button(
                                                                        onClick = { isPreviewMode = !isPreviewMode },
                                                                        colors = ButtonDefaults.buttonColors(
                                                                            containerColor = if (isPreviewMode) ZegaNeonCyan else ZegaDeepIndigo,
                                                                            contentColor = if (isPreviewMode) CosmicBackground else ZegaNeonCyan
                                                                        ),
                                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                                        modifier = Modifier.height(24.dp),
                                                                        shape = RoundedCornerShape(4.dp),
                                                                        border = BorderStroke(1.dp, ZegaNeonCyan)
                                                                    ) {
                                                                        Text(
                                                                            text = if (isPreviewMode) "CODE" else "PREVIEW",
                                                                            fontSize = 9.sp,
                                                                            fontWeight = FontWeight.Bold
                                                                        )
                                                                    }
                                                                }
                                                                
                                                                IconButton(
                                                                    onClick = {
                                                                        selectedFileNameForView = null
                                                                        fileViewContent = ""
                                                                        isPreviewMode = false
                                                                    },
                                                                    modifier = Modifier.size(20.dp)
                                                                ) {
                                                                    Icon(Icons.Default.Close, "Close", tint = ZegaWhite, modifier = Modifier.size(12.dp))
                                                                }
                                                            }
                                                        }
                                                        HorizontalDivider(color = CosmicCardBorder, modifier = Modifier.padding(vertical = 4.dp))
                                                        
                                                        if (isPreviewMode) {
                                                            AndroidView(
                                                                factory = { ctx ->
                                                                    WebView(ctx).apply {
                                                                        settings.javaScriptEnabled = true
                                                                        settings.domStorageEnabled = true
                                                                        settings.useWideViewPort = true
                                                                        settings.loadWithOverviewMode = true
                                                                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                                                    }
                                                                },
                                                                update = { webView ->
                                                                    val extension = selectedFileNameForView?.substringAfterLast('.', "") ?: ""
                                                                    val htmlContent = when (extension) {
                                                                        "html", "htm" -> fileViewContent
                                                                        "js", "ts" -> {
                                                                            """
                                                                            <!DOCTYPE html>
                                                                            <html>
                                                                            <head>
                                                                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                                                            <style>
                                                                                body {
                                                                                    background-color: #0c0f1d;
                                                                                    color: #ffffff;
                                                                                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                                                                                    padding: 12px;
                                                                                    margin: 0;
                                                                                    display: flex;
                                                                                    flex-direction: column;
                                                                                    align-items: center;
                                                                                    justify-content: center;
                                                                                }
                                                                                canvas {
                                                                                    border: 2px solid #00E5FF;
                                                                                    background-color: #000000;
                                                                                    max-width: 100%;
                                                                                    height: auto;
                                                                                    border-radius: 8px;
                                                                                    box-shadow: 0 0 15px rgba(0, 229, 255, 0.5);
                                                                                    margin-top: 10px;
                                                                                }
                                                                                #console {
                                                                                    width: 100%;
                                                                                    max-height: 120px;
                                                                                    overflow-y: auto;
                                                                                    background: #151a30;
                                                                                    border: 1px solid #1f2747;
                                                                                    padding: 8px;
                                                                                    font-family: monospace;
                                                                                    font-size: 11px;
                                                                                    border-radius: 6px;
                                                                                    box-sizing: border-box;
                                                                                    margin-top: 12px;
                                                                                    color: #00ffcc;
                                                                                }
                                                                            </style>
                                                                            </head>
                                                                            <body>
                                                                            <div style="font-size: 11px; font-weight: bold; color: #00E5FF; margin-bottom: 5px;">Canvas Live Preview</div>
                                                                            <canvas id="canvas" width="300" height="300"></canvas>
                                                                            <div id="console"></div>
                                                                            <script>
                                                                                const consoleDiv = document.getElementById('console');
                                                                                console.log = function(...args) {
                                                                                    const p = document.createElement('div');
                                                                                    p.textContent = "> " + args.join(' ');
                                                                                    consoleDiv.appendChild(p);
                                                                                    consoleDiv.scrollTop = consoleDiv.scrollHeight;
                                                                                };
                                                                                window.onerror = function(message, source, lineno, colno, error) {
                                                                                    console.log("Error on line " + lineno + ": " + message);
                                                                                    return true;
                                                                                };
                                                                                
                                                                                try {
                                                                                    $fileViewContent
                                                                                } catch (err) {
                                                                                    console.log("Execution error: " + err.message);
                                                                                }
                                                                            </script>
                                                                            </body>
                                                                            </html>
                                                                            """.trimIndent()
                                                                        }
                                                                        else -> {
                                                                            """
                                                                            <!DOCTYPE html>
                                                                            <html>
                                                                            <body style="background-color: #0c0f1d; color: #ffffff; font-family: sans-serif; padding: 15px;">
                                                                            <pre style="white-space: pre-wrap; word-wrap: break-word;">$fileViewContent</pre>
                                                                            </body>
                                                                            </html>
                                                                            """.trimIndent()
                                                                        }
                                                                    }
                                                                    webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
                                                                },
                                                                modifier = Modifier.fillMaxSize().weight(1f)
                                                            )
                                                        } else {
                                                            Box(
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .fillMaxWidth()
                                                                    .verticalScroll(rememberScrollState())
                                                            ) {
                                                                Text(
                                                                    text = fileViewContent,
                                                                    color = ZegaWhite,
                                                                    fontSize = 11.sp,
                                                                    fontFamily = FontFamily.Monospace
                                                                )
                                                            }
                                                        }
                                                    } else {
                                                        Box(
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                "Select a file to preview",
                                                                color = ZegaGrayText,
                                                                fontSize = 11.sp,
                                                                textAlign = TextAlign.Center
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            2 -> {
                                // Integrated WebView Browser App / Free Google.com client
                                Column(modifier = Modifier.weight(1f)) {
                                    // Address Bar Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        TextField(
                                            value = webUrlInput,
                                            onValueChange = { webUrlInput = it },
                                            placeholder = { Text("Search Google or type web address...", fontSize = 11.sp, color = ZegaGrayText) },
                                            singleLine = true,
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = CosmicBackground,
                                                unfocusedContainerColor = CosmicBackground,
                                                focusedTextColor = ZegaWhite,
                                                unfocusedTextColor = ZegaWhite,
                                                focusedIndicatorColor = ZegaNeonCyan,
                                                unfocusedIndicatorColor = Color.Transparent
                                            ),
                                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                                            modifier = Modifier.weight(1f).height(38.dp).testTag("web_address_input")
                                        )

                                        IconButton(
                                            onClick = {
                                                val destination = if (webUrlInput.startsWith("http://") || webUrlInput.startsWith("https://")) {
                                                    webUrlInput
                                                } else if (webUrlInput.contains(".") && !webUrlInput.contains(" ")) {
                                                    "https://$webUrlInput"
                                                } else {
                                                    "https://www.google.com/search?q=${java.net.URLEncoder.encode(webUrlInput, "UTF-8")}"
                                                }
                                                webUrlInput = destination
                                                webViewInstance?.loadUrl(destination)
                                            },
                                            modifier = Modifier.size(38.dp).background(ZegaDeepIndigo, RoundedCornerShape(8.dp))
                                        ) {
                                            Icon(Icons.Default.Search, contentDescription = "Go", tint = ZegaNeonCyan, modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    // Control Bar Row (Back, Forward, Refresh, Google Home)
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { if (webViewInstance?.canGoBack() == true) webViewInstance?.goBack() },
                                            modifier = Modifier.size(26.dp).background(CosmicBackground, CircleShape)
                                        ) {
                                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ZegaWhite, modifier = Modifier.size(12.dp))
                                        }

                                        IconButton(
                                            onClick = { if (webViewInstance?.canGoForward() == true) webViewInstance?.goForward() },
                                            modifier = Modifier.size(26.dp).background(CosmicBackground, CircleShape)
                                        ) {
                                            Icon(Icons.Default.ArrowForward, contentDescription = "Forward", tint = ZegaWhite, modifier = Modifier.size(12.dp))
                                        }

                                        IconButton(
                                            onClick = { webViewInstance?.reload() },
                                            modifier = Modifier.size(26.dp).background(CosmicBackground, CircleShape)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = ZegaWhite, modifier = Modifier.size(12.dp))
                                        }

                                        IconButton(
                                            onClick = {
                                                webUrlInput = "https://www.google.com"
                                                webViewInstance?.loadUrl("https://www.google.com")
                                            },
                                            modifier = Modifier.size(26.dp).background(CosmicBackground, CircleShape)
                                        ) {
                                            Icon(Icons.Default.Home, contentDescription = "Home", tint = ZegaWhite, modifier = Modifier.size(12.dp))
                                        }

                                        Spacer(modifier = Modifier.weight(1f))

                                        Text(
                                            text = "FREE BROWSER API",
                                            fontSize = 9.sp,
                                            color = ZegaPrivacyGreen,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }

                                    // Integrated Web Layout Screen Panel
                                    Card(
                                        modifier = Modifier.weight(1f).fillMaxWidth(),
                                        border = BorderStroke(1.dp, CosmicCardBorder),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        AndroidView(
                                            factory = { ctx ->
                                                WebView(ctx).apply {
                                                    webViewClient = object : WebViewClient() {
                                                        override fun onPageFinished(view: WebView?, url: String?) {
                                                            super.onPageFinished(view, url)
                                                            if (url != null) {
                                                                webUrlInput = url
                                                            }
                                                        }
                                                    }
                                                    settings.javaScriptEnabled = true
                                                    settings.domStorageEnabled = true
                                                    settings.useWideViewPort = true
                                                    settings.loadWithOverviewMode = true
                                                    webViewInstance = this
                                                    loadUrl(webUrlInput)
                                                }
                                            },
                                            update = { webView ->
                                                webViewInstance = webView
                                            },
                                            modifier = Modifier.fillMaxSize().testTag("integrated_webview")
                                        )
                                    }
                                }
                            }
                            3 -> {
                                // Systems Tools & Hardware Apps panel
                                LazyColumn(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // 1. Hardware Utility Card
                                    item {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = CosmicBackground),
                                            border = BorderStroke(1.dp, ZegaNeonCyan.copy(alpha = 0.3f))
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text("SYS HARDWARE & UTILITIES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ZegaNeonCyan)
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    // Flashlight Tool
                                                    Button(
                                                        onClick = { viewModel.setFlashlightInternal(context, !flashlightEnabled) },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = if (flashlightEnabled) ZegaPrivacyGreen.copy(alpha = 0.2f) else CosmicCard
                                                        ),
                                                        border = BorderStroke(1.dp, if (flashlightEnabled) ZegaPrivacyGreen else CosmicCardBorder),
                                                        modifier = Modifier.weight(1f),
                                                        shape = RoundedCornerShape(10.dp),
                                                        contentPadding = PaddingValues(6.dp)
                                                    ) {
                                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Icon(
                                                                imageVector = Icons.Default.Lightbulb, 
                                                                contentDescription = "Flashlight", 
                                                                tint = if (flashlightEnabled) ZegaPrivacyGreen else ZegaWhite,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            Text(
                                                                text = if (flashlightEnabled) "FLASHLIGHT: ON" else "FLASHLIGHT: OFF", 
                                                                fontSize = 9.sp, 
                                                                fontWeight = FontWeight.Bold, 
                                                                color = ZegaWhite
                                                            )
                                                        }
                                                    }

                                                    // Vibrate / Haptic Pulsator Tool
                                                    Button(
                                                        onClick = { viewModel.triggerVibrationInternal(context) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = CosmicCard),
                                                        border = BorderStroke(1.dp, CosmicCardBorder),
                                                        modifier = Modifier.weight(1f),
                                                        shape = RoundedCornerShape(10.dp),
                                                        contentPadding = PaddingValues(6.dp)
                                                    ) {
                                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Icon(
                                                                imageVector = Icons.Default.Vibration, 
                                                                contentDescription = "Vibrate", 
                                                                tint = ZegaNeonViolet,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            Text(
                                                                text = "BUZZ HAPTICS", 
                                                                fontSize = 9.sp, 
                                                                fontWeight = FontWeight.Bold, 
                                                                color = ZegaWhite
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // 2. Battery Health diagnostics
                                    item {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = CosmicBackground),
                                            border = BorderStroke(1.dp, ZegaNeonViolet.copy(alpha = 0.3f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.BatteryChargingFull,
                                                    contentDescription = "Battery",
                                                    tint = if ((batteryLevel ?: 100) > 20) ZegaPrivacyGreen else ZegaWarningRed,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("BATTERY DIAGNOSTICS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ZegaNeonViolet)
                                                    Text("Current Charge: ${batteryLevel ?: 100}%", fontSize = 12.sp, color = ZegaWhite)
                                                    Text("Status: Connected to offline diagnostic core", fontSize = 9.sp, color = ZegaGrayText)
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .clip(CircleShape)
                                                        .background(if ((batteryLevel ?: 100) > 20) ZegaPrivacyGreen.copy(alpha = 0.15f) else ZegaWarningRed.copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "${batteryLevel ?: 100}%",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if ((batteryLevel ?: 100) > 20) ZegaPrivacyGreen else ZegaWarningRed
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // 3. System Count-down Timer
                                    item {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = CosmicBackground),
                                            border = BorderStroke(1.dp, ZegaPrivacyGreen.copy(alpha = 0.3f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Timer,
                                                    contentDescription = "Timer",
                                                    tint = ZegaNeonCyan,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("COUNTDOWN ALARM TIMER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ZegaPrivacyGreen)
                                                    if (timerActive) {
                                                        Text("Active: $timerRemaining seconds left", fontSize = 12.sp, color = ZegaWhite)
                                                    } else {
                                                        Text("Timer idle / Ready to launch", fontSize = 12.sp, color = ZegaWhite)
                                                    }
                                                }
                                                if (timerActive) {
                                                    Button(
                                                        onClick = { viewModel.stopLocalTimer() },
                                                        colors = ButtonDefaults.buttonColors(containerColor = ZegaWarningRed),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(26.dp)
                                                    ) {
                                                        Text("Stop", fontSize = 9.sp, color = ZegaWhite)
                                                    }
                                                } else {
                                                    Button(
                                                        onClick = { viewModel.startLocalTimer(30) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = ZegaDeepIndigo),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(26.dp)
                                                    ) {
                                                        Text("30s", fontSize = 9.sp, color = ZegaWhite)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // 4. Voice Synthesis (TTS) - Male Mode & Timbre
                                    item {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = CosmicBackground),
                                            border = BorderStroke(1.dp, ZegaNeonCyan.copy(alpha = 0.4f))
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        "TTS SYNTHESIS — ${ttsVoiceMode.title.uppercase()}",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = ZegaNeonCyan
                                                    )
                                                    TextButton(
                                                        onClick = {
                                                            viewModel.refreshAvailableVoices()
                                                            showTtsSettingsDialog = true
                                                        },
                                                        contentPadding = PaddingValues(0.dp)
                                                    ) {
                                                        Text("TUNER", fontSize = 10.sp, color = ZegaNeonCyan, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    // Male Mode Button
                                                    Button(
                                                        onClick = { viewModel.setTtsVoiceMode(TtsVoiceMode.MALE) },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = if (ttsVoiceMode == TtsVoiceMode.MALE) ZegaNeonCyan else CosmicCard,
                                                            contentColor = if (ttsVoiceMode == TtsVoiceMode.MALE) CosmicBackground else ZegaWhite
                                                        ),
                                                        border = BorderStroke(1.dp, if (ttsVoiceMode == TtsVoiceMode.MALE) ZegaNeonCyan else CosmicCardBorder.copy(alpha = 0.4f)),
                                                        modifier = Modifier.weight(1f),
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(vertical = 4.dp)
                                                    ) {
                                                        Text("👨 MALE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }

                                                    // Female Mode Button
                                                    Button(
                                                        onClick = { viewModel.setTtsVoiceMode(TtsVoiceMode.FEMALE) },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = if (ttsVoiceMode == TtsVoiceMode.FEMALE) ZegaNeonCyan else CosmicCard,
                                                            contentColor = if (ttsVoiceMode == TtsVoiceMode.FEMALE) CosmicBackground else ZegaWhite
                                                        ),
                                                        border = BorderStroke(1.dp, if (ttsVoiceMode == TtsVoiceMode.FEMALE) ZegaNeonCyan else CosmicCardBorder.copy(alpha = 0.4f)),
                                                        modifier = Modifier.weight(1f),
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(vertical = 4.dp)
                                                    ) {
                                                        Text("👩 FEMALE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }

                                                    // Test Voice Button
                                                    Button(
                                                        onClick = { viewModel.testTtsVoice() },
                                                        colors = ButtonDefaults.buttonColors(containerColor = ZegaDeepIndigo),
                                                        border = BorderStroke(1.dp, CosmicCardBorder),
                                                        modifier = Modifier.weight(1f),
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(vertical = 4.dp)
                                                    ) {
                                                        Text("🔊 TEST", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ZegaWhite)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // 5. Timezone Sync Indicator
                                    item {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = CosmicBackground),
                                            border = BorderStroke(1.dp, ZegaGrayText.copy(alpha = 0.3f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Sync,
                                                    contentDescription = "Timezone Auto Sync",
                                                    tint = ZegaPrivacyGreen,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                val activeTz = java.util.TimeZone.getDefault()
                                                val tzName = activeTz.getDisplayName(false, java.util.TimeZone.SHORT, Locale.getDefault())
                                                val tzId = activeTz.id
                                                Column {
                                                    Text("TIMEZONE DYNAMICS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ZegaPrivacyGreen)
                                                    Text("Synced: $tzId ($tzName)", fontSize = 12.sp, color = ZegaWhite)
                                                    Text("Perfect synchronization with on-device hardware.", fontSize = 9.sp, color = ZegaGrayText)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Authentication Dialog (Sign In / Sign Up / Social)
    if (showAuthDialog) {
        AuthDialog(
            viewModel = viewModel,
            onDismiss = { showAuthDialog = false }
        )
    }

    // User Profile & Account Settings Dialog
    if (showProfileDialog && currentUser != null) {
        UserProfileDialog(
            viewModel = viewModel,
            user = currentUser!!,
            onDismiss = { showProfileDialog = false },
            onOpenAuth = { showAuthDialog = true }
        )
    }

    // Wake-Word Detection Configuration Dialog (DSP & Acoustic Models)
    if (showWakeWordConfigDialog) {
        WakeWordConfigDialog(
            viewModel = viewModel,
            onDismiss = { showWakeWordConfigDialog = false }
        )
    }

    // TTS Voice Mode & Acoustic Config Dialog
    if (showTtsSettingsDialog) {
        TtsVoiceSettingsDialog(
            currentMode = ttsVoiceMode,
            pitchMultiplier = ttsPitchMultiplier,
            speedRate = ttsSpeechRate,
            availableVoices = availableVoices,
            selectedVoiceName = selectedVoiceName,
            onSelectMode = { viewModel.setTtsVoiceMode(it) },
            onPitchChange = { viewModel.setTtsPitchMultiplier(it) },
            onSpeedChange = { viewModel.setTtsSpeechRate(it) },
            onSelectSpecificVoice = { viewModel.selectSpecificVoice(it) },
            onTestVoice = { viewModel.testTtsVoice() },
            onDismiss = { showTtsSettingsDialog = false }
        )
    }

    // Veo 3.1 Fast Video Studio Dialog
    if (showVeoStudio) {
        VeoStudioDialog(
            viewModel = viewModel,
            onDismiss = { showVeoStudio = false }
        )
    }

    // Lyria 3 Music Studio Dialog
    if (showLyriaStudio) {
        LyriaStudioDialog(
            viewModel = viewModel,
            onDismiss = { showLyriaStudio = false }
        )
    }

    // Pending Tool Permission Dialog (Camera, App Launch, etc.)
    if (pendingPermission != null) {
        val perm = pendingPermission!!
        AlertDialog(
            onDismissRequest = { viewModel.denyPendingToolPermission() },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = ZegaNeonCyan
                    )
                    Text(
                        text = perm.title,
                        color = ZegaNeonCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = perm.description,
                        color = ZegaWhite,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Your permission is required before executing this device command.",
                        color = ZegaGrayText,
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.allowPendingToolPermission(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = ZegaPrivacyGreen),
                    modifier = Modifier.testTag("confirm_tool_permission_button")
                ) {
                    Text("Allow Action", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.denyPendingToolPermission() },
                    border = BorderStroke(1.dp, ZegaWarningRed),
                    modifier = Modifier.testTag("deny_tool_permission_button")
                ) {
                    Text("Deny", color = ZegaWarningRed)
                }
            },
            containerColor = ZegaDarkCard,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.testTag("tool_permission_dialog")
        )
    }

    // In-App Browser Dialog for AI Web Navigation
    if (browserUrl != null) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { viewModel.closeInAppBrowser() }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, ZegaNeonCyan, RoundedCornerShape(16.dp))
                    .testTag("in_app_browser_dialog"),
                colors = CardDefaults.cardColors(containerColor = CosmicBackground)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CosmicCard)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                tint = ZegaNeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = browserUrl ?: "",
                                color = ZegaNeonCyan,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { viewModel.closeInAppBrowser() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Browser",
                                tint = ZegaWhite,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                webViewClient = WebViewClient()
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                loadUrl(browserUrl ?: "https://google.com")
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    // Virtual File System (VFS) Explorer Dialog
    if (vfsOpen) {
        VfsExplorerDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.setVfsDialogOpen(false) }
        )
    }

    // AI Web Search Tool Dialog
    if (isAiSearchOpen) {
        AiSearchToolDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.setAiSearchDialogOpen(false) },
            onOpenInBrowser = { url ->
                viewModel.openInAppBrowser(url)
            }
        )
    }
}

@Composable
fun ChatMessageRow(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val isUser = message.sender == "user"
    val bubbleColor = if (isUser) ZegaDeepIndigo else CosmicCard
    val bubbleBorder = if (isUser) ZegaPurpleAccent.copy(alpha = 0.5f) else CosmicCardBorder
    val textColor = ZegaWhite

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            // Intent Label Tag inside Assistant Speech to show intelligence
            if (!isUser) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(bottom = 2.dp, start = 8.dp)
                ) {
                    val icon = when (message.intentType) {
                        "time" -> Icons.Default.AccessTime
                        "date" -> Icons.Default.CalendarToday
                        "device" -> Icons.Default.Build
                        "timer" -> Icons.Default.Timer
                        "notes" -> Icons.Default.Note
                        "math" -> Icons.Default.Calculate
                        "joke" -> Icons.Default.SentimentSatisfiedAlt
                        "about" -> Icons.Default.Info
                        "map" -> Icons.Default.Map
                        "search" -> Icons.Default.Search
                        else -> Icons.Default.ChatBubbleOutline
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = message.intentType,
                        tint = ZegaNeonCyan,
                        modifier = Modifier.size(10.dp)
                    )

                    Text(
                        text = message.intentType.uppercase(Locale.getDefault()),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = ZegaNeonCyan,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Collapsible thinking block if available
            if (!isUser && !message.thinking.isNullOrBlank()) {
                var thinkingExpanded by remember { mutableStateOf(false) }
                
                Column(
                    modifier = Modifier
                        .padding(bottom = 6.dp, start = 8.dp)
                        .widthIn(max = 280.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CosmicCard)
                        .border(1.dp, CosmicCardBorder.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .clickable { thinkingExpanded = !thinkingExpanded }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (thinkingExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle Thinking",
                            tint = ZegaNeonCyan.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (message.thinkingDurationSeconds > 0) {
                                "THOUGHTS & REASONING (${message.thinkingDurationSeconds}s)"
                            } else {
                                "THOUGHTS & REASONING"
                            },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ZegaNeonCyan.copy(alpha = 0.8f),
                            letterSpacing = 1.sp
                        )
                    }
                    if (thinkingExpanded) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = message.thinking,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = ZegaGrayText,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
            }

            // Core Message Balloon Card
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 2.dp,
                            bottomEnd = if (isUser) 2.dp else 16.dp
                        )
                    )
                    .background(bubbleColor)
                    .border(
                        width = 1.dp,
                        color = if (message.intentType == "grounded_search") ZegaPrivacyGreen.copy(alpha = 0.6f) else bubbleBorder,
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 2.dp,
                            bottomEnd = if (isUser) 2.dp else 16.dp
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .widthIn(max = 280.dp)
            ) {
                Column {
                    if (message.intentType == "grounded_search") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(ZegaPrivacyGreen.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TravelExplore,
                                contentDescription = null,
                                tint = ZegaPrivacyGreen,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Grounded with Google Search • gemini-3.5-flash",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = ZegaPrivacyGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    MarkdownText(
                        text = message.text,
                        textColor = textColor
                    )

                    if (message.imageUrl != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        AsyncImage(
                            model = message.imageUrl,
                            contentDescription = "Static Map Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, CosmicCardBorder, RoundedCornerShape(12.dp))
                        )
                    }

                    // Display vocal/typed icon indicators at the bottom right of the bubble
                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (message.isVoice) Icons.Default.KeyboardVoice else Icons.Default.Keyboard,
                            contentDescription = null,
                            tint = ZegaGrayText,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.timestamp)),
                            fontSize = 8.sp,
                            color = ZegaGrayText
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MarkdownText(
    text: String,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val lines = text.split("\n")
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        var inCodeBlock = false
        val currentCodeBlock = StringBuilder()

        for (line in lines) {
            if (line.trim().startsWith("```")) {
                if (inCodeBlock) {
                    CodeBlock(code = currentCodeBlock.toString().trimEnd())
                    currentCodeBlock.clear()
                    inCodeBlock = false
                } else {
                    inCodeBlock = true
                }
                continue
            }

            if (inCodeBlock) {
                currentCodeBlock.append(line).append("\n")
                continue
            }

            val trimmedLine = line.trim()
            when {
                trimmedLine.startsWith("# ") -> {
                    Text(
                        text = parseMarkdownInline(trimmedLine.removePrefix("# "), textColor),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ZegaNeonCyan,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }
                trimmedLine.startsWith("## ") -> {
                    Text(
                        text = parseMarkdownInline(trimmedLine.removePrefix("## "), textColor),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ZegaNeonViolet,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }
                trimmedLine.startsWith("### ") -> {
                    Text(
                        text = parseMarkdownInline(trimmedLine.removePrefix("### "), textColor),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ZegaWhite,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }
                trimmedLine.startsWith("- ") || trimmedLine.startsWith("* ") || trimmedLine.startsWith("• ") -> {
                    val content = if (trimmedLine.startsWith("- ")) {
                        trimmedLine.removePrefix("- ")
                    } else if (trimmedLine.startsWith("* ")) {
                        trimmedLine.removePrefix("* ")
                    } else {
                        trimmedLine.removePrefix("• ")
                    }
                    Row(
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "•", color = ZegaNeonCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = parseMarkdownInline(content, textColor),
                            fontSize = 13.sp,
                            color = textColor,
                            lineHeight = 17.sp
                        )
                    }
                }
                trimmedLine.matches(Regex("^\\d+\\.\\s+.*")) -> {
                    val parts = trimmedLine.split(Regex("^\\d+\\.\\s+"), limit = 2)
                    val numberText = trimmedLine.substring(0, trimmedLine.indexOf('.')) + "."
                    val content = parts.getOrNull(1) ?: ""
                    Row(
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = numberText, color = ZegaNeonCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = parseMarkdownInline(content, textColor),
                            fontSize = 13.sp,
                            color = textColor,
                            lineHeight = 17.sp
                        )
                    }
                }
                trimmedLine.isEmpty() -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                else -> {
                    Text(
                        text = parseMarkdownInline(line, textColor),
                        fontSize = 13.sp,
                        color = textColor,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        if (inCodeBlock && currentCodeBlock.isNotEmpty()) {
            CodeBlock(code = currentCodeBlock.toString().trimEnd())
        }
    }
}

@Composable
fun CodeBlock(code: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ZegaDeepIndigo.copy(alpha = 0.6f))
            .border(1.dp, CosmicCardBorder, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Text(
            text = code,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = ZegaNeonCyan,
            lineHeight = 15.sp
        )
    }
}

fun parseMarkdownInline(text: String, baseColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var index = 0
        while (index < text.length) {
            val nextBold = text.indexOf("**", index)
            val nextItalic = text.indexOf("*", index)
            val nextCode = text.indexOf("`", index)

            val minIndex = listOf(
                if (nextBold != -1) nextBold else Int.MAX_VALUE,
                if (nextItalic != -1) nextItalic else Int.MAX_VALUE,
                if (nextCode != -1) nextCode else Int.MAX_VALUE
            ).minOrNull() ?: Int.MAX_VALUE

            if (minIndex == Int.MAX_VALUE) {
                append(text.substring(index))
                break
            }

            if (minIndex > index) {
                append(text.substring(index, minIndex))
            }

            if (minIndex == nextBold) {
                val endBold = text.indexOf("**", nextBold + 2)
                if (endBold != -1) {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(parseMarkdownInline(text.substring(nextBold + 2, endBold), baseColor))
                    }
                    index = endBold + 2
                } else {
                    append("**")
                    index = nextBold + 2
                }
            } else if (minIndex == nextItalic) {
                val endItalic = text.indexOf("*", nextItalic + 1)
                if (endItalic != -1) {
                    withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(parseMarkdownInline(text.substring(nextItalic + 1, endItalic), baseColor))
                    }
                    index = endItalic + 1
                } else {
                    append("*")
                    index = nextItalic + 1
                }
            } else {
                val endCode = text.indexOf("`", nextCode + 1)
                if (endCode != -1) {
                    withStyle(style = SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = Color(0xFF222222),
                        color = ZegaNeonCyan
                    )) {
                        append(text.substring(nextCode + 1, endCode))
                    }
                    index = endCode + 1
                } else {
                    append("`")
                    index = nextCode + 1
                }
            }
        }
    }
}

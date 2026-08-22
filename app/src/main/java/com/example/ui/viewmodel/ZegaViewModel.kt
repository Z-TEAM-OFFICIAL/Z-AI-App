package com.example.ui.viewmodel

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ZegaRepository
import com.example.data.database.ChatMessage
import com.example.data.database.VoiceNote
import com.example.data.brain.ZegaAction
import com.example.data.brain.ZegaOfflineBrain
import com.example.data.brain.ZegaResponse
import com.example.data.voice.SpeechStateListener
import com.example.data.voice.SpeechToTextManager
import com.example.data.voice.TextToSpeechManager
import com.example.data.voice.ZegaWakeWordDetector
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.data.api.GoogleSearchScraper

import com.example.data.auth.AuthRepository
import com.example.data.auth.AuthResult
import com.example.data.database.UserAccount
import com.example.data.voice.HapticIntensity
import com.example.data.voice.TtsVoiceMode
import com.example.data.voice.VoiceProfileInfo
import com.example.data.voice.WakeWordEngineType
import com.example.data.voice.WakeWordSensitivity
import com.example.data.voice.ZegaHapticManager

enum class AssistantState {
    IDLE,          // Waiting for user to tap mic
    LISTENING,     // Microphone is recording and listening to command
    PROCESSING,    // Processing speech-to-text and AI offline brain
    SPEAKING,      // Speaking response via TTS
    WAKE_WORD_WAIT // Passive continuous background listening for "Hey Z-AI"
}

sealed class PendingToolPermission(val title: String, val description: String) {
    object Camera : PendingToolPermission("Camera Access Permission", "Z-AI is requesting permission to open the camera to take a photo. Do you grant access?")
    data class LaunchApp(val packageName: String, val appName: String) : PendingToolPermission("App Control Permission", "Z-AI is requesting permission to launch $appName ($packageName). Do you allow this action?")
}

class ZegaViewModel(
    private val repository: ZegaRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val brain = ZegaOfflineBrain()
    val vfs = com.example.data.vfs.VirtualFileSystem(repository.vfsDao)

    val vfsNodes: StateFlow<List<com.example.data.vfs.VfsNode>> = vfs.allNodes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isCloudSyncing = MutableStateFlow(false)
    val isCloudSyncing: StateFlow<Boolean> = _isCloudSyncing.asStateFlow()

    fun writeVfsFile(parentPath: String, name: String, content: String) {
        viewModelScope.launch {
            vfs.writeFile(parentPath, name, content)
        }
    }

    fun createVfsDirectory(parentPath: String, name: String) {
        viewModelScope.launch {
            vfs.createDirectory(parentPath, name)
        }
    }

    fun deleteVfsNode(fullPath: String) {
        viewModelScope.launch {
            vfs.deleteNode(fullPath)
        }
    }

    private var sttManager: SpeechToTextManager? = null
    private var ttsManager: TextToSpeechManager? = null
    private var wakeWordDetector: ZegaWakeWordDetector? = null

    // Sensitive Tool Permissions State
    private val _pendingToolPermission = MutableStateFlow<PendingToolPermission?>(null)
    val pendingToolPermission: StateFlow<PendingToolPermission?> = _pendingToolPermission.asStateFlow()

    // In-App Browser State
    private val _inAppBrowserUrl = MutableStateFlow<String?>(null)
    val inAppBrowserUrl: StateFlow<String?> = _inAppBrowserUrl.asStateFlow()

    // VFS Dialog State
    private val _vfsDialogOpen = MutableStateFlow(false)
    val vfsDialogOpen: StateFlow<Boolean> = _vfsDialogOpen.asStateFlow()

    // Multi-tier Dynamic Model Tracking
    private val _currentModelTier = MutableStateFlow("gemini-3.7-flash")
    val currentModelTier: StateFlow<String> = _currentModelTier.asStateFlow()

    // Power & Eco-Mode State
    private val _ecoModeActive = MutableStateFlow(false)
    val ecoModeActive: StateFlow<Boolean> = _ecoModeActive.asStateFlow()

    fun openInAppBrowser(url: String) {
        val clean = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
        _inAppBrowserUrl.value = clean
    }

    fun closeInAppBrowser() {
        _inAppBrowserUrl.value = null
    }

    fun setVfsDialogOpen(open: Boolean) {
        _vfsDialogOpen.value = open
    }

    fun toggleEcoMode(context: Context) {
        val pm = com.example.data.voice.ZegaPowerManager.getInstance(context)
        val next = !_ecoModeActive.value
        pm.setEcoMode(next)
        _ecoModeActive.value = pm.isEcoModeActive()
        if (next) {
            _wakeWordEnabled.value = false
            wakeWordDetector?.stopListening()
            speakLocal("Power Saver Eco Mode enabled. Wake-word detection paused to preserve battery.")
        } else {
            speakLocal("High Performance Mode restored.")
        }
    }

    fun allowPendingToolPermission(context: Context) {
        val pending = _pendingToolPermission.value ?: return
        _pendingToolPermission.value = null
        when (pending) {
            is PendingToolPermission.Camera -> {
                try {
                    val intent = android.content.Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    viewModelScope.launch {
                        repository.insertMessage(
                            ChatMessage(
                                sender = "zega",
                                text = "Camera launched successfully.",
                                isVoice = true,
                                intentType = "camera"
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.e("ZegaVM", "Failed to launch camera: ${e.message}")
                    viewModelScope.launch {
                        repository.insertMessage(
                            ChatMessage(
                                sender = "zega",
                                text = "Could not open camera: ${e.localizedMessage}",
                                isVoice = false,
                                intentType = "camera"
                            )
                        )
                    }
                }
            }
            is PendingToolPermission.LaunchApp -> {
                try {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(pending.packageName)
                    if (launchIntent != null) {
                        launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                    } else {
                        val searchIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("market://search?q=${pending.appName}")).apply {
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(searchIntent)
                    }
                    viewModelScope.launch {
                        repository.insertMessage(
                            ChatMessage(
                                sender = "zega",
                                text = "Launched ${pending.appName}.",
                                isVoice = true,
                                intentType = "app_control"
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.e("ZegaVM", "Failed to launch app: ${e.message}")
                }
            }
        }
    }

    fun denyPendingToolPermission() {
        _pendingToolPermission.value = null
        viewModelScope.launch {
            repository.insertMessage(
                ChatMessage(
                    sender = "zega",
                    text = "Action permission was declined by user.",
                    isVoice = false,
                    intentType = "permission"
                )
            )
        }
    }

    // Authentication States
    val currentUser: StateFlow<UserAccount?> = authRepository.currentUser
    val allUsers: Flow<List<UserAccount>> = authRepository.allUsers
    
    // AI Creations (Veo Videos & Lyria Music)
    val allCreations: Flow<List<com.example.data.database.AICreation>> = repository.allCreations

    private val _searchGroundingEnabled = MutableStateFlow(true)
    val searchGroundingEnabled: StateFlow<Boolean> = _searchGroundingEnabled.asStateFlow()

    fun toggleSearchGrounding() {
        _searchGroundingEnabled.value = !_searchGroundingEnabled.value
    }

    fun saveAICreation(creation: com.example.data.database.AICreation) {
        viewModelScope.launch {
            repository.insertCreation(creation)
        }
    }

    fun deleteCreationById(id: Long) {
        viewModelScope.launch {
            repository.deleteCreation(id)
        }
    }

    fun clearAllCreations() {
        viewModelScope.launch {
            repository.clearCreations()
        }
    }

    fun loginWithGoogle(
        email: String,
        displayName: String,
        idToken: String? = null,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            val result = authRepository.loginWithGoogle(email, displayName, idToken)
            _isAuthLoading.value = false
            when (result) {
                is AuthResult.Success -> onComplete(true)
                is AuthResult.Error -> {
                    _authError.value = result.message
                    onComplete(false)
                }
            }
        }
    }

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    fun clearAuthError() {
        _authError.value = null
    }

    fun signUp(email: String, password: String, displayName: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            val result = authRepository.signUp(email, password, displayName)
            _isAuthLoading.value = false
            when (result) {
                is AuthResult.Success -> {
                    onComplete(true)
                }
                is AuthResult.Error -> {
                    _authError.value = result.message
                    onComplete(false)
                }
            }
        }
    }

    fun login(email: String, password: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            val result = authRepository.login(email, password)
            _isAuthLoading.value = false
            when (result) {
                is AuthResult.Success -> {
                    onComplete(true)
                }
                is AuthResult.Error -> {
                    _authError.value = result.message
                    onComplete(false)
                }
            }
        }
    }

    fun loginWithSocial(
        provider: String,
        email: String,
        displayName: String,
        avatarColor: String = "#00FFCC",
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            val result = authRepository.loginWithSocial(provider, email, displayName, avatarColor)
            _isAuthLoading.value = false
            when (result) {
                is AuthResult.Success -> onComplete(true)
                is AuthResult.Error -> {
                    _authError.value = result.message
                    onComplete(false)
                }
            }
        }
    }

    fun switchAccount(user: UserAccount) {
        viewModelScope.launch {
            authRepository.switchAccount(user)
        }
    }

    fun logout() {
        authRepository.logout()
    }

    // State flows
    private val _assistantState = MutableStateFlow(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _wakeWordEnabled = MutableStateFlow(false)
    val wakeWordEnabled: StateFlow<Boolean> = _wakeWordEnabled.asStateFlow()

    private val _wakeWordConfidence = MutableStateFlow(0f)
    val wakeWordConfidence: StateFlow<Float> = _wakeWordConfidence.asStateFlow()

    private val _wakeWordEngineType = MutableStateFlow(WakeWordEngineType.TFLITE_ACOUSTIC)
    val wakeWordEngineType: StateFlow<WakeWordEngineType> = _wakeWordEngineType.asStateFlow()

    private val _wakeWordSensitivity = MutableStateFlow(WakeWordSensitivity.BALANCED)
    val wakeWordSensitivity: StateFlow<WakeWordSensitivity> = _wakeWordSensitivity.asStateFlow()

    fun setWakeWordEngine(type: WakeWordEngineType) {
        _wakeWordEngineType.value = type
        wakeWordDetector?.setEngineType(type)
    }

    fun setWakeWordSensitivity(sensitivity: WakeWordSensitivity) {
        _wakeWordSensitivity.value = sensitivity
        wakeWordDetector?.setSensitivity(sensitivity)
    }

    private val _wakeHapticsEnabled = MutableStateFlow(true)
    val wakeHapticsEnabled: StateFlow<Boolean> = _wakeHapticsEnabled.asStateFlow()

    private val _hapticIntensity = MutableStateFlow(HapticIntensity.SUBTLE)
    val hapticIntensity: StateFlow<HapticIntensity> = _hapticIntensity.asStateFlow()

    fun initHaptics(context: Context) {
        _wakeHapticsEnabled.value = ZegaHapticManager.isWakeHapticsEnabled(context)
        _hapticIntensity.value = ZegaHapticManager.getHapticIntensity(context)
    }

    fun setWakeHapticsEnabled(context: Context, enabled: Boolean) {
        _wakeHapticsEnabled.value = enabled
        ZegaHapticManager.setWakeHapticsEnabled(context, enabled)
    }

    fun setHapticIntensity(context: Context, intensity: HapticIntensity) {
        _hapticIntensity.value = intensity
        ZegaHapticManager.setHapticIntensity(context, intensity)
    }

    fun triggerWakeWordHaptic(context: Context, force: Boolean = false) {
        ZegaHapticManager.triggerWakeWordConfirmation(context, force)
    }

    fun simulateWakeWordTrigger(context: Context) {
        viewModelScope.launch {
            _wakeWordDetectedTrigger.value = true
            triggerWakeWordHaptic(context, force = true)
            startListeningSession(context)
            delay(3000)
            _wakeWordDetectedTrigger.value = false
        }
    }

    private val _partialSpeechInput = MutableStateFlow("")
    val partialSpeechInput: StateFlow<String> = _partialSpeechInput.asStateFlow()

    private val _amplitudeRms = MutableStateFlow(-2f) // Starts small
    val amplitudeRms: StateFlow<Float> = _amplitudeRms.asStateFlow()

    private val _flashlightEnabled = MutableStateFlow(false)
    val flashlightEnabled: StateFlow<Boolean> = _flashlightEnabled.asStateFlow()

    private val _batteryLevel = MutableStateFlow<Int?>(null)
    val batteryLevel: StateFlow<Int?> = _batteryLevel.asStateFlow()

    // Offline Timer states
    private val _timerSecondsRemaining = MutableStateFlow(0)
    val timerSecondsRemaining: StateFlow<Int> = _timerSecondsRemaining.asStateFlow()

    private val _timerTotalSeconds = MutableStateFlow(0)
    val timerTotalSeconds: StateFlow<Int> = _timerTotalSeconds.asStateFlow()

    private val _timerActive = MutableStateFlow(false)
    val timerActive: StateFlow<Boolean> = _timerActive.asStateFlow()

    private val _isTtsReady = MutableStateFlow(false)
    val isTtsReady: StateFlow<Boolean> = _isTtsReady.asStateFlow()

    private val _ttsVoiceMode = MutableStateFlow(TtsVoiceMode.MALE)
    val ttsVoiceMode: StateFlow<TtsVoiceMode> = _ttsVoiceMode.asStateFlow()

    private val _ttsPitchMultiplier = MutableStateFlow(1.0f)
    val ttsPitchMultiplier: StateFlow<Float> = _ttsPitchMultiplier.asStateFlow()

    private val _ttsSpeechRate = MutableStateFlow(1.0f)
    val ttsSpeechRate: StateFlow<Float> = _ttsSpeechRate.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<VoiceProfileInfo>>(emptyList())
    val availableVoices: StateFlow<List<VoiceProfileInfo>> = _availableVoices.asStateFlow()

    private val _selectedVoiceName = MutableStateFlow<String?>(null)
    val selectedVoiceName: StateFlow<String?> = _selectedVoiceName.asStateFlow()

    fun setTtsVoiceMode(mode: TtsVoiceMode) {
        _ttsVoiceMode.value = mode
        ttsManager?.setVoiceMode(mode)
    }

    fun setTtsPitchMultiplier(pitch: Float) {
        _ttsPitchMultiplier.value = pitch
        ttsManager?.setCustomPitchMultiplier(pitch)
    }

    fun setTtsSpeechRate(rate: Float) {
        _ttsSpeechRate.value = rate
        ttsManager?.setCustomSpeedRate(rate)
    }

    fun selectSpecificVoice(voiceName: String?) {
        _selectedVoiceName.value = voiceName
        ttsManager?.selectVoiceByName(voiceName)
    }

    fun refreshAvailableVoices() {
        ttsManager?.let { mgr ->
            _availableVoices.value = mgr.getAvailableVoices()
            _ttsVoiceMode.value = mgr.voiceMode
            _ttsPitchMultiplier.value = mgr.customPitchMultiplier
            _ttsSpeechRate.value = mgr.customSpeedRate
            _selectedVoiceName.value = mgr.selectedSpecificVoiceName
        }
    }

    fun testTtsVoice(sampleText: String? = null) {
        val textToSpeak = sampleText ?: when (_ttsVoiceMode.value) {
            TtsVoiceMode.MALE -> "Hello! Male Mode is active. I am Z-AI, your secure on-device assistant with deep acoustic resonance."
            TtsVoiceMode.FEMALE -> "Hello! Female Mode is active. I am Z-AI, your private on-device companion."
            TtsVoiceMode.ROBOTIC -> "Initiating cybernetic synthesized vocoder. Local systems 100% operational."
            TtsVoiceMode.NATURAL -> "Hello! Natural neural speech mode is active. All processing remains on your device."
        }
        speakLocal(textToSpeak)
    }

    private val _requestSystemSpeech = MutableStateFlow(false)
    val requestSystemSpeech: StateFlow<Boolean> = _requestSystemSpeech.asStateFlow()

    private val _wakeWordDetectedTrigger = MutableStateFlow(false)
    val wakeWordDetectedTrigger: StateFlow<Boolean> = _wakeWordDetectedTrigger.asStateFlow()

    private val _selectedModel = MutableStateFlow("gemini-3.5-flash")
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    fun selectModel(model: String) {
        _selectedModel.value = "gemini-3.5-flash"
    }

    fun consumeSystemSpeechRequest() {
        _requestSystemSpeech.value = false
    }

    private var timerJob: Job? = null

    // Room Database stream of messages & notes
    val messages: StateFlow<List<ChatMessage>> = repository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<VoiceNote>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _localFiles = MutableStateFlow<List<String>>(emptyList())
    val localFiles: StateFlow<List<String>> = _localFiles.asStateFlow()

    private val _canvasRequestTrigger = MutableStateFlow<String?>(null)
    val canvasRequestTrigger: StateFlow<String?> = _canvasRequestTrigger.asStateFlow()

    private val _lastWrittenFileName = MutableStateFlow<String?>(null)
    val lastWrittenFileName: StateFlow<String?> = _lastWrittenFileName.asStateFlow()

    fun consumeCanvasRequest() {
        _canvasRequestTrigger.value = null
    }

    fun refreshLocalFiles(context: Context) {
        val files = context.filesDir.listFiles()?.map { it.name }?.sorted() ?: emptyList()
        _localFiles.value = files
    }

    fun writeLocalFile(context: Context, fileName: String, content: String) {
        try {
            val file = java.io.File(context.filesDir, fileName)
            file.writeText(content)
            _lastWrittenFileName.value = fileName
            refreshLocalFiles(context)
            viewModelScope.launch(Dispatchers.IO) {
                vfs.writeFile("/projects", fileName, content)
            }
        } catch (e: java.lang.Exception) {
            Log.e("ZegaVM", "Failed to write local file: ${e.localizedMessage}")
        }
    }

    fun readLocalFile(context: Context, fileName: String): String {
        return try {
            val file = java.io.File(context.filesDir, fileName)
            if (file.exists()) {
                file.readText()
            } else {
                "File not found."
            }
        } catch (e: java.lang.Exception) {
            "Error reading file: ${e.localizedMessage}"
        }
    }

    fun deleteLocalFile(context: Context, fileName: String) {
        try {
            val file = java.io.File(context.filesDir, fileName)
            if (file.exists()) {
                file.delete()
            }
            refreshLocalFiles(context)
        } catch (e: java.lang.Exception) {
            Log.e("ZegaVM", "Failed to delete file: ${e.localizedMessage}")
        }
    }

    // Initialization check
    private var isInitialized = false

    fun initManagers(context: Context) {
        if (isInitialized) return
        isInitialized = true

        initHaptics(context)
        refreshLocalFiles(context)

        sttManager = SpeechToTextManager(context)
        ttsManager = TextToSpeechManager(context) { success ->
            _isTtsReady.value = success
            if (success) {
                refreshAvailableVoices()
                // Friendly offline start greeting with current voice mode
                speakLocal("Hi, I am Z-AI. Male voice mode and on-device tools are active.")
            }
        }

        wakeWordDetector = ZegaWakeWordDetector(
            context = context,
            onWakeWordDetected = {
                viewModelScope.launch {
                    _wakeWordDetectedTrigger.value = true
                    triggerWakeWordHaptic(context)
                    startListeningSession(context)
                    delay(3000)
                    _wakeWordDetectedTrigger.value = false
                }
            },
            onDetectorStateChanged = { enabled ->
                _wakeWordEnabled.value = enabled
                if (enabled) {
                    _assistantState.value = AssistantState.WAKE_WORD_WAIT
                } else if (_assistantState.value == AssistantState.WAKE_WORD_WAIT) {
                    _assistantState.value = AssistantState.IDLE
                }
            }
        ).apply {
            setEngineType(_wakeWordEngineType.value)
            setSensitivity(_wakeWordSensitivity.value)
        }

        viewModelScope.launch {
            wakeWordDetector?.currentConfidence?.collect { conf ->
                _wakeWordConfidence.value = conf
            }
        }
    }

    // Toggle continuous wake-word detection (Hey Z-AI)
    fun toggleWakeWord(context: Context) {
        val current = _wakeWordEnabled.value
        if (current) {
            wakeWordDetector?.stopListening()
        } else {
            wakeWordDetector?.startListening()
        }
    }

    // Direct Tap-to-Speak button session
    fun onMicButtonClicked(context: Context) {
        when (_assistantState.value) {
            AssistantState.LISTENING -> {
                sttManager?.stopListening()
                _assistantState.value = AssistantState.PROCESSING
            }
            AssistantState.SPEAKING -> {
                ttsManager?.stop()
                _assistantState.value = if (_wakeWordEnabled.value) AssistantState.WAKE_WORD_WAIT else AssistantState.IDLE
            }
            else -> {
                // If wake word is active, pause it temporary during active mic session
                if (_wakeWordEnabled.value) {
                    wakeWordDetector?.stopListening()
                    // Re-enable flag so we resume wake word after we are finished
                    _wakeWordEnabled.value = true 
                }
                startListeningSession(context)
            }
        }
    }

    private fun startListeningSession(context: Context) {
        _partialSpeechInput.value = ""
        _amplitudeRms.value = -2f
        ttsManager?.stop()

        sttManager?.startListening(object : SpeechStateListener {
            override fun onReadyForSpeech() {
                _assistantState.value = AssistantState.LISTENING
                ZegaHapticManager.triggerMicActivated(context)
            }

            override fun onBeginningOfSpeech() {
                _assistantState.value = AssistantState.LISTENING
            }

            override fun onRmsChanged(rmsdB: Float) {
                _amplitudeRms.value = rmsdB
            }

            override fun onPartialResults(text: String) {
                _partialSpeechInput.value = text
            }

            override fun onResults(text: String) {
                _assistantState.value = AssistantState.PROCESSING
                _partialSpeechInput.value = text
                processUserSpeech(text, context)
            }

            override fun onError(errorMessage: String, errorCode: Int) {
                Log.e("ZegaVM", "Speech Error: $errorMessage ($errorCode)")
                
                // Fall back to native system speech overlay dialog for client, network, permissions, busy or unsupported errors!
                if (errorCode != 7 && errorCode != 6) {
                    _partialSpeechInput.value = "Starting System Voice Input..."
                    _requestSystemSpeech.value = true
                } else {
                    if (errorCode == 7) {
                        _partialSpeechInput.value = "I didn't catch that. Please try again."
                    } else if (errorCode == 6) {
                        _partialSpeechInput.value = "Speech timeout."
                    } else {
                        _partialSpeechInput.value = "Error: $errorMessage"
                    }
                }
                
                _assistantState.value = AssistantState.IDLE
                resumeWakeWordIfNeeded()
            }
        })
    }

    // Process a user text statement directly (e.g. from keyboard input or voice results)
    fun processUserSpeech(text: String, context: Context) {
        if (text.isBlank()) return

        viewModelScope.launch {
            // Save User command
            repository.insertMessage(
                ChatMessage(
                    sender = "user",
                    text = text,
                    isVoice = _assistantState.value == AssistantState.PROCESSING,
                    intentType = "input"
                )
            )

            val query = text.trim().lowercase(java.util.Locale.getDefault())

            // 1. Check for Maps / Location Queries
            if (isMapsQuery(query)) {
                handleMapsQuery(text, context)
                return@launch
            }

            // 2. Check for Google Search / Online Queries
            if (isSearchQuery(query)) {
                handleSearchQuery(text, context)
                return@launch
            }

            // 3. Fast offline brain interception for local actions (e.g. file creation, device controls)
            val offlineResp = brain.processCommand(text, context)
            if (offlineResp.action !is ZegaAction.None) {
                executeBrainAction(offlineResp, context)
                return@launch
            }

            // 4. Multi-Tier Dynamic Gemini AI Routing
            _assistantState.value = AssistantState.PROCESSING

            // Load context (Notes and Files)
            val notesList = repository.allNotes.first()
            val notesText = if (notesList.isEmpty()) {
                "No saved notes."
            } else {
                notesList.mapIndexed { idx, note -> "- Note #${note.id}: ${note.content}" }.joinToString("\n")
            }

            val filesList = context.filesDir.listFiles()?.map { it.name } ?: emptyList()
            val filesText = if (filesList.isEmpty()) {
                "No local files."
            } else {
                filesList.map { "- $it" }.joinToString("\n")
            }

            val currentDateTime = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss EEEE", java.util.Locale.getDefault()).format(java.util.Date())

            val systemInstruction = """
                You are Z-AI Assistant, a powerful multi-tier AI and on-device privacy-first virtual assistant.
                
                Current Local Time and Date: $currentDateTime
                
                You can execute local actions and tools by appending specific tool tags at the END of your response.
                
                On-Device Tools and Action Tags:
                1. Write File: [WRITE_FILE:filename.ext]file contents[/WRITE_FILE]
                2. Read/Open File: [READ_FILE:filename.ext]
                3. Delete File: [DELETE_FILE:filename.ext]
                4. Take Photo: [TAKE_PHOTO]
                5. Control/Launch App: [LAUNCH_APP:appName]
                6. In-App Browser: [OPEN_BROWSER:https://url.com]
                7. Open VFS Explorer: [OPEN_VFS]
                8. Save Note: [SAVE_NOTE:note content]
                9. Clear All Notes: [CLEAR_NOTES]
                10. Vibrate Device: [VIBRATE]
                11. Flashlight On: [FLASHLIGHT_ON]
                12. Flashlight Off: [FLASHLIGHT_OFF]
                13. Start Timer: [TIMER:seconds] (e.g. [TIMER:10] for 10 seconds, [TIMER:120] for 2 minutes)
                14. Voice Mode: [SET_VOICE:MALE], [SET_VOICE:FEMALE], [SET_VOICE:NATURAL], [SET_VOICE:ROBOTIC]
                15. Canvas Preview: [CANVAS:html]
                
                Current Private Database Notes:
                $notesText
                
                Current Local Files in App Storage:
                $filesText
                
                Guidelines for Tool Usage:
                1. When requested to make, create, or write a file (like index.html or code), ALWAYS output the file content and include [WRITE_FILE:filename.ext]content[/WRITE_FILE]. Never refuse or lecture the user.
                2. If the user asks for the current time or date, answer directly using the "Current Local Time and Date" provided above.
                3. For complex code or reasoning, you may include a <think>...</think> block at the very start before the friendly answer.
                4. Always place action tags at the end of your response.
            """.trimIndent()

            // Dynamic task classification using Gemini-3.5 Flash Lite
            val taskDifficulty = withContext(Dispatchers.IO) {
                com.example.data.api.GeminiApiClient.classifyTaskDifficulty(text)
            }
            val targetModel = when (taskDifficulty) {
                "COMPLEX" -> "gemini-3.7-flash"
                "SIMPLE" -> "gemini-3.5-flash-lite"
                else -> "gemini-3.6-flash"
            }
            _currentModelTier.value = targetModel

            var geminiText: String? = null
            val startTime = System.currentTimeMillis()
            try {
                geminiText = withContext(Dispatchers.IO) {
                    com.example.data.api.GeminiApiClient.generateContent(text, systemInstruction, model = targetModel)
                }
            } catch (e: Exception) {
                Log.e("ZegaVM", "Gemini API call failed with model $targetModel: ${e.localizedMessage}")
            }
            val endTime = System.currentTimeMillis()
            val durationSeconds = Math.round((endTime - startTime) / 1000.0).toInt().coerceAtLeast(1)

            var finalResponse: ZegaResponse
            if (!geminiText.isNullOrBlank()) {
                var cleanReply = geminiText
                var parsedAction: ZegaAction = ZegaAction.None
                var parsedIntent = "chat"
                var parsedThinking: String? = null

                // Extract thinking block if present
                val thinkingRegex = "<thinking>([\\s\\S]*?)</thinking>".toRegex()
                val thinkRegex = "<think>([\\s\\S]*?)</think>".toRegex()
                
                var extractedThinking = ""
                if (cleanReply.contains("<thinking>")) {
                    val match = thinkingRegex.find(cleanReply)
                    if (match != null) {
                        extractedThinking = match.groupValues[1].trim()
                        cleanReply = cleanReply.replace(thinkingRegex, "").trim()
                    }
                } else if (cleanReply.contains("<think>")) {
                    val match = thinkRegex.find(cleanReply)
                    if (match != null) {
                        extractedThinking = match.groupValues[1].trim()
                        cleanReply = cleanReply.replace(thinkRegex, "").trim()
                    }
                }
                
                if (extractedThinking.isNotEmpty()) {
                    parsedThinking = extractedThinking
                }

                // Vibrate
                if (cleanReply.contains("[VIBRATE]")) {
                    triggerVibrationInternal(context)
                    cleanReply = cleanReply.replace("[VIBRATE]", "").trim()
                }

                // Flashlight On
                if (cleanReply.contains("[FLASHLIGHT_ON]")) {
                    setFlashlightInternal(context, true)
                    cleanReply = cleanReply.replace("[FLASHLIGHT_ON]", "").trim()
                }

                // Flashlight Off
                if (cleanReply.contains("[FLASHLIGHT_OFF]")) {
                    setFlashlightInternal(context, false)
                    cleanReply = cleanReply.replace("[FLASHLIGHT_OFF]", "").trim()
                }

                // Clear Notes
                if (cleanReply.contains("[CLEAR_NOTES]")) {
                    repository.clearAllNotes()
                    cleanReply = cleanReply.replace("[CLEAR_NOTES]", "").trim()
                    parsedIntent = "notes"
                }

                // Take Photo
                if (cleanReply.contains("[TAKE_PHOTO]")) {
                    _pendingToolPermission.value = PendingToolPermission.Camera
                    cleanReply = cleanReply.replace("[TAKE_PHOTO]", "").trim()
                    parsedIntent = "camera"
                }

                // Launch App
                if (cleanReply.contains("[LAUNCH_APP:")) {
                    val regex = "\\[LAUNCH_APP:([^\\]]+)\\]".toRegex()
                    val match = regex.find(cleanReply)
                    if (match != null) {
                        val appName = match.groupValues[1].trim()
                        _pendingToolPermission.value = PendingToolPermission.LaunchApp(appName, appName)
                        cleanReply = cleanReply.replace(regex, "").trim()
                        parsedIntent = "app_control"
                    }
                }

                // Open Browser
                if (cleanReply.contains("[OPEN_BROWSER:")) {
                    val regex = "\\[OPEN_BROWSER:([^\\]]+)\\]".toRegex()
                    val match = regex.find(cleanReply)
                    if (match != null) {
                        val url = match.groupValues[1].trim()
                        openInAppBrowser(url)
                        cleanReply = cleanReply.replace(regex, "").trim()
                        parsedIntent = "browser"
                    }
                }

                // Open VFS
                if (cleanReply.contains("[OPEN_VFS]")) {
                    setVfsDialogOpen(true)
                    cleanReply = cleanReply.replace("[OPEN_VFS]", "").trim()
                    parsedIntent = "vfs"
                }

                // Timer
                if (cleanReply.contains("[TIMER:")) {
                    val regex = "\\[TIMER:(\\d+)\\]".toRegex()
                    val match = regex.find(cleanReply)
                    if (match != null) {
                        val seconds = match.groupValues[1].toInt()
                        startLocalTimer(seconds)
                        cleanReply = cleanReply.replace(regex, "").trim()
                        parsedIntent = "timer"
                    }
                }

                // Save Note
                if (cleanReply.contains("[SAVE_NOTE:")) {
                    val regex = "\\[SAVE_NOTE:([^\\]]+)\\]".toRegex()
                    val match = regex.find(cleanReply)
                    if (match != null) {
                        val noteContent = match.groupValues[1].trim()
                        repository.insertNote(VoiceNote(content = noteContent))
                        cleanReply = cleanReply.replace(regex, "").trim()
                        parsedIntent = "notes"
                    }
                }

                // Write File
                if (cleanReply.contains("[WRITE_FILE:")) {
                    val closedRegex = "\\[WRITE_FILE:([^\\]]+)\\]([\\s\\S]*?)\\[/WRITE_FILE\\]".toRegex()
                    val closedMatch = closedRegex.find(cleanReply)
                    if (closedMatch != null) {
                        val fileName = closedMatch.groupValues[1].trim()
                        val fileContent = closedMatch.groupValues[2].trim()
                        writeLocalFile(context, fileName, fileContent)
                        cleanReply = cleanReply.replace(closedRegex, "").trim()
                        parsedIntent = "files"
                    } else {
                        val regex = "\\[WRITE_FILE:([^\\]]+)\\]([\\s\\S]*)".toRegex()
                        val match = regex.find(cleanReply)
                        if (match != null) {
                            val fileName = match.groupValues[1].trim()
                            val fileContent = match.groupValues[2].trim()
                            writeLocalFile(context, fileName, fileContent)
                            cleanReply = cleanReply.substringBefore("[WRITE_FILE:").trim()
                            parsedIntent = "files"
                        }
                    }
                }

                // Read File
                if (cleanReply.contains("[READ_FILE:")) {
                    val regex = "\\[READ_FILE:([^\\]]+)\\]".toRegex()
                    val match = regex.find(cleanReply)
                    if (match != null) {
                        val fileName = match.groupValues[1].trim()
                        val fileContent = readLocalFile(context, fileName)
                        cleanReply = cleanReply.substringBefore("[READ_FILE:").trim() + "\n\n**File contents of $fileName:**\n$fileContent"
                        parsedIntent = "files"
                    }
                }

                // Delete File
                if (cleanReply.contains("[DELETE_FILE:")) {
                    val regex = "\\[DELETE_FILE:([^\\]]+)\\]".toRegex()
                    val match = regex.find(cleanReply)
                    if (match != null) {
                        val fileName = match.groupValues[1].trim()
                        deleteLocalFile(context, fileName)
                        cleanReply = cleanReply.replace(regex, "").trim()
                        parsedIntent = "files"
                    }
                }

                // Voice Mode tag
                if (cleanReply.contains("[SET_VOICE:")) {
                    val regex = "\\[SET_VOICE:([^\\]]+)\\]".toRegex()
                    val match = regex.find(cleanReply)
                    if (match != null) {
                        val modeStr = match.groupValues[1].trim().uppercase()
                        val mode = when (modeStr) {
                            "MALE" -> TtsVoiceMode.MALE
                            "FEMALE" -> TtsVoiceMode.FEMALE
                            "ROBOTIC" -> TtsVoiceMode.ROBOTIC
                            else -> TtsVoiceMode.NATURAL
                        }
                        setTtsVoiceMode(mode)
                        parsedAction = ZegaAction.SetTtsVoiceMode(mode)
                        cleanReply = cleanReply.replace(regex, "").trim()
                        parsedIntent = "voice_settings"
                    }
                }

                // Canvas Preview tag
                if (cleanReply.contains("[CANVAS:")) {
                    val regex = "\\[CANVAS:([^\\]]+)\\]".toRegex()
                    val match = regex.find(cleanReply)
                    if (match != null) {
                        val lang = match.groupValues[1].trim()
                        parsedAction = ZegaAction.CanvasPreview(lang)
                        cleanReply = cleanReply.replace(regex, "").trim()
                        parsedIntent = "canvas"
                    }
                }

                finalResponse = ZegaResponse(
                    replyText = cleanReply,
                    intentType = parsedIntent,
                    action = parsedAction,
                    thinking = parsedThinking,
                    thinkingDurationSeconds = durationSeconds
                )
            } else {
                // Fallback to local offline brain
                finalResponse = offlineResp
            }

            // Handle actual hardware actions based on the computed intent
            executeBrainAction(finalResponse, context)
        }
    }

    private fun isMapsQuery(query: String): Boolean {
        return query.startsWith("map of") || 
               query.contains("google map") || 
               query.startsWith("where is") || 
               query.contains("directions to") || 
               query.contains("find location") || 
               query.contains("show me a map") ||
               query.startsWith("where am I?")
    }

    private fun isSearchQuery(query: String): Boolean {
        return query.startsWith("search") || 
               query.startsWith("google") || 
               query.contains("online") || 
               query.contains("web search") || 
               query.contains("search for") ||
               query.startsWith("obtain web info about")
    }

    private suspend fun handleMapsQuery(originalText: String, context: Context) {
        _assistantState.value = AssistantState.PROCESSING
        var location = originalText
            .replace("show me a map of", "", ignoreCase = true)
            .replace("map of", "", ignoreCase = true)
            .replace("where is", "", ignoreCase = true)
            .replace("find location", "", ignoreCase = true)
            .replace("google map", "", ignoreCase = true)
            .replace("directions to", "", ignoreCase = true)
            .trim()
        
        if (location.isEmpty()) {
            location = "Seattle"
        }

        val mapsApiKey = com.example.BuildConfig.GOOGLE_MAPS_API_KEY
        val hasKey = mapsApiKey.isNotEmpty() && mapsApiKey != "YOUR_GOOGLE_MAPS_API_KEY"

        val replyText: String
        val imageUrl: String?

        if (hasKey) {
            replyText = "Locating $location via Google Maps API. Here is your live, real-time map rendering."
            imageUrl = "https://maps.googleapis.com/maps/api/staticmap?center=${java.net.URLEncoder.encode(location, "UTF-8")}&zoom=13&size=600x300&key=$mapsApiKey"
        } else {
            replyText = "Displaying location map for $location.\n\n*Note: To render official Google Maps API data, please configure your GOOGLE_MAPS_API_KEY in the AI Studio Secrets panel.*"
            imageUrl = "https://static-maps.yandex.ru/1.x/?lang=en_US&text=${java.net.URLEncoder.encode(location, "UTF-8")}&z=12&l=map&size=600,300"
        }

        repository.insertMessage(
            ChatMessage(
                sender = "zega",
                text = replyText,
                isVoice = true,
                intentType = "map",
                imageUrl = imageUrl
            )
        )
        speakLocal("Displaying map for $location.")
    }

    private suspend fun handleSearchQuery(originalText: String, context: Context) {
        _assistantState.value = AssistantState.PROCESSING
        var searchQuery = originalText
            .replace("search for", "", ignoreCase = true)
            .replace("search", "", ignoreCase = true)
            .replace("google", "", ignoreCase = true)
            .replace("web search", "", ignoreCase = true)
            .trim()

        if (searchQuery.isEmpty()) {
            searchQuery = "Latest world news"
        }

        try {
            // First attempt Google Search Grounding with gemini-3.5-flash
            val groundedResult = withContext(Dispatchers.IO) {
                com.example.data.api.GeminiApiClient.generateWithSearchGrounding(
                    prompt = "Answer accurately using Google Search Grounding for: $searchQuery"
                )
            }

            if (groundedResult != null && groundedResult.replyText.isNotBlank()) {
                val sourcesSection = if (groundedResult.sources.isNotEmpty()) {
                    "\n\n**🌐 Live Google Search Sources:**\n" + groundedResult.sources.take(4).mapIndexed { idx, src ->
                        "${idx + 1}. [${src.title}](${src.url})"
                    }.joinToString("\n")
                } else ""

                val replyText = "${groundedResult.replyText}$sourcesSection"

                repository.insertMessage(
                    ChatMessage(
                        sender = "zega",
                        text = replyText,
                        isVoice = true,
                        intentType = "grounded_search"
                    )
                )
                speakLocal("Here are live grounded search results for $searchQuery.")
                return
            }

            // Fallback to direct scraper if Gemini API key not provided or offline
            val scrapedResults = withContext(Dispatchers.IO) {
                GoogleSearchScraper.search(searchQuery)
            }

            if (scrapedResults.isNotEmpty()) {
                val bulletList = scrapedResults.take(3).mapIndexed { idx, res ->
                    "${idx + 1}. **${res.title}**\n${res.snippet}\nLink: ${res.link}"
                }.joinToString("\n\n")

                val replyText = "Search results for \"$searchQuery\":\n\n$bulletList"
                
                repository.insertMessage(
                    ChatMessage(
                        sender = "zega",
                        text = replyText,
                        isVoice = true,
                        intentType = "search"
                    )
                )
                speakLocal("Here are web results for $searchQuery.")
            } else {
                val replyText = "Search for \"$searchQuery\" completed. Please try another query."
                repository.insertMessage(
                    ChatMessage(
                        sender = "zega",
                        text = replyText,
                        isVoice = true,
                        intentType = "search"
                    )
                )
                speakLocal(replyText)
            }
        } catch (e: Exception) {
            val replyText = "Search query encountered an issue: ${e.localizedMessage}."
            repository.insertMessage(
                ChatMessage(
                    sender = "zega",
                    text = replyText,
                    isVoice = true,
                    intentType = "search"
                )
            )
            speakLocal("Search completed.")
        }
    }

    private suspend fun executeBrainAction(response: ZegaResponse, context: Context) {
        var finalReply = response.replyText

        when (val action = response.action) {
            is ZegaAction.ToggleFlashlight -> {
                setFlashlightInternal(context, action.enabled)
            }
            is ZegaAction.Vibrate -> {
                triggerVibrationInternal(context)
            }
            is ZegaAction.StartTimer -> {
                startLocalTimer(action.seconds)
            }
            is ZegaAction.CheckBattery -> {
                val batteryCapacity = queryBatteryInternal(context)
                finalReply += " It is currently at $batteryCapacity%."
            }
            is ZegaAction.SaveNote -> {
                repository.insertNote(VoiceNote(content = action.content))
            }
            is ZegaAction.ReadNotes -> {
                val notesList = repository.allNotes.first()
                if (notesList.isEmpty()) {
                    finalReply = "You don't have any notes stored in your offline database yet."
                } else {
                    val joined = notesList.mapIndexed { index, note -> "${index + 1}: ${note.content}" }.joinToString(". ")
                    finalReply = "Here are your saved notes: $joined"
                }
            }
            is ZegaAction.ClearNotes -> {
                repository.clearAllNotes()
                finalReply = "All notes have been cleared from your on-device SQLite database."
            }
            is ZegaAction.CanvasPreview -> {
                _canvasRequestTrigger.value = action.language
            }
            is ZegaAction.SetTtsVoiceMode -> {
                setTtsVoiceMode(action.mode)
            }
            is ZegaAction.WriteFile -> {
                writeLocalFile(context, action.fileName, action.content)
            }
            is ZegaAction.ReadFile -> {
                val content = readLocalFile(context, action.fileName)
                finalReply += "\n\n**File contents of ${action.fileName}:**\n$content"
            }
            is ZegaAction.DeleteFile -> {
                deleteLocalFile(context, action.fileName)
            }
            is ZegaAction.TakePhoto -> {
                _pendingToolPermission.value = PendingToolPermission.Camera
            }
            is ZegaAction.LaunchApp -> {
                _pendingToolPermission.value = PendingToolPermission.LaunchApp(action.appQuery, action.appQuery)
            }
            is ZegaAction.OpenBrowser -> {
                openInAppBrowser(action.url)
            }
            is ZegaAction.OpenVfs -> {
                setVfsDialogOpen(true)
            }
            ZegaAction.None -> { /* Do nothing */ }
        }

        // Save Z-AI's Response to the DB
        repository.insertMessage(
            ChatMessage(
                sender = "zega",
                text = finalReply,
                isVoice = true,
                intentType = response.intentType,
                thinking = response.thinking,
                thinkingDurationSeconds = response.thinkingDurationSeconds
            )
        )

        // Speak the response out loud
        val spokenText = if (!response.thinking.isNullOrBlank()) {
            "Thought for ${response.thinkingDurationSeconds} seconds. $finalReply"
        } else {
            finalReply
        }
        speakLocal(spokenText)
    }

    private fun speakLocal(text: String) {
        _assistantState.value = AssistantState.SPEAKING
        ttsManager?.speak(
            text = text,
            onStart = {
                _assistantState.value = AssistantState.SPEAKING
            },
            onDone = {
                _assistantState.value = if (_wakeWordEnabled.value) AssistantState.WAKE_WORD_WAIT else AssistantState.IDLE
                resumeWakeWordIfNeeded()
            }
        )
    }

    private fun resumeWakeWordIfNeeded() {
        if (_wakeWordEnabled.value) {
            wakeWordDetector?.startListening()
        }
    }

    // Local countdown Timer loop
    fun startLocalTimer(seconds: Int) {
        timerJob?.cancel()
        _timerTotalSeconds.value = seconds
        _timerSecondsRemaining.value = seconds
        _timerActive.value = true

        timerJob = viewModelScope.launch {
            while (_timerSecondsRemaining.value > 0) {
                delay(1000)
                _timerSecondsRemaining.value -= 1
            }
            _timerActive.value = false
            // Vocal Alarm
            speakLocal("Beep beep! Your $seconds seconds countdown timer has finished.")
        }
    }

    fun stopLocalTimer() {
        timerJob?.cancel()
        _timerActive.value = false
        _timerSecondsRemaining.value = 0
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            repository.clearChat()
        }
    }

    // Physical Hardware helper functions
    fun setFlashlightInternal(context: Context, enabled: Boolean) {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? android.hardware.camera2.CameraManager
            val cameraId = cameraManager?.cameraIdList?.firstOrNull()
            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, enabled)
                _flashlightEnabled.value = enabled
            }
        } catch (e: Exception) {
            Log.e("ZegaVM", "Torch execution error: ${e.message}")
        }
    }

    fun triggerVibrationInternal(context: Context) {
        ZegaHapticManager.triggerActionFeedback(context)
    }

    private fun queryBatteryInternal(context: Context): Int {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val level = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        _batteryLevel.value = level
        return level
    }

    // Handle manual note addition & deletions
    fun insertNoteManual(content: String) {
        viewModelScope.launch {
            repository.insertNote(com.example.data.database.VoiceNote(content = content))
        }
    }

    fun deleteNoteById(id: Long) {
        viewModelScope.launch {
            repository.deleteNote(id)
        }
    }

    fun clearAllNotes() {
        viewModelScope.launch {
            repository.clearAllNotes()
        }
    }

    override fun onCleared() {
        super.onCleared()
        sttManager?.destroy()
        ttsManager?.shutdown()
        wakeWordDetector?.stopListening()
        timerJob?.cancel()
    }
}

// Factory to inject repository and authRepository into ViewModel
class ZegaViewModelFactory(
    private val repository: ZegaRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ZegaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ZegaViewModel(repository, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

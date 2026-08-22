package com.example.data.brain

import android.content.Context
import com.example.data.voice.TtsVoiceMode
import java.util.Locale
import kotlin.random.Random

sealed class ZegaAction {
    object None : ZegaAction()
    data class ToggleFlashlight(val enabled: Boolean) : ZegaAction()
    object Vibrate : ZegaAction()
    data class StartTimer(val seconds: Int) : ZegaAction()
    object CheckBattery : ZegaAction()
    data class SaveNote(val content: String) : ZegaAction()
    object ReadNotes : ZegaAction()
    object ClearNotes : ZegaAction()
    data class CanvasPreview(val language: String) : ZegaAction()
    data class SetTtsVoiceMode(val mode: TtsVoiceMode) : ZegaAction()
    data class WriteFile(val fileName: String, val content: String) : ZegaAction()
    data class ReadFile(val fileName: String) : ZegaAction()
    data class DeleteFile(val fileName: String) : ZegaAction()
    object TakePhoto : ZegaAction()
    data class LaunchApp(val appQuery: String) : ZegaAction()
    data class OpenBrowser(val url: String) : ZegaAction()
    object OpenVfs : ZegaAction()
}

data class ZegaResponse(
    val replyText: String,
    val intentType: String,
    val action: ZegaAction = ZegaAction.None,
    val thinking: String? = null,
    val thinkingDurationSeconds: Int = 0
)

class ZegaOfflineBrain {

    val systemPrompt = """
        You are Z-AI Assistant, a local, privacy-first virtual assistant.
        
        System Instruction:
        - You are Z-AI, your private, secure local assistant.
        - You process all speech and commands with multi-tier Gemini intelligence and local device tools.
        - When requested to create or write files, ALWAYS perform the file creation immediately with the exact requested content.
        
        On-Device Tools and Action Tags:
        1. Write File: [WRITE_FILE:filename.ext]file contents[/WRITE_FILE]
        2. Read File: [READ_FILE:filename.ext]
        3. Delete File: [DELETE_FILE:filename.ext]
        4. Save Note: [SAVE_NOTE:note content]
        5. Clear Notes: [CLEAR_NOTES]
        6. Vibrate Device: [VIBRATE]
        7. Flashlight On: [FLASHLIGHT_ON]
        8. Flashlight Off: [FLASHLIGHT_OFF]
        9. Start Timer: [TIMER:seconds]
        10. Take Photo: [TAKE_PHOTO]
        11. Control/Launch App: [LAUNCH_APP:appName]
        12. In-App Browser: [OPEN_BROWSER:url]
        13. Open VFS Explorer: [OPEN_VFS]
        14. Voice Mode: [SET_VOICE:MALE], [SET_VOICE:FEMALE], [SET_VOICE:NATURAL], [SET_VOICE:ROBOTIC]
    """.trimIndent()

    fun processCommand(input: String, context: Context? = null): ZegaResponse {
        val lower = input.trim().lowercase(Locale.getDefault())

        // 1. Direct File Creation Check (e.g., "Make an index.html file with <a>Hello World</a> inside")
        val fileCreationRegex = "(?:make|create|write)\\s+(?:an?\\s+)?(?:file\\s+)?([a-zA-Z0-9_.-]+)(?:\\s+file)?\\s+(?:with|containing|having|holding)\\s+([\\s\\S]+)".toRegex(RegexOption.IGNORE_CASE)
        val fileCreationMatch = fileCreationRegex.find(input.trim())
        if (fileCreationMatch != null) {
            val rawFileName = fileCreationMatch.groupValues[1].trim()
            var rawContent = fileCreationMatch.groupValues[2].trim()
            if (rawContent.endsWith("inside", ignoreCase = true)) {
                rawContent = rawContent.substring(0, rawContent.length - 6).trim()
            }
            if (rawContent.startsWith("\"") && rawContent.endsWith("\"") && rawContent.length > 2) {
                rawContent = rawContent.substring(1, rawContent.length - 1)
            } else if (rawContent.startsWith("'") && rawContent.endsWith("'") && rawContent.length > 2) {
                rawContent = rawContent.substring(1, rawContent.length - 1)
            }

            val fileType = when {
                rawFileName.endsWith(".html") -> "html"
                rawFileName.endsWith(".js") -> "javascript"
                rawFileName.endsWith(".css") -> "css"
                rawFileName.endsWith(".json") -> "json"
                rawFileName.endsWith(".md") -> "markdown"
                rawFileName.endsWith(".py") -> "python"
                rawFileName.endsWith(".kt") -> "kotlin"
                else -> "text"
            }

            val artifactContent = """
                ```$fileType
                $rawContent
                ```
            """.trimIndent()

            return ZegaResponse(
                replyText = "Created virtual file `$rawFileName` in your Virtual File System with your requested content.\n\n$artifactContent\n\n[WRITE_FILE:$rawFileName]$rawContent[/WRITE_FILE]",
                intentType = "files",
                action = ZegaAction.WriteFile(rawFileName, rawContent)
            )
        }

        // 2. Camera / Take Photo Check
        if (lower.contains("take a photo") || lower.contains("take photo") || lower.contains("take picture") ||
            lower.contains("capture photo") || lower.contains("open camera") || lower.contains("snap picture")) {
            return ZegaResponse(
                replyText = "Opening Camera capture tool. Please grant camera permission if prompted.",
                intentType = "camera",
                action = ZegaAction.TakePhoto
            )
        }

        // 3. App Controlling / Launching Check
        if (lower.startsWith("open app") || lower.startsWith("launch app") || lower.startsWith("open ") || lower.startsWith("launch ")) {
            val appQuery = input
                .replace("open app", "", ignoreCase = true)
                .replace("launch app", "", ignoreCase = true)
                .replace("open", "", ignoreCase = true)
                .replace("launch", "", ignoreCase = true)
                .trim()
            if (appQuery.isNotEmpty() && !appQuery.equals("camera", ignoreCase = true) && !appQuery.equals("browser", ignoreCase = true) && !appQuery.equals("vfs", ignoreCase = true)) {
                return ZegaResponse(
                    replyText = "Requesting permission to launch app: $appQuery.",
                    intentType = "app_control",
                    action = ZegaAction.LaunchApp(appQuery)
                )
            }
        }

        // 4. In-App Browser Check
        if (lower.contains("open browser") || lower.contains("in-app browser") || lower.startsWith("browse ")) {
            val url = input
                .replace("open browser", "", ignoreCase = true)
                .replace("in-app browser", "", ignoreCase = true)
                .replace("browse", "", ignoreCase = true)
                .trim()
            val targetUrl = if (url.startsWith("http://") || url.startsWith("https://")) url else if (url.isNotEmpty()) "https://$url" else "https://google.com"
            return ZegaResponse(
                replyText = "Launching In-App AI Sandboxed Browser for $targetUrl.",
                intentType = "browser",
                action = ZegaAction.OpenBrowser(targetUrl)
            )
        }

        // 5. Open VFS Check
        if (lower.contains("open vfs") || lower.contains("file manager") || lower.contains("virtual file system") || lower.contains("explore files")) {
            return ZegaResponse(
                replyText = "Opening Virtual File System (VFS) explorer.",
                intentType = "vfs",
                action = ZegaAction.OpenVfs
            )
        }

        // Voice mode switching checks
        if (lower.contains("male mode") || lower.contains("male voice") || lower.contains("set voice to male") || 
            lower.contains("switch to male") || lower.contains("change voice to male") || lower.contains("use male voice") ||
            lower.contains("man voice") || lower.contains("speak like a man")) {
            return ZegaResponse(
                replyText = "Switching speech synthesis to Male Mode with deep acoustic resonance.",
                intentType = "voice_settings",
                action = ZegaAction.SetTtsVoiceMode(TtsVoiceMode.MALE)
            )
        }

        if (lower.contains("female mode") || lower.contains("female voice") || lower.contains("set voice to female") || 
            lower.contains("switch to female") || lower.contains("change voice to female") || lower.contains("use female voice") ||
            lower.contains("woman voice") || lower.contains("speak like a woman")) {
            return ZegaResponse(
                replyText = "Switching speech synthesis to Female Mode with clear melodic timbre.",
                intentType = "voice_settings",
                action = ZegaAction.SetTtsVoiceMode(TtsVoiceMode.FEMALE)
            )
        }

        if (lower.contains("robot voice") || lower.contains("robotic mode") || lower.contains("cybernetic voice")) {
            return ZegaResponse(
                replyText = "Switching speech synthesis to Cybernetic Vocoder mode.",
                intentType = "voice_settings",
                action = ZegaAction.SetTtsVoiceMode(TtsVoiceMode.ROBOTIC)
            )
        }

        if (lower.contains("default voice") || lower.contains("natural voice") || lower.contains("reset voice")) {
            return ZegaResponse(
                replyText = "Switching speech synthesis to Natural default mode.",
                intentType = "voice_settings",
                action = ZegaAction.SetTtsVoiceMode(TtsVoiceMode.NATURAL)
            )
        }

        // Flashlight checks
        if (lower.contains("turn on flashlight") || lower.contains("flashlight on") || lower.contains("turn on torch") || lower.contains("torch on")) {
            return ZegaResponse(
                replyText = "Turning on the device flashlight.",
                intentType = "hardware",
                action = ZegaAction.ToggleFlashlight(true)
            )
        }
        if (lower.contains("turn off flashlight") || lower.contains("flashlight off") || lower.contains("turn off torch") || lower.contains("torch off")) {
            return ZegaResponse(
                replyText = "Turning off the device flashlight.",
                intentType = "hardware",
                action = ZegaAction.ToggleFlashlight(false)
            )
        }

        // Vibrate check
        if (lower.contains("vibrate") || lower.contains("buzz")) {
            return ZegaResponse(
                replyText = "Vibrating device haptic feedback motor.",
                intentType = "hardware",
                action = ZegaAction.Vibrate
            )
        }

        // Battery check
        if (lower.contains("battery") || lower.contains("charge level")) {
            return ZegaResponse(
                replyText = "Checking local battery health telemetry.",
                intentType = "hardware",
                action = ZegaAction.CheckBattery
            )
        }

        // Timer checks
        val timerMatch = "timer for (\\d+) (seconds|second|minutes|minute|min|sec)".toRegex().find(lower)
        if (timerMatch != null) {
            val amount = timerMatch.groupValues[1].toIntOrNull() ?: 10
            val unit = timerMatch.groupValues[2]
            val totalSeconds = if (unit.startsWith("min")) amount * 60 else amount
            return ZegaResponse(
                replyText = "Starting a countdown timer for $totalSeconds seconds.",
                intentType = "timer",
                action = ZegaAction.StartTimer(totalSeconds)
            )
        }

        // Save Note check
        if (lower.startsWith("save note") || lower.startsWith("take a note") || lower.startsWith("note down")) {
            val content = input
                .replace("save note", "", ignoreCase = true)
                .replace("take a note", "", ignoreCase = true)
                .replace("note down", "", ignoreCase = true)
                .replace(":", "")
                .trim()
            val noteToSave = if (content.isEmpty()) "Quick note created via Z-AI" else content
            return ZegaResponse(
                replyText = "Saved note: \"$noteToSave\" to your on-device Room database.",
                intentType = "notes",
                action = ZegaAction.SaveNote(noteToSave)
            )
        }

        // Read notes check
        if (lower.contains("read notes") || lower.contains("show notes") || lower.contains("what are my notes")) {
            return ZegaResponse(
                replyText = "Fetching your saved notes from the secure local database.",
                intentType = "notes",
                action = ZegaAction.ReadNotes
            )
        }

        // Clear notes check
        if (lower.contains("clear notes") || lower.contains("delete all notes")) {
            return ZegaResponse(
                replyText = "Purging all saved notes from the local SQLite database.",
                intentType = "notes",
                action = ZegaAction.ClearNotes
            )
        }

        return ZegaResponse(
            replyText = generateLocalGenerativeResponse(input),
            intentType = "chat"
        )
    }

    private fun generateLocalGenerativeResponse(prompt: String): String {
        val lower = prompt.lowercase(Locale.getDefault())
        if (lower.contains("system prompt") || lower.contains("system instruction")) {
            return "My System Prompt:\n\n$systemPrompt"
        }
        if (lower.contains("who are you") || lower.contains("your name") || lower.contains("z-ai") || lower.contains("zega") || lower.contains("z ai")) {
            return "I am Z-AI Assistant, your privacy-first AI companion. I execute multi-tier AI reasoning, file creation in the Virtual File System, camera tasks, and on-device hardware tools."
        }
        if (lower.contains("tool") || lower.contains("action") || lower.contains("command") || lower.contains("tag") || lower.contains("how to use")) {
            return "I support powerful on-device and AI tools:\n\n" +
                   "- Create files & code artifacts: 'Make an index.html file with <a>Hello World</a> inside'\n" +
                   "- Camera capture: 'Take a photo'\n" +
                   "- Control/Launch apps: 'Open YouTube' (requires your permission confirmation)\n" +
                   "- In-App AI Browser: 'Open browser to google.com'\n" +
                   "- VFS File Manager: 'Open VFS'\n" +
                   "- Hardware tools: Flashlight, Vibrations, Timer, Battery telemetry\n" +
                   "- Voice profiles: Male, Female, Natural, Cybernetic Vocoder"
        }

        return "I have processed your request locally. I can generate code artifacts, create files in your Virtual File System, take photos, launch apps with permission, and execute on-device commands."
    }
}


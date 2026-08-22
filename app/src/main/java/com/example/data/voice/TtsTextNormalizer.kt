package com.example.data.voice

import java.util.regex.Pattern

/**
 * High-performance, memory-efficient Text Normalizer and Speech Sanitizer.
 * 
 * Complies with strict pronunciation smoothing and special character sanitization:
 * 1. Sanitizes repeated code symbols (###, ---, ===, ***, ~~~, etc.) into structural pauses/silence.
 * 2. Strips markdown tokens, bullet points, formatting symbols, and code fences.
 * 3. Extracts Non-Spoken Artifact blocks so that code bodies are completely excluded from TTS audio,
 *    announcing only metadata titles.
 */
object TtsTextNormalizer {

    // Regex patterns compiled once for maximum performance across API 17+
    private val ARTIFACT_BLOCK_REGEX = Pattern.compile(
        """\[ARTIFACT:([^:]+):([^\]]+)\]([\s\S]*?)\[/ARTIFACT\]""",
        Pattern.CASE_INSENSITIVE
    )

    private val CODE_FENCE_REGEX = Pattern.compile(
        """```([a-zA-Z0-9_-]*)\s*([\s\S]*?)```"""
    )

    private val THINKING_BLOCK_REGEX = Pattern.compile(
        """<(think|thinking)>([\s\S]*?)</(think|thinking)>""",
        Pattern.CASE_INSENSITIVE
    )

    private val ACTION_TAGS_REGEX = Pattern.compile(
        """\[(WRITE_FILE|READ_FILE|DELETE_FILE|SAVE_NOTE|CLEAR_NOTES|VIBRATE|FLASHLIGHT_ON|FLASHLIGHT_OFF|TIMER|CANVAS|SET_VOICE)[^\]]*\]"""
    )

    private val MARKDOWN_URL_REGEX = Pattern.compile("""\[([^\]]+)\]\(([^)]+)\)""")
    private val RAW_URL_REGEX = Pattern.compile("""https?://[^\s]+""")
    private val REPEATED_CHARS_REGEX = Pattern.compile("""([#\-=*~_`|\\/]){2,}""")
    private val MARKDOWN_HEADINGS_REGEX = Pattern.compile("""^#{1,6}\s*""", Pattern.MULTILINE)
    private val MARKDOWN_BULLETS_REGEX = Pattern.compile("""^[\s]*[-*+]\s+""", Pattern.MULTILINE)
    private val NUMBERED_LIST_REGEX = Pattern.compile("""^[\s]*\d+\.\s+""", Pattern.MULTILINE)
    private val XML_HTML_TAGS_REGEX = Pattern.compile("""<[^>]+>""")
    private val EXCESSIVE_PUNCTUATION_REGEX = Pattern.compile("""([.!?]){2,}""")
    private val INLINE_CODE_REGEX = Pattern.compile("""`([^`]+)`""")

    /**
     * Sanitizes raw conversational or assistant text for natural, smooth Text-to-Speech synthesis.
     * Guarantees that code artifacts are non-spoken and symbols like ### or --- are never read aloud.
     */
    fun normalizeForSpeech(rawText: String): String {
        if (rawText.isBlank()) return ""

        var processed = rawText

        // 1. Strip internal thought blocks
        processed = THINKING_BLOCK_REGEX.matcher(processed).replaceAll("")

        // 2. Extract & Exclude Artifact blocks (Only speak metadata announcement, completely exclude body)
        val artifactMatcher = ARTIFACT_BLOCK_REGEX.matcher(processed)
        val sb = StringBuffer()
        while (artifactMatcher.find()) {
            val title = artifactMatcher.group(1)?.trim() ?: "Artifact"
            val type = artifactMatcher.group(2)?.trim()?.uppercase() ?: "CODE"
            val replacement = " Created $type artifact: $title. "
            artifactMatcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(replacement))
        }
        artifactMatcher.appendTail(sb)
        processed = sb.toString()

        // 3. Exclude standalone large code blocks from speech, summarizing them cleanly
        val codeMatcher = CODE_FENCE_REGEX.matcher(processed)
        val codeSb = StringBuffer()
        while (codeMatcher.find()) {
            val lang = codeMatcher.group(1)?.trim() ?: ""
            val replacement = if (lang.isNotBlank()) " Here is the $lang code snippet. " else " Here is the code snippet. "
            codeMatcher.appendReplacement(codeSb, java.util.regex.Matcher.quoteReplacement(replacement))
        }
        codeMatcher.appendTail(codeSb)
        processed = codeSb.toString()

        // 4. Remove internal hardware action tags ([WRITE_FILE:...], [TIMER:...], etc.)
        processed = ACTION_TAGS_REGEX.matcher(processed).replaceAll("")

        // 5. Convert Markdown Links [Title](URL) -> Title (ignore raw URL)
        processed = MARKDOWN_URL_REGEX.matcher(processed).replaceAll("$1")

        // 6. Strip standalone raw URLs
        processed = RAW_URL_REGEX.matcher(processed).replaceAll("link")

        // 7. Strip HTML/XML tags
        processed = XML_HTML_TAGS_REGEX.matcher(processed).replaceAll("")

        // 8. Replace repeated punctuation / horizontal rules (---, ===, ***, ###) with structural pause
        processed = REPEATED_CHARS_REGEX.matcher(processed).replaceAll(", ")

        // 9. Remove Markdown headings (###, ##, #) from start of lines
        processed = MARKDOWN_HEADINGS_REGEX.matcher(processed).replaceAll("")

        // 10. Smooth bullet points & numbered list prefixes
        processed = MARKDOWN_BULLETS_REGEX.matcher(processed).replaceAll("")
        processed = NUMBERED_LIST_REGEX.matcher(processed).replaceAll("")

        // 11. Normalize inline code `foo()` -> foo
        processed = INLINE_CODE_REGEX.matcher(processed).replaceAll("$1")

        // 12. Remove single stray markdown syntax markers (*, _, ~, `, #, >, |, {, }, [, ])
        processed = processed.replace("*", "")
            .replace("_", " ")
            .replace("~", "")
            .replace("`", "")
            .replace("#", "")
            .replace(">", "")
            .replace("|", ", ")
            .replace("{", "")
            .replace("}", "")
            .replace("[", "")
            .replace("]", "")
            .replace("\\", "")
            .replace("^", "")

        // 13. Smooth excessive punctuation (e.g. "..." -> ".")
        processed = EXCESSIVE_PUNCTUATION_REGEX.matcher(processed).replaceAll("$1")

        // 14. Normalize multiple whitespaces and trim
        processed = processed.replace(Regex("""\s+"""), " ").trim()

        return processed
    }
}

package com.example.data.artifacts

import java.util.regex.Pattern

object ArtifactExtractor {

    private val EXPLICIT_ARTIFACT_REGEX = Pattern.compile(
        """\[ARTIFACT:([^:]+):([^\]]+)\]([\s\S]*?)\[/ARTIFACT\]""",
        Pattern.CASE_INSENSITIVE
    )

    private val CODE_FENCE_REGEX = Pattern.compile(
        """```([a-zA-Z0-9_-]*)\n([\s\S]*?)```"""
    )

    data class ExtractionResult(
        val cleanDisplayText: String,
        val artifacts: List<Artifact>
    )

    /**
     * Parses incoming assistant response text, extracting structured non-spoken Artifacts
     * while preserving clean readable conversational text for the UI.
     */
    fun extractArtifacts(rawText: String): ExtractionResult {
        val artifacts = mutableListOf<Artifact>()
        var text = rawText

        // 1. Match explicit [ARTIFACT:title:type]...[/ARTIFACT] blocks
        val matcher = EXPLICIT_ARTIFACT_REGEX.matcher(text)
        val sb = StringBuffer()
        while (matcher.find()) {
            val title = matcher.group(1)?.trim() ?: "Artifact"
            val typeStr = matcher.group(2)?.trim() ?: "html"
            val content = matcher.group(3)?.trim() ?: ""

            val type = ArtifactType.fromString(typeStr)
            val artifact = Artifact(
                title = title,
                type = type,
                content = content
            )
            artifacts.add(artifact)

            // Replace in display text with a clean placeholder reference
            val placeholder = "\n\n📦 *Artifact: ${artifact.title} (${artifact.type.displayName})*\n"
            matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(placeholder))
        }
        matcher.appendTail(sb)
        text = sb.toString()

        // 2. If no explicit artifact was found but there's an HTML/JS/JSON/SVG/MD code block > 3 lines,
        // convert it into an interactive artifact
        if (artifacts.isEmpty()) {
            val codeMatcher = CODE_FENCE_REGEX.matcher(text)
            var codeIndex = 1
            while (codeMatcher.find()) {
                val lang = codeMatcher.group(1)?.trim()?.lowercase() ?: ""
                val codeContent = codeMatcher.group(2)?.trim() ?: ""

                if (codeContent.lines().size >= 2) {
                    val artifactType = when (lang) {
                        "html", "htm" -> ArtifactType.HTML
                        "javascript", "js" -> ArtifactType.JAVASCRIPT
                        "json" -> ArtifactType.JSON
                        "svg" -> ArtifactType.SVG
                        "css" -> ArtifactType.CSS
                        "markdown", "md" -> ArtifactType.MARKDOWN
                        "python", "py" -> ArtifactType.PYTHON
                        "kotlin", "kt" -> ArtifactType.KOTLIN
                        else -> null
                    }

                    if (artifactType != null) {
                        val title = when (artifactType) {
                            ArtifactType.HTML -> "Web Application Preview"
                            ArtifactType.JAVASCRIPT -> "Interactive Script"
                            ArtifactType.JSON -> "Structured Data Payload"
                            ArtifactType.SVG -> "Vector Graphic Visualizer"
                            ArtifactType.MARKDOWN -> "Formatted Documentation"
                            else -> "Code Component #$codeIndex"
                        }
                        artifacts.add(
                            Artifact(
                                title = title,
                                type = artifactType,
                                content = codeContent
                            )
                        )
                        codeIndex++
                    }
                }
            }
        }

        return ExtractionResult(
            cleanDisplayText = text.trim(),
            artifacts = artifacts
        )
    }
}

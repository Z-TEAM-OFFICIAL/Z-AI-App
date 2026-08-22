package com.example.data.artifacts

import java.util.UUID

enum class ArtifactType(val displayName: String, val extension: String, val isExecutable: Boolean) {
    HTML("HTML / Web App", "html", true),
    JAVASCRIPT("JavaScript", "js", true),
    MARKDOWN("Markdown Document", "md", true),
    JSON("JSON Data", "json", true),
    CSS("CSS Stylesheet", "css", false),
    SVG("SVG Vector Graphics", "svg", true),
    PYTHON("Python Script", "py", false),
    KOTLIN("Kotlin Code", "kt", false),
    CODE("Generic Code", "txt", false);

    companion object {
        fun fromString(typeStr: String): ArtifactType {
            return when (typeStr.lowercase().trim()) {
                "html", "htm" -> HTML
                "javascript", "js" -> JAVASCRIPT
                "markdown", "md" -> MARKDOWN
                "json" -> JSON
                "css" -> CSS
                "svg" -> SVG
                "python", "py" -> PYTHON
                "kotlin", "kt" -> KOTLIN
                else -> CODE
            }
        }
    }
}

data class Artifact(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val type: ArtifactType,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val virtualPath: String = "/artifacts/${title.lowercase().replace(" ", "_")}.${type.extension}"
)

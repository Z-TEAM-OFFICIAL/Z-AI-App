package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "ai_creations")
@JsonClass(generateAdapter = true)
data class AICreation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long = 0,
    val type: String, // "VEO_VIDEO" or "LYRIA_MUSIC"
    val title: String,
    val prompt: String,
    val modelName: String, // "veo-3.1-fast-generate-preview", "lyria-3-clip-preview", "lyria-3-pro-preview"
    val aspectRatio: String = "16:9", // "16:9" or "9:16"
    val durationSeconds: Int = 15,
    val mediaUrl: String? = null,
    val thumbnailBase64: String? = null,
    val genreOrStyle: String = "Cinematic",
    val timestamp: Long = System.currentTimeMillis(),
    val isSyncedToCloud: Boolean = false
)

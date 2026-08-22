package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String, // "user" or "zega"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isVoice: Boolean = false,
    val intentType: String = "chat",
    val imageUrl: String? = null,
    val thinking: String? = null,
    val thinkingDurationSeconds: Int = 0
)

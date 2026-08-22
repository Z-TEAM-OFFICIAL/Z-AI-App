package com.example.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_accounts",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserAccount(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val email: String,
    val displayName: String,
    val passwordHash: String, // Hashed password
    val provider: String = "EMAIL", // "EMAIL", "GOOGLE", "GITHUB", "GUEST"
    val avatarColorHex: String = "#00FFCC", // Neon cyan default
    val bio: String = "Z-AI Local Intelligence User",
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
)

package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ZegaDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clearChat()

    @Query("SELECT * FROM voice_notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<VoiceNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: VoiceNote): Long

    @Query("DELETE FROM voice_notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    @Query("DELETE FROM voice_notes")
    suspend fun clearNotes()

    @Query("SELECT * FROM voice_notes ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastNote(): VoiceNote?

    @Query("SELECT * FROM ai_creations ORDER BY timestamp DESC")
    fun getAllCreations(): Flow<List<AICreation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreation(creation: AICreation): Long

    @Query("DELETE FROM ai_creations WHERE id = :id")
    suspend fun deleteCreationById(id: Long)

    @Query("DELETE FROM ai_creations")
    suspend fun clearCreations()
}

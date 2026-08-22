package com.example.data

import com.example.data.database.ChatMessage
import com.example.data.database.UserAccount
import com.example.data.database.UserAccountDao
import com.example.data.database.VoiceNote
import com.example.data.database.ZegaDao
import com.example.data.vfs.VfsDao
import com.example.data.vfs.VfsNode
import kotlinx.coroutines.flow.Flow

class ZegaRepository(
    private val zegaDao: ZegaDao,
    private val userAccountDao: UserAccountDao,
    val vfsDao: VfsDao
) {
    val allMessages: Flow<List<ChatMessage>> = zegaDao.getAllMessages()
    val allNotes: Flow<List<VoiceNote>> = zegaDao.getAllNotes()
    val allUsers: Flow<List<UserAccount>> = userAccountDao.getAllUsers()
    val allVfsNodes: Flow<List<VfsNode>> = vfsDao.getAllNodesFlow()

    suspend fun insertMessage(message: ChatMessage): Long {
        return zegaDao.insertMessage(message)
    }

    suspend fun clearChat() {
        zegaDao.clearChat()
    }

    suspend fun insertNote(note: VoiceNote): Long {
        return zegaDao.insertNote(note)
    }

    suspend fun deleteNote(id: Long) {
        zegaDao.deleteNoteById(id)
    }

    suspend fun clearAllNotes() {
        zegaDao.clearNotes()
    }

    suspend fun getLastNote(): VoiceNote? {
        return zegaDao.getLastNote()
    }

    suspend fun getUserByEmail(email: String): UserAccount? {
        return userAccountDao.getUserByEmail(email)
    }

    suspend fun getUserById(id: Long): UserAccount? {
        return userAccountDao.getUserById(id)
    }

    suspend fun insertUser(user: UserAccount): Long {
        return userAccountDao.insertUser(user)
    }

    suspend fun insertOrUpdateUser(user: UserAccount): Long {
        return userAccountDao.insertOrUpdate(user)
    }

    suspend fun updateUser(user: UserAccount) {
        userAccountDao.updateUser(user)
    }

    suspend fun deleteUser(id: Long) {
        userAccountDao.deleteUserById(id)
    }

    val allCreations: Flow<List<com.example.data.database.AICreation>> = zegaDao.getAllCreations()

    suspend fun insertCreation(creation: com.example.data.database.AICreation): Long {
        return zegaDao.insertCreation(creation)
    }

    suspend fun deleteCreation(id: Long) {
        zegaDao.deleteCreationById(id)
    }

    suspend fun clearCreations() {
        zegaDao.clearCreations()
    }
}

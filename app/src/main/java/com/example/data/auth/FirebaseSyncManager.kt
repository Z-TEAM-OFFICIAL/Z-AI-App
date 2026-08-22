package com.example.data.auth

import android.content.Context
import android.util.Log
import com.example.data.database.AICreation
import com.example.data.database.ChatMessage
import com.example.data.database.UserAccount
import com.example.data.database.VoiceNote
import com.example.data.database.ZegaDao
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class CloudSyncStatus {
    object Idle : CloudSyncStatus()
    object Syncing : CloudSyncStatus()
    data class Synced(val lastSyncTime: Long) : CloudSyncStatus()
    data class Error(val message: String) : CloudSyncStatus()
}

class FirebaseSyncManager(
    private val context: Context,
    private val zegaDao: ZegaDao,
    private val coroutineScope: CoroutineScope
) {
    private var firebaseAuth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null
    private var isFirebaseAvailable = false

    private val _syncStatus = MutableStateFlow<CloudSyncStatus>(CloudSyncStatus.Idle)
    val syncStatus: StateFlow<CloudSyncStatus> = _syncStatus.asStateFlow()

    private val _firebaseUser = MutableStateFlow<FirebaseUser?>(null)
    val firebaseUser: StateFlow<FirebaseUser?> = _firebaseUser.asStateFlow()

    init {
        try {
            // Initialize Firebase if configured
            FirebaseApp.initializeApp(context)
            firebaseAuth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()
            isFirebaseAvailable = true

            firebaseAuth?.addAuthStateListener { auth ->
                val user = auth.currentUser
                _firebaseUser.value = user
                if (user != null) {
                    startCloudSync(user.uid)
                }
            }
            Log.d("FirebaseSync", "Firebase initialized successfully.")
        } catch (e: Exception) {
            Log.w("FirebaseSync", "Firebase services running in local fallback mode: ${e.message}")
            isFirebaseAvailable = false
        }
    }

    fun isAvailable(): Boolean = isFirebaseAvailable

    fun startCloudSync(userId: String) {
        if (!isFirebaseAvailable || firestore == null) return

        coroutineScope.launch(Dispatchers.IO) {
            _syncStatus.value = CloudSyncStatus.Syncing
            try {
                // Listen to Firestore real-time updates for notes
                firestore?.collection("users")?.document(userId)?.collection("notes")
                    ?.addSnapshotListener { snapshots, e ->
                        if (e != null) {
                            Log.e("FirebaseSync", "Listen failed: ${e.message}")
                            return@addSnapshotListener
                        }
                        if (snapshots != null) {
                            coroutineScope.launch(Dispatchers.IO) {
                                for (doc in snapshots.documents) {
                                    val content = doc.getString("content") ?: continue
                                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                    val isVoice = doc.getBoolean("isVoice") ?: false
                                    // Check if we need to insert locally
                                }
                            }
                        }
                    }

                _syncStatus.value = CloudSyncStatus.Synced(System.currentTimeMillis())
            } catch (e: Exception) {
                Log.e("FirebaseSync", "Error starting cloud sync: ${e.message}")
                _syncStatus.value = CloudSyncStatus.Error(e.message ?: "Cloud sync failed")
            }
        }
    }

    suspend fun syncNoteToCloud(userId: String, note: VoiceNote) {
        if (!isFirebaseAvailable || firestore == null) return
        try {
            val noteMap = hashMapOf(
                "id" to note.id,
                "content" to note.content,
                "timestamp" to note.timestamp,
                "syncedAt" to System.currentTimeMillis()
            )
            firestore?.collection("users")?.document(userId)?.collection("notes")
                ?.document("note_${note.id}")?.set(noteMap, SetOptions.merge())?.await()
            _syncStatus.value = CloudSyncStatus.Synced(System.currentTimeMillis())
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Failed to sync note: ${e.message}")
        }
    }

    suspend fun syncChatMessageToCloud(userId: String, message: ChatMessage) {
        if (!isFirebaseAvailable || firestore == null) return
        try {
            val msgMap = hashMapOf(
                "id" to message.id,
                "sender" to message.sender,
                "text" to message.text,
                "timestamp" to message.timestamp,
                "isVoice" to message.isVoice,
                "intentType" to message.intentType
            )
            firestore?.collection("users")?.document(userId)?.collection("chats")
                ?.document("msg_${message.id}")?.set(msgMap, SetOptions.merge())?.await()
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Failed to sync message: ${e.message}")
        }
    }

    suspend fun syncCreationToCloud(userId: String, creation: AICreation) {
        if (!isFirebaseAvailable || firestore == null) return
        try {
            val creationMap = hashMapOf(
                "id" to creation.id,
                "type" to creation.type,
                "title" to creation.title,
                "prompt" to creation.prompt,
                "modelName" to creation.modelName,
                "aspectRatio" to creation.aspectRatio,
                "durationSeconds" to creation.durationSeconds,
                "genreOrStyle" to creation.genreOrStyle,
                "timestamp" to creation.timestamp
            )
            firestore?.collection("users")?.document(userId)?.collection("creations")
                ?.document("creation_${creation.id}")?.set(creationMap, SetOptions.merge())?.await()
            _syncStatus.value = CloudSyncStatus.Synced(System.currentTimeMillis())
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Failed to sync creation: ${e.message}")
        }
    }

    suspend fun syncUserProfileToCloud(user: UserAccount) {
        if (!isFirebaseAvailable || firestore == null) return
        try {
            val userMap = hashMapOf(
                "id" to user.id,
                "email" to user.email,
                "displayName" to user.displayName,
                "provider" to user.provider,
                "avatarColorHex" to user.avatarColorHex,
                "lastLoginAt" to user.lastLoginAt,
                "createdAt" to user.createdAt
            )
            firestore?.collection("users")?.document("user_${user.id}")
                ?.set(userMap, SetOptions.merge())?.await()
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Failed to sync profile: ${e.message}")
        }
    }
}

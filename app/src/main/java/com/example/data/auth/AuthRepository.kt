package com.example.data.auth

import android.content.Context
import com.example.data.database.UserAccount
import com.example.data.database.UserAccountDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.MessageDigest

sealed class AuthResult {
    data class Success(val user: UserAccount) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthRepository(
    private val userAccountDao: UserAccountDao,
    private val context: Context
) {
    private val prefs = context.getSharedPreferences("zega_auth_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<UserAccount?>(null)
    val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

    private val _allUsers = userAccountDao.getAllUsers()
    val allUsers: Flow<List<UserAccount>> = _allUsers

    private val authScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    init {
        // Restore last logged in session if any
        val lastUserId = prefs.getLong("current_user_id", -1L)
        if (lastUserId != -1L) {
            authScope.launch {
                val user = userAccountDao.getUserById(lastUserId)
                if (user != null) {
                    _currentUser.value = user
                }
            }
        }
    }

    suspend fun signUp(email: String, password: String, displayName: String): AuthResult {
        val trimmedEmail = email.trim().lowercase()
        val trimmedName = displayName.trim().ifEmpty { trimmedEmail.substringBefore("@") }

        if (trimmedEmail.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            return AuthResult.Error("Please enter a valid email address.")
        }
        if (password.length < 6) {
            return AuthResult.Error("Password must be at least 6 characters.")
        }

        val existing = userAccountDao.getUserByEmail(trimmedEmail)
        if (existing != null) {
            return AuthResult.Error("An account with this email already exists.")
        }

        val passwordHash = hashPassword(password)
        val colors = listOf("#00FFCC", "#9D00FF", "#00E5FF", "#39FF14", "#FF007F", "#FFB703")
        val randomColor = colors.random()

        val newUser = UserAccount(
            email = trimmedEmail,
            displayName = trimmedName,
            passwordHash = passwordHash,
            provider = "EMAIL",
            avatarColorHex = randomColor,
            createdAt = System.currentTimeMillis(),
            lastLoginAt = System.currentTimeMillis()
        )

        val id = userAccountDao.insertUser(newUser)
        val createdUser = newUser.copy(id = id)
        saveSession(createdUser)
        return AuthResult.Success(createdUser)
    }

    suspend fun login(email: String, password: String): AuthResult {
        val trimmedEmail = email.trim().lowercase()
        if (trimmedEmail.isEmpty()) {
            return AuthResult.Error("Please enter your email.")
        }
        if (password.isEmpty()) {
            return AuthResult.Error("Please enter your password.")
        }

        val user = userAccountDao.getUserByEmail(trimmedEmail)
            ?: return AuthResult.Error("No account found with this email.")

        if (user.provider != "EMAIL" && user.passwordHash.isEmpty()) {
            return AuthResult.Error("This account was created with ${user.provider}. Please sign in with that method.")
        }

        val inputHash = hashPassword(password)
        if (user.passwordHash != inputHash) {
            return AuthResult.Error("Incorrect password. Please try again.")
        }

        userAccountDao.updateLastLogin(user.id, System.currentTimeMillis())
        val updatedUser = user.copy(lastLoginAt = System.currentTimeMillis())
        saveSession(updatedUser)
        return AuthResult.Success(updatedUser)
    }

    suspend fun loginWithSocial(
        provider: String, // "GOOGLE", "GITHUB", "GUEST"
        email: String,
        displayName: String,
        avatarColor: String = "#00FFCC"
    ): AuthResult {
        val trimmedEmail = email.trim().lowercase()
        var existing = userAccountDao.getUserByEmail(trimmedEmail)

        if (existing == null) {
            val newUser = UserAccount(
                email = trimmedEmail,
                displayName = displayName.trim(),
                passwordHash = "", // Social sign in has no raw password hash
                provider = provider,
                avatarColorHex = avatarColor,
                createdAt = System.currentTimeMillis(),
                lastLoginAt = System.currentTimeMillis()
            )
            val id = userAccountDao.insertOrUpdate(newUser)
            existing = newUser.copy(id = id)
        } else {
            userAccountDao.updateLastLogin(existing.id, System.currentTimeMillis())
            existing = existing.copy(lastLoginAt = System.currentTimeMillis())
        }

        saveSession(existing)
        return AuthResult.Success(existing)
    }

    suspend fun loginWithGoogle(
        email: String,
        displayName: String,
        idToken: String? = null
    ): AuthResult {
        try {
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            if (idToken != null && idToken.isNotEmpty()) {
                val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential)
            }
        } catch (e: Exception) {
            android.util.Log.w("AuthRepository", "Firebase Google auth note: ${e.message}")
        }
        return loginWithSocial(
            provider = "GOOGLE",
            email = email,
            displayName = displayName,
            avatarColor = "#4285F4"
        )
    }

    suspend fun switchAccount(user: UserAccount) {
        userAccountDao.updateLastLogin(user.id, System.currentTimeMillis())
        saveSession(user.copy(lastLoginAt = System.currentTimeMillis()))
    }

    fun logout() {
        prefs.edit().remove("current_user_id").apply()
        _currentUser.value = null
    }

    private fun saveSession(user: UserAccount) {
        prefs.edit().putLong("current_user_id", user.id).apply()
        _currentUser.value = user
    }

    private fun hashPassword(password: String): String {
        val salt = "z_ai_salt_2026_"
        val bytes = MessageDigest.getInstance("SHA-256").digest((salt + password).toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

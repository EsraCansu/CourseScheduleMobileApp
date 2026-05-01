package com.coursescheduling.data

import android.content.Context
import com.coursescheduling.data.local.AppDatabase
import com.coursescheduling.data.repository.LecturerRepository
import com.coursescheduling.domain.model.Department
import com.coursescheduling.domain.model.Role
import com.coursescheduling.domain.model.User
import com.coursescheduling.domain.model.LecturerEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.security.MessageDigest

/**
 * Authentication Manager handles user login/signup credential verification.
 * Migrated to Room Database for Lecturer storage and SharedPreferences for session.
 */
class AuthManager(private val context: Context) {

    private val sessionManager = SessionManager.getInstance(context)
    private val db = AppDatabase.getInstance(context)
    private val lecturerRepository = LecturerRepository(db.lecturerDao())

    private val _themeModeFlow = MutableStateFlow(sessionManager.themeMode)
    val themeModeFlow: StateFlow<ThemeMode> = _themeModeFlow

    fun setThemeMode(mode: ThemeMode) {
        sessionManager.themeMode = mode
        _themeModeFlow.value = mode
    }

    private val _currentUserFlow = MutableStateFlow<User?>(runBlocking { getCurrentUser() })
    val currentUserFlow: StateFlow<User?> = _currentUserFlow

    /**
     * Attempts to sign in with email and password.
     */
    fun signIn(email: String, password: String): User? = runBlocking {
        // 1. Check Admin
        if (email.equals(ADMIN_EMAIL, ignoreCase = true)) {
            if (password == ADMIN_PASSWORD) {
                sessionManager.startSession("admin_id", Role.ADMIN.name, false)
                val admin = User(
                    id = "admin_id",
                    name = "Admin",
                    surname = "User",
                    email = ADMIN_EMAIL,
                    role = Role.ADMIN,
                    mustChangePassword = false
                )
                _currentUserFlow.value = admin
                return@runBlocking admin
            }
        }

        // 2. Check Lecturers in Room
        val lecturer = lecturerRepository.getLecturerByEmail(email)
        if (lecturer != null) {
            val inputHash = hashPassword(password)
            if (inputHash == lecturer.passwordHash) {
                sessionManager.startSession(lecturer.lecturerId, Role.LECTURER.name, lecturer.mustChangePassword)
                val user = mapLecturerToUser(lecturer)
                _currentUserFlow.value = user
                return@runBlocking user
            }
        }

        return@runBlocking null
    }

    private fun mapLecturerToUser(lecturer: LecturerEntity): User {
        val nameParts = lecturer.lecturerName.split(" ", limit = 2)
        return User(
            id = lecturer.lecturerId,
            name = nameParts.getOrElse(0) { "" },
            surname = nameParts.getOrElse(1) { "" },
            email = lecturer.email,
            role = Role.LECTURER,
            isFirstLogin = lecturer.mustChangePassword,
            mustChangePassword = lecturer.mustChangePassword,
            department = Department.values().find { it.displayName.equals(lecturer.department, ignoreCase = true) } ?: Department.COMPUTER
        )
    }

    fun isLoggedIn(): Boolean = sessionManager.isLoggedIn

    suspend fun getCurrentUser(): User? {
        if (!isLoggedIn()) return null
        val userId = sessionManager.currentUserId ?: return null
        val role = sessionManager.currentRole ?: return null

        if (role == Role.ADMIN.name) {
            return User(
                id = userId,
                name = "Admin",
                surname = "User",
                email = ADMIN_EMAIL,
                role = Role.ADMIN,
                mustChangePassword = false
            )
        }

        val lecturer = db.lecturerDao().getLecturerById(userId)
        return lecturer?.let { mapLecturerToUser(it) }
    }

    fun signOut() {
        sessionManager.clearSession()
        _currentUserFlow.value = null
    }

    fun logout() = signOut()

    fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun changePassword(newPassword: String): Boolean = runBlocking {
        val user = _currentUserFlow.value ?: return@runBlocking false
        if (user.role == Role.ADMIN) return@runBlocking false // Admin password managed via config for now

        val lecturer = db.lecturerDao().getLecturerById(user.id) ?: return@runBlocking false
        val updatedLecturer = lecturer.copy(
            passwordHash = hashPassword(newPassword),
            mustChangePassword = false,
            updatedAt = System.currentTimeMillis()
        )
        db.lecturerDao().insertAll(listOf(updatedLecturer))
        _currentUserFlow.value = mapLecturerToUser(updatedLecturer)
        return@runBlocking true
    }

    suspend fun signUp(
        username: String,
        email: String,
        password: String,
        role: String,
        department: String
    ): Boolean = withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            if (role.equals(Role.ADMIN.name, ignoreCase = true)) {
                // Manual admin creation not supported in this flow, but we can allow it for testing
                return@withContext false 
            }

            val lecturerId = java.util.UUID.randomUUID().toString()
            val lecturer = LecturerEntity(
                lecturerId = lecturerId,
                lecturerName = username,
                email = email,
                department = department,
                passwordHash = hashPassword(password),
                mustChangePassword = false, // Manually signed up users don't need to change immediately
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            db.lecturerDao().insertAll(listOf(lecturer))
            return@withContext true
        } catch (e: Exception) {
            android.util.Log.e("AUTH_TRACE", "Sign up failed: ${e.message}")
            false
        }
    }

    fun createOrUpdateLecturerFromImport(
        lecturerId: String,
        lecturerName: String,
        email: String,
        password: String,
        department: String,
        lecturerTitle: String? = null
    ) = runBlocking {
        val passwordHash = hashPassword(password)
        val lecturer = LecturerEntity(
            lecturerId = lecturerId,
            lecturerName = lecturerName,
            lecturerTitle = lecturerTitle,
            email = email,
            department = department,
            passwordHash = passwordHash,
            mustChangePassword = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        db.lecturerDao().insertAll(listOf(lecturer))
    }

    companion object {
        val ADMIN_EMAIL: String get() = com.coursescheduling.BuildConfig.ADMIN_EMAIL
        private val ADMIN_PASSWORD: String get() = com.coursescheduling.BuildConfig.ADMIN_PASSWORD

        @Volatile
        private var INSTANCE: AuthManager? = null

        fun getInstance(context: Context): AuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

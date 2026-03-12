package com.university.courseschedule.data

import android.content.Context
import android.content.SharedPreferences
import com.university.courseschedule.data.model.Role
import com.university.courseschedule.data.model.User
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Authentication Manager handles user login/signup credential verification.
 * Uses SharedPreferences for user data persistence with password hashing.
 * Supports multiple users via JSON array storage.
 */
class AuthManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Attempts to sign in with email and password.
     * Returns a User if credentials are valid, null otherwise.
     */
    fun signIn(email: String, password: String, role: String): User? {
        val usersJson = prefs.getString(KEY_USERS, "[]") ?: "[]"
        val users = JSONArray(usersJson)

        for (i in 0 until users.length()) {
            val userObj = users.getJSONObject(i)
            val savedEmail = userObj.optString("email", "")
            val savedPasswordHash = userObj.optString("passwordHash", "")
            val savedRole = userObj.optString("role", "")

            if (email.equals(savedEmail, ignoreCase = true) &&
                hashPassword(password) == savedPasswordHash &&
                role.equals(savedRole, ignoreCase = true)
            ) {
                return loadUserFromJson(userObj)
            }
        }
        return null
    }

    /**
     * Creates a new user account.
     * Returns true if successful, false if email already exists.
     */
    fun signUp(username: String, email: String, password: String, role: String, department: String): Boolean {
        val usersJson = prefs.getString(KEY_USERS, "[]") ?: "[]"
        val users = JSONArray(usersJson)

        // Check if email already exists
        for (i in 0 until users.length()) {
            val userObj = users.getJSONObject(i)
            val existingEmail = userObj.optString("email", "")
            if (existingEmail.equals(email, ignoreCase = true)) {
                return false
            }
        }

        // Create new user
        val newUser = JSONObject().apply {
            put("id", java.util.UUID.randomUUID().toString())
            put("username", username)
            put("email", email)
            put("passwordHash", hashPassword(password))
            put("role", role)
            put("department", department)
            put("name", "")
            put("surname", "")
        }

        users.put(newUser)
        prefs.edit().putString(KEY_USERS, users.toString()).apply()

        return true
    }

    /**
     * Checks if a user session exists (user is logged in).
     */
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    /**
     * Returns the currently logged-in user, or null if not logged in.
     */
    fun getCurrentUser(): User? {
        if (!isLoggedIn()) return null
        
        val currentUserId = prefs.getString(KEY_CURRENT_USER_ID, "") ?: ""
        if (currentUserId.isEmpty()) return null
        
        val usersJson = prefs.getString(KEY_USERS, "[]") ?: "[]"
        val users = JSONArray(usersJson)

        for (i in 0 until users.length()) {
            val userObj = users.getJSONObject(i)
            val userId = userObj.optString("id", "")
            if (userId == currentUserId) {
                return loadUserFromJson(userObj)
            }
        }
        return null
    }

    /**
     * Returns the current user's ID for lecturer filtering.
     */
    fun getCurrentUserId(): String? {
        return prefs.getString(KEY_CURRENT_USER_ID, null)
    }

    /**
     * Sets the current user ID after successful login.
     */
    fun setCurrentUserId(userId: String) {
        prefs.edit()
            .putString(KEY_CURRENT_USER_ID, userId)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
    }

    /**
     * Signs out the current user.
     */
    fun signOut() {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_CURRENT_USER_ID)
            .apply()
    }

    /**
     * Loads user data from JSON into a User object.
     */
    private fun loadUserFromJson(userObj: JSONObject): User {
        val name = userObj.optString("name", "")
        val surname = userObj.optString("surname", "")
        val username = userObj.optString("username", "")
        val email = userObj.optString("email", "")
        val roleStr = userObj.optString("role", "")
        val department = userObj.optString("department", "")
        
        val role = try {
            Role.valueOf(roleStr.uppercase())
        } catch (e: IllegalArgumentException) {
            Role.LECTURER
        }

        return User(
            id = userObj.optString("id", java.util.UUID.randomUUID().toString()),
            name = name,
            surname = surname,
            department = com.university.courseschedule.data.model.Department.values()
                .find { it.displayName == department } 
                ?: com.university.courseschedule.data.model.Department.COMPUTER,
            role = role,
            isRegistered = true
        )
    }

    /**
     * Hashes password using SHA-256.
     */
    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Updates user profile information after initial registration.
     */
    fun updateProfile(name: String, surname: String, department: String) {
        val currentUserId = prefs.getString(KEY_CURRENT_USER_ID, "") ?: return
        
        val usersJson = prefs.getString(KEY_USERS, "[]") ?: "[]"
        val users = JSONArray(usersJson)

        for (i in 0 until users.length()) {
            val userObj = users.getJSONObject(i)
            if (userObj.optString("id") == currentUserId) {
                userObj.put("name", name)
                userObj.put("surname", surname)
                userObj.put("department", department)
                prefs.edit().putString(KEY_USERS, users.toString()).apply()
                break
            }
        }
    }

    /**
     * Creates or updates a lecturer user from Excel import.
     * This allows imported lecturers to immediately sign in using the password from the Excel file.
     * If the lecturer already exists, their password will be updated to the imported value.
     *
     * @param lecturerID The unique identifier for the lecturer.
     * @param lecturerName The display name of the lecturer.
     * @param email The email address (derived from name if not provided).
     * @param password The password from the Excel file.
     * @param departmentIndex The department index for the lecturer.
     */
    fun createOrUpdateLecturerFromImport(
        lecturerID: String,
        lecturerName: String,
        email: String,
        password: String,
        departmentIndex: Int
    ): Boolean {
        val usersJson = prefs.getString(KEY_USERS, "[]") ?: "[]"
        val users = JSONArray(usersJson)

        // Parse name into first name and surname
        val nameParts = lecturerName.trim().split(" ", limit = 2)
        val firstName = nameParts.getOrElse(0) { lecturerName }
        val surname = nameParts.getOrElse(1) { "" }
        val username = "${firstName.lowercase()}_${surname.lowercase()}".replace(" ", "")

        val department = com.university.courseschedule.data.model.Department.values()
            .getOrElse(departmentIndex) { com.university.courseschedule.data.model.Department.COMPUTER }

        // Check if user already exists
        var existingIndex = -1
        for (i in 0 until users.length()) {
            val userObj = users.getJSONObject(i)
            val existingId = userObj.optString("id", "")
            val existingEmail = userObj.optString("email", "")
            if (existingId == lecturerID || existingEmail.equals(email, ignoreCase = true)) {
                existingIndex = i
                break
            }
        }

        val userJson = JSONObject().apply {
            put("id", lecturerID)
            put("username", username)
            put("email", email)
            put("passwordHash", hashPassword(password))
            put("role", "LECTURER")
            put("department", department.displayName)
            put("name", firstName)
            put("surname", surname)
        }

        if (existingIndex >= 0) {
            // Update existing user
            users.put(existingIndex, userJson)
        } else {
            // Add new user
            users.put(userJson)
        }

        prefs.edit().putString(KEY_USERS, users.toString()).apply()
        return true
    }

    companion object {
        private const val PREFS_NAME = "auth_prefs"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_CURRENT_USER_ID = "current_user_id"
        private const val KEY_USERS = "users"  // JSON array of user objects

        @Volatile
        private var INSTANCE: AuthManager? = null

        fun getInstance(context: Context): AuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

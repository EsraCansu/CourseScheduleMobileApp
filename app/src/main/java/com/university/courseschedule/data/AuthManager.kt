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
 *
 * This is the SINGLE source of truth for auth state — all fragments must
 * interact with this class rather than accessing raw SharedPreferences.
 */
class AuthManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Attempts to sign in with email and password.
     * Returns a User if credentials are valid, null otherwise.
     * 
     * Note: Role is derived from stored user data, not from UI input.
     * This prevents users from bypassing authentication by selecting a different role.
     */
    fun signIn(email: String, password: String): User? {
        val usersJson = prefs.getString(KEY_USERS, "[]") ?: "[]"
        val users = JSONArray(usersJson)

        for (i in 0 until users.length()) {
            val userObj = users.getJSONObject(i)
            val savedEmail = userObj.optString("email", "")
            val savedPasswordHash = userObj.optString("passwordHash", "")

            // Match only by email and password - role is determined by stored data
            if (email.equals(savedEmail, ignoreCase = true) &&
                hashPassword(password) == savedPasswordHash
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

        // Validate email format before checking if email already exists
        if (!isValidEmail(email)) {
            return false
        }

        // Check if email already exists
        for (i in 0 until users.length()) {
            val userObj = users.getJSONObject(i)
            val existingEmail = userObj.optString("email", "")
            if (existingEmail.equals(email, ignoreCase = true)) {
                return false
            }
        }

        // Create new user — isFirstLogin = true by default
        val newUser = JSONObject().apply {
            put("id", java.util.UUID.randomUUID().toString())
            put("username", username)
            put("email", email)
            put("passwordHash", hashPassword(password))
            put("role", role)
            put("department", department)
            put("name", "")
            put("surname", "")
            put("isFirstLogin", true)
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
     * Signs out the current user and clears session state.
     */
    fun signOut() {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_CURRENT_USER_ID)
            .apply()
    }

    /** Convenience alias for signOut(). */
    fun logout() = signOut()

    /**
     * Changes the password for the currently logged-in user.
     * Also sets isFirstLogin to false for that user.
     *
     * @return true if the password was changed successfully, false otherwise.
     */
    fun changePassword(currentPassword: String, newPassword: String): Boolean {
        val currentUserId = prefs.getString(KEY_CURRENT_USER_ID, "") ?: return false
        if (currentUserId.isEmpty()) return false

        val usersJson = prefs.getString(KEY_USERS, "[]") ?: "[]"
        val users = JSONArray(usersJson)

        for (i in 0 until users.length()) {
            val userObj = users.getJSONObject(i)
            if (userObj.optString("id") == currentUserId) {
                // Verify current password
                val savedHash = userObj.optString("passwordHash", "")
                if (hashPassword(currentPassword) != savedHash) {
                    return false  // Incorrect current password
                }

                // Update password and clear first-login flag
                userObj.put("passwordHash", hashPassword(newPassword))
                userObj.put("isFirstLogin", false)
                prefs.edit().putString(KEY_USERS, users.toString()).apply()
                return true
            }
        }
        return false
    }

    /**
     * Loads user data from JSON into a User object.
     */
    private fun loadUserFromJson(userObj: JSONObject): User {
        val name = userObj.optString("name", "")
        val surname = userObj.optString("surname", "")
        val email = userObj.optString("email", "")
        val roleStr = userObj.optString("role", "")
        val department = userObj.optString("department", "")
        val isFirstLogin = userObj.optBoolean("isFirstLogin", true)
        
        val role = try {
            Role.valueOf(roleStr.uppercase())
        } catch (e: IllegalArgumentException) {
            Role.LECTURER
        }

        return User(
            id = userObj.optString("id", java.util.UUID.randomUUID().toString()),
            name = name,
            surname = surname,
            email = email,
            department = com.university.courseschedule.data.model.Department.values()
                .find { it.displayName == department } 
                ?: com.university.courseschedule.data.model.Department.COMPUTER,
            role = role,
            isFirstLogin = isFirstLogin
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
     * Validates email format.
     * Checks for:
     * - Contains exactly one @ symbol
     * - Has at least one character before the @
     * - Has at least one character after the @ followed by a dot (domain)
     *
     * @return true if email format is valid, false otherwise
     */
    private fun isValidEmail(email: String): Boolean {
        if (email.isEmpty()) return false

        // Check for exactly one @ symbol
        val atIndex = email.indexOf('@')
        if (atIndex <= 0) return false // No @ or @ is first character
        if (email.indexOf('@', atIndex + 1) != -1) return false // More than one @

        // Check for domain with dot after @
        val domainPart = email.substring(atIndex + 1)
        if (domainPart.isEmpty()) return false // No domain
        if (!domainPart.contains('.')) return false // No dot in domain

        // Check there's at least one character before @ (already checked with atIndex > 0)
        // Check there's at least one character after the dot
        val dotIndex = domainPart.indexOf('.')
        if (dotIndex == 0 || dotIndex == domainPart.length - 1) return false

        return true
    }

    /**
     * Updates user profile information.
     * Called from SettingsFragment when the user saves their profile.
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
            put("isFirstLogin", true)  // Imported lecturers should change their password
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
        private const val KEY_ADMIN_INITIALIZED = "admin_initialized"

        // Admin credentials - MUST be configured in gradle.properties
        // SECURITY: No hardcoded fallbacks - build will fail if not configured
        val ADMIN_EMAIL: String
            get() = try {
                val email = com.university.courseschedule.BuildConfig.ADMIN_EMAIL
                require(email.isNotBlank()) { "ADMIN_EMAIL not configured in gradle.properties" }
                email
            } catch (e: Exception) {
                throw IllegalStateException("ADMIN_EMAIL must be configured in gradle.properties. " +
                    "Add: ADMIN_EMAIL=your-admin@email.com")
            }

        private val ADMIN_PASSWORD: String
            get() = try {
                val password = com.university.courseschedule.BuildConfig.ADMIN_PASSWORD
                require(password.isNotBlank()) { "ADMIN_PASSWORD not configured in gradle.properties" }
                require(password.length >= 6) { "ADMIN_PASSWORD must be at least 6 characters" }
                password
            } catch (e: Exception) {
                throw IllegalStateException("ADMIN_PASSWORD must be configured in gradle.properties. " +
                    "Add: ADMIN_PASSWORD=your-secure-password")
            }

        val ADMIN_NAME: String
            get() = try {
                com.university.courseschedule.BuildConfig.ADMIN_NAME
            } catch (e: Exception) {
                "Admin"
            }

        val ADMIN_SURNAME: String
            get() = try {
                com.university.courseschedule.BuildConfig.ADMIN_SURNAME
            } catch (e: Exception) {
                "User"
            }

        @Volatile
        private var INSTANCE: AuthManager? = null

        fun getInstance(context: Context): AuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthManager(context.applicationContext).also { instance ->
                    INSTANCE = instance
                    instance.initializeAdminSeedData()
                }
            }
        }
    }

    /**
     * Initializes (or re-seeds) the admin account based on the current BuildConfig credentials.
     *
     * This function is idempotent — it checks whether an admin with [ADMIN_EMAIL] already
     * exists in SharedPrefs. If the email in gradle.properties changes, the new admin is
     * automatically seeded on the next app launch without clearing app data.
     *
     * Called automatically on AuthManager instantiation.
     */
    private fun initializeAdminSeedData() {
        val targetEmail = try { ADMIN_EMAIL } catch (e: Exception) { return }

        val usersJson = prefs.getString(KEY_USERS, "[]") ?: "[]"
        val users = JSONArray(usersJson)

        // Check if an admin with the current target email already exists
        for (i in 0 until users.length()) {
            val userObj = users.getJSONObject(i)
            if (userObj.optString("email", "").equals(targetEmail, ignoreCase = true)) {
                // Admin already seeded — nothing to do
                prefs.edit().putBoolean(KEY_ADMIN_INITIALIZED, true).apply()
                return
            }
        }

        // No admin with this email found → create the seed account
        val adminUser = JSONObject().apply {
            put("id", "admin_seed")          // Fixed ID so we can update rather than duplicate
            put("username", "admin")
            put("email", targetEmail)
            put("passwordHash", hashPassword(try { ADMIN_PASSWORD } catch (e: Exception) { return }))
            put("role", "ADMIN")
            put("department", "COMPUTER")
            put("name", try { ADMIN_NAME } catch (e: Exception) { "Admin" })
            put("surname", try { ADMIN_SURNAME } catch (e: Exception) { "User" })
            put("isFirstLogin", false)       // Admin never needs to change password
        }
        users.put(adminUser)
        prefs.edit()
            .putString(KEY_USERS, users.toString())
            .putBoolean(KEY_ADMIN_INITIALIZED, true)
            .apply()
    }
}

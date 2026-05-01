package com.coursescheduling.data

import android.content.Context
import android.content.SharedPreferences

/**
 * SessionManager handles the persistent login state of the user.
 * It stores the current user's ID, role, and login timestamp in SharedPreferences.
 */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        get() = ThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value.name).apply()

    var currentUserId: String?
        get() = prefs.getString(KEY_USER_ID, null)
        set(value) = prefs.edit().putString(KEY_USER_ID, value).apply()

    var currentRole: String?
        get() = prefs.getString(KEY_ROLE, null)
        set(value) = prefs.edit().putString(KEY_ROLE, value).apply()

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

    var lastLogin: Long
        get() = prefs.getLong(KEY_LAST_LOGIN, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_LOGIN, value).apply()

    var mustChangePassword: Boolean
        get() = prefs.getBoolean(KEY_MUST_CHANGE_PASSWORD, false)
        set(value) = prefs.edit().putBoolean(KEY_MUST_CHANGE_PASSWORD, value).apply()

    fun startSession(userId: String, role: String, mustChange: Boolean = false) {
        currentUserId = userId
        currentRole = role
        isLoggedIn = true
        mustChangePassword = mustChange
        lastLogin = System.currentTimeMillis()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "session_prefs"
        private const val KEY_USER_ID = "current_user_id"
        private const val KEY_ROLE = "current_role"
        private const val KEY_IS_LOGGED_IN = "login_state"
        private const val KEY_LAST_LOGIN = "last_login"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_MUST_CHANGE_PASSWORD = "must_change_password"

        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

package com.coursescheduling.presentation.settings

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coursescheduling.data.AuthManager
import com.coursescheduling.data.ThemeMode
import com.coursescheduling.data.local.AppDatabase
import com.coursescheduling.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val user: User? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val authManager = AuthManager.getInstance(app)
    private val db = AppDatabase.getInstance(app)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        Log.d("FEATURE_FIX_DEBUG", "Settings screen initialization")
        viewModelScope.launch {
            authManager.currentUserFlow.collect { user ->
                _uiState.value = _uiState.value.copy(user = user)
            }
        }
        viewModelScope.launch {
            authManager.themeModeFlow.collect { mode ->
                Log.d("FEATURE_FIX_DEBUG", "Theme load state: $mode")
                _uiState.value = _uiState.value.copy(themeMode = mode)
            }
        }
    }

    fun setTheme(mode: ThemeMode) {
        authManager.setThemeMode(mode)
    }

    fun updateProfile(name: String, email: String) {
        val user = _uiState.value.user ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                if (user.role == com.coursescheduling.domain.model.Role.LECTURER) {
                    val lecturer = db.lecturerDao().getLecturerById(user.id)
                    if (lecturer != null) {
                        val updated = lecturer.copy(
                            lecturerName = name,
                            email = email,
                            updatedAt = System.currentTimeMillis()
                        )
                        db.lecturerDao().updateLecturer(updated)
                        // Note: CurrentUserFlow in AuthManager will update if we refresh it
                        // For now we'll rely on the DB update
                        _uiState.value = _uiState.value.copy(successMessage = "Profile updated successfully")
                    }
                } else {
                    // Admin profile update (in-memory/session only for now)
                    _uiState.value = _uiState.value.copy(successMessage = "Admin profile updated (session only)")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun changePassword(newPass: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val success = authManager.changePassword(newPass)
            if (success) {
                _uiState.value = _uiState.value.copy(successMessage = "Password changed successfully", isLoading = false)
            } else {
                _uiState.value = _uiState.value.copy(error = "Failed to change password", isLoading = false)
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }
}

package com.coursescheduling.presentation

sealed class UiState<out T> {
    // No operation has started yet
    object Idle : UiState<Nothing>()
    
    // Data is being fetched; optional progress percentage
    data class Loading(val progress: Int? = null) : UiState<Nothing>()
    
    // Fetch succeeded; holds the result
    data class Success<T>(val data: T) : UiState<T>()
    
    // Fetch failed; holds the message and whether a retry makes sense
    data class Error(
        val message: String,
        val retryable: Boolean = true
    ) : UiState<Nothing>()
}

package com.desire.photos.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desire.photos.config.AppConfig
import com.desire.photos.di.ServiceLocator
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AuthUiState(
    val isSignUp: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
)

class AuthViewModel : ViewModel() {

    private val auth = ServiceLocator.authRepository

    val user: StateFlow<FirebaseUser?> =
        auth.authState.stateIn(viewModelScope, SharingStarted.Eagerly, auth.currentUser)

    private val _ui = MutableStateFlow(AuthUiState())
    val ui: StateFlow<AuthUiState> = _ui.asStateFlow()

    val isGoogleConfigured: Boolean = AppConfig.isGoogleSignInConfigured

    fun toggleMode() {
        _ui.value = _ui.value.copy(isSignUp = !_ui.value.isSignUp, error = null)
    }

    fun clearError() {
        _ui.value = _ui.value.copy(error = null)
    }

    fun submitEmail(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _ui.value = _ui.value.copy(error = "Enter an email and password")
            return
        }
        if (_ui.value.isSignUp && password.length < 6) {
            _ui.value = _ui.value.copy(error = "Password must be at least 6 characters")
            return
        }
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isLoading = true, error = null)
            val result = if (_ui.value.isSignUp) {
                auth.signUpWithEmail(email, password)
            } else {
                auth.signInWithEmail(email, password)
            }
            _ui.value = _ui.value.copy(
                isLoading = false,
                error = result.exceptionOrNull()?.message,
            )
        }
    }

    fun signInWithGoogle(activityContext: Context) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isLoading = true, error = null)
            val result = auth.signInWithGoogle(activityContext)
            _ui.value = _ui.value.copy(
                isLoading = false,
                error = result.exceptionOrNull()?.message,
            )
        }
    }
}

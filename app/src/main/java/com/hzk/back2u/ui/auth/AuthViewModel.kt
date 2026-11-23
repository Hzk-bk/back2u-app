package com.hzk.back2u.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hzk.back2u.data.AuthException
import com.hzk.back2u.data.AuthFailure
import com.hzk.back2u.data.AuthRepository
import com.hzk.back2u.domain.AuthValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val isLoggedIn: StateFlow<Boolean> = repository.isLoggedIn
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = repository.currentUserId != null
        )

    val userEmail: String
        get() = repository.currentUserEmail ?: ""

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null, generalError = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null, generalError = null) }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update {
            it.copy(confirmPassword = value, confirmPasswordError = null, generalError = null)
        }
    }

    fun resetForm() {
        _uiState.value = AuthUiState()
    }

    fun login() {
        val state = _uiState.value
        val emailError = AuthValidator.validateEmail(state.email)
        val passwordError = AuthValidator.validatePassword(state.password)
        if (emailError != null || passwordError != null) {
            _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }
        _uiState.update { it.copy(isLoading = true, generalError = null) }
        viewModelScope.launch {
            repository.login(state.email.trim(), state.password)
                .onSuccess { _uiState.update { it.copy(isLoading = false) } }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, generalError = mapError(e)) }
                }
        }
    }

    fun register() {
        val state = _uiState.value
        val emailError = AuthValidator.validateEmail(state.email)
        val passwordError = AuthValidator.validatePassword(state.password)
        val confirmError =
            AuthValidator.validatePasswordConfirmation(state.password, state.confirmPassword)
        if (emailError != null || passwordError != null || confirmError != null) {
            _uiState.update {
                it.copy(
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmError
                )
            }
            return
        }
        _uiState.update { it.copy(isLoading = true, generalError = null) }
        viewModelScope.launch {
            repository.register(state.email.trim(), state.password)
                .onSuccess { _uiState.update { it.copy(isLoading = false) } }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, generalError = mapError(e)) }
                }
        }
    }

    fun logout() {
        repository.logout()
        resetForm()
    }

    private fun mapError(e: Throwable): String = when ((e as? AuthException)?.failure) {
        AuthFailure.EMAIL_IN_USE -> "An account with this email already exists"
        AuthFailure.WEAK_PASSWORD -> "The password is too weak"
        AuthFailure.INVALID_CREDENTIALS,
        AuthFailure.USER_NOT_FOUND -> "Incorrect email or password"
        AuthFailure.NETWORK -> "No internet connection"
        AuthFailure.UNKNOWN, null -> "Something went wrong. Please try again"
    }

}
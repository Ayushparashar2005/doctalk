package com.doctalk.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doctalk.app.data.model.User
import com.doctalk.app.network.NetworkResult
import com.doctalk.app.repository.AuthRepository
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for authentication operations
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        // Check if user is already authenticated
        checkAuthState()
        
        // Listen to auth state changes
        viewModelScope.launch {
            authRepository.getAuthStateFlow().collect { user ->
                _currentUser.value = user
                _authState.value = if (user != null) AuthState.Authenticated else AuthState.NotAuthenticated
            }
        }
    }

    /**
     * Checks current authentication state
     */
    private fun checkAuthState() {
        val user = authRepository.currentUser
        _currentUser.value = user
        _authState.value = if (user != null) AuthState.Authenticated else AuthState.NotAuthenticated
    }

    /**
     * Signs in with email and password
     */
    fun signInWithEmail(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _errorMessage.value = "Email and password cannot be empty"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            when (val result = authRepository.signInWithEmail(email, password)) {
                is NetworkResult.Success -> {
                    _authState.value = AuthState.Authenticated
                    _currentUser.value = result.data
                }
                is NetworkResult.Error -> {
                    _errorMessage.value = result.message
                    _authState.value = AuthState.NotAuthenticated
                }
                is NetworkResult.Loading -> {
                    // Loading state is handled by _isLoading
                }
            }
            
            _isLoading.value = false
        }
    }

    /**
     * Signs up with email and password
     */
    fun signUpWithEmail(email: String, password: String, displayName: String) {
        if (email.isBlank() || password.isBlank() || displayName.isBlank()) {
            _errorMessage.value = "All fields are required"
            return
        }

        if (password.length < 6) {
            _errorMessage.value = "Password must be at least 6 characters"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            when (val result = authRepository.signUpWithEmail(email, password, displayName)) {
                is NetworkResult.Success -> {
                    _authState.value = AuthState.Authenticated
                    _currentUser.value = result.data
                }
                is NetworkResult.Error -> {
                    _errorMessage.value = result.message
                    _authState.value = AuthState.NotAuthenticated
                }
                is NetworkResult.Loading -> {
                    // Loading state is handled by _isLoading
                }
            }
            
            _isLoading.value = false
        }
    }

    /**
     * Signs in with Google
     */
    fun signInWithGoogle(account: GoogleSignInAccount) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            when (val result = authRepository.signInWithGoogle(account)) {
                is NetworkResult.Success -> {
                    _authState.value = AuthState.Authenticated
                    _currentUser.value = result.data
                }
                is NetworkResult.Error -> {
                    _errorMessage.value = result.message
                    _authState.value = AuthState.NotAuthenticated
                }
                is NetworkResult.Loading -> {
                    // Loading state is handled by _isLoading
                }
            }
            
            _isLoading.value = false
        }
    }

    /**
     * Sends password reset email
     */
    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _errorMessage.value = "Email cannot be empty"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            when (val result = authRepository.resetPassword(email)) {
                is NetworkResult.Success -> {
                    _authState.value = AuthState.PasswordResetSent
                }
                is NetworkResult.Error -> {
                    _errorMessage.value = result.message
                }
                is NetworkResult.Loading -> {
                    // Loading state is handled by _isLoading
                }
            }
            
            _isLoading.value = false
        }
    }

    /**
     * Signs out the current user
     */
    fun signOut() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            when (authRepository.signOut()) {
                is NetworkResult.Success -> {
                    _authState.value = AuthState.NotAuthenticated
                    _currentUser.value = null
                }
                is NetworkResult.Error -> {
                    _errorMessage.value = "Failed to sign out"
                }
                is NetworkResult.Loading -> {
                    // Loading state is handled by _isLoading
                }
            }
            
            _isLoading.value = false
        }
    }

    /**
     * Reloads the current user
     */
    fun reloadUser() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            when (val result = authRepository.reloadUser()) {
                is NetworkResult.Success -> {
                    _currentUser.value = result.data
                }
                is NetworkResult.Error -> {
                    _errorMessage.value = result.message
                }
                is NetworkResult.Loading -> {
                    // Loading state is handled by _isLoading
                }
            }
            
            _isLoading.value = false
        }
    }

    /**
     * Updates user profile
     */
    fun updateProfile(displayName: String?, photoUrl: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            when (val result = authRepository.updateProfile(displayName, photoUrl)) {
                is NetworkResult.Success -> {
                    _currentUser.value = result.data
                }
                is NetworkResult.Error -> {
                    _errorMessage.value = result.message
                }
                is NetworkResult.Loading -> {
                    // Loading state is handled by _isLoading
                }
            }
            
            _isLoading.value = false
        }
    }

    /**
     * Clears error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Resets auth state to idle
     */
    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }
}

/**
 * Sealed class representing authentication states
 */
sealed class AuthState {
    object Idle : AuthState()
    object NotAuthenticated : AuthState()
    object Authenticated : AuthState()
    object PasswordResetSent : AuthState()
    data class Error(val message: String) : AuthState()
}

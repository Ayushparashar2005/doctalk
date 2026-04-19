package com.doctalk.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doctalk.app.data.model.Document
import com.doctalk.app.data.model.DocumentStatus
import com.doctalk.app.repository.AuthRepository
import com.doctalk.app.repository.DocumentRepository
import com.doctalk.app.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the home screen
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val documentRepository: DocumentRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _currentUser = MutableStateFlow<com.doctalk.app.data.model.User?>(null)
    val currentUser: StateFlow<com.doctalk.app.data.model.User?> = _currentUser.asStateFlow()

    private val _documents = MutableStateFlow<List<Document>>(emptyList())
    val documents: StateFlow<List<Document>> = _documents.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _stats = MutableStateFlow<HomeStats>(HomeStats())
    val stats: StateFlow<HomeStats> = _stats.asStateFlow()

    init {
        loadUserData()
        observeDocuments()
        initializeNotifications()
    }

    /**
     * Loads current user data
     */
    private fun loadUserData() {
        viewModelScope.launch {
            authRepository.getAuthStateFlow().collect { user ->
                _currentUser.value = user
            }
        }
    }

    /**
     * Observes document changes
     */
    private fun observeDocuments() {
        viewModelScope.launch {
            documentRepository.getUserDocumentsFlow().collect { documents ->
                _documents.value = documents
                updateStats(documents)
            }
        }
    }

    /**
     * Initializes FCM notifications
     */
    private fun initializeNotifications() {
        viewModelScope.launch {
            when (val tokenResult = notificationRepository.getFCMToken()) {
                is com.doctalk.app.network.NetworkResult.Success -> {
                    notificationRepository.saveFCMToken(tokenResult.data)
                }
                is com.doctalk.app.network.NetworkResult.Error -> {
                    // Handle error - don't show to user as it's not critical
                }
                is com.doctalk.app.network.NetworkResult.Loading -> {
                    // Loading state
                }
            }
        }
    }

    /**
     * Updates home screen statistics
     */
    private fun updateStats(documents: List<Document>) {
        val processedCount = documents.count { it.status == DocumentStatus.PROCESSED }
        val processingCount = documents.count { 
            it.status == DocumentStatus.UPLOADING || it.status == DocumentStatus.PROCESSING 
        }
        val failedCount = documents.count { it.status == DocumentStatus.FAILED }
        val totalCount = documents.size

        _stats.value = HomeStats(
            totalDocuments = totalCount,
            processedDocuments = processedCount,
            processingDocuments = processingCount,
            failedDocuments = failedCount
        )
    }

    /**
     * Refreshes data
     */
    fun refresh() {
        loadUserData()
        // Documents are automatically updated via flow
    }

    /**
     * Signs out the user
     */
    fun signOut() {
        viewModelScope.launch {
            when (authRepository.signOut()) {
                is com.doctalk.app.network.NetworkResult.Success -> {
                    _currentUser.value = null
                    _documents.value = emptyList()
                    _stats.value = HomeStats()
                    _successMessage.value = "Signed out successfully"
                }
                is com.doctalk.app.network.NetworkResult.Error -> {
                    _errorMessage.value = "Failed to sign out"
                }
                is com.doctalk.app.network.NetworkResult.Loading -> {
                    // Loading state is handled by _isLoading
                }
            }
        }
    }

    /**
     * Clears error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Clears success message.
     */
    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    /**
     * Gets processed documents for quick access
     */
    fun getProcessedDocuments(): List<Document> {
        return _documents.value.filter { it.status == DocumentStatus.PROCESSED }
    }

    /**
     * Gets recent documents (last 5)
     */
    fun getRecentDocuments(): List<Document> {
        return _documents.value.take(5)
    }

    /**
     * Gets documents currently being processed
     */
    fun getProcessingDocuments(): List<Document> {
        return _documents.value.filter { 
            it.status == DocumentStatus.UPLOADING || it.status == DocumentStatus.PROCESSING 
        }
    }
}

/**
 * Data class for home screen statistics
 */
data class HomeStats(
    val totalDocuments: Int = 0,
    val processedDocuments: Int = 0,
    val processingDocuments: Int = 0,
    val failedDocuments: Int = 0
)

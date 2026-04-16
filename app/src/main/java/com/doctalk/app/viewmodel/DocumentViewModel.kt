package com.doctalk.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doctalk.app.data.model.Document
import com.doctalk.app.data.model.DocumentStatus
import com.doctalk.app.network.NetworkResult
import com.doctalk.app.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for document operations
 */
@HiltViewModel
class DocumentViewModel @Inject constructor(
    private val documentRepository: DocumentRepository
) : ViewModel() {

    private val _documents = MutableStateFlow<List<Document>>(emptyList())
    val documents: StateFlow<List<Document>> = _documents.asStateFlow()

    private val _selectedDocument = MutableStateFlow<Document?>(null)
    val selectedDocument: StateFlow<Document?> = _selectedDocument.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _uploadProgress = MutableStateFlow(0f)
    val uploadProgress: StateFlow<Float> = _uploadProgress.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    init {
        loadDocuments()
        observeDocuments()
    }

    /**
     * Loads all documents for the current user
     */
    private fun loadDocuments() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            when (val result = documentRepository.getUserDocuments()) {
                is NetworkResult.Success -> {
                    _documents.value = result.data
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
     * Observes document changes in real-time
     */
    private fun observeDocuments() {
        viewModelScope.launch {
            documentRepository.getUserDocumentsFlow().collect { documents ->
                _documents.value = documents
            }
        }
    }

    /**
     * Uploads a document
     */
    fun uploadDocument(file: File, fileName: String, fileType: String) {
        if (file.length() > 10 * 1024 * 1024) { // 10MB limit
            _errorMessage.value = "File size exceeds 10MB limit"
            return
        }

        if (!listOf("pdf", "txt").contains(fileType.lowercase())) {
            _errorMessage.value = "Only PDF and TXT files are supported"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _uploadProgress.value = 0f
            
            when (val result = documentRepository.uploadDocument(file, fileName, fileType)) {
                is NetworkResult.Success -> {
                    _successMessage.value = "Document uploaded successfully"
                    _uploadProgress.value = 1f
                    // Documents will be updated via the flow observer
                }
                is NetworkResult.Error -> {
                    _errorMessage.value = result.message
                    _uploadProgress.value = 0f
                }
                is NetworkResult.Loading -> {
                    // Loading state is handled by _isLoading
                }
            }
            
            _isLoading.value = false
        }
    }

    /**
     * Gets a specific document
     */
    fun getDocument(documentId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            when (val result = documentRepository.getDocument(documentId)) {
                is NetworkResult.Success -> {
                    _selectedDocument.value = result.data
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
     * Deletes a document
     */
    fun deleteDocument(documentId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            when (documentRepository.deleteDocument(documentId)) {
                is NetworkResult.Success -> {
                    _successMessage.value = "Document deleted successfully"
                    if (_selectedDocument.value?.id == documentId) {
                        _selectedDocument.value = null
                    }
                    // Documents will be updated via the flow observer
                }
                is NetworkResult.Error -> {
                    _errorMessage.value = "Failed to delete document"
                }
                is NetworkResult.Loading -> {
                    // Loading state is handled by _isLoading
                }
            }
            
            _isLoading.value = false
        }
    }

    /**
     * Updates document status
     */
    fun updateDocumentStatus(
        documentId: String,
        status: DocumentStatus,
        errorMessage: String? = null,
        pageCount: Int? = null,
        wordCount: Int? = null
    ) {
        viewModelScope.launch {
            when (documentRepository.updateDocumentStatus(documentId, status, errorMessage, pageCount, wordCount)) {
                is NetworkResult.Success -> {
                    // Documents will be updated via the flow observer
                }
                is NetworkResult.Error -> {
                    _errorMessage.value = "Failed to update document status"
                }
                is NetworkResult.Loading -> {
                    // Loading state is handled by _isLoading
                }
            }
        }
    }

    /**
     * Gets document summary
     */
    fun getDocumentSummary(documentId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            when (val result = documentRepository.getDocumentSummary(documentId)) {
                is NetworkResult.Success -> {
                    _successMessage.value = result.data
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
     * Searches within a document
     */
    fun searchDocument(documentId: String, query: String) {
        if (query.isBlank()) {
            _errorMessage.value = "Search query cannot be empty"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            when (val result = documentRepository.searchDocument(documentId, query)) {
                is NetworkResult.Success -> {
                    // Handle search results - could be emitted through another StateFlow
                    _successMessage.value = "Found ${result.data.size} results"
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
     * Sets the selected document
     */
    fun selectDocument(document: Document) {
        _selectedDocument.value = document
    }

    /**
     * Clears the selected document
     */
    fun clearSelectedDocument() {
        _selectedDocument.value = null
    }

    /**
     * Clears error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Clears success message
     */
    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    /**
     * Refreshes documents
     */
    fun refreshDocuments() {
        loadDocuments()
    }

    /**
     * Gets documents filtered by status
     */
    fun getDocumentsByStatus(status: DocumentStatus): List<Document> {
        return _documents.value.filter { it.status == status }
    }

    /**
     * Gets processed documents ready for chat
     */
    fun getProcessedDocuments(): List<Document> {
        return _documents.value.filter { it.status == DocumentStatus.PROCESSED }
    }

    /**
     * Gets uploading/processing documents
     */
    fun getProcessingDocuments(): List<Document> {
        return _documents.value.filter { 
            it.status == DocumentStatus.UPLOADING || it.status == DocumentStatus.PROCESSING 
        }
    }

    /**
     * Gets failed documents
     */
    fun getFailedDocuments(): List<Document> {
        return _documents.value.filter { it.status == DocumentStatus.FAILED }
    }
}

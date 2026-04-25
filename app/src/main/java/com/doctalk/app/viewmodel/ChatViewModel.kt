package com.doctalk.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doctalk.app.data.model.ChatSession
import com.doctalk.app.data.model.Message
import com.doctalk.app.data.model.MessageType
import com.doctalk.app.network.NetworkResult
import com.doctalk.app.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for chat operations
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _chatSessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val chatSessions: StateFlow<List<ChatSession>> = _chatSessions.asStateFlow()

    private val _selectedSession = MutableStateFlow<ChatSession?>(null)
    val selectedSession: StateFlow<ChatSession?> = _selectedSession.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSendingMessage = MutableStateFlow(false)
    val isSendingMessage: StateFlow<Boolean> = _isSendingMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    init {
        loadChatSessions()
    }

    /**
     * Loads all chat sessions for the current user
     */
    private fun loadChatSessions() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            when (val result = chatRepository.getChatSessions()) {
                is NetworkResult.Success -> {
                    _chatSessions.value = result.data
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
     * Creates a new chat session
     */
    fun createChatSession(documentId: String, documentName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            when (val result = chatRepository.createChatSession(documentId, documentName)) {
                is NetworkResult.Success -> {
                    _selectedSession.value = result.data
                    _chatSessions.value = listOf(result.data) + _chatSessions.value
                    loadMessages(result.data.id)
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
     * Selects a chat session and loads its messages
     */
    fun selectChatSession(session: ChatSession) {
        _selectedSession.value = session
        loadMessages(session.id)
    }

    /**
     * Loads messages for a specific chat session
     */
    private fun loadMessages(sessionId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            when (val result = chatRepository.getChatMessages(sessionId)) {
                is NetworkResult.Success -> {
                    _messages.value = result.data
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
     * Sends a message and gets AI response
     */
    fun sendMessage(content: String) {
        if (content.isBlank()) {
            _errorMessage.value = "Message cannot be empty"
            return
        }

        val session = _selectedSession.value
        if (session == null) {
            _errorMessage.value = "No chat session selected"
            return
        }

        viewModelScope.launch {
            _isSendingMessage.value = true
            _isTyping.value = true
            _errorMessage.value = null
            
            // Add typing indicator message
            val typingMessage = Message(
                id = "typing_${System.currentTimeMillis()}",
                documentId = session.documentId,
                userId = "", // Will be set in repository
                content = "",
                messageType = MessageType.AI,
                isTyping = true,
                timestamp = System.currentTimeMillis()
            )
            
            _messages.value = _messages.value + typingMessage
            
            when (val result = chatRepository.sendMessage(session.id, session.documentId, content)) {
                is NetworkResult.Success -> {
                    val (userMessage, aiMessage) = result.data
                    
                    // Remove typing indicator and add actual messages
                    _messages.value = _messages.value
                        .filter { it.id != typingMessage.id }
                        .plus(userMessage)
                        .plus(aiMessage)
                    
                    _isTyping.value = false
                }
                is NetworkResult.Error -> {
                    _errorMessage.value = result.message
                    _isTyping.value = false
                    
                    // Remove typing indicator and refresh to show the user's message
                    _messages.value = _messages.value.filter { it.id != typingMessage.id }
                    refreshMessages()
                }
                is NetworkResult.Loading -> {
                    // Loading state is handled by _isSendingMessage
                }
            }
            
            _isSendingMessage.value = false
        }
    }

    /**
     * Deletes a chat session
     */
    fun deleteChatSession(sessionId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            when (chatRepository.deleteChatSession(sessionId)) {
                is NetworkResult.Success -> {
                    _chatSessions.value = _chatSessions.value.filter { it.id != sessionId }
                    if (_selectedSession.value?.id == sessionId) {
                        _selectedSession.value = null
                        _messages.value = emptyList()
                    }
                }
                is NetworkResult.Error -> {
                    _errorMessage.value = "Failed to delete chat session"
                }
                is NetworkResult.Loading -> {
                    // Loading state is handled by _isLoading
                }
            }
            
            _isLoading.value = false
        }
    }

    /**
     * Deletes a specific message
     */
    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            when (chatRepository.deleteMessage(messageId)) {
                is NetworkResult.Success -> {
                    _messages.value = _messages.value.filter { it.id != messageId }
                }
                is NetworkResult.Error -> {
                    _errorMessage.value = "Failed to delete message"
                }
                is NetworkResult.Loading -> {
                    // Loading state is handled by _isLoading
                }
            }
        }
    }

    /**
     * Gets chat statistics for a document
     */
    fun getChatStats(documentId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            when (val result = chatRepository.getChatStats(documentId)) {
                is NetworkResult.Success -> {
                    // Handle stats - could be emitted through another StateFlow
                    val stats = result.data
                    _errorMessage.value = "Total messages: ${stats["totalMessages"]}"
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
     * Refreshes chat sessions
     */
    fun refreshChatSessions() {
        loadChatSessions()
    }

    /**
     * Refreshes messages for current session
     */
    fun refreshMessages() {
        _selectedSession.value?.let { session ->
            loadMessages(session.id)
        }
    }

    /**
     * Gets user messages from the current session
     */
    fun getUserMessages(): List<Message> {
        return _messages.value.filter { it.messageType == MessageType.USER }
    }

    /**
     * Gets AI messages from the current session
     */
    fun getAIMessages(): List<Message> {
        return _messages.value.filter { it.messageType == MessageType.AI }
    }

    /**
     * Gets the last message from the current session
     */
    fun getLastMessage(): Message? {
        return _messages.value.lastOrNull()
    }

    /**
     * Gets message count for the current session
     */
    fun getMessageCount(): Int {
        return _messages.value.size
    }

    /**
     * Clears the current chat session
     */
    fun clearCurrentSession() {
        _selectedSession.value = null
        _messages.value = emptyList()
    }

    /**
     * Checks if there are any messages in the current session
     */
    fun hasMessages(): Boolean {
        return _messages.value.isNotEmpty()
    }
}

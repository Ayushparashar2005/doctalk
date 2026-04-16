package com.doctalk.app.repository

import com.doctalk.app.data.model.ChatRequest
import com.doctalk.app.data.model.ChatResponse
import com.doctalk.app.data.model.ChatSession
import com.doctalk.app.data.model.Message
import com.doctalk.app.data.model.MessageType
import com.doctalk.app.data.model.MessageMetadata
import com.doctalk.app.network.ApiService
import com.doctalk.app.network.NetworkResult
import com.doctalk.app.network.groq.GroqRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for handling chat operations
 */
@Singleton
class ChatRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val apiService: ApiService,
    private val groqRepository: GroqRepository,
    private val firebaseAuth: FirebaseAuth
) {

    /**
     * Gets all chat sessions for the current user
     */
    suspend fun getChatSessions(): NetworkResult<List<ChatSession>> {
        return try {
            val userId = firebaseAuth.currentUser?.uid
                ?: return NetworkResult.error("User not authenticated")
            
            val sessions = firestore.collection("chat_sessions")
                .whereEqualTo("userId", userId)
                .orderBy("lastMessageAt", Query.Direction.DESCENDING)
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    try {
                        ChatSession.fromMap(document.data ?: emptyMap())
                    } catch (e: Exception) {
                        null
                    }
                }
            
            NetworkResult.success(sessions)
        } catch (e: Exception) {
            NetworkResult.error(e.message ?: "Failed to get chat sessions", e)
        }
    }

    /**
     * Creates a new chat session
     */
    suspend fun createChatSession(documentId: String, title: String): NetworkResult<ChatSession> {
        return try {
            val userId = firebaseAuth.currentUser?.uid
                ?: return NetworkResult.error("User not authenticated")
            
            val sessionId = firestore.collection("chat_sessions").document().id
            val session = ChatSession(
                id = sessionId,
                documentId = documentId,
                userId = userId,
                title = title
            )
            
            firestore.collection("chat_sessions")
                .document(sessionId)
                .set(session.toMap())
                .await()
            
            NetworkResult.success(session)
        } catch (e: Exception) {
            NetworkResult.error(e.message ?: "Failed to create chat session", e)
        }
    }

    /**
     * Gets messages for a specific chat session
     */
    suspend fun getChatMessages(sessionId: String): NetworkResult<List<Message>> {
        return try {
            val messages = firestore.collection("messages")
                .whereEqualTo("sessionId", sessionId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    try {
                        Message.fromMap(document.data ?: emptyMap())
                    } catch (e: Exception) {
                        null
                    }
                }
            
            NetworkResult.success(messages)
        } catch (e: Exception) {
            NetworkResult.error(e.message ?: "Failed to get messages", e)
        }
    }

    /**
     * Listens to message updates for a specific chat session
     */
    fun getChatMessagesFlow(sessionId: String): Flow<List<Message>> = callbackFlow {
        val listener = firestore.collection("messages")
            .whereEqualTo("sessionId", sessionId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val messages = snapshot?.documents?.mapNotNull { document ->
                    try {
                        Message.fromMap(document.data ?: emptyMap())
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                
                trySend(messages)
            }
        
        awaitClose { listener.remove() }
    }

    /**
     * Sends a user message and gets AI response
     */
    suspend fun sendMessage(
        sessionId: String,
        documentId: String,
        content: String
    ): NetworkResult<Pair<Message, Message>> {
        return try {
            val userId = firebaseAuth.currentUser?.uid
                ?: return NetworkResult.error("User not authenticated")
            
            // Create user message
            val userMessageId = firestore.collection("messages").document().id
            val userMessage = Message(
                id = userMessageId,
                documentId = documentId,
                userId = userId,
                content = content,
                messageType = MessageType.USER,
                timestamp = System.currentTimeMillis()
            )
            
            // Save user message to Firestore
            firestore.collection("messages")
                .document(userMessageId)
                .set(userMessage.toMap())
                .await()
            
            // Update chat session
            updateChatSession(sessionId, content, userMessage.timestamp)
            
            // Get document context for RAG (in a real app, this would come from your RAG system)
            val documentContext = getDocumentContext(documentId, content)
            
            // Send query to Groq for LLM response
            val startTime = System.currentTimeMillis()
            val groqResult = groqRepository.sendRAGChatMessage(
                message = content,
                documentContext = documentContext
            )
            
            val responseTime = System.currentTimeMillis() - startTime
            
            when (groqResult) {
                is NetworkResult.Success -> {
                    val chatResponse = groqResult.data
                    
                    // Create AI message
                    val aiMessageId = firestore.collection("messages").document().id
                    val aiMessage = Message(
                        id = aiMessageId,
                        documentId = documentId,
                        userId = userId,
                        content = chatResponse.answer,
                        messageType = MessageType.AI,
                        timestamp = System.currentTimeMillis(),
                        metadata = MessageMetadata(
                            contextUsed = chatResponse.context.isNotEmpty(),
                            responseTime = responseTime,
                            tokensUsed = chatResponse.tokensUsed,
                            modelUsed = chatResponse.modelUsed,
                            confidence = chatResponse.confidence
                        )
                    )
                    
                    // Save AI message to Firestore
                    firestore.collection("messages")
                        .document(aiMessageId)
                        .set(aiMessage.toMap())
                        .await()
                    
                    // Update chat session again
                    updateChatSession(sessionId, chatResponse.answer, aiMessage.timestamp)
                    
                    NetworkResult.success(Pair(userMessage, aiMessage))
                }
                is NetworkResult.Error -> {
                    NetworkResult.error(groqResult.message)
                }
                is NetworkResult.Loading -> {
                    // This shouldn't happen with our implementation
                    NetworkResult.error("Unexpected loading state")
                }
            }
        } catch (e: Exception) {
            NetworkResult.error(e.message ?: "Failed to send message", e)
        }
    }

    /**
     * Updates chat session with last message info
     */
    private suspend fun updateChatSession(
        sessionId: String,
        lastMessageContent: String,
        timestamp: Long
    ) {
        try {
            // Get current session to update message count
            val sessionDoc = firestore.collection("chat_sessions")
                .document(sessionId)
                .get()
                .await()
            
            if (sessionDoc.exists()) {
                val currentCount = sessionDoc.getLong("messageCount")?.toInt() ?: 0
                
                val updates = mapOf(
                    "lastMessageAt" to timestamp,
                    "lastMessagePreview" to lastMessageContent.take(50),
                    "messageCount" to (currentCount + 2) // +2 for user and AI messages
                )
                
                firestore.collection("chat_sessions")
                    .document(sessionId)
                    .update(updates)
                    .await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Deletes a chat session and all its messages
     */
    suspend fun deleteChatSession(sessionId: String): NetworkResult<Unit> {
        return try {
            // Get all messages for the session
            val messagesResult = getChatMessages(sessionId)
            if (messagesResult is NetworkResult.Success) {
                // Delete all messages
                for (message in messagesResult.data) {
                    firestore.collection("messages")
                        .document(message.id)
                        .delete()
                        .await()
                }
            }
            
            // Delete the session
            firestore.collection("chat_sessions")
                .document(sessionId)
                .delete()
                .await()
            
            NetworkResult.success(Unit)
        } catch (e: Exception) {
            NetworkResult.error(e.message ?: "Failed to delete chat session", e)
        }
    }

    /**
     * Deletes a specific message
     */
    suspend fun deleteMessage(messageId: String): NetworkResult<Unit> {
        return try {
            firestore.collection("messages")
                .document(messageId)
                .delete()
                .await()
            
            NetworkResult.success(Unit)
        } catch (e: Exception) {
            NetworkResult.error(e.message ?: "Failed to delete message", e)
        }
    }

    /**
     * Gets document context for RAG (placeholder implementation)
     * In a real app, this would retrieve relevant chunks from your vector database
     */
    private suspend fun getDocumentContext(documentId: String, query: String): List<String> {
        return try {
            // This is a placeholder implementation
            // In a real app, you would:
            // 1. Use your RAG system to search for relevant document chunks
            // 2. Query Pinecone/Weaviate/Chroma with the user's query
            // 3. Return the most relevant document chunks as context
            
            // For now, return empty context - the system will work without context
            // but with better responses when context is available
            emptyList<String>()
            
            // Example implementation would look like:
            /*
            val ragService = RAGService() // Your RAG implementation
            return ragService.searchDocuments(
                documentId = documentId,
                query = query,
                maxResults = 5
            )
            */
        } catch (e: Exception) {
            // If context retrieval fails, continue without context
            emptyList<String>()
        }
    }

    /**
     * Gets chat statistics for a document
     */
    suspend fun getChatStats(documentId: String): NetworkResult<Map<String, Any>> {
        return try {
            val messages = firestore.collection("messages")
                .whereEqualTo("documentId", documentId)
                .get()
                .await()
                .documents
            
            val userMessages = messages.count { 
                Message.fromMap(it.data ?: emptyMap()).messageType == MessageType.USER 
            }
            val aiMessages = messages.count { 
                Message.fromMap(it.data ?: emptyMap()).messageType == MessageType.AI 
            }
            
            val stats = mapOf(
                "totalMessages" to messages.size,
                "userMessages" to userMessages,
                "aiMessages" to aiMessages,
                "lastActivity" to (messages.maxOfOrNull { 
                    Message.fromMap(it.data ?: emptyMap()).timestamp 
                } ?: 0L)
            )
            
            NetworkResult.success(stats)
        } catch (e: Exception) {
            NetworkResult.error(e.message ?: "Failed to get chat stats", e)
        }
    }
}

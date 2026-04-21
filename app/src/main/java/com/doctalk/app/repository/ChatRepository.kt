package com.doctalk.app.repository

import android.content.Context
import com.doctalk.app.data.local.ChatDao
import com.doctalk.app.data.model.ChatRequest
import com.doctalk.app.data.model.ChatResponse
import com.doctalk.app.data.model.ChatSession
import com.doctalk.app.data.model.Message
import com.doctalk.app.data.model.MessageMetadata
import com.doctalk.app.data.model.MessageType
import com.doctalk.app.network.ApiService
import com.doctalk.app.network.NetworkResult
import com.doctalk.app.utils.AppPreferences
import com.doctalk.app.utils.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for handling local chat operations
 */
@Singleton
class ChatRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatDao: ChatDao,
    private val apiService: ApiService
) {

    /**
     * Gets all chat sessions
     */
    suspend fun getChatSessions(): NetworkResult<List<ChatSession>> {
        return try {
            val sessions = chatDao.getChatSessions()
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
            val sessionId = UUID.randomUUID().toString()
            val session = ChatSession(
                id = sessionId,
                documentId = documentId,
                userId = "local_user",
                title = title
            )
            chatDao.insertChatSession(session)
            NetworkResult.success(session)
        } catch (e: Exception) {
            NetworkResult.error(e.message ?: "Failed to create chat session", e)
        }
    }

    /**
     * Listens to message updates for a specific chat session
     */
    fun getChatMessagesFlow(sessionId: String): Flow<List<Message>> = 
        chatDao.getMessagesForSession(sessionId)

    /**
     * Gets all messages for a session as a one-shot fetch.
     */
    suspend fun getChatMessages(sessionId: String): NetworkResult<List<Message>> {
        return try {
            NetworkResult.success(chatDao.getMessagesForSessionList(sessionId))
        } catch (e: Exception) {
            NetworkResult.error(e.message ?: "Failed to get chat messages", e)
        }
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
            // Create user message
            val userMessage = Message(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                documentId = documentId,
                userId = "local_user",
                content = content,
                messageType = MessageType.USER,
                timestamp = System.currentTimeMillis()
            )
            
            chatDao.insertMessage(userMessage)
            
            // Update session
            val session = chatDao.getChatSessions().find { it.id == sessionId }
            session?.let {
                chatDao.updateChatSession(it.copy(
                    lastMessageAt = userMessage.timestamp,
                    lastMessagePreview = content.take(50),
                    messageCount = it.messageCount + 1
                ))
            }

            val request = ChatRequest(
                query = content,
                documentId = documentId,
                userId = "local_user",
                sessionId = sessionId,
                model = AppPreferences.getSelectedGroqModel(context),
                maxContext = 5
            )

            val startTime = System.currentTimeMillis()
            val response = apiService.sendChatQuery(request)
            val responseTime = System.currentTimeMillis() - startTime
            val body = response.body()

            if (response.isSuccessful && body?.success == true && body.data != null) {
                val chatResponse: ChatResponse = body.data
                val aiMessage = Message(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    documentId = documentId,
                    userId = "local_user",
                    content = chatResponse.answer,
                    messageType = MessageType.AI,
                    timestamp = System.currentTimeMillis(),
                    metadata = MessageMetadata(
                        contextUsed = chatResponse.context.isNotEmpty(),
                        responseTime = responseTime,
                        tokensUsed = chatResponse.tokensUsed,
                        modelUsed = chatResponse.modelUsed.ifBlank { Constants.DEFAULT_GROQ_MODEL },
                        confidence = chatResponse.confidence
                    )
                )

                chatDao.insertMessage(aiMessage)

                val updatedSession = chatDao.getChatSessions().find { it.id == sessionId }
                updatedSession?.let {
                    chatDao.updateChatSession(
                        it.copy(
                            lastMessageAt = aiMessage.timestamp,
                            lastMessagePreview = aiMessage.content.take(50),
                            messageCount = it.messageCount + 1
                        )
                    )
                }

                NetworkResult.success(Pair(userMessage, aiMessage))
            } else {
                NetworkResult.error(body?.message ?: response.message() ?: "Failed to get a response from the backend")
            }
        } catch (e: Exception) {
            NetworkResult.error(e.message ?: "Failed to send message", e)
        }
    }

    /**
     * Deletes a chat session
     */
    suspend fun deleteChatSession(sessionId: String): NetworkResult<Unit> {
        return try {
            chatDao.deleteMessagesForSession(sessionId)
            chatDao.deleteChatSession(sessionId)
            NetworkResult.success(Unit)
        } catch (e: Exception) {
            NetworkResult.error(e.message ?: "Failed to delete chat session", e)
        }
    }

    /**
     * Deletes a single message by id.
     */
    suspend fun deleteMessage(messageId: String): NetworkResult<Unit> {
        return try {
            chatDao.deleteMessageById(messageId)
            NetworkResult.success(Unit)
        } catch (e: Exception) {
            NetworkResult.error(e.message ?: "Failed to delete message", e)
        }
    }

    /**
     * Computes simple chat statistics for a document.
     */
    suspend fun getChatStats(documentId: String): NetworkResult<Map<String, Int>> {
        return try {
            val sessions = chatDao.getChatSessions().filter { it.documentId == documentId }
            var totalMessages = 0
            var userMessages = 0
            var aiMessages = 0

            sessions.forEach { session ->
                val messages = chatDao.getMessagesForSessionList(session.id)
                totalMessages += messages.size
                userMessages += messages.count { it.messageType == MessageType.USER }
                aiMessages += messages.count { it.messageType == MessageType.AI }
            }

            NetworkResult.success(
                mapOf(
                    "totalSessions" to sessions.size,
                    "totalMessages" to totalMessages,
                    "userMessages" to userMessages,
                    "aiMessages" to aiMessages
                )
            )
        } catch (e: Exception) {
            NetworkResult.error(e.message ?: "Failed to get chat stats", e)
        }
    }
}

package com.doctalk.app.network.groq

import com.doctalk.app.data.model.ApiResponse
import com.doctalk.app.data.model.ChatResponse
import com.doctalk.app.network.NetworkResult
import com.doctalk.app.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for handling Groq API operations
 */
@Singleton
class GroqRepository @Inject constructor(
    private val groqApiService: GroqApiService
) {

    /**
     * Sends a chat completion request to Groq
     */
    suspend fun sendChatMessage(
        message: String,
        context: List<String> = emptyList(),
        model: String = GroqConfig.DEFAULT_MODEL,
        maxTokens: Int = GroqConfig.DEFAULT_MAX_TOKENS,
        temperature: Float = GroqConfig.DEFAULT_TEMPERATURE
    ): NetworkResult<ChatResponse> {
        return try {
            withContext(Dispatchers.IO) {
                val apiKey = getGroqApiKey()
                if (apiKey.isBlank()) {
                    return@withContext NetworkResult.error("Groq API key not configured")
                }

                // Build messages list
                val messages = mutableListOf<GroqMessage>()
                
                // Add system prompt with context
                val systemPrompt = if (context.isNotEmpty()) {
                    GroqConfig.RAG_SYSTEM_PROMPT
                        .replace("{context}", context.joinToString("\n"))
                        .replace("{question}", message)
                } else {
                    GroqConfig.DEFAULT_SYSTEM_PROMPT
                }
                messages.add(GroqMessage(role = "system", content = systemPrompt))
                
                // Add user message
                messages.add(GroqMessage(role = "user", content = message))

                val request = GroqChatRequest(
                    model = model,
                    messages = messages,
                    maxTokens = maxTokens,
                    temperature = temperature,
                    topP = GroqConfig.DEFAULT_TOP_P,
                    stream = false
                )

                val response = groqApiService.createChatCompletion(
                    authorization = "Bearer $apiKey",
                    request = request
                )

                if (response.isSuccessful) {
                    val groqResponse = response.body()
                    if (groqResponse != null && groqResponse.choices.isNotEmpty()) {
                        val choice = groqResponse.choices.first()
                        val chatResponse = ChatResponse(
                            answer = choice.message.content,
                            context = context,
                            confidence = 1.0f, // Groq doesn't provide confidence scores
                            sources = emptyList(), // Will be populated by RAG system
                            responseTime = 0L, // Will be calculated by caller
                            tokensUsed = groqResponse.usage.totalTokens,
                            modelUsed = groqResponse.model
                        )
                        NetworkResult.success(chatResponse)
                    } else {
                        NetworkResult.error("No response from Groq API")
                    }
                } else {
                    NetworkResult.error("Groq API error: ${response.code()} - ${response.message()}")
                }
            }
        } catch (e: Exception) {
            NetworkResult.error("Failed to send message to Groq: ${e.message}", e)
        }
    }

    /**
     * Sends a simple chat message without context
     */
    suspend fun sendSimpleChatMessage(
        message: String,
        model: String = GroqConfig.DEFAULT_MODEL
    ): NetworkResult<ChatResponse> {
        return sendChatMessage(message, emptyList(), model)
    }

    /**
     * Sends a RAG-enhanced chat message with document context
     */
    suspend fun sendRAGChatMessage(
        message: String,
        documentContext: List<String>,
        model: String = GroqConfig.DEFAULT_MODEL
    ): NetworkResult<ChatResponse> {
        return sendChatMessage(message, documentContext, model)
    }

    /**
     * Gets available Groq models
     */
    fun getAvailableModels(): List<String> {
        return GroqModels.ALL_MODELS
    }

    /**
     * Gets model display name
     */
    fun getModelDisplayName(model: String): String {
        return GroqModels.getModelDisplayName(model)
    }

    /**
     * Validates API key format
     */
    fun validateApiKey(apiKey: String): Boolean {
        return apiKey.startsWith("gsk_") && apiKey.length >= 39
    }

    /**
     * Gets Groq API key from secure storage
     * In a real app, this should be stored securely (e.g., Android Keystore)
     */
    private fun getGroqApiKey(): String {
        // TODO: Implement secure API key storage
        // For now, return placeholder - user should set this
        return System.getenv("GROQ_API_KEY") ?: ""
    }

    /**
     * Sets Groq API key (for development/testing)
     * In production, this should be handled securely
     */
    fun setGroqApiKey(apiKey: String): Boolean {
        return if (validateApiKey(apiKey)) {
            // TODO: Store API key securely
            true
        } else {
            false
        }
    }

    /**
     * Estimates token count for a message (rough approximation)
     */
    fun estimateTokenCount(text: String): Int {
        // Rough approximation: 1 token ~ 4 characters
        return (text.length / 4).coerceAtLeast(1)
    }

    /**
     * Checks if a message is likely to exceed token limits
     */
    fun willExceedTokenLimit(messages: List<GroqMessage>, maxTokens: Int = GroqConfig.DEFAULT_MAX_TOKENS): Boolean {
        val totalTokens = messages.sumOf { estimateTokenCount(it.content) }
        return totalTokens > maxTokens * 0.8 // Leave 20% margin
    }
}

package com.doctalk.app.network.groq

import com.doctalk.app.data.model.ApiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Groq API service interface for LLM responses
 */
interface GroqApiService {

    /**
     * Sends a chat completion request to Groq
     */
    @POST("chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: GroqChatRequest
    ): Response<GroqChatResponse>

    /**
     * Sends a chat completion request with streaming (for future implementation)
     */
    @POST("chat/completions")
    suspend fun createChatCompletionStream(
        @Header("Authorization") authorization: String,
        @Header("Accept") accept: String = "text/event-stream",
        @Body request: GroqChatRequest
    ): Response<GroqChatResponse>
}

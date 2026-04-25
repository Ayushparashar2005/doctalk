package com.doctalk.app.network.groq

import com.google.gson.annotations.SerializedName

/**
 * Groq API request models
 */
data class GroqChatRequest(
    val model: String,
    val messages: List<GroqMessage>,
    @SerializedName("max_tokens")
    val maxTokens: Int = 1000,
    @SerializedName("temperature")
    val temperature: Float = 0.7f,
    @SerializedName("top_p")
    val topP: Float = 1.0f,
    @SerializedName("stream")
    val stream: Boolean = false,
    @SerializedName("stop")
    val stop: List<String>? = null,
    @SerializedName("response_format")
    val responseFormat: GroqResponseFormat? = null
)

/**
 * Groq message model
 */
data class GroqMessage(
    val role: String, // "system", "user", "assistant"
    val content: String
)

/**
 * Groq response format model
 */
data class GroqResponseFormat(
    val type: String = "text"
)

/**
 * Groq API response models
 */
data class GroqChatResponse(
    val id: String,
    @SerializedName("object")
    val objectType: String,
    val created: Long,
    val model: String,
    val choices: List<GroqChoice>,
    val usage: GroqUsage,
    @SerializedName("system_fingerprint")
    val systemFingerprint: String? = null
)

/**
 * Groq choice model
 */
data class GroqChoice(
    val index: Int,
    val message: GroqMessage,
    @SerializedName("logprobs")
    val logProbs: Any? = null,
    @SerializedName("finish_reason")
    val finishReason: String
)

/**
 * Groq usage statistics
 */
data class GroqUsage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int,
    @SerializedName("completion_tokens")
    val completionTokens: Int,
    @SerializedName("total_tokens")
    val totalTokens: Int,
    @SerializedName("prompt_time")
    val promptTime: Float? = null,
    @SerializedName("completion_time")
    val completionTime: Float? = null
)

/**
 * Groq model constants
 */
object GroqModels {
    const val LLAMA3_8B_8192 = "llama3-8b-8192"
    const val LLAMA3_70B_8192 = "llama3-70b-8192"
    const val MIXTRAL_8X7B_INSTRUCT = "mixtral-8x7b-32768"
    const val GEMMA_7B_INSTRUCT = "gemma-7b-it"
    
    val ALL_MODELS = listOf(
        LLAMA3_8B_8192,
        LLAMA3_70B_8192,
        MIXTRAL_8X7B_INSTRUCT,
        GEMMA_7B_INSTRUCT
    )
    
    fun getModelDisplayName(model: String): String {
        return when (model) {
            LLAMA3_8B_8192 -> "Llama 3 8B"
            LLAMA3_70B_8192 -> "Llama 3 70B"
            MIXTRAL_8X7B_INSTRUCT -> "Mixtral 8x7B"
            GEMMA_7B_INSTRUCT -> "Gemma 7B"
            else -> model
        }
    }
}

/**
 * Groq API configuration
 */
object GroqConfig {
    const val BASE_URL = "https://api.groq.com/openai/v1/"
    const val DEFAULT_MODEL = GroqModels.LLAMA3_8B_8192
    const val DEFAULT_MAX_TOKENS = 1000
    const val DEFAULT_TEMPERATURE = 0.7f
    const val DEFAULT_TOP_P = 1.0f
    
    // System prompts for different contexts
    const val DEFAULT_SYSTEM_PROMPT = """
        You are a helpful AI assistant designed to help users understand and analyze documents.
        When answering questions about documents, be concise, accurate, and helpful.
        If you don't have enough context from the document, let the user know.
        Always base your answers on the provided document context when available.
    """
    
    const val RAG_SYSTEM_PROMPT = """
        You are a helpful AI assistant designed to help users understand and analyze documents.
        You have been provided with relevant context from a document to answer the user's question.
        Use the provided context to formulate your answer. If the context doesn't contain enough information to answer the question, say so politely.
        Be concise, accurate, and helpful in your responses.
        
        Context:
        {context}
        
        User Question:
        {question}
        
        Answer:
    """
}

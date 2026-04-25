package com.doctalk.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Generic API response wrapper
 */
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null,
    val message: String? = null
) {
    companion object {
        /**
         * Creates a successful response
         */
        fun <T> success(data: T, message: String? = null): ApiResponse<T> {
            return ApiResponse(success = true, data = data, message = message)
        }

        /**
         * Creates an error response
         */
        fun <T> error(error: String, message: String? = null): ApiResponse<T> {
            return ApiResponse(success = false, error = error, message = message)
        }
    }
}

/**
 * Request model for chat queries
 */
data class ChatRequest(
    val query: String,
    @SerializedName("document_id")
    val documentId: String,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("session_id")
    val sessionId: String? = null,
    val model: String? = null,
    @SerializedName("max_context")
    val maxContext: Int? = null
)

/**
 * Response model for chat queries
 */
data class ChatResponse(
    val success: Boolean = true,
    val message: String? = null,
    val answer: String,
    val context: List<String> = emptyList(),
    val confidence: Float = 0.0f,
    val sources: List<String> = emptyList(),
    @SerializedName("response_time")
    val responseTime: Double = 0.0,
    @SerializedName("tokens_used")
    val tokensUsed: Int = 0,
    @SerializedName("model_used")
    val modelUsed: String = ""
)

/**
 * Request model for document processing
 */
data class DocumentProcessRequest(
    @SerializedName("document_id")
    val documentId: String,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("file_name")
    val fileName: String,
    @SerializedName("file_type")
    val fileType: String,
    @SerializedName("download_url")
    val downloadUrl: String
)

/**
 * Response model for document processing
 */
data class DocumentProcessResponse(
    val success: Boolean,
    @SerializedName("document_id")
    val documentId: String,
    val status: String,
    val message: String? = null,
    @SerializedName("page_count")
    val pageCount: Int? = null,
    @SerializedName("word_count")
    val wordCount: Int? = null,
    @SerializedName("chunk_count")
    val chunkCount: Int? = null,
    @SerializedName("processing_time")
    val processingTime: Double? = null
)

/**
 * Request model for document search.
 */
data class DocumentSearchRequest(
    val query: String,
    @SerializedName("document_id")
    val documentId: String,
    @SerializedName("top_k")
    val topK: Int = 5
)

/**
 * Response model for document search
 */
data class DocumentSearchResponse(
    val success: Boolean,
    val results: List<String>,
    val scores: List<Float>,
    val query: String
)

/**
 * Response model for document status
 */
data class DocumentStatusResponse(
    val success: Boolean,
    @SerializedName("document_id")
    val documentId: String,
    val status: String,
    @SerializedName("error_message")
    val errorMessage: String? = null
)

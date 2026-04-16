package com.doctalk.app.data.model

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
    val documentId: String,
    val userId: String,
    val sessionId: String? = null
)

/**
 * Response model for chat queries
 */
data class ChatResponse(
    val answer: String,
    val context: List<String> = emptyList(),
    val confidence: Float = 0.0f,
    val sources: List<String> = emptyList(),
    val responseTime: Long = 0L,
    val tokensUsed: Int = 0,
    val modelUsed: String = ""
)

/**
 * Request model for document processing
 */
data class DocumentProcessRequest(
    val documentId: String,
    val userId: String,
    val fileName: String,
    val fileType: String,
    val downloadUrl: String
)

/**
 * Response model for document processing
 */
data class DocumentProcessResponse(
    val success: Boolean,
    val documentId: String,
    val status: String,
    val message: String? = null,
    val pageCount: Int? = null,
    val wordCount: Int? = null
)

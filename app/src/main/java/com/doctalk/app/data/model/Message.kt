package com.doctalk.app.data.model

/**
 * Data class representing a chat message
 */
data class Message(
    val id: String = "",
    val documentId: String = "",
    val userId: String = "",
    val content: String = "",
    val messageType: MessageType = MessageType.USER,
    val timestamp: Long = System.currentTimeMillis(),
    val isTyping: Boolean = false,
    val metadata: MessageMetadata? = null
) {
    /**
     * Converts Message to a Map for Firestore
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "documentId" to documentId,
            "userId" to userId,
            "content" to content,
            "messageType" to messageType.name,
            "timestamp" to timestamp,
            "isTyping" to isTyping,
            "metadata" to (metadata?.toMap() ?: emptyMap<String, Any>())
        )
    }

    companion object {
        /**
         * Creates a Message from a Firestore document
         */
        fun fromMap(map: Map<String, Any>): Message {
            return Message(
                id = map["id"] as? String ?: "",
                documentId = map["documentId"] as? String ?: "",
                userId = map["userId"] as? String ?: "",
                content = map["content"] as? String ?: "",
                messageType = try {
                    MessageType.valueOf((map["messageType"] as? String) ?: MessageType.USER.name)
                } catch (e: IllegalArgumentException) {
                    MessageType.USER
                },
                timestamp = (map["timestamp"] as? Long) ?: System.currentTimeMillis(),
                isTyping = map["isTyping"] as? Boolean ?: false,
                metadata = try {
                    val metadataMap = map["metadata"] as? Map<String, Any>
                    metadataMap?.let { MessageMetadata.fromMap(it) }
                } catch (e: Exception) {
                    null
                }
            )
        }
    }
}

/**
 * Enum representing message type
 */
enum class MessageType {
    USER,       // Message sent by user
    AI,         // Message sent by AI assistant
    SYSTEM      // System message (e.g., notifications)
}

/**
 * Data class for message metadata
 */
data class MessageMetadata(
    val contextUsed: Boolean = false,
    val responseTime: Long = 0L, // in milliseconds
    val tokensUsed: Int = 0,
    val modelUsed: String = "",
    val confidence: Float = 0.0f
) {
    /**
     * Converts MessageMetadata to a Map for Firestore
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "contextUsed" to contextUsed,
            "responseTime" to responseTime,
            "tokensUsed" to tokensUsed,
            "modelUsed" to modelUsed,
            "confidence" to confidence
        )
    }

    companion object {
        /**
         * Creates MessageMetadata from a Map
         */
        fun fromMap(map: Map<String, Any>): MessageMetadata {
            return MessageMetadata(
                contextUsed = map["contextUsed"] as? Boolean ?: false,
                responseTime = (map["responseTime"] as? Long) ?: 0L,
                tokensUsed = (map["tokensUsed"] as? Long)?.toInt() ?: 0,
                modelUsed = map["modelUsed"] as? String ?: "",
                confidence = (map["confidence"] as? Double)?.toFloat() ?: 0.0f
            )
        }
    }
}

/**
 * Extension function to check if message is from AI
 */
fun Message.isFromAI(): Boolean {
    return messageType == MessageType.AI
}

/**
 * Extension function to check if message is from user
 */
fun Message.isFromUser(): Boolean {
    return messageType == MessageType.USER
}

/**
 * Extension function to format timestamp
 */
fun Message.getFormattedTime(): String {
    val date = java.util.Date(timestamp)
    val format = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
    return format.format(date)
}

package com.doctalk.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson

/**
 * Data class representing a chat message, stored in local database
 */
@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: String = "",
    val sessionId: String = "",
    val documentId: String = "",
    val userId: String = "",
    val content: String = "",
    val messageType: MessageType = MessageType.USER,
    val timestamp: Long = System.currentTimeMillis(),
    val isTyping: Boolean = false,
    val metadata: MessageMetadata? = null
) {
    /**
     * Converts Message to a Map
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "sessionId" to sessionId,
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
        fun fromMap(map: Map<String, Any>): Message {
            return Message(
                id = map["id"] as? String ?: "",
                sessionId = map["sessionId"] as? String ?: "",
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

enum class MessageType {
    USER, AI, SYSTEM
}

data class MessageMetadata(
    val contextUsed: Boolean = false,
    val responseTime: Long = 0L,
    val tokensUsed: Int = 0,
    val modelUsed: String = "",
    val confidence: Float = 0.0f
) {
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

class Converters {
    @TypeConverter
    fun fromMessageType(value: MessageType): String = value.name

    @TypeConverter
    fun toMessageType(value: String): MessageType = MessageType.valueOf(value)

    @TypeConverter
    fun fromMetadata(value: MessageMetadata?): String? = value?.let { Gson().toJson(it) }

    @TypeConverter
    fun toMetadata(value: String?): MessageMetadata? = value?.let { Gson().fromJson(it, MessageMetadata::class.java) }
}

fun Message.isFromAI(): Boolean = messageType == MessageType.AI
fun Message.isFromUser(): Boolean = messageType == MessageType.USER
fun Message.getFormattedTime(): String {
    val date = java.util.Date(timestamp)
    val format = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
    return format.format(date)
}

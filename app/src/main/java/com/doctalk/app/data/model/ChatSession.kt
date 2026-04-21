package com.doctalk.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Data class representing a chat session for a document
 */
@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey val id: String = "",
    val documentId: String = "",
    val userId: String = "",
    val title: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastMessageAt: Long = System.currentTimeMillis(),
    val messageCount: Int = 0,
    val isActive: Boolean = true,
    val lastMessagePreview: String = ""
) {
    /**
        * Converts ChatSession to a Map for local persistence
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "documentId" to documentId,
            "userId" to userId,
            "title" to title,
            "createdAt" to createdAt,
            "lastMessageAt" to lastMessageAt,
            "messageCount" to messageCount,
            "isActive" to isActive,
            "lastMessagePreview" to lastMessagePreview
        )
    }

    companion object {
        /**
         * Creates a ChatSession from a stored map
         */
        fun fromMap(map: Map<String, Any>): ChatSession {
            return ChatSession(
                id = map["id"] as? String ?: "",
                documentId = map["documentId"] as? String ?: "",
                userId = map["userId"] as? String ?: "",
                title = map["title"] as? String ?: "",
                createdAt = (map["createdAt"] as? Long) ?: System.currentTimeMillis(),
                lastMessageAt = (map["lastMessageAt"] as? Long) ?: System.currentTimeMillis(),
                messageCount = (map["messageCount"] as? Long)?.toInt() ?: 0,
                isActive = map["isActive"] as? Boolean ?: true,
                lastMessagePreview = map["lastMessagePreview"] as? String ?: ""
            )
        }
    }
}

/**
 * Extension function to format last message time
 */
fun ChatSession.getFormattedLastMessageTime(): String {
    val now = System.currentTimeMillis()
    val diff = now - lastMessageAt
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 604_800_000 -> "${diff / 86_400_000}d ago"
        else -> {
            val date = java.util.Date(lastMessageAt)
            val format = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
            format.format(date)
        }
    }
}

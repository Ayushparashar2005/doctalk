package com.doctalk.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Data class representing a document uploaded by a user, stored in local database
 */
@Entity(tableName = "documents")
data class Document(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val fileName: String = "",
    val fileType: String = "", // pdf, txt
    val fileSize: Long = 0L, // in bytes
    val downloadUrl: String = "", // In local mode, this might be a local file path
    val storagePath: String = "",
    val status: DocumentStatus = DocumentStatus.PROCESSING,
    val uploadedAt: Long = System.currentTimeMillis(),
    val processedAt: Long? = null,
    val errorMessage: String? = null,
    val pageCount: Int? = null,
    val wordCount: Int? = null,
    val thumbnailUrl: String? = null
) {
    /**
     * Converts Document to a Map
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "fileName" to fileName,
            "fileType" to fileType,
            "fileSize" to fileSize,
            "downloadUrl" to downloadUrl,
            "storagePath" to storagePath,
            "status" to status.name,
            "uploadedAt" to uploadedAt,
            "processedAt" to (processedAt ?: 0L),
            "errorMessage" to (errorMessage ?: ""),
            "pageCount" to (pageCount ?: 0),
            "wordCount" to (wordCount ?: 0),
            "thumbnailUrl" to (thumbnailUrl ?: "")
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>): Document {
            return Document(
                id = map["id"] as? String ?: "",
                userId = map["userId"] as? String ?: "",
                fileName = map["fileName"] as? String ?: "",
                fileType = map["fileType"] as? String ?: "",
                fileSize = (map["fileSize"] as? Long) ?: 0L,
                downloadUrl = map["downloadUrl"] as? String ?: "",
                storagePath = map["storagePath"] as? String ?: "",
                status = try {
                    DocumentStatus.valueOf((map["status"] as? String) ?: DocumentStatus.PROCESSING.name)
                } catch (e: IllegalArgumentException) {
                    DocumentStatus.PROCESSING
                },
                uploadedAt = (map["uploadedAt"] as? Long) ?: System.currentTimeMillis(),
                processedAt = (map["processedAt"] as? Long)?.takeIf { it > 0L },
                errorMessage = (map["errorMessage"] as? String)?.takeIf { it.isNotEmpty() },
                pageCount = (map["pageCount"] as? Long)?.toInt(),
                wordCount = (map["wordCount"] as? Long)?.toInt(),
                thumbnailUrl = (map["thumbnailUrl"] as? String)?.takeIf { it.isNotEmpty() }
            )
        }
    }
}

/**
 * Enum representing document processing status
 */
enum class DocumentStatus {
    UPLOADING,
    PROCESSING,
    PROCESSED,
    FAILED,
    DELETED
}

/**
 * Extension function to get human-readable status
 */
fun DocumentStatus.getDisplayName(): String {
    return when (this) {
        DocumentStatus.UPLOADING -> "Uploading"
        DocumentStatus.PROCESSING -> "Processing"
        DocumentStatus.PROCESSED -> "Processed"
        DocumentStatus.FAILED -> "Failed"
        DocumentStatus.DELETED -> "Deleted"
    }
}

/**
 * Extension function to check if document is ready for chat
 */
fun DocumentStatus.isReadyForChat(): Boolean {
    return this == DocumentStatus.PROCESSED
}

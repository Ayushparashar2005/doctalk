package com.doctalk.app.data.model

/**
 * Data class representing a document uploaded by a user
 */
data class Document(
    val id: String = "",
    val userId: String = "",
    val fileName: String = "",
    val fileType: String = "", // pdf, txt
    val fileSize: Long = 0L, // in bytes
    val downloadUrl: String = "",
    val storagePath: String = "",
    val status: DocumentStatus = DocumentStatus.UPLOADING,
    val uploadedAt: Long = System.currentTimeMillis(),
    val processedAt: Long? = null,
    val errorMessage: String? = null,
    val pageCount: Int? = null,
    val wordCount: Int? = null,
    val thumbnailUrl: String? = null
) {
    /**
     * Converts Document to a Map for Firestore
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
        /**
         * Creates a Document from a Firestore document
         */
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
                    DocumentStatus.valueOf((map["status"] as? String) ?: DocumentStatus.UPLOADING.name)
                } catch (e: IllegalArgumentException) {
                    DocumentStatus.UPLOADING
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
    UPLOADING,    // Document is being uploaded to Firebase Storage
    PROCESSING,   // Document is being processed by backend
    PROCESSED,    // Document has been processed and is ready for chat
    FAILED,       // Document processing failed
    DELETED       // Document has been deleted
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

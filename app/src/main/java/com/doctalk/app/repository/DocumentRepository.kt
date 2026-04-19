package com.doctalk.app.repository

import com.doctalk.app.data.local.DocumentDao
import com.doctalk.app.data.model.Document
import com.doctalk.app.data.model.DocumentStatus
import com.doctalk.app.network.NetworkResult
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for handling local document operations
 */
@Singleton
class DocumentRepository @Inject constructor(
    private val documentDao: DocumentDao
) {

    /**
     * Gets all documents for the current user
     */
    suspend fun getUserDocuments(): NetworkResult<List<Document>> {
        return try {
            NetworkResult.success(documentDao.getAllDocumentsList())
        } catch (e: Exception) {
            NetworkResult.error(e.message ?: "Failed to get documents", e)
        }
    }

    /**
     * Listens to document changes
     */
    fun getUserDocumentsFlow(): Flow<List<Document>> = documentDao.getAllDocuments()

    /**
     * Uploads (saves locally) a document
     */
    suspend fun uploadDocument(
        file: File,
        fileName: String,
        fileType: String
    ): NetworkResult<Document> {
        return try {
            val documentId = UUID.randomUUID().toString()
            
            // In local mode, we just record the file info
            val document = Document(
                id = documentId,
                userId = "local_user",
                fileName = fileName,
                fileType = fileType,
                fileSize = file.length(),
                downloadUrl = file.absolutePath, // Local path
                storagePath = file.absolutePath,
                status = DocumentStatus.PROCESSED // Mark as processed immediately in local mode
            )
            
            documentDao.insertDocument(document)
            NetworkResult.success(document)
        } catch (e: Exception) {
            NetworkResult.error(e.message ?: "Upload failed", e)
        }
    }

    /**
     * Gets a specific document by ID
     */
    suspend fun getDocument(documentId: String): NetworkResult<Document> {
        return try {
            val document = documentDao.getDocumentById(documentId)
            if (document != null) {
                NetworkResult.success(document)
            } else {
                NetworkResult.error("Document not found")
            }
        } catch (e: Exception) {
            NetworkResult.error(e.message ?: "Failed to get document", e)
        }
    }

    /**
     * Deletes a document
     */
    suspend fun deleteDocument(documentId: String): NetworkResult<Unit> {
        return try {
            val document = documentDao.getDocumentById(documentId)
            if (document != null) {
                documentDao.deleteDocument(document)
                NetworkResult.success(Unit)
            } else {
                NetworkResult.error("Document not found")
            }
        } catch (e: Exception) {
            NetworkResult.error(e.message ?: "Failed to delete document", e)
        }
    }

    /**
     * Updates document status
     */
    suspend fun updateDocumentStatus(
        documentId: String,
        status: DocumentStatus,
        errorMessage: String? = null,
        pageCount: Int? = null,
        wordCount: Int? = null
    ): NetworkResult<Unit> {
        return try {
            val document = documentDao.getDocumentById(documentId)
            if (document != null) {
                val updatedDocument = document.copy(
                    status = status,
                    errorMessage = errorMessage,
                    pageCount = pageCount,
                    wordCount = wordCount,
                    processedAt = if (status == DocumentStatus.PROCESSED) System.currentTimeMillis() else document.processedAt
                )
                documentDao.updateDocument(updatedDocument)
                NetworkResult.success(Unit)
            } else {
                NetworkResult.error("Document not found")
            }
        } catch (e: Exception) {
            NetworkResult.error(e.message ?: "Failed to update status", e)
        }
    }

    /**
     * Returns a lightweight local summary for a document.
     */
    suspend fun getDocumentSummary(documentId: String): NetworkResult<String> {
        return try {
            val document = documentDao.getDocumentById(documentId)
                ?: return NetworkResult.error("Document not found")

            val summary = buildString {
                append("File: ${document.fileName}. ")
                append("Type: ${document.fileType.uppercase()}. ")
                append("Status: ${document.status.name.lowercase().replaceFirstChar { it.uppercase() }}. ")
                document.pageCount?.let { append("Pages: $it. ") }
                document.wordCount?.let { append("Words: $it. ") }
            }

            NetworkResult.success(summary.trim())
        } catch (e: Exception) {
            NetworkResult.error(e.message ?: "Failed to get document summary", e)
        }
    }

    /**
     * Performs a basic local metadata search for a document.
     */
    suspend fun searchDocument(documentId: String, query: String): NetworkResult<List<String>> {
        return try {
            val document = documentDao.getDocumentById(documentId)
                ?: return NetworkResult.error("Document not found")

            val normalizedQuery = query.trim().lowercase()
            val matches = mutableListOf<String>()

            if (document.fileName.lowercase().contains(normalizedQuery)) {
                matches.add("Matched file name: ${document.fileName}")
            }
            if (document.fileType.lowercase().contains(normalizedQuery)) {
                matches.add("Matched file type: ${document.fileType}")
            }
            document.errorMessage?.let {
                if (it.lowercase().contains(normalizedQuery)) {
                    matches.add("Matched error message: $it")
                }
            }

            NetworkResult.success(matches)
        } catch (e: Exception) {
            NetworkResult.error(e.message ?: "Failed to search document", e)
        }
    }
}

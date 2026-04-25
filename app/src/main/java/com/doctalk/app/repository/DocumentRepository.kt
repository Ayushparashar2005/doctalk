package com.doctalk.app.repository

import com.doctalk.app.data.local.DocumentDao
import com.doctalk.app.data.model.Document
import com.doctalk.app.data.model.DocumentStatus
import com.doctalk.app.network.ApiService
import com.doctalk.app.network.NetworkResult
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for handling local document operations
 */
@Singleton
class DocumentRepository @Inject constructor(
    private val documentDao: DocumentDao,
    private val apiService: ApiService
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
            val fileBody = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", fileName, fileBody)
            val userIdPart = "local_user".toRequestBody("text/plain".toMediaType())

            val response = apiService.uploadDocument(filePart, userIdPart)
            if (!response.isSuccessful || response.body() == null) {
                val errorBody = response.errorBody()?.string()?.takeIf { it.isNotBlank() }
                return NetworkResult.error(errorBody ?: "Upload failed with code ${response.code()}")
            }

            val uploadResponse = response.body()!!
            val status = try {
                DocumentStatus.valueOf(uploadResponse.status.uppercase())
            } catch (_: IllegalArgumentException) {
                DocumentStatus.PROCESSING
            }

            val document = Document(
                id = uploadResponse.documentId,
                userId = "local_user",
                fileName = fileName,
                fileType = fileType,
                fileSize = file.length(),
                downloadUrl = "",
                storagePath = "",
                status = status
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
     * Checks and updates the document status from the backend
     */
    suspend fun checkAndUpdateDocumentStatus(documentId: String): NetworkResult<DocumentStatus> {
        return try {
            val response = apiService.getDocumentStatus(documentId)
            if (!response.isSuccessful || response.body() == null) {
                return NetworkResult.error("Failed to get status")
            }

            val statusResponse = response.body()!!
            val newStatus = try {
                DocumentStatus.valueOf(statusResponse.status.uppercase())
            } catch (_: IllegalArgumentException) {
                DocumentStatus.PROCESSING
            }

            val document = documentDao.getDocumentById(documentId)
            if (document != null && document.status != newStatus) {
                documentDao.updateDocument(
                    document.copy(
                        status = newStatus,
                        errorMessage = statusResponse.errorMessage,
                        processedAt = if (newStatus == DocumentStatus.PROCESSED) System.currentTimeMillis() else document.processedAt
                    )
                )
            }
            NetworkResult.success(newStatus)
        } catch (e: Exception) {
            NetworkResult.error(e.message ?: "Failed to check status", e)
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

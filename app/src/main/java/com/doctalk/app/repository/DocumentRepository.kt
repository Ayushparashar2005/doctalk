package com.doctalk.app.repository

import com.doctalk.app.data.model.Document
import com.doctalk.app.data.model.DocumentStatus
import com.doctalk.app.data.model.DocumentProcessRequest
import com.doctalk.app.data.model.DocumentProcessResponse
import com.doctalk.app.network.ApiService
import com.doctalk.app.network.NetworkResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for handling document operations
 */
@Singleton
class DocumentRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val apiService: ApiService,
    private val firebaseAuth: FirebaseAuth
) {

    /**
     * Gets all documents for the current user
     */
    suspend fun getUserDocuments(): NetworkResult<List<Document>> {
        return try {
            val userId = firebaseAuth.currentUser?.uid
                ?: return NetworkResult.error("User not authenticated")
            
            val documents = firestore.collection("documents")
                .whereEqualTo("userId", userId)
                .orderBy("uploadedAt", Query.Direction.DESCENDING)
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    try {
                        Document.fromMap(document.data ?: emptyMap())
                    } catch (e: Exception) {
                        null
                    }
                }
            
            NetworkResult.success(documents)
        } catch (e: Exception) {
            NetworkResult.error(e.message ?: "Failed to get documents", e)
        }
    }

    /**
     * Listens to document changes for the current user
     */
    fun getUserDocumentsFlow(): Flow<List<Document>> = callbackFlow {
        val userId = firebaseAuth.currentUser?.uid
        
        if (userId == null) {
            close(Exception("User not authenticated"))
            return@callbackFlow
        }
        
        val listener = firestore.collection("documents")
            .whereEqualTo("userId", userId)
            .orderBy("uploadedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val documents = snapshot?.documents?.mapNotNull { document ->
                    try {
                        Document.fromMap(document.data ?: emptyMap())
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                
                trySend(documents)
            }
        
        awaitClose { listener.remove() }
    }

    /**
     * Uploads a document to Firebase Storage and creates Firestore document
     */
    suspend fun uploadDocument(
        file: File,
        fileName: String,
        fileType: String
    ): NetworkResult<Document> {
        return try {
            val userId = firebaseAuth.currentUser?.uid
                ?: return NetworkResult.error("User not authenticated")
            
            // Create document reference in Firestore first
            val documentId = firestore.collection("documents").document().id
            val storagePath = "documents/$userId/$documentId"
            
            val document = Document(
                id = documentId,
                userId = userId,
                fileName = fileName,
                fileType = fileType,
                fileSize = file.length(),
                storagePath = storagePath,
                status = DocumentStatus.UPLOADING
            )
            
            // Save document to Firestore
            firestore.collection("documents")
                .document(documentId)
                .set(document.toMap())
                .await()
            
            // Upload file to Firebase Storage
            val storageRef = storage.reference.child(storagePath)
            val fileInputStream = FileInputStream(file)
            val uploadTask = storageRef.putStream(fileInputStream)
            
            uploadTask.await()
            
            // Get download URL
            val downloadUrl = storageRef.downloadUrl.await().toString()
            
            // Update document with download URL
            val updatedDocument = document.copy(
                downloadUrl = downloadUrl,
                status = DocumentStatus.PROCESSING
            )
            
            firestore.collection("documents")
                .document(documentId)
                .update(updatedDocument.toMap())
                .await()
            
            // Trigger backend processing
            triggerDocumentProcessing(updatedDocument)
            
            NetworkResult.success(updatedDocument)
        } catch (e: Exception) {
            NetworkResult.error(e.message ?: "Upload failed", e)
        }
    }

    /**
     * Triggers document processing in the backend
     */
    private suspend fun triggerDocumentProcessing(document: Document): NetworkResult<DocumentProcessResponse> {
        return try {
            val request = DocumentProcessRequest(
                documentId = document.id,
                userId = document.userId,
                fileName = document.fileName,
                fileType = document.fileType,
                downloadUrl = document.downloadUrl
            )
            
            val response = apiService.processDocument(request)
            if (response.isSuccessful) {
                val apiResponse = response.body()
                if (apiResponse?.success == true) {
                    NetworkResult.success(apiResponse.data ?: DocumentProcessResponse(
                        success = false,
                        documentId = document.id,
                        status = "failed",
                        message = "No data received"
                    ))
                } else {
                    NetworkResult.error(apiResponse?.error ?: "Processing failed")
                }
            } else {
                NetworkResult.error("API call failed: ${response.code()}")
            }
        } catch (e: Exception) {
            NetworkResult.error(e.message ?: "Failed to trigger processing", e)
        }
    }

    /**
     * Gets a specific document by ID
     */
    suspend fun getDocument(documentId: String): NetworkResult<Document> {
        return try {
            val document = firestore.collection("documents")
                .document(documentId)
                .get()
                .await()
            
            if (document.exists()) {
                val doc = Document.fromMap(document.data ?: emptyMap())
                NetworkResult.success(doc)
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
            // Get document first to get storage path
            val documentResult = getDocument(documentId)
            if (documentResult is NetworkResult.Success) {
                val document = documentResult.data
                
                // Delete from Firestore
                firestore.collection("documents")
                    .document(documentId)
                    .delete()
                    .await()
                
                // Delete from Firebase Storage
                if (document.storagePath.isNotEmpty()) {
                    storage.reference.child(document.storagePath)
                        .delete()
                        .await()
                }
                
                // Delete from backend
                try {
                    apiService.deleteDocument(documentId)
                } catch (e: Exception) {
                    // Don't fail the operation if backend deletion fails
                    e.printStackTrace()
                }
                
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
            val updates = mutableMapOf<String, Any>(
                "status" to status.name
            )
            
            errorMessage?.let { updates["errorMessage"] = it }
            pageCount?.let { updates["pageCount"] = it }
            wordCount?.let { updates["wordCount"] = it }
            
            if (status == DocumentStatus.PROCESSED) {
                updates["processedAt"] = System.currentTimeMillis()
            }
            
            firestore.collection("documents")
                .document(documentId)
                .update(updates)
                .await()
            
            NetworkResult.success(Unit)
        } catch (e: Exception) {
            NetworkResult.error(e.message ?: "Failed to update document status", e)
        }
    }

    /**
     * Gets document summary from backend
     */
    suspend fun getDocumentSummary(documentId: String): NetworkResult<String> {
        return try {
            val response = apiService.getDocumentSummary(documentId)
            if (response.isSuccessful) {
                val apiResponse = response.body()
                if (apiResponse?.success == true) {
                    NetworkResult.success(apiResponse.data ?: "No summary available")
                } else {
                    NetworkResult.error(apiResponse?.error ?: "Failed to get summary")
                }
            } else {
                NetworkResult.error("API call failed: ${response.code()}")
            }
        } catch (e: Exception) {
            NetworkResult.error(e.message ?: "Failed to get document summary", e)
        }
    }

    /**
     * Searches within a document
     */
    suspend fun searchDocument(documentId: String, query: String): NetworkResult<List<String>> {
        return try {
            val response = apiService.searchDocument(
                documentId = documentId,
                query = mapOf("query" to query)
            )
            if (response.isSuccessful) {
                val apiResponse = response.body()
                if (apiResponse?.success == true) {
                    NetworkResult.success(apiResponse.data ?: emptyList())
                } else {
                    NetworkResult.error(apiResponse?.error ?: "Search failed")
                }
            } else {
                NetworkResult.error("API call failed: ${response.code()}")
            }
        } catch (e: Exception) {
            NetworkResult.error(e.message ?: "Search failed", e)
        }
    }
}

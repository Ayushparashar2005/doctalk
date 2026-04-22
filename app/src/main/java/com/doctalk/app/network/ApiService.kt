package com.doctalk.app.network

import com.doctalk.app.data.model.ApiResponse
import com.doctalk.app.data.model.ChatRequest
import com.doctalk.app.data.model.ChatResponse
import com.doctalk.app.data.model.DocumentSearchRequest
import com.doctalk.app.data.model.DocumentProcessRequest
import com.doctalk.app.data.model.DocumentProcessResponse
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Part
import okhttp3.MultipartBody
import okhttp3.RequestBody

/**
 * API service interface for backend communication
 */
interface ApiService {

    /**
     * Uploads a document file for local storage and background RAG processing.
     */
    @Multipart
    @POST("documents/upload")
    suspend fun uploadDocument(
        @Part file: MultipartBody.Part,
        @Part("user_id") userId: RequestBody
    ): Response<DocumentProcessResponse>

    /**
     * Sends a chat query to the RAG backend
     */
    @POST("chat/query")
    suspend fun sendChatQuery(@Body request: ChatRequest): Response<ApiResponse<ChatResponse>>

    /**
     * Triggers document processing in the backend
     */
    @POST("documents/process")
    suspend fun processDocument(@Body request: DocumentProcessRequest): Response<ApiResponse<DocumentProcessResponse>>

    /**
     * Gets processing status of a document
     */
    @GET("documents/{documentId}/status")
    suspend fun getDocumentStatus(@Path("documentId") documentId: String): Response<ApiResponse<String>>

    /**
     * Deletes a document from the backend
     */
    @DELETE("documents/{documentId}/delete")
    suspend fun deleteDocument(@Path("documentId") documentId: String): Response<ApiResponse<Unit>>

    /**
     * Gets document summary
     */
    @GET("documents/{documentId}/summary")
    suspend fun getDocumentSummary(@Path("documentId") documentId: String): Response<ApiResponse<String>>

    /**
     * Searches within a document
     */
    @POST("documents/{documentId}/search")
    suspend fun searchDocument(
        @Path("documentId") documentId: String,
        @Body query: DocumentSearchRequest
    ): Response<ApiResponse<List<String>>>
}

package com.doctalk.app.repository

import com.doctalk.app.network.NetworkResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local-only repository for handling notification operations
 */
@Singleton
class NotificationRepository @Inject constructor() {

    /**
     * FCM not used in local mode
     */
    suspend fun getFCMToken(): NetworkResult<String> {
        return NetworkResult.success("local_token")
    }

    /**
     * Local implementation of saving token (no-op)
     */
    suspend fun saveFCMToken(token: String): NetworkResult<Unit> {
        return NetworkResult.success(Unit)
    }

    /**
     * Local implementation of updating settings (no-op)
     */
    suspend fun updateNotificationSettings(enabled: Boolean): NetworkResult<Unit> {
        return NetworkResult.success(Unit)
    }

    suspend fun subscribeToDocumentTopic(documentId: String): NetworkResult<Unit> {
        return NetworkResult.success(Unit)
    }

    suspend fun unsubscribeFromDocumentTopic(documentId: String): NetworkResult<Unit> {
        return NetworkResult.success(Unit)
    }
}

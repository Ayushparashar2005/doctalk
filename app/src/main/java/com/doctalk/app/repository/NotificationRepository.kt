package com.doctalk.app.repository

import com.doctalk.app.network.NetworkResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for handling notification operations
 */
@Singleton
class NotificationRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) {

    /**
     * Gets FCM token for the current user
     */
    suspend fun getFCMToken(): NetworkResult<String> {
        return try {
            val token = FirebaseMessaging.getInstance().token.await()
            NetworkResult.success(token)
        } catch (e: Exception) {
            NetworkResult.error(e.message ?: "Failed to get FCM token", e)
        }
    }

    /**
     * Saves FCM token to Firestore
     */
    suspend fun saveFCMToken(token: String): NetworkResult<Unit> {
        return try {
            val userId = firebaseAuth.currentUser?.uid
                ?: return NetworkResult.error("User not authenticated")
            
            firestore.collection("users")
                .document(userId)
                .update("fcmToken", token)
                .await()
            
            NetworkResult.success(Unit)
        } catch (e: Exception) {
            NetworkResult.error(e.message ?: "Failed to save FCM token", e)
        }
    }

    /**
     * Enables/disables notifications for the user
     */
    suspend fun updateNotificationSettings(enabled: Boolean): NetworkResult<Unit> {
        return try {
            val userId = firebaseAuth.currentUser?.uid
                ?: return NetworkResult.error("User not authenticated")
            
            firestore.collection("users")
                .document(userId)
                .update("notificationsEnabled", enabled)
                .await()
            
            NetworkResult.success(Unit)
        } catch (e: Exception) {
            NetworkResult.error(e.message ?: "Failed to update notification settings", e)
        }
    }

    /**
     * Subscribes to a topic for document notifications
     */
    suspend fun subscribeToDocumentTopic(documentId: String): NetworkResult<Unit> {
        return try {
            FirebaseMessaging.getInstance()
                .subscribeToTopic("document_$documentId")
                .await()
            
            NetworkResult.success(Unit)
        } catch (e: Exception) {
            NetworkResult.error(e.message ?: "Failed to subscribe to topic", e)
        }
    }

    /**
     * Unsubscribes from a document topic
     */
    suspend fun unsubscribeFromDocumentTopic(documentId: String): NetworkResult<Unit> {
        return try {
            FirebaseMessaging.getInstance()
                .unsubscribeFromTopic("document_$documentId")
                .await()
            
            NetworkResult.success(Unit)
        } catch (e: Exception) {
            NetworkResult.error(e.message ?: "Failed to unsubscribe from topic", e)
        }
    }
}

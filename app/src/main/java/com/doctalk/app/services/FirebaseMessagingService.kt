package com.doctalk.app.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.doctalk.app.R
import com.doctalk.app.presentation.MainActivity
import com.doctalk.app.utils.Constants
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * Firebase Cloud Messaging service for handling push notifications
 */
@AndroidEntryPoint
class FirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationManager: NotificationManagerCompat

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("FCM Token: $token")
        
        // Save the token to Firestore or send it to your backend
        saveFCMToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Timber.d("Received FCM message: ${remoteMessage.messageId}")
        
        // Check if message contains a notification payload
        remoteMessage.notification?.let { notification ->
            showNotification(
                title = notification.title ?: getString(R.string.app_name),
                body = notification.body ?: "",
                documentId = remoteMessage.data["documentId"],
                sessionId = remoteMessage.data["sessionId"]
            )
        }
        
        // Check if message contains data payload
        if (remoteMessage.data.isNotEmpty()) {
            handleDataMessage(remoteMessage.data)
        }
    }

    /**
     * Saves FCM token to Firestore
     */
    private fun saveFCMToken(token: String) {
        // This would typically be handled by a repository
        // For now, we'll just log it
        Timber.d("FCM Token saved: $token")
    }

    /**
     * Handles data messages from FCM
     */
    private fun handleDataMessage(data: Map<String, String>) {
        val type = data["type"]
        
        when (type) {
            "document_processed" -> {
                val documentId = data["documentId"]
                val documentName = data["documentName"]
                
                showNotification(
                    title = getString(R.string.document_processed),
                    body = getString(R.string.document_processed_message),
                    documentId = documentId
                )
            }
            "new_message" -> {
                val sessionId = data["sessionId"]
                val message = data["message"]
                
                showNotification(
                    title = getString(R.string.new_message),
                    body = message ?: "",
                    sessionId = sessionId
                )
            }
        }
    }

    /**
     * Shows a notification
     */
    private fun showNotification(
        title: String,
        body: String,
        documentId: String? = null,
        sessionId: String? = null
    ) {
        // Create notification channel (required for Android 8.0+)
        createNotificationChannel()
        
        // Create intent to open MainActivity
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            documentId?.let { putExtra(Constants.EXTRA_DOCUMENT_ID, it) }
            sessionId?.let { putExtra(Constants.EXTRA_SESSION_ID, it) }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build notification
        val notification = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        // Show notification
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    /**
     * Creates notification channel (required for Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                Constants.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = Constants.NOTIFICATION_CHANNEL_DESCRIPTION
            }
            
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}

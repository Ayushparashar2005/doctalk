package com.doctalk.app.utils

/**
 * Constants used throughout the application
 */
object Constants {
    
    // API Configuration
    const val BASE_URL = "https://your-backend-api.com/api/" // Replace with actual backend URL
    const val API_TIMEOUT = 30_000L // 30 seconds
    
    // Firebase Collections
    const val USERS_COLLECTION = "users"
    const val DOCUMENTS_COLLECTION = "documents"
    const val MESSAGES_COLLECTION = "messages"
    const val CHAT_SESSIONS_COLLECTION = "chat_sessions"
    const val NOTIFICATIONS_COLLECTION = "notifications"
    
    // Firebase Storage Paths
    const val DOCUMENTS_STORAGE_PATH = "documents/"
    const val THUMBNAILS_STORAGE_PATH = "thumbnails/"
    
    // File Upload Limits
    const val MAX_FILE_SIZE = 10 * 1024 * 1024L // 10MB in bytes
    const val SUPPORTED_PDF_MIME_TYPE = "application/pdf"
    const val SUPPORTED_TEXT_MIME_TYPE = "text/plain"
    val SUPPORTED_MIME_TYPES = listOf(SUPPORTED_PDF_MIME_TYPE, SUPPORTED_TEXT_MIME_TYPE)
    
    // Chat Configuration
    const val MAX_MESSAGE_LENGTH = 4000
    const val TYPING_INDICATOR_DURATION = 3000L // 3 seconds
    const val MESSAGE_LOAD_BATCH_SIZE = 20
    
    // UI Configuration
    const val ANIMATION_DURATION = 300
    const val SPLASH_SCREEN_DURATION = 2000L // 2 seconds
    const val DEBOUNCE_DELAY = 500L // 0.5 seconds
    
    // Error Messages
    const val ERROR_NETWORK = "Network error. Please check your connection."
    const val ERROR_AUTHENTICATION = "Authentication failed. Please sign in again."
    const val ERROR_FILE_TOO_LARGE = "File size exceeds the maximum limit of 10MB."
    const val ERROR_UNSUPPORTED_FILE_TYPE = "Unsupported file type. Please upload PDF or TXT files."
    const val ERROR_DOCUMENT_PROCESSING = "Document processing failed. Please try again."
    const val ERROR_UPLOAD_FAILED = "Upload failed. Please try again."
    const val ERROR_CHAT_FAILED = "Failed to send message. Please try again."
    
    // Success Messages
    const val SUCCESS_DOCUMENT_UPLOADED = "Document uploaded successfully."
    const val SUCCESS_DOCUMENT_PROCESSED = "Document processed successfully."
    const val SUCCESS_MESSAGE_SENT = "Message sent successfully."
    
    // Shared Preferences Keys
    const val PREFS_NAME = "doctalk_prefs"
    const val PREF_USER_ID = "user_id"
    const val PREF_USER_EMAIL = "user_email"
    const val PREF_IS_LOGGED_IN = "is_logged_in"
    const val PREF_DARK_MODE = "dark_mode"
    const val PREF_NOTIFICATION_ENABLED = "notification_enabled"
    
    // Notification Channels
    const val NOTIFICATION_CHANNEL_ID = "doctalk_notifications"
    const val NOTIFICATION_CHANNEL_NAME = "DocTalk Notifications"
    const val NOTIFICATION_CHANNEL_DESCRIPTION = "Notifications for document processing and chat updates"
    
    // Intent Extras
    const val EXTRA_DOCUMENT_ID = "document_id"
    const val EXTRA_DOCUMENT_NAME = "document_name"
    const val EXTRA_SESSION_ID = "session_id"
    const val EXTRA_CHAT_TITLE = "chat_title"
    
    // RAG Configuration
    const val RAG_CONTEXT_LIMIT = 5
    const val RAG_MAX_TOKENS = 1000
    const val RAG_TEMPERATURE = 0.7f
    
    // Animation Durations
    const val FADE_IN_DURATION = 300
    const val FADE_OUT_DURATION = 200
    const val SLIDE_IN_DURATION = 400
    const val SLIDE_OUT_DURATION = 300
    
    // Pagination
    const val DEFAULT_PAGE_SIZE = 20
    const val MAX_PAGE_SIZE = 50
}

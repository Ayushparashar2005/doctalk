package com.doctalk.app.data.model

import com.google.firebase.auth.FirebaseUser

/**
 * Data class representing a user in the system
 */
data class User(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis(),
    val isEmailVerified: Boolean = false,
    val provider: String = "email" // email, google, etc.
) {
    companion object {
        /**
         * Creates a User object from FirebaseUser
         */
        fun fromFirebaseUser(firebaseUser: FirebaseUser): User {
            return User(
                id = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                displayName = firebaseUser.displayName ?: "",
                photoUrl = firebaseUser.photoUrl?.toString(),
                isEmailVerified = firebaseUser.isEmailVerified,
                provider = getProvider(firebaseUser)
            )
        }

        /**
         * Determines the authentication provider
         */
        private fun getProvider(firebaseUser: FirebaseUser): String {
            return when {
                firebaseUser.providerData.any { it.providerId == "google.com" } -> "google"
                firebaseUser.providerData.any { it.providerId == "password" } -> "email"
                else -> "unknown"
            }
        }
    }

    /**
     * Converts User to a Map for Firestore
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "email" to email,
            "displayName" to displayName,
            "photoUrl" to (photoUrl ?: ""),
            "createdAt" to createdAt,
            "lastLoginAt" to lastLoginAt,
            "isEmailVerified" to isEmailVerified,
            "provider" to provider
        )
    }

    companion object {
        /**
         * Creates a User from a Firestore document
         */
        fun fromMap(map: Map<String, Any>): User {
            return User(
                id = map["id"] as? String ?: "",
                email = map["email"] as? String ?: "",
                displayName = map["displayName"] as? String ?: "",
                photoUrl = (map["photoUrl"] as? String)?.takeIf { it.isNotEmpty() },
                createdAt = (map["createdAt"] as? Long) ?: System.currentTimeMillis(),
                lastLoginAt = (map["lastLoginAt"] as? Long) ?: System.currentTimeMillis(),
                isEmailVerified = map["isEmailVerified"] as? Boolean ?: false,
                provider = map["provider"] as? String ?: "email"
            )
        }
    }
}

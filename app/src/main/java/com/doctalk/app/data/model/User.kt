package com.doctalk.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Data class representing a user in the system, stored in local database
 */
@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis(),
    val isEmailVerified: Boolean = false,
    val provider: String = "local"
) {
    /**
     * Converts User to a Map (if needed for some operations)
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
         * Creates a User from a map
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
                provider = map["provider"] as? String ?: "local"
            )
        }
    }
}

package com.doctalk.app.repository

import com.doctalk.app.data.local.UserDao
import com.doctalk.app.data.model.User
import com.doctalk.app.network.NetworkResult
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for handling local authentication operations
 */
@Singleton
class AuthRepository @Inject constructor(
    private val userDao: UserDao
) {

    /**
     * Listens to authentication state changes
     */
    fun getAuthStateFlow(): Flow<User?> = userDao.getFirstUser()

    /**
     * Signs in with email and password (local implementation)
     */
    suspend fun signInWithEmail(email: String, password: String): NetworkResult<User> {
        return try {
            // For local-only, we just "log in" the first user or create one
            val existingUser = userDao.getUserById("local_user")
            if (existingUser != null && existingUser.email == email) {
                userDao.updateUser(existingUser.copy(lastLoginAt = System.currentTimeMillis()))
                NetworkResult.success(existingUser)
            } else {
                NetworkResult.error("Invalid credentials")
            }
        } catch (e: Exception) {
            NetworkResult.error(e.message ?: "Sign in failed", e)
        }
    }

    /**
     * Signs up with email and password (local implementation)
     */
    suspend fun signUpWithEmail(email: String, password: String, displayName: String): NetworkResult<User> {
        return try {
            val user = User(
                id = "local_user",
                email = email,
                displayName = displayName,
                isEmailVerified = true,
                provider = "local"
            )
            userDao.insertUser(user)
            NetworkResult.success(user)
        } catch (e: Exception) {
            NetworkResult.error(e.message ?: "Sign up failed", e)
        }
    }

    /**
     * Signs out the current user
     */
    suspend fun signOut(): NetworkResult<Unit> {
        return try {
            userDao.deleteAllUsers()
            NetworkResult.success(Unit)
        } catch (e: Exception) {
            NetworkResult.error(e.message ?: "Sign out failed", e)
        }
    }

    /**
     * Updates user profile
     */
    suspend fun updateProfile(displayName: String?, photoUrl: String?): NetworkResult<User> {
        return try {
            val user = userDao.getUserById("local_user") ?: return NetworkResult.error("No user found")
            val updatedUser = user.copy(
                displayName = displayName ?: user.displayName,
                photoUrl = photoUrl ?: user.photoUrl
            )
            userDao.updateUser(updatedUser)
            NetworkResult.success(updatedUser)
        } catch (e: Exception) {
            NetworkResult.error(e.message ?: "Failed to update profile", e)
        }
    }
}

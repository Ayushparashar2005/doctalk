package com.doctalk.app.repository

import com.doctalk.app.data.model.User
import com.doctalk.app.network.NetworkResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for handling authentication operations
 */
@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    /**
     * Gets the current authenticated user
     */
    val currentUser: User?
        get() = firebaseAuth.currentUser?.let { User.fromFirebaseUser(it) }

    /**
     * Checks if user is currently authenticated
     */
    val isUserAuthenticated: Boolean
        get() = firebaseAuth.currentUser != null

    /**
     * Listens to authentication state changes
     */
    fun getAuthStateFlow(): Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.let { User.fromFirebaseUser(it) })
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    /**
     * Signs in with email and password
     */
    suspend fun signInWithEmail(email: String, password: String): NetworkResult<User> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            result.user?.let { firebaseUser ->
                val user = User.fromFirebaseUser(firebaseUser)
                saveUserToFirestore(user)
                NetworkResult.success(user)
            } ?: NetworkResult.error("Sign in failed")
        } catch (e: Exception) {
            NetworkResult.error(e.message ?: "Sign in failed", e)
        }
    }

    /**
     * Signs up with email and password
     */
    suspend fun signUpWithEmail(email: String, password: String, displayName: String): NetworkResult<User> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let { firebaseUser ->
                // Update display name
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
                firebaseUser.updateProfile(profileUpdates).await()
                
                val user = User.fromFirebaseUser(firebaseUser).copy(
                    displayName = displayName,
                    isEmailVerified = false
                )
                saveUserToFirestore(user)
                
                // Send email verification
                firebaseUser.sendEmailVerification().await()
                
                NetworkResult.success(user)
            } ?: NetworkResult.error("Sign up failed")
        } catch (e: Exception) {
            NetworkResult.error(e.message ?: "Sign up failed", e)
        }
    }

    /**
     * Signs in with Google
     */
    suspend fun signInWithGoogle(account: GoogleSignInAccount): NetworkResult<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            result.user?.let { firebaseUser ->
                val user = User.fromFirebaseUser(firebaseUser)
                saveUserToFirestore(user)
                NetworkResult.success(user)
            } ?: NetworkResult.error("Google sign in failed")
        } catch (e: Exception) {
            NetworkResult.error(e.message ?: "Google sign in failed", e)
        }
    }

    /**
     * Sends password reset email
     */
    suspend fun resetPassword(email: String): NetworkResult<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            NetworkResult.success(Unit)
        } catch (e: Exception) {
            NetworkResult.error(e.message ?: "Failed to send reset email", e)
        }
    }

    /**
     * Signs out the current user
     */
    suspend fun signOut(): NetworkResult<Unit> {
        return try {
            firebaseAuth.signOut()
            NetworkResult.success(Unit)
        } catch (e: Exception) {
            NetworkResult.error(e.message ?: "Sign out failed", e)
        }
    }

    /**
     * Reloads the current user
     */
    suspend fun reloadUser(): NetworkResult<User> {
        return try {
            firebaseAuth.currentUser?.reload()?.await()
            currentUser?.let { NetworkResult.success(it) }
                ?: NetworkResult.error("No user found")
        } catch (e: Exception) {
            NetworkResult.error(e.message ?: "Failed to reload user", e)
        }
    }

    /**
     * Saves user data to Firestore
     */
    private suspend fun saveUserToFirestore(user: User) {
        try {
            firestore.collection("users")
                .document(user.id)
                .set(user.toMap())
                .await()
        } catch (e: Exception) {
            // Log error but don't fail the operation
            e.printStackTrace()
        }
    }

    /**
     * Gets user data from Firestore
     */
    suspend fun getUserFromFirestore(userId: String): NetworkResult<User> {
        return try {
            val document = firestore.collection("users")
                .document(userId)
                .get()
                .await()
            
            if (document.exists()) {
                val user = User.fromMap(document.data ?: emptyMap())
                NetworkResult.success(user)
            } else {
                NetworkResult.error("User not found")
            }
        } catch (e: Exception) {
            NetworkResult.error(e.message ?: "Failed to get user data", e)
        }
    }

    /**
     * Updates user profile
     */
    suspend fun updateProfile(displayName: String?, photoUrl: String?): NetworkResult<User> {
        return try {
            val user = firebaseAuth.currentUser ?: return NetworkResult.error("No user found")
            
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .apply {
                    displayName?.let { setDisplayName(it) }
                    photoUrl?.let { setPhotoUrl(android.net.Uri.parse(it)) }
                }
                .build()
            
            user.updateProfile(profileUpdates).await()
            user.reload().await()
            
            currentUser?.let { NetworkResult.success(it) }
                ?: NetworkResult.error("Failed to update profile")
        } catch (e: Exception) {
            NetworkResult.error(e.message ?: "Failed to update profile", e)
        }
    }
}

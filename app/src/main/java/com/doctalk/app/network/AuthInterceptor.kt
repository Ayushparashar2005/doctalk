package com.doctalk.app.network

import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp interceptor to add Firebase authentication token to API requests
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Get current user
        val currentUser = firebaseAuth.currentUser
        
        // Add auth token if user is logged in
        val authenticatedRequest = if (currentUser != null) {
            currentUser.getIdToken(true).addOnSuccessListener { tokenResult ->
                val token = tokenResult.token
                if (token != null) {
                    originalRequest.newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .addHeader("Content-Type", "application/json")
                        .build()
                } else {
                    originalRequest.newBuilder()
                        .addHeader("Content-Type", "application/json")
                        .build()
                }
            }.addOnFailureListener {
                originalRequest.newBuilder()
                    .addHeader("Content-Type", "application/json")
                    .build()
            }.result
            originalRequest.newBuilder()
                .addHeader("Content-Type", "application/json")
                .build()
        } else {
            originalRequest.newBuilder()
                .addHeader("Content-Type", "application/json")
                .build()
        }
        
        return chain.proceed(authenticatedRequest)
    }
}

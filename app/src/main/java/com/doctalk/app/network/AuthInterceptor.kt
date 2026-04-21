package com.doctalk.app.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local-only OkHttp interceptor
 */
@Singleton
class AuthInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Add only the headers needed by the local backend.
        val localRequest = originalRequest.newBuilder()
            .addHeader("Content-Type", "application/json")
            .build()
        
        return chain.proceed(localRequest)
    }
}

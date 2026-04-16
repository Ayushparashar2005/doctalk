package com.doctalk.app.di

import com.doctalk.app.network.groq.GroqApiService
import com.doctalk.app.network.groq.GroqRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

/**
 * Dagger Hilt module for providing Groq dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object GroqModule {

    /**
     * Provides Groq API service
     */
    @Provides
    @Singleton
    fun provideGroqApiService(): GroqApiService {
        return Retrofit.Builder()
            .baseUrl("https://api.groq.com/openai/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GroqApiService::class.java)
    }

    /**
     * Provides Groq repository
     */
    @Provides
    @Singleton
    fun provideGroqRepository(
        groqApiService: GroqApiService
    ): GroqRepository {
        return GroqRepository(groqApiService)
    }
}

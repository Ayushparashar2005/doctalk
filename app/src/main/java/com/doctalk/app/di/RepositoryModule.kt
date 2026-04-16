package com.doctalk.app.di

import com.doctalk.app.repository.AuthRepository
import com.doctalk.app.repository.ChatRepository
import com.doctalk.app.repository.DocumentRepository
import com.doctalk.app.repository.NotificationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for providing repository dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    /**
     * Provides Authentication repository
     */
    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuth: com.google.firebase.auth.FirebaseAuth,
        firestore: com.google.firebase.firestore.FirebaseFirestore
    ): AuthRepository {
        return AuthRepository(firebaseAuth, firestore)
    }

    /**
     * Provides Document repository
     */
    @Provides
    @Singleton
    fun provideDocumentRepository(
        firestore: com.google.firebase.firestore.FirebaseFirestore,
        storage: com.google.firebase.storage.FirebaseStorage,
        apiService: com.doctalk.app.network.ApiService
    ): DocumentRepository {
        return DocumentRepository(firestore, storage, apiService)
    }

    /**
     * Provides Chat repository
     */
    @Provides
    @Singleton
    fun provideChatRepository(
        firestore: com.google.firebase.firestore.FirebaseFirestore,
        apiService: com.doctalk.app.network.ApiService
    ): ChatRepository {
        return ChatRepository(firestore, apiService)
    }

    /**
     * Provides Notification repository
     */
    @Provides
    @Singleton
    fun provideNotificationRepository(
        firestore: com.google.firebase.firestore.FirebaseFirestore
    ): NotificationRepository {
        return NotificationRepository(firestore)
    }
}

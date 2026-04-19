package com.doctalk.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Empty Hilt module since Firebase is removed
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {
    // Provide any required mocks here if needed
}

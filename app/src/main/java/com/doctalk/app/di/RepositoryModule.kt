package com.doctalk.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Repositories are provided via constructor injection.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
}

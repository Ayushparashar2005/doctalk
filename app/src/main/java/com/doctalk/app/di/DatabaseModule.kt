package com.doctalk.app.di

import android.content.Context
import androidx.room.Room
import com.doctalk.app.data.local.AppDatabase
import com.doctalk.app.data.local.UserDao
import com.doctalk.app.data.local.ChatDao
import com.doctalk.app.data.local.DocumentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "doctalk_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    fun provideChatDao(database: AppDatabase): ChatDao {
        return database.chatDao()
    }

    @Provides
    fun provideDocumentDao(database: AppDatabase): DocumentDao {
        return database.documentDao()
    }
}

package com.doctalk.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.doctalk.app.data.model.User
import com.doctalk.app.data.model.ChatSession
import com.doctalk.app.data.model.Message
import com.doctalk.app.data.model.Document
import com.doctalk.app.data.model.Converters

@Database(entities = [User::class, ChatSession::class, Message::class, Document::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun chatDao(): ChatDao
    abstract fun documentDao(): DocumentDao
}

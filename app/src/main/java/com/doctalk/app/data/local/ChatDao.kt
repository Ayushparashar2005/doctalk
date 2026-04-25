package com.doctalk.app.data.local

import androidx.room.*
import com.doctalk.app.data.model.ChatSession
import com.doctalk.app.data.model.Message
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_sessions ORDER BY lastMessageAt DESC")
    suspend fun getChatSessions(): List<ChatSession>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatSession(session: ChatSession)

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: String): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesForSessionList(sessionId: String): List<Message>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteChatSession(sessionId: String)

    @Query("DELETE FROM messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: String)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: String)

    @Update
    suspend fun updateChatSession(session: ChatSession)
}

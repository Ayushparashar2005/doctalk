package com.doctalk.app.data.local

import androidx.room.*
import com.doctalk.app.data.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: String): User?

    @Query("SELECT * FROM users LIMIT 1")
    fun getFirstUser(): Flow<User?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
}

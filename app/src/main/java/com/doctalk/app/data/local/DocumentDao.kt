package com.doctalk.app.data.local

import androidx.room.*
import com.doctalk.app.data.model.Document
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY uploadedAt DESC")
    fun getAllDocuments(): Flow<List<Document>>

    @Query("SELECT * FROM documents ORDER BY uploadedAt DESC")
    suspend fun getAllDocumentsList(): List<Document>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getDocumentById(id: String): Document?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: Document)

    @Update
    suspend fun updateDocument(document: Document)

    @Delete
    suspend fun deleteDocument(document: Document)
}

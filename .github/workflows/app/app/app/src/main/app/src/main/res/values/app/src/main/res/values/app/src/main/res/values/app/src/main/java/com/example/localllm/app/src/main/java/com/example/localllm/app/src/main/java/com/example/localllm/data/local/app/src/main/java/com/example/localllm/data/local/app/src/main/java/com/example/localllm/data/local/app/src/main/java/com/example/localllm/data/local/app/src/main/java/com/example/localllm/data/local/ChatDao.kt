package com.example.localllm.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_sessions WHERE modelId = :modelId ORDER BY updatedAt DESC")
    fun getSessionsForModel(modelId: String): Flow<List<ChatSessionEntity>>
    
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessageEntity>>
    
    @Insert
    suspend fun insertSession(session: ChatSessionEntity)
    
    @Insert
    suspend fun insertMessage(message: ChatMessageEntity)
    
    @Query("UPDATE chat_sessions SET title = :title, updatedAt = :time WHERE id = :id")
    suspend fun updateSessionTitle(id: String, title: String, time: Long = System.currentTimeMillis())
    
    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun clearSessionMessages(sessionId: String)
    
    @Delete
    suspend fun deleteSession(session: ChatSessionEntity)
}

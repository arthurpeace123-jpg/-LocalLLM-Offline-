package com.example.localllm.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelDao {
    @Query("SELECT * FROM local_models ORDER BY lastUsed DESC, addedDate DESC")
    fun getAllModels(): Flow<List<LocalModel>>
    
    @Query("SELECT * FROM local_models WHERE id = :id")
    suspend fun getModelById(id: String): LocalModel?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModel(model: LocalModel)
    
    @Delete
    suspend fun deleteModel(model: LocalModel)
    
    @Query("UPDATE local_models SET lastUsed = :timestamp WHERE id = :id")
    suspend fun updateLastUsed(id: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE local_models SET isDownloaded = :downloaded, downloadProgress = :progress WHERE id = :id")
    suspend fun updateDownloadStatus(id: String, downloaded: Boolean, progress: Float)
}

package com.example.localllm.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_models")
data class LocalModel(
    @PrimaryKey
    val id: String,
    val name: String,
    val displayName: String,
    val filePath: String,
    val fileSize: Long,
    val parameters: String,
    val modelType: ModelType,
    val isDownloaded: Boolean = false,
    val downloadProgress: Float = 0f,
    val addedDate: Long = System.currentTimeMillis(),
    val lastUsed: Long? = null,
    val contextLength: Int = 2048,
    val description: String = ""
)

enum class ModelType {
    GEMMA, PHI, FALCON, STABLE_LM, CUSTOM
}

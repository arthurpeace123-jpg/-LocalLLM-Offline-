package com.example.localllm.data.repository

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.localllm.data.local.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelRepository @Inject constructor(
    private val modelDao: ModelDao,
    @ApplicationContext private val context: Context
) {
    val allModels: Flow<List<LocalModel>> = modelDao.getAllModels()

    suspend fun importModel(
        uri: Uri,
        name: String,
        displayName: String,
        parameters: String,
        modelType: ModelType,
        description: String = ""
    ): Result<LocalModel> = withContext(Dispatchers.IO) {
        try {
            val documentFile = DocumentFile.fromSingleUri(context, uri)
                ?: return@withContext Result.failure(Exception("Cannot access file"))
            
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Cannot read file"))
            
            val modelsDir = File(context.filesDir, "models").apply { mkdirs() }
            val destFile = File(modelsDir, "$name.bin")
            
            inputStream.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            val model = LocalModel(
                id = UUID.randomUUID().toString(),
                name = name,
                displayName = displayName,
                filePath = destFile.absolutePath,
                fileSize = destFile.length(),
                parameters = parameters,
                modelType = modelType,
                isDownloaded = true,
                description = description
            )
            
            modelDao.insertModel(model)
            Result.success(model)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteModel(model: LocalModel) = withContext(Dispatchers.IO) {
        File(model.filePath).delete()
        modelDao.deleteModel(model)
    }

    suspend fun getModel(id: String): LocalModel? = modelDao.getModelById(id)

    suspend fun updateLastUsed(id: String) = modelDao.updateLastUsed(id)
}

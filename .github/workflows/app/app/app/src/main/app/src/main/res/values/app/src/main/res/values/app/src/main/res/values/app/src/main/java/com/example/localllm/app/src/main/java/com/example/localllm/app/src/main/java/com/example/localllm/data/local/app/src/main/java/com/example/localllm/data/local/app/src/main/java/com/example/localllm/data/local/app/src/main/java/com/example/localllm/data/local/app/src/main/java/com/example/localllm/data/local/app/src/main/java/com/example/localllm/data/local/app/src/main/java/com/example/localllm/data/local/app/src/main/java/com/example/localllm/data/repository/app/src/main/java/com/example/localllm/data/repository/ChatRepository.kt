package com.example.localllm.data.repository

import com.example.localllm.data.local.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val chatDao: ChatDao
) {
    fun getSessions(modelId: String): Flow<List<ChatSessionEntity>> = 
        chatDao.getSessionsForModel(modelId)

    fun getMessages(sessionId: String): Flow<List<ChatMessageEntity>> = 
        chatDao.getMessagesForSession(sessionId)

    suspend fun createSession(modelId: String, title: String = "New Chat"): String {
        val id = UUID.randomUUID().toString()
        chatDao.insertSession(
            ChatSessionEntity(id = id, modelId = modelId, title = title)
        )
        return id
    }

    suspend fun sendMessage(sessionId: String, content: String, role: MessageRole) {
        chatDao.insertMessage(
            ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                role = role,
                content = content
            )
        )
    }

    suspend fun updateSessionTitle(sessionId: String, title: String) {
        chatDao.updateSessionTitle(sessionId, title)
    }

    suspend fun deleteSession(session: ChatSessionEntity) {
        chatDao.deleteSession(session)
    }

    suspend fun clearHistory(sessionId: String) {
        chatDao.clearSessionMessages(sessionId)
    }
}

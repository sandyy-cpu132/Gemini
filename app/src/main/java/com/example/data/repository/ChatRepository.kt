package com.example.data.repository

import com.example.data.local.ChatDao
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.local.SettingsStorage
import com.example.data.model.ChatMessagePayload
import com.example.data.model.ConnectionTestResult
import com.example.data.model.EngineConfig
import com.example.data.model.EngineType
import com.example.data.model.StreamEvent
import com.example.data.network.LocalInferenceClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

class ChatRepository(
    private val chatDao: ChatDao,
    private val settingsStorage: SettingsStorage,
    private val inferenceClient: LocalInferenceClient = LocalInferenceClient()
) {
    val activeEngineConfigFlow: StateFlow<EngineConfig> = settingsStorage.engineConfigFlow

    val allSessionsFlow: Flow<List<ChatSessionEntity>> = chatDao.getAllSessionsFlow()

    fun getMessagesFlow(sessionId: Long): Flow<List<ChatMessageEntity>> =
        chatDao.getMessagesForSessionFlow(sessionId)

    suspend fun getOrCreateActiveSession(): ChatSessionEntity {
        val sessions = allSessionsFlow.first()
        return if (sessions.isNotEmpty()) {
            sessions.first()
        } else {
            val newId = chatDao.insertSession(
                ChatSessionEntity(
                    title = "New Conversation",
                    engineName = activeEngineConfigFlow.value.displayTitle
                )
            )
            chatDao.getSessionById(newId) ?: ChatSessionEntity(id = newId)
        }
    }

    suspend fun createNewSession(title: String = "New Conversation"): Long {
        return chatDao.insertSession(
            ChatSessionEntity(
                title = title,
                engineName = activeEngineConfigFlow.value.displayTitle
            )
        )
    }

    suspend fun deleteSession(sessionId: Long) {
        chatDao.deleteSession(sessionId)
    }

    suspend fun clearSessionMessages(sessionId: Long) {
        chatDao.clearMessagesForSession(sessionId)
    }

    suspend fun deleteMessage(messageId: Long) {
        chatDao.deleteMessage(messageId)
    }

    fun saveConfig(config: EngineConfig) {
        settingsStorage.saveConfig(config)
    }

    fun applyPreset(type: EngineType): EngineConfig {
        return settingsStorage.applyPreset(type)
    }

    suspend fun testConnection(host: String, port: Int): ConnectionTestResult {
        return inferenceClient.testConnection(host, port)
    }

    suspend fun insertUserMessage(sessionId: Long, content: String): Long {
        return chatDao.insertMessage(
            ChatMessageEntity(
                sessionId = sessionId,
                role = "user",
                content = content.trim(),
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateSessionLastUpdated(sessionId: Long, firstUserQuery: String? = null) {
        val session = chatDao.getSessionById(sessionId)
        if (session != null) {
            val updatedTitle = if (session.title == "New Conversation" && !firstUserQuery.isNullOrBlank()) {
                if (firstUserQuery.length > 28) firstUserQuery.take(28) + "…" else firstUserQuery
            } else {
                session.title
            }
            chatDao.updateSession(
                session.copy(
                    title = updatedTitle,
                    lastUpdated = System.currentTimeMillis(),
                    engineName = activeEngineConfigFlow.value.displayTitle
                )
            )
        }
    }

    suspend fun prepareAssistantMessage(sessionId: Long): Long {
        return chatDao.insertMessage(
            ChatMessageEntity(
                sessionId = sessionId,
                role = "assistant",
                content = "",
                timestamp = System.currentTimeMillis(),
                engineUsed = activeEngineConfigFlow.value.displayTitle
            )
        )
    }

    suspend fun updateAssistantMessage(
        messageId: Long,
        content: String,
        durationMs: Long = 0,
        tokens: Int = 0,
        isError: Boolean = false
    ) {
        val existing = chatDao.getMessageById(messageId)
        if (existing != null) {
            chatDao.updateMessage(
                existing.copy(
                    content = content,
                    durationMs = durationMs,
                    tokensEstimated = tokens,
                    isError = isError,
                    engineUsed = activeEngineConfigFlow.value.displayTitle
                )
            )
        }
    }

    suspend fun getRecentMessages(sessionId: Long): List<ChatMessagePayload> {
        val entities = chatDao.getMessagesForSession(sessionId)
        // Convert to payload, filter out errors or empty responses
        return entities
            .filter { !it.isError && it.content.isNotBlank() }
            .map { ChatMessagePayload(role = it.role, content = it.content) }
    }

    fun streamInference(
        config: EngineConfig,
        messages: List<ChatMessagePayload>
    ): Flow<StreamEvent> {
        return inferenceClient.streamChatCompletions(config, messages)
    }
}

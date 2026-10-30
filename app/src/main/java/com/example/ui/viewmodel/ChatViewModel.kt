package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.local.SettingsStorage
import com.example.data.model.ConnectionTestResult
import com.example.data.model.EngineConfig
import com.example.data.model.EngineType
import com.example.data.model.StreamEvent
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ConnectionState {
    CHECKING,
    CONNECTED,
    DISCONNECTED
}

data class ChatUiState(
    val currentSessionId: Long = 0L,
    val sessions: List<ChatSessionEntity> = emptyList(),
    val messages: List<ChatMessageEntity> = emptyList(),
    val inputText: String = "",
    val isStreaming: Boolean = false,
    val streamingContent: String = "",
    val streamingTokens: Int = 0,
    val streamingElapsedMs: Long = 0L,
    val activeEngine: EngineConfig = EngineConfig.LLAMA_PRESET,
    val connectionState: ConnectionState = ConnectionState.CHECKING,
    val connectionLatencyMs: Long = 0L,
    val errorMessage: String? = null
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val settingsStorage = SettingsStorage(application)
    val repository = ChatRepository(db.chatDao(), settingsStorage)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var activeStreamJob: Job? = null
    private var streamingTimerJob: Job? = null

    init {
        // Collect sessions
        viewModelScope.launch {
            repository.allSessionsFlow.collectLatest { sessionsList ->
                if (sessionsList.isEmpty()) {
                    val defaultId = repository.createNewSession("New Conversation")
                    _uiState.update { it.copy(currentSessionId = defaultId, sessions = sessionsList) }
                } else {
                    _uiState.update { current ->
                        val activeId = if (current.currentSessionId != 0L && sessionsList.any { it.id == current.currentSessionId }) {
                            current.currentSessionId
                        } else {
                            sessionsList.first().id
                        }
                        current.copy(currentSessionId = activeId, sessions = sessionsList)
                    }
                }
            }
        }

        // Listen to active engine config changes
        viewModelScope.launch {
            repository.activeEngineConfigFlow.collectLatest { config ->
                _uiState.update { it.copy(activeEngine = config) }
                checkConnection()
            }
        }

        // Listen to messages for current active session
        viewModelScope.launch {
            _uiState.collectLatest { state ->
                if (state.currentSessionId != 0L) {
                    repository.getMessagesFlow(state.currentSessionId).collectLatest { msgs ->
                        _uiState.update { it.copy(messages = msgs) }
                    }
                }
            }
        }
    }

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun selectSession(sessionId: Long) {
        if (_uiState.value.isStreaming) {
            stopStreaming()
        }
        _uiState.update { it.copy(currentSessionId = sessionId, errorMessage = null) }
    }

    fun createNewSession() {
        if (_uiState.value.isStreaming) {
            stopStreaming()
        }
        viewModelScope.launch {
            val newId = repository.createNewSession("New Conversation")
            _uiState.update { it.copy(currentSessionId = newId, errorMessage = null) }
        }
    }

    fun deleteCurrentSession() {
        val currId = _uiState.value.currentSessionId
        if (currId == 0L) return
        if (_uiState.value.isStreaming) {
            stopStreaming()
        }
        viewModelScope.launch {
            repository.deleteSession(currId)
        }
    }

    fun clearCurrentMessages() {
        val currId = _uiState.value.currentSessionId
        if (currId == 0L) return
        if (_uiState.value.isStreaming) {
            stopStreaming()
        }
        viewModelScope.launch {
            repository.clearSessionMessages(currId)
        }
    }

    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            repository.deleteMessage(messageId)
        }
    }

    fun switchEngine(type: EngineType) {
        val updated = repository.applyPreset(type)
        _uiState.update { it.copy(activeEngine = updated) }
        checkConnection()
    }

    fun checkConnection() {
        val config = _uiState.value.activeEngine
        _uiState.update { it.copy(connectionState = ConnectionState.CHECKING) }
        viewModelScope.launch {
            val result = repository.testConnection(config.host, config.port)
            _uiState.update {
                it.copy(
                    connectionState = if (result.isSuccess) ConnectionState.CONNECTED else ConnectionState.DISCONNECTED,
                    connectionLatencyMs = result.latencyMs
                )
            }
        }
    }

    fun sendMessage(promptOverride: String? = null) {
        val query = promptOverride ?: _uiState.value.inputText.trim()
        if (query.isBlank() || _uiState.value.isStreaming) return

        val sessionId = _uiState.value.currentSessionId
        if (sessionId == 0L) return

        // Clear input box
        if (promptOverride == null) {
            _uiState.update { it.copy(inputText = "", errorMessage = null) }
        }

        viewModelScope.launch {
            // Save user message
            repository.insertUserMessage(sessionId, query)
            repository.updateSessionLastUpdated(sessionId, query)

            // Prepare history payload for OpenAI compatible endpoint
            val historyPayload = repository.getRecentMessages(sessionId)

            // Insert placeholder assistant message in Room
            val assistantMsgId = repository.prepareAssistantMessage(sessionId)

            // Start streaming
            _uiState.update {
                it.copy(
                    isStreaming = true,
                    streamingContent = "",
                    streamingTokens = 0,
                    streamingElapsedMs = 0L,
                    errorMessage = null
                )
            }

            val startTime = System.currentTimeMillis()
            startTimer(startTime)

            val builder = StringBuilder()
            var tokenCount = 0

            activeStreamJob = launch {
                try {
                    repository.streamInference(_uiState.value.activeEngine, historyPayload)
                        .collect { event ->
                            when (event) {
                                is StreamEvent.Token -> {
                                    builder.append(event.text)
                                    tokenCount++
                                    _uiState.update {
                                        it.copy(
                                            streamingContent = builder.toString(),
                                            streamingTokens = tokenCount
                                        )
                                    }
                                }
                                is StreamEvent.Done -> {
                                    val duration = System.currentTimeMillis() - startTime
                                    val finalContent = builder.toString().trim()
                                    repository.updateAssistantMessage(
                                        messageId = assistantMsgId,
                                        content = finalContent.ifBlank { "(Empty response received from engine)" },
                                        durationMs = duration,
                                        tokens = if (event.totalTokensEstimated > 0) event.totalTokensEstimated else tokenCount,
                                        isError = false
                                    )
                                    stopStreamingState()
                                }
                                is StreamEvent.Error -> {
                                    val duration = System.currentTimeMillis() - startTime
                                    val currentText = builder.toString()
                                    val errorDisplay = if (currentText.isNotBlank()) {
                                        "$currentText\n\n⚠️ ${event.message}"
                                    } else {
                                        "⚠️ ${event.message}"
                                    }
                                    repository.updateAssistantMessage(
                                        messageId = assistantMsgId,
                                        content = errorDisplay,
                                        durationMs = duration,
                                        tokens = tokenCount,
                                        isError = true
                                    )
                                    _uiState.update {
                                        it.copy(
                                            errorMessage = event.message,
                                            connectionState = ConnectionState.DISCONNECTED
                                        )
                                    }
                                    stopStreamingState()
                                }
                            }
                        }
                } catch (e: Exception) {
                    val duration = System.currentTimeMillis() - startTime
                    val finalContent = builder.toString()
                    val errorMsg = "Generation failed: ${e.localizedMessage ?: e.message}"
                    repository.updateAssistantMessage(
                        messageId = assistantMsgId,
                        content = if (finalContent.isNotBlank()) "$finalContent\n\n⚠️ $errorMsg" else "⚠️ $errorMsg",
                        durationMs = duration,
                        tokens = tokenCount,
                        isError = true
                    )
                    _uiState.update { it.copy(errorMessage = errorMsg) }
                    stopStreamingState()
                }
            }
        }
    }

    fun stopStreaming() {
        activeStreamJob?.cancel()
        activeStreamJob = null
        streamingTimerJob?.cancel()
        streamingTimerJob = null
        _uiState.update { it.copy(isStreaming = false) }
    }

    private fun stopStreamingState() {
        streamingTimerJob?.cancel()
        streamingTimerJob = null
        _uiState.update {
            it.copy(
                isStreaming = false,
                streamingContent = ""
            )
        }
    }

    private fun startTimer(startTime: Long) {
        streamingTimerJob?.cancel()
        streamingTimerJob = viewModelScope.launch {
            while (_uiState.value.isStreaming) {
                delay(100)
                val elapsed = System.currentTimeMillis() - startTime
                _uiState.update { it.copy(streamingElapsedMs = elapsed) }
            }
        }
    }

    fun retryLastMessage() {
        val messages = _uiState.value.messages
        val lastUserMessage = messages.lastOrNull { it.role == "user" }
        if (lastUserMessage != null) {
            sendMessage(lastUserMessage.content)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopStreaming()
    }
}

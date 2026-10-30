package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsStorage
import com.example.data.model.ConnectionTestResult
import com.example.data.model.EngineConfig
import com.example.data.model.EngineType
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val currentConfig: EngineConfig = EngineConfig.LLAMA_PRESET,
    val selectedEngineType: EngineType = EngineType.LLAMA_CPP,
    val hostInput: String = "127.0.0.1",
    val portInput: String = "8080",
    val modelInput: String = "default",
    val temperatureInput: Float = 0.7f,
    val maxTokensInput: Int = 2048,
    val systemPromptInput: String = "You are a fast, concise AI assistant running locally on mobile via Termux.",
    val streamEnabled: Boolean = true,
    val isTestingConnection: Boolean = false,
    val testResult: ConnectionTestResult? = null,
    val bannerMessage: String? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val settingsStorage = SettingsStorage(application)
    private val repository = ChatRepository(db.chatDao(), settingsStorage)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        val initial = settingsStorage.loadConfig()
        _uiState.update {
            it.copy(
                currentConfig = initial,
                selectedEngineType = initial.engineType,
                hostInput = initial.host,
                portInput = initial.port.toString(),
                modelInput = initial.model,
                temperatureInput = initial.temperature,
                maxTokensInput = initial.maxTokens,
                systemPromptInput = initial.systemPrompt,
                streamEnabled = initial.stream
            )
        }
        testCurrentConnection()
    }

    fun selectPreset(type: EngineType) {
        val newConfig = when (type) {
            EngineType.LLAMA_CPP -> EngineConfig.LLAMA_PRESET
            EngineType.PRISM_ML -> EngineConfig.PRISM_PRESET
            EngineType.CUSTOM -> _uiState.value.currentConfig.copy(
                engineType = EngineType.CUSTOM,
                name = "Custom Engine"
            )
        }

        _uiState.update {
            it.copy(
                selectedEngineType = type,
                currentConfig = newConfig,
                hostInput = newConfig.host,
                portInput = newConfig.port.toString(),
                modelInput = newConfig.model,
                temperatureInput = newConfig.temperature,
                maxTokensInput = newConfig.maxTokens,
                systemPromptInput = newConfig.systemPrompt,
                streamEnabled = newConfig.stream,
                testResult = null,
                bannerMessage = "Switched to ${newConfig.name}"
            )
        }

        // Persist immediately on preset selection
        repository.saveConfig(newConfig)
        testCurrentConnection()
    }

    fun onHostChanged(host: String) {
        _uiState.update { it.copy(hostInput = host) }
    }

    fun onPortChanged(port: String) {
        _uiState.update { it.copy(portInput = port) }
    }

    fun onModelChanged(model: String) {
        _uiState.update { it.copy(modelInput = model) }
    }

    fun onTemperatureChanged(temp: Float) {
        _uiState.update { it.copy(temperatureInput = temp) }
    }

    fun onMaxTokensChanged(tokens: Int) {
        _uiState.update { it.copy(maxTokensInput = tokens) }
    }

    fun onSystemPromptChanged(prompt: String) {
        _uiState.update { it.copy(systemPromptInput = prompt) }
    }

    fun onStreamToggled(enabled: Boolean) {
        _uiState.update { it.copy(streamEnabled = enabled) }
    }

    fun saveCustomSettings() {
        val port = _uiState.value.portInput.toIntOrNull() ?: 8080
        val host = _uiState.value.hostInput.ifBlank { "127.0.0.1" }
        val model = _uiState.value.modelInput.ifBlank { "default" }

        val type = _uiState.value.selectedEngineType
        val engineName = when (type) {
            EngineType.LLAMA_CPP -> "llama.cpp (llama-server)"
            EngineType.PRISM_ML -> "Prism ML Engine"
            EngineType.CUSTOM -> "Custom Engine"
        }

        val updatedConfig = EngineConfig(
            engineType = type,
            name = engineName,
            host = host,
            port = port,
            model = model,
            temperature = _uiState.value.temperatureInput,
            maxTokens = _uiState.value.maxTokensInput,
            systemPrompt = _uiState.value.systemPromptInput,
            stream = _uiState.value.streamEnabled
        )

        repository.saveConfig(updatedConfig)
        _uiState.update {
            it.copy(
                currentConfig = updatedConfig,
                bannerMessage = "Settings saved & active for loopback ($host:$port)"
            )
        }
        testCurrentConnection()
    }

    fun testCurrentConnection() {
        val host = _uiState.value.hostInput.ifBlank { "127.0.0.1" }
        val port = _uiState.value.portInput.toIntOrNull() ?: 8080

        _uiState.update { it.copy(isTestingConnection = true, testResult = null) }
        viewModelScope.launch {
            val result = repository.testConnection(host, port)
            _uiState.update {
                it.copy(
                    isTestingConnection = false,
                    testResult = result
                )
            }
        }
    }

    fun clearBanner() {
        _uiState.update { it.copy(bannerMessage = null) }
    }
}

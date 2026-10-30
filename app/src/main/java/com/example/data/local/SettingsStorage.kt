package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.EngineConfig
import com.example.data.model.EngineType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsStorage(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("termux_ai_engine_prefs", Context.MODE_PRIVATE)

    private val _engineConfigFlow = MutableStateFlow(loadConfig())
    val engineConfigFlow: StateFlow<EngineConfig> = _engineConfigFlow.asStateFlow()

    fun loadConfig(): EngineConfig {
        val typeStr = prefs.getString(KEY_ENGINE_TYPE, EngineType.LLAMA_CPP.name) ?: EngineType.LLAMA_CPP.name
        val engineType = try {
            EngineType.valueOf(typeStr)
        } catch (_: Exception) {
            EngineType.LLAMA_CPP
        }

        val defaultPort = when (engineType) {
            EngineType.LLAMA_CPP -> 8080
            EngineType.PRISM_ML -> 8081
            EngineType.CUSTOM -> 8080
        }

        val defaultName = when (engineType) {
            EngineType.LLAMA_CPP -> "llama.cpp (llama-server)"
            EngineType.PRISM_ML -> "Prism ML Engine"
            EngineType.CUSTOM -> "Custom Engine"
        }

        val defaultModel = when (engineType) {
            EngineType.LLAMA_CPP -> "default"
            EngineType.PRISM_ML -> "prism-ternary"
            EngineType.CUSTOM -> "default"
        }

        return EngineConfig(
            engineType = engineType,
            name = prefs.getString(KEY_NAME, defaultName) ?: defaultName,
            host = prefs.getString(KEY_HOST, "127.0.0.1") ?: "127.0.0.1",
            port = prefs.getInt(KEY_PORT, defaultPort),
            model = prefs.getString(KEY_MODEL, defaultModel) ?: defaultModel,
            temperature = prefs.getFloat(KEY_TEMPERATURE, 0.7f),
            maxTokens = prefs.getInt(KEY_MAX_TOKENS, 2048),
            systemPrompt = prefs.getString(
                KEY_SYSTEM_PROMPT,
                "You are a fast, concise AI assistant running locally on mobile via Termux."
            ) ?: "You are a fast, concise AI assistant running locally on mobile via Termux.",
            stream = prefs.getBoolean(KEY_STREAM, true)
        )
    }

    fun saveConfig(config: EngineConfig) {
        prefs.edit()
            .putString(KEY_ENGINE_TYPE, config.engineType.name)
            .putString(KEY_NAME, config.name)
            .putString(KEY_HOST, config.host)
            .putInt(KEY_PORT, config.port)
            .putString(KEY_MODEL, config.model)
            .putFloat(KEY_TEMPERATURE, config.temperature)
            .putInt(KEY_MAX_TOKENS, config.maxTokens)
            .putString(KEY_SYSTEM_PROMPT, config.systemPrompt)
            .putBoolean(KEY_STREAM, config.stream)
            .apply()

        _engineConfigFlow.value = config
    }

    fun applyPreset(presetType: EngineType): EngineConfig {
        val newConfig = when (presetType) {
            EngineType.LLAMA_CPP -> EngineConfig.LLAMA_PRESET
            EngineType.PRISM_ML -> EngineConfig.PRISM_PRESET
            EngineType.CUSTOM -> loadConfig().copy(engineType = EngineType.CUSTOM, name = "Custom Inference")
        }
        saveConfig(newConfig)
        return newConfig
    }

    companion object {
        private const val KEY_ENGINE_TYPE = "engine_type"
        private const val KEY_NAME = "name"
        private const val KEY_HOST = "host"
        private const val KEY_PORT = "port"
        private const val KEY_MODEL = "model"
        private const val KEY_TEMPERATURE = "temperature"
        private const val KEY_MAX_TOKENS = "max_tokens"
        private const val KEY_SYSTEM_PROMPT = "system_prompt"
        private const val KEY_STREAM = "stream"
    }
}

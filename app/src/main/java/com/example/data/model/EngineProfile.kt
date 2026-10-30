package com.example.data.model

enum class EngineType {
    LLAMA_CPP,
    PRISM_ML,
    CUSTOM
}

data class EngineConfig(
    val engineType: EngineType = EngineType.LLAMA_CPP,
    val name: String = "llama.cpp (llama-server)",
    val host: String = "127.0.0.1",
    val port: Int = 8080,
    val model: String = "default",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 2048,
    val systemPrompt: String = "You are a fast, concise AI assistant running locally on mobile via Termux.",
    val stream: Boolean = true
) {
    val baseUrl: String
        get() = "http://${host.trim()}:$port"

    val chatCompletionsUrl: String
        get() = "$baseUrl/v1/chat/completions"

    val modelsUrl: String
        get() = "$baseUrl/v1/models"

    val healthUrl: String
        get() = "$baseUrl/health"

    val displayTitle: String
        get() = when (engineType) {
            EngineType.LLAMA_CPP -> "llama.cpp :$port"
            EngineType.PRISM_ML -> "Prism ML :$port"
            EngineType.CUSTOM -> "$name :$port"
        }

    val modelFormatNote: String
        get() = when (engineType) {
            EngineType.LLAMA_CPP -> "Standard GGUF models (Q4_K, Q8_0, F16)"
            EngineType.PRISM_ML -> "Specialized ultra-low-bit / ternary model formats (1.58-bit BitNet / Prism)"
            EngineType.CUSTOM -> "Custom OpenAI-compatible inference endpoint"
        }

    companion object {
        val LLAMA_PRESET = EngineConfig(
            engineType = EngineType.LLAMA_CPP,
            name = "llama.cpp (llama-server)",
            host = "127.0.0.1",
            port = 8080,
            model = "default",
            temperature = 0.7f,
            maxTokens = 2048,
            systemPrompt = "You are a fast, concise AI assistant running locally on mobile via Termux.",
            stream = true
        )

        val PRISM_PRESET = EngineConfig(
            engineType = EngineType.PRISM_ML,
            name = "Prism ML Engine",
            host = "127.0.0.1",
            port = 8081,
            model = "prism-ternary",
            temperature = 0.6f,
            maxTokens = 2048,
            systemPrompt = "You are a specialized ultra-low-bit ternary AI assistant running via Prism ML.",
            stream = true
        )
    }
}

package com.example

import com.example.data.model.EngineConfig
import com.example.data.model.EngineType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun engineConfig_defaultLlamaPreset_hasCorrectPortAndEndpoints() {
        val llama = EngineConfig.LLAMA_PRESET
        assertEquals(8080, llama.port)
        assertEquals("127.0.0.1", llama.host)
        assertEquals("http://127.0.0.1:8080", llama.baseUrl)
        assertEquals("http://127.0.0.1:8080/v1/chat/completions", llama.chatCompletionsUrl)
        assertEquals("http://127.0.0.1:8080/v1/models", llama.modelsUrl)
        assertEquals(EngineType.LLAMA_CPP, llama.engineType)
    }

    @Test
    fun engineConfig_prismPreset_hasCorrectPortAndFormat() {
        val prism = EngineConfig.PRISM_PRESET
        assertEquals(8081, prism.port)
        assertEquals("http://127.0.0.1:8081", prism.baseUrl)
        assertEquals(EngineType.PRISM_ML, prism.engineType)
        assertTrue(prism.modelFormatNote.contains("ternary", ignoreCase = true))
    }

    @Test
    fun engineConfig_customPort_updatesUrlsProperly() {
        val custom = EngineConfig(
            engineType = EngineType.CUSTOM,
            name = "Custom Termux Runner",
            host = "localhost",
            port = 9000
        )
        assertEquals("http://localhost:9000/v1/chat/completions", custom.chatCompletionsUrl)
        assertEquals("Custom Termux Runner :9000", custom.displayTitle)
    }
}

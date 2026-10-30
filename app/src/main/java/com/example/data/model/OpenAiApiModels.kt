package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ChatMessagePayload(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class ChatCompletionRequest(
    @Json(name = "model") val model: String,
    @Json(name = "messages") val messages: List<ChatMessagePayload>,
    @Json(name = "temperature") val temperature: Float = 0.7f,
    @Json(name = "max_tokens") val maxTokens: Int = 2048,
    @Json(name = "stream") val stream: Boolean = true
)

sealed class StreamEvent {
    data class Token(val text: String) : StreamEvent()
    data class Done(val totalTokensEstimated: Int, val elapsedMs: Long) : StreamEvent()
    data class Error(val message: String) : StreamEvent()
}

data class ConnectionTestResult(
    val isSuccess: Boolean,
    val latencyMs: Long = 0,
    val message: String = "",
    val detectedModels: List<String> = emptyList(),
    val serverBanner: String = ""
)

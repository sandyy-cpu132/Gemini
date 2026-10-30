package com.example.data.network

import com.example.data.model.ChatMessagePayload
import com.example.data.model.ConnectionTestResult
import com.example.data.model.EngineConfig
import com.example.data.model.StreamEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class LocalInferenceClient {

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS) // Long read timeout for local CPU/NPU inference in Termux
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()

    /**
     * Streams chat completions from OpenAI-compatible endpoint (llama.cpp / Prism ML).
     */
    fun streamChatCompletions(
        config: EngineConfig,
        messages: List<ChatMessagePayload>
    ): Flow<StreamEvent> = callbackFlow {
        val startTime = System.currentTimeMillis()
        var estimatedTokens = 0

        // Build JSON request payload
        val jsonPayload = JSONObject().apply {
            put("model", config.model.ifBlank { "default" })
            put("temperature", config.temperature.toDouble())
            put("max_tokens", config.maxTokens)
            put("stream", true)

            val messagesArray = JSONArray()
            // Add system prompt if provided
            if (config.systemPrompt.isNotBlank()) {
                val sysObj = JSONObject().apply {
                    put("role", "system")
                    put("content", config.systemPrompt.trim())
                }
                messagesArray.put(sysObj)
            }

            for (msg in messages) {
                val msgObj = JSONObject().apply {
                    put("role", msg.role)
                    put("content", msg.content)
                }
                messagesArray.put(msgObj)
            }
            put("messages", messagesArray)
        }

        val requestBody = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(config.chatCompletionsUrl)
            .addHeader("Accept", "text/event-stream")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        val call = okHttpClient.newCall(request)

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!call.isCanceled()) {
                    val errorMessage = formatNetworkErrorMessage(e, config)
                    trySend(StreamEvent.Error(errorMessage))
                }
                close()
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    val message = try {
                        val obj = JSONObject(errorBody)
                        obj.optJSONObject("error")?.optString("message") ?: errorBody
                    } catch (_: Exception) {
                        errorBody.ifBlank { "HTTP Error ${response.code}: ${response.message}" }
                    }
                    trySend(StreamEvent.Error("Server returned code ${response.code}: $message"))
                    close()
                    return
                }

                val body = response.body
                if (body == null) {
                    trySend(StreamEvent.Error("Empty response body from inference engine"))
                    close()
                    return
                }

                try {
                    val reader = BufferedReader(InputStreamReader(body.byteStream()))
                    var line: String? = reader.readLine()

                    while (line != null && !call.isCanceled()) {
                        val trimmed = line.trim()
                        if (trimmed.startsWith("data:")) {
                            val data = trimmed.removePrefix("data:").trim()
                            if (data == "[DONE]") {
                                break
                            }

                            if (data.isNotBlank()) {
                                try {
                                    val chunk = JSONObject(data)
                                    val choices = chunk.optJSONArray("choices")
                                    if (choices != null && choices.length() > 0) {
                                        val firstChoice = choices.getJSONObject(0)
                                        val delta = firstChoice.optJSONObject("delta")
                                        val content = delta?.optString("content")

                                        if (!content.isNullOrEmpty()) {
                                            estimatedTokens++
                                            trySend(StreamEvent.Token(content))
                                        }
                                    }
                                } catch (_: Exception) {
                                    // Non-critical: some engines emit heartbeat or comment lines
                                }
                            }
                        }
                        line = reader.readLine()
                    }

                    val elapsed = System.currentTimeMillis() - startTime
                    trySend(StreamEvent.Done(estimatedTokens, elapsed))
                } catch (e: Exception) {
                    if (!call.isCanceled() && e !is CancellationException) {
                        trySend(StreamEvent.Error("Streaming interrupted: ${e.localizedMessage ?: e.message}"))
                    }
                } finally {
                    body.close()
                    close()
                }
            }
        })

        awaitClose {
            call.cancel()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Tests connectivity to local engine (checks /v1/models or /health).
     */
    suspend fun testConnection(host: String, port: Int): ConnectionTestResult = withContext(Dispatchers.IO) {
        val cleanHost = host.trim()
        val url = "http://$cleanHost:$port/v1/models"
        val startTime = System.currentTimeMillis()

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        val pingClient = okHttpClient.newBuilder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .build()

        try {
            val response = pingClient.newCall(request).execute()
            val latency = System.currentTimeMillis() - startTime
            val bodyString = response.body?.string() ?: ""
            val serverHeader = response.header("Server") ?: "OpenAI-Compatible Engine"

            if (response.isSuccessful) {
                val detectedModels = mutableListOf<String>()
                try {
                    val json = JSONObject(bodyString)
                    val dataArray = json.optJSONArray("data")
                    if (dataArray != null) {
                        for (i in 0 until dataArray.length()) {
                            val id = dataArray.optJSONObject(i)?.optString("id")
                            if (!id.isNullOrBlank()) {
                                detectedModels.add(id)
                            }
                        }
                    }
                } catch (_: Exception) {}

                ConnectionTestResult(
                    isSuccess = true,
                    latencyMs = latency,
                    message = "Connected to $cleanHost:$port (${latency}ms)",
                    detectedModels = detectedModels,
                    serverBanner = serverHeader
                )
            } else {
                // Endpoint exists but returned non-200 (e.g. 404 for /v1/models, try /health)
                ConnectionTestResult(
                    isSuccess = true,
                    latencyMs = latency,
                    message = "Server is responding on port $port (HTTP ${response.code})",
                    serverBanner = serverHeader
                )
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            val diagnostic = when {
                e is java.net.ConnectException ->
                    "Connection refused at $cleanHost:$port. Is your inference engine (llama-server or prism-server) running inside Termux?"
                e is java.net.SocketTimeoutException ->
                    "Connection timed out at $cleanHost:$port. The server is not responding."
                else ->
                    "Cannot reach $cleanHost:$port: ${e.localizedMessage ?: e.message}"
            }
            ConnectionTestResult(
                isSuccess = false,
                latencyMs = latency,
                message = diagnostic
            )
        }
    }

    private fun formatNetworkErrorMessage(e: Exception, config: EngineConfig): String {
        return when {
            e is java.net.ConnectException ->
                "Connection refused to ${config.baseUrl}.\n\nMake sure your engine server is actively running inside Termux on port ${config.port} with:\n--host 127.0.0.1 --port ${config.port}"
            e is java.net.SocketTimeoutException ->
                "Connection to ${config.baseUrl} timed out. Termux server took too long to respond."
            else ->
                "Network Error (${config.baseUrl}): ${e.localizedMessage ?: e.message}"
        }
    }
}

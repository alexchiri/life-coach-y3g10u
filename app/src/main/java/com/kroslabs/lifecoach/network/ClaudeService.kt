package com.kroslabs.lifecoach.network

import com.google.gson.Gson
import com.kroslabs.lifecoach.util.DebugLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.io.IOException
import java.util.concurrent.TimeUnit

class ClaudeService(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    companion object {
        private const val BASE_URL = "https://api.anthropic.com/v1/messages"
        private const val ANTHROPIC_VERSION = "2023-06-01"
        private const val INPUT_COST_PER_1M = 3.0  // Claude Sonnet pricing
        private const val OUTPUT_COST_PER_1M = 15.0
    }

    data class StreamResult(
        val text: String,
        val inputTokens: Int,
        val outputTokens: Int
    )

    fun streamMessage(messages: List<ClaudeMessage>): Flow<String> = callbackFlow {
        val request = ClaudeRequest(messages = messages)
        val jsonBody = gson.toJson(request)

        val httpRequest = Request.Builder()
            .url(BASE_URL)
            .addHeader("Content-Type", "application/json")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", ANTHROPIC_VERSION)
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()

        val eventSourceFactory = EventSources.createFactory(client)

        val listener = object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                if (data == "[DONE]") {
                    close()
                    return
                }

                try {
                    val event = gson.fromJson(data, StreamEvent::class.java)
                    when (event.type) {
                        "content_block_delta" -> {
                            event.delta?.text?.let { text ->
                                trySend(text)
                            }
                        }
                        "message_stop" -> {
                            close()
                        }
                        "error" -> {
                            close(IOException("Claude API error"))
                        }
                    }
                } catch (e: Exception) {
                    // Ignore parse errors for non-JSON events
                }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                close(t ?: IOException("Unknown error"))
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }
        }

        val eventSource = eventSourceFactory.newEventSource(httpRequest, listener)

        awaitClose {
            eventSource.cancel()
        }
    }

    suspend fun sendMessage(messages: List<ClaudeMessage>): Result<StreamResult> = withContext(Dispatchers.IO) {
        DebugLogger.d("Claude", "sendMessage called with ${messages.size} messages")
        try {
            val request = ClaudeRequest(messages = messages, stream = false)
            val jsonBody = gson.toJson(request)
            DebugLogger.d("Claude", "Request body size: ${jsonBody.length} chars, model: ${request.model}")

            val httpRequest = Request.Builder()
                .url(BASE_URL)
                .addHeader("Content-Type", "application/json")
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", ANTHROPIC_VERSION)
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            DebugLogger.d("Claude", "Executing HTTP request to $BASE_URL")
            val response = client.newCall(httpRequest).execute()
            DebugLogger.d("Claude", "HTTP response code: ${response.code}")

            val responseBody = response.body?.string() ?: throw IOException("Empty response")
            DebugLogger.d("Claude", "Response body size: ${responseBody.length} chars")

            if (!response.isSuccessful) {
                DebugLogger.e("Claude", "API error: ${response.code} - ${responseBody.take(500)}")
                return@withContext Result.failure(IOException("API error: ${response.code} - $responseBody"))
            }

            val claudeResponse = gson.fromJson(responseBody, ClaudeResponse::class.java)
            val text = claudeResponse.content?.firstOrNull()?.text ?: ""
            val usage = claudeResponse.usage

            DebugLogger.i("Claude", "API call successful - Input: ${usage?.inputTokens ?: 0} tokens, Output: ${usage?.outputTokens ?: 0} tokens")
            DebugLogger.d("Claude", "Response text (first 200 chars): ${text.take(200)}")

            Result.success(
                StreamResult(
                    text = text,
                    inputTokens = usage?.inputTokens ?: 0,
                    outputTokens = usage?.outputTokens ?: 0
                )
            )
        } catch (e: Exception) {
            DebugLogger.e("Claude", "sendMessage exception", e)
            Result.failure(e)
        }
    }

    fun calculateCost(inputTokens: Int, outputTokens: Int): Float {
        val inputCost = (inputTokens / 1_000_000.0) * INPUT_COST_PER_1M
        val outputCost = (outputTokens / 1_000_000.0) * OUTPUT_COST_PER_1M
        return ((inputCost + outputCost) * 100).toFloat() // Return cents
    }
}

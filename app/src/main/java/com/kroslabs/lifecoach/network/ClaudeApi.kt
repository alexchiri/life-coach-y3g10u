package com.kroslabs.lifecoach.network

import com.google.gson.annotations.SerializedName

data class ClaudeRequest(
    val model: String = "claude-sonnet-4-5-20250929",
    @SerializedName("max_tokens")
    val maxTokens: Int = 4096,
    val stream: Boolean = true,
    val messages: List<ClaudeMessage>
)

data class ClaudeMessage(
    val role: String,
    val content: String
)

data class ClaudeResponse(
    val id: String?,
    val type: String,
    val role: String?,
    val content: List<ContentBlock>?,
    val model: String?,
    @SerializedName("stop_reason")
    val stopReason: String?,
    val usage: Usage?
)

data class ContentBlock(
    val type: String,
    val text: String?
)

data class Usage(
    @SerializedName("input_tokens")
    val inputTokens: Int,
    @SerializedName("output_tokens")
    val outputTokens: Int
)

data class StreamEvent(
    val type: String,
    val message: ClaudeResponse? = null,
    val index: Int? = null,
    val delta: Delta? = null,
    val usage: Usage? = null
)

data class Delta(
    val type: String?,
    val text: String?
)

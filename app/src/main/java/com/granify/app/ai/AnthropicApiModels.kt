package com.granify.app.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTOs for Anthropic's Messages API (https://platform.claude.com/docs/en/api/messages).
 * Field names match the API's JSON exactly (snake_case fields get an explicit [SerialName])
 * so no custom serializers are needed — mirrors the approach in
 * com.granify.app.data.gmail.GmailApiModels for the Gmail REST API.
 */

@Serializable
data class AnthropicMessage(
    val role: String,
    val content: String,
)

@Serializable
data class AnthropicRequest(
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int,
    val system: String,
    val messages: List<AnthropicMessage>,
)

@Serializable
data class AnthropicContentBlock(
    val type: String = "",
    val text: String = "",
)

@Serializable
data class AnthropicResponse(
    val content: List<AnthropicContentBlock> = emptyList(),
    @SerialName("stop_reason") val stopReason: String? = null,
)

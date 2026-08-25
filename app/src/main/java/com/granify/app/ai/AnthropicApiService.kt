package com.granify.app.ai

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * The user-supplied API key is passed explicitly per call, the same way
 * [com.granify.app.data.gmail.GmailApiService] takes its OAuth token — see AppContainer for
 * where the key actually comes from (a Settings-entered value, never bundled with the app).
 */
interface AnthropicApiService {
    @POST("v1/messages")
    suspend fun createMessage(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String = "2023-06-01",
        @Body request: AnthropicRequest,
    ): AnthropicResponse

    companion object {
        const val BASE_URL = "https://api.anthropic.com/"
    }
}

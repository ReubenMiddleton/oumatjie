package com.granify.app.data.gmail

import com.granify.app.BuildConfig
import com.granify.app.ai.AnthropicApiService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

object NetworkModule {
    private val json = Json { ignoreUnknownKeys = true }

    fun createGmailApiService(): GmailApiService =
        buildRetrofit(GmailApiService.BASE_URL).create(GmailApiService::class.java)

    fun createAnthropicApiService(): AnthropicApiService =
        buildRetrofit(AnthropicApiService.BASE_URL).create(AnthropicApiService::class.java)

    private fun buildRetrofit(baseUrl: String): Retrofit {
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .apply {
                if (BuildConfig.DEBUG) {
                    // BASIC (method/URL/status only): headers and bodies can contain the
                    // user's mail, so they are never logged even in debug builds. Applies
                    // equally to the AI provider call, whose bodies are also the user's own
                    // email text (docs/PRODUCT_PRINCIPLES.md's privacy rules).
                    addInterceptor(
                        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC },
                    )
                }
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
}

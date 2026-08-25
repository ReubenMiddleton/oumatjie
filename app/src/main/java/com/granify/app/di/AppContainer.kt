package com.granify.app.di

import android.content.Context
import com.granify.app.ai.AiProvider
import com.granify.app.ai.AnthropicAiProvider
import com.granify.app.ai.AnthropicApiService
import com.granify.app.ai.DemoAiProvider
import com.granify.app.auth.AuthManager
import com.granify.app.auth.GoogleAuthManager
import com.granify.app.data.MailRepository
import com.granify.app.data.MockMailRepository
import com.granify.app.data.attachments.AttachmentDownloader
import com.granify.app.data.attachments.GmailAttachmentDownloader
import com.granify.app.data.attachments.MockAttachmentDownloader
import com.granify.app.data.gmail.GmailApiService
import com.granify.app.data.gmail.GmailMailRepository
import com.granify.app.data.gmail.NetworkModule
import com.granify.app.data.senders.DataStoreKnownSendersRepository
import com.granify.app.data.senders.KnownSendersRepository
import com.granify.app.data.settings.SettingsRepository
import com.granify.app.session.DataStoreSessionRepository
import com.granify.app.session.SessionRepository

/**
 * Hand-written dependency container. The app is small enough that a DI framework would add
 * more ceremony than it saves; everything here is a plain lazily-built singleton. See
 * docs/DECISIONS.md, "Manual DI over Hilt" — the AI-assistant scaffolding added three more
 * singletons (below) without changing that assessment.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val authManager: AuthManager by lazy { GoogleAuthManager(appContext) }
    private val gmailApiService: GmailApiService by lazy { NetworkModule.createGmailApiService() }

    val gmailMailRepository: GmailMailRepository by lazy {
        GmailMailRepository(gmailApiService, authManager)
    }
    val mockMailRepository: MailRepository by lazy { MockMailRepository() }

    val gmailAttachmentDownloader: AttachmentDownloader by lazy {
        GmailAttachmentDownloader(appContext, gmailApiService, authManager)
    }
    val mockAttachmentDownloader: AttachmentDownloader by lazy { MockAttachmentDownloader(appContext) }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }
    val sessionRepository: SessionRepository by lazy { DataStoreSessionRepository(appContext) }
    val knownSendersRepository: KnownSendersRepository by lazy { DataStoreKnownSendersRepository(appContext) }

    // --- AI provider (docs/AI_ASSISTANT.md) ---

    /** Always available, fully offline — used for summarization when no real key is
     * configured yet, and for tests/previews. See ai/DemoAiProvider.kt for why it is
     * deliberately *not* used for the live scam-check trigger. */
    val demoAiProvider: AiProvider by lazy { DemoAiProvider() }

    private val anthropicApiService: AnthropicApiService by lazy { NetworkModule.createAnthropicApiService() }

    /** Builds a real provider around the user's own key. Called fresh with whatever key
     * Settings currently holds, rather than cached, so a key entered or cleared mid-session
     * takes effect immediately. */
    fun anthropicAiProvider(apiKey: String): AiProvider = AnthropicAiProvider(anthropicApiService, apiKey)
}

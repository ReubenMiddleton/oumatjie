package com.granify.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "oumatjie_settings")

class SettingsRepository(private val context: Context) {
    val textScale: Flow<TextScale> = context.settingsDataStore.data.map { prefs ->
        TextScale.entries.find { it.name == prefs[TEXT_SCALE_KEY] } ?: TextScale.STANDARD
    }

    suspend fun setTextScale(scale: TextScale) {
        context.settingsDataStore.edit { prefs -> prefs[TEXT_SCALE_KEY] = scale.name }
    }

    // --- AI features (docs/AI_ASSISTANT.md) ---
    // Off by default, and the key is never bundled with the app — both read as plain
    // DataStore Preferences values, the same as everything else in this file, never logged
    // (see data/gmail/NetworkModule.kt) and never included anywhere bug reports might read.

    /** Whether the user has explicitly turned on any message-level AI feature. Everything in
     * docs/AI_ASSISTANT.md stays off, with the app behaving identically to one without it,
     * until this is true. */
    val aiFeaturesEnabled: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[AI_FEATURES_ENABLED_KEY] ?: false
    }

    suspend fun setAiFeaturesEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[AI_FEATURES_ENABLED_KEY] = enabled }
    }

    /** The user's own Anthropic API key, or null if none has been entered yet. */
    val anthropicApiKey: Flow<String?> = context.settingsDataStore.data.map { prefs ->
        prefs[ANTHROPIC_API_KEY_KEY]?.takeIf { it.isNotBlank() }
    }

    suspend fun setAnthropicApiKey(key: String?) {
        context.settingsDataStore.edit { prefs ->
            if (key.isNullOrBlank()) prefs.remove(ANTHROPIC_API_KEY_KEY) else prefs[ANTHROPIC_API_KEY_KEY] = key
        }
    }

    /** Whether the first-use AI disclosure (docs/AI_ASSISTANT.md, "UI/UX shape") has already
     * been shown and acknowledged once. */
    val aiDisclosureAcknowledged: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[AI_DISCLOSURE_ACKNOWLEDGED_KEY] ?: false
    }

    suspend fun setAiDisclosureAcknowledged(acknowledged: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[AI_DISCLOSURE_ACKNOWLEDGED_KEY] = acknowledged }
    }

    private companion object {
        val TEXT_SCALE_KEY = stringPreferencesKey("text_scale")
        val AI_FEATURES_ENABLED_KEY = booleanPreferencesKey("ai_features_enabled")
        val ANTHROPIC_API_KEY_KEY = stringPreferencesKey("anthropic_api_key")
        val AI_DISCLOSURE_ACKNOWLEDGED_KEY = booleanPreferencesKey("ai_disclosure_acknowledged")
    }
}

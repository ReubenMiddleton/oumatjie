package com.granify.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.granify.app.data.settings.SettingsRepository
import com.granify.app.data.settings.TextScale
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {
    val textScale: StateFlow<TextScale> = settingsRepository.textScale
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TextScale.STANDARD)

    val aiFeaturesEnabled: StateFlow<Boolean> = settingsRepository.aiFeaturesEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val anthropicApiKey: StateFlow<String?> = settingsRepository.anthropicApiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setTextScale(scale: TextScale) {
        viewModelScope.launch { settingsRepository.setTextScale(scale) }
    }

    /** Call only after the user has seen and confirmed the first-use AI disclosure
     * (docs/AI_ASSISTANT.md, "UI/UX shape" — shown once, before the first AI call ever
     * fires). See ui/settings/SettingsScreen.kt for where that confirmation happens. */
    fun confirmEnableAiFeatures() {
        viewModelScope.launch {
            settingsRepository.setAiDisclosureAcknowledged(true)
            settingsRepository.setAiFeaturesEnabled(true)
        }
    }

    fun disableAiFeatures() {
        viewModelScope.launch { settingsRepository.setAiFeaturesEnabled(false) }
    }

    fun setAnthropicApiKey(key: String) {
        viewModelScope.launch { settingsRepository.setAnthropicApiKey(key) }
    }

    fun clearAnthropicApiKey() {
        viewModelScope.launch { settingsRepository.setAnthropicApiKey(null) }
    }

    companion object {
        fun factory(settingsRepository: SettingsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SettingsViewModel(settingsRepository) as T
            }
    }
}

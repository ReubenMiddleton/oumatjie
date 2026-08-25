package com.granify.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.granify.app.BuildConfig
import com.granify.app.data.settings.SettingsRepository
import com.granify.app.data.settings.TextScale
import com.granify.app.session.SessionState
import com.granify.app.ui.components.ConfirmationDialog
import com.granify.app.ui.components.OumatjieButton
import com.granify.app.ui.components.OumatjieSecondaryButton
import com.granify.app.ui.components.OumatjieTertiaryButton

@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    session: SessionState,
    onSignOut: () -> Unit,
    onBack: () -> Unit,
) {
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(settingsRepository))
    val textScale by viewModel.textScale.collectAsStateWithLifecycle()
    val aiFeaturesEnabled by viewModel.aiFeaturesEnabled.collectAsStateWithLifecycle()
    val anthropicApiKey by viewModel.anthropicApiKey.collectAsStateWithLifecycle()

    var showAiDisclosure by remember { mutableStateOf(false) }
    // Local-only, in-memory (not rememberSaveable): a typed-but-unsaved key should not end up
    // in a saved-instance-state Bundle that could outlive this screen — see
    // docs/DECISIONS.md, "AI provider API key field".
    var apiKeyInput by remember(anthropicApiKey) { mutableStateOf(anthropicApiKey.orEmpty()) }

    if (showAiDisclosure) {
        ConfirmationDialog(
            title = "Turn on AI features?",
            explanation = "When you open a message, its text will be sent to Anthropic (the " +
                "company behind Claude) to check for scam signals. When you tap Summarize " +
                "this, its text is sent the same way to create a short summary. Oumatjie never " +
                "sends your whole inbox, and never sends anything unless you turn this on. " +
                "If you have not added an API key below, summaries use a clearly-labelled demo " +
                "instead, and scam checks stay off.",
            confirmLabel = "Turn on AI features",
            onConfirm = {
                showAiDisclosure = false
                viewModel.confirmEnableAiFeatures()
            },
            onDismiss = { showAiDisclosure = false },
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                OumatjieTertiaryButton(label = "Back to your mail", onClick = onBack)
                Text(
                    "Settings",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() },
                )
            }

            item {
                SettingsSection(title = "Your account") {
                    Text(accountDescription(session), style = MaterialTheme.typography.bodyLarge)
                }
            }

            item {
                SettingsSection(title = "Text size") {
                    Text(
                        "Choose how large text appears in Oumatjie.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            items(TextScale.entries.toList()) { scale ->
                TextScaleOption(
                    scale = scale,
                    isSelected = scale == textScale,
                    onSelect = { viewModel.setTextScale(scale) },
                )
            }

            item {
                SettingsSection(title = "AI features") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Oumatjie can check messages you open for scam signals and " +
                                "summarize them, using an AI provider. This is off by default.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            if (aiFeaturesEnabled) "AI features are turned on." else "AI features are turned off.",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        OumatjieSecondaryButton(
                            label = if (aiFeaturesEnabled) "Turn off AI features" else "Turn on AI features",
                            onClick = {
                                if (aiFeaturesEnabled) viewModel.disableAiFeatures() else showAiDisclosure = true
                            },
                        )
                        Text(
                            "Anthropic API key (optional — without one, summaries use a demo " +
                                "and scam checks stay off):",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            singleLine = true,
                            label = { Text("API key") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OumatjieSecondaryButton(
                            label = "Save key",
                            enabled = apiKeyInput.isNotBlank() && apiKeyInput != anthropicApiKey.orEmpty(),
                            onClick = { viewModel.setAnthropicApiKey(apiKeyInput) },
                        )
                        if (anthropicApiKey != null) {
                            OumatjieTertiaryButton(
                                label = "Remove key",
                                onClick = {
                                    apiKeyInput = ""
                                    viewModel.clearAnthropicApiKey()
                                },
                            )
                        }
                    }
                }
            }

            item {
                SettingsSection(title = "Privacy") {
                    Text(
                        "Oumatjie never asks for your Google password. Documents you open are " +
                            "downloaded only when you tap Open document, and Oumatjie deletes its " +
                            "copy afterwards.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            item {
                OumatjieSecondaryButton(
                    label = if (session is SessionState.Demo) "Exit demo" else "Sign out",
                    onClick = onSignOut,
                )
            }

            item {
                Text(
                    "Oumatjie version ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            // Lets TalkBack users jump between "Your account" / "Text size" / "AI features" /
            // "Privacy" with the "next heading" gesture instead of swiping through every field.
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}

@Composable
private fun TextScaleOption(scale: TextScale, isSelected: Boolean, onSelect: () -> Unit) {
    if (isSelected) {
        OumatjieButton(label = "${scale.label} ✓", onClick = onSelect)
    } else {
        OumatjieSecondaryButton(label = scale.label, onClick = onSelect)
    }
}

private fun accountDescription(session: SessionState): String = when (session) {
    is SessionState.SignedIn -> session.email?.let { "Signed in as $it" } ?: "Signed in with Google"
    SessionState.Demo -> "Using the demo inbox with sample messages"
    SessionState.SignedOut -> "Not signed in"
}

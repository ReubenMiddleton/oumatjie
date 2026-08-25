package com.granify.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Three-tier button hierarchy (docs/ROADMAP.md, "Design direction: final plan" — Button
 * hierarchy). Visual hierarchy through size is itself an accessibility tool: it tells a user
 * where to look first without them having to read and compare every option.
 *
 * - Hero (~80dp): the one primary forward-moving action on a screen — "Continue with Google",
 *   "Done reading", "Open document", "Try again".
 * - Standard (64dp, OumatjieButton/OumatjieSecondaryButton): secondary but real actions —
 *   "Try the demo inbox", "Move to Trash" (deliberately not Hero — destructive actions
 *   shouldn't invite fast taps), the text-size picker.
 * - Tertiary (~56dp, text-style): low-stakes, always-available navigation — "Back to your
 *   mail", "Get help", "Settings", a dialog's "Cancel".
 *
 * Every tap also gets a light haptic tick (docs/ROADMAP.md's motion/haptics research: haptics
 * are the more foundational feedback channel for this audience, since roughly a third of
 * adults 65-74 have some hearing loss — a tap-back vibration asks nothing of hearing the way a
 * sound cue would). This was previously the "zero haptics anywhere" gap flagged in the
 * retrospective.
 */

/** The one primary forward-moving action on a screen. Boldest and largest of the three tiers. */
@Composable
fun OumatjieHeroButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val haptics = LocalHapticFeedback.current
    Button(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            onClick()
        },
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 80.dp),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 18.dp),
        colors = ButtonDefaults.buttonColors(),
    ) {
        Text(label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}

/** A large, filled, standard-tier action button. Every Oumatjie action has a visible text label. */
@Composable
fun OumatjieButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val haptics = LocalHapticFeedback.current
    Button(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
            onClick()
        },
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 64.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        colors = ButtonDefaults.buttonColors(),
    ) {
        Text(label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

/** A large, outlined, standard-tier action button — used for actions that are not the main
 * choice on a screen (for example a destructive action placed below the primary one). */
@Composable
fun OumatjieSecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val haptics = LocalHapticFeedback.current
    OutlinedButton(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
            onClick()
        },
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 64.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Text(label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

/** A low-stakes, always-available navigation action — "Back to your mail", "Get help". Text
 * style, smaller than the other two tiers, but still clears the 48dp absolute floor. */
@Composable
fun OumatjieTertiaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val haptics = LocalHapticFeedback.current
    TextButton(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
            onClick()
        },
        enabled = enabled,
        modifier = modifier.heightIn(min = 56.dp),
    ) {
        Text(label, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun LoadingScreen(message: String = "Please wait.") {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(24.dp))
        Text(message, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun ErrorScreen(message: String, onTryAgain: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Something went wrong", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))
        OumatjieHeroButton(label = "Try again", onClick = onTryAgain)
    }
}

/**
 * A plain-language confirmation for an action that is hard to undo, explaining what will
 * happen next (docs/PRODUCT_PRINCIPLES.md: "Destructive or external actions are explained
 * and reversible where possible").
 */
@Composable
fun ConfirmationDialog(
    title: String,
    explanation: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
        text = { Text(explanation, style = MaterialTheme.typography.bodyLarge) },
        confirmButton = {
            TextButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onConfirm()
                },
                modifier = Modifier.heightIn(min = 56.dp),
            ) {
                Text(confirmLabel, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 56.dp)) {
                Text("Cancel", style = MaterialTheme.typography.titleLarge)
            }
        },
    )
}

/**
 * The color a [OumatjieInfoCard] takes on. Chooses a color role, not a literal color, so every
 * card of a given tone stays in sync with the rest of the app if the palette in `Theme.kt` ever
 * changes — see docs/DESIGN_SYSTEM.md, "Shared components".
 *
 * - [Neutral]: routine, non-urgent information — a summary, a result the user asked for.
 * - [Highlight]: worth a second look, but calm rather than alarming — a new sender, an AI scam
 *   check that found something (docs/PRODUCT_PRINCIPLES.md: "calm, factual warnings," never a
 *   red alert for something that isn't confirmed harmful).
 * - [Problem]: something genuinely failed or blocks the user from continuing.
 */
enum class InfoCardTone {
    Neutral,
    Highlight,
    Problem,
}

@Composable
private fun InfoCardTone.colors(): CardColors = when (this) {
    InfoCardTone.Neutral -> CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    InfoCardTone.Highlight -> CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    )
    InfoCardTone.Problem -> CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    )
}

/**
 * The single shared "informational surface" used across the app for anything that isn't a
 * primary tappable content card (that's [MaterialTheme.shapes]'s `large`, used directly by
 * `MailCard` in `ui/mail/MailScreens.kt`) — a banner, a note, a result. Every screen that needs
 * this look (the first-contact note, the scam-check result, a message summary, an inline error,
 * a document's details) builds on this one composable instead of re-declaring its own
 * `Card`/shape/color/padding, the same way a shared Angular component centralizes one visual
 * pattern instead of letting each feature module re-style its own version. Changing this
 * function's shape, color mapping, or padding changes every one of those surfaces at once. See
 * docs/DESIGN_SYSTEM.md, "Shared components", for the full inventory this belongs to.
 *
 * Callers supply their own title/body/action content via [content] — this function owns only
 * the shared *look* (shape, color, padding, spacing), not the content structure, since that
 * genuinely differs per use (a plain note vs. a card with a trailing button vs. one with a
 * loading row).
 */
@Composable
fun OumatjieInfoCard(
    modifier: Modifier = Modifier,
    tone: InfoCardTone = InfoCardTone.Neutral,
    contentSpacing: Dp = 8.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = tone.colors(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(contentSpacing),
            content = content,
        )
    }
}

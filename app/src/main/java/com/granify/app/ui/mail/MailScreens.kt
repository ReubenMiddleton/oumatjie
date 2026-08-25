package com.granify.app.ui.mail

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.granify.app.ai.AiProvider
import com.granify.app.ai.ScamAssessment
import com.granify.app.data.MailAttachment
import com.granify.app.data.MailMessage
import com.granify.app.data.MailRepository
import com.granify.app.data.MailSummary
import com.granify.app.data.attachments.AttachmentDownloader
import com.granify.app.data.categories.MailCategory
import com.granify.app.data.senders.KnownSendersRepository
import com.granify.app.data.settings.SettingsRepository
import com.granify.app.pdf.PdfViewerActivity
import com.granify.app.ui.components.ConfirmationDialog
import com.granify.app.ui.components.ErrorScreen
import com.granify.app.ui.components.InfoCardTone
import com.granify.app.ui.components.OumatjieHeroButton
import com.granify.app.ui.components.OumatjieInfoCard
import com.granify.app.ui.components.OumatjieSecondaryButton
import com.granify.app.ui.components.OumatjieTertiaryButton
import com.granify.app.ui.components.LoadingScreen

@Composable
fun MailRoute(
    repository: MailRepository,
    attachmentDownloader: AttachmentDownloader,
    knownSendersRepository: KnownSendersRepository,
    settingsRepository: SettingsRepository,
    demoAiProvider: AiProvider,
    realAiProviderFor: (apiKey: String) -> AiProvider,
    onOpenSettings: () -> Unit,
) {
    val viewModel: MailViewModel = viewModel(
        factory = MailViewModel.factory(repository, attachmentDownloader, knownSendersRepository),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val aiFeaturesEnabled by settingsRepository.aiFeaturesEnabled.collectAsStateWithLifecycle(initialValue = false)
    val anthropicApiKey by settingsRepository.anthropicApiKey.collectAsStateWithLifecycle(initialValue = null)
    val realProvider = anthropicApiKey?.let { key -> realAiProviderFor(key) }

    LaunchedEffect(Unit) { viewModel.loadInbox() }
    LaunchedEffect(viewModel) {
        viewModel.openDocumentEvents.collect { uriString ->
            context.startActivity(PdfViewerActivity.createIntent(context, Uri.parse(uriString)))
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.snackbarMessages.collect { message -> snackbarHostState.showSnackbar(message) }
    }
    // Scam checking is triggered by opening a message, not by a separate button — but only
    // when a real provider is configured (never the demo heuristic; see ai/DemoAiProvider.kt)
    // and only when the user has explicitly turned AI features on (docs/AI_ASSISTANT.md,
    // "Trigger model").
    LaunchedEffect(state.selectedMessage?.summary?.id, aiFeaturesEnabled, anthropicApiKey) {
        val provider = realProvider
        if (aiFeaturesEnabled && provider != null && state.selectedMessage != null) {
            viewModel.checkForScamSignals(provider)
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(padding),
            color = MaterialTheme.colorScheme.background,
        ) {
            when {
                state.isLoading -> LoadingScreen()
                state.selectedMessage != null -> MessageScreen(
                    message = state.selectedMessage!!,
                    errorMessage = state.errorMessage,
                    downloadingAttachmentId = state.downloadingAttachmentId,
                    isFirstContact = state.selectedMessage!!.summary.id in state.firstContactMessageIds,
                    scamCheck = state.scamCheck,
                    aiFeaturesEnabled = aiFeaturesEnabled,
                    hasRealAiProvider = realProvider != null,
                    summary = state.summary,
                    onBack = viewModel::closeMessage,
                    onDone = viewModel::finishMessage,
                    onMoveToTrash = viewModel::moveSelectedMessageToTrash,
                    onOpenAttachment = viewModel::openAttachment,
                    onSummarize = {
                        viewModel.summarizeSelectedMessage(
                            provider = realProvider ?: demoAiProvider,
                            isDemo = realProvider == null,
                        )
                    },
                )
                state.errorMessage != null -> ErrorScreen(
                    message = state.errorMessage.orEmpty(),
                    onTryAgain = viewModel::loadInbox,
                )
                else -> InboxScreen(
                    messages = state.inbox,
                    firstContactMessageIds = state.firstContactMessageIds,
                    categoryByMessageId = state.categoryByMessageId,
                    onOpen = viewModel::openMessage,
                    onOpenSettings = onOpenSettings,
                )
            }
        }
    }
}

@Composable
private fun InboxScreen(
    messages: List<MailSummary>,
    firstContactMessageIds: Set<String>,
    categoryByMessageId: Map<String, MailCategory>,
    onOpen: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var showHelp by remember { mutableStateOf(false) }

    if (showHelp) {
        HelpDialog(onDismiss = { showHelp = false })
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = "Your mail",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                // Lets TalkBack users jump straight here with the "next heading" gesture,
                // instead of swiping through every message first. See docs/DESIGN_SYSTEM.md.
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = "Tap a message to read it.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
        }
        if (messages.isEmpty()) {
            item {
                Text(
                    text = "Your inbox is empty right now.",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
        items(messages, key = { it.id }) { message ->
            MailCard(
                message = message,
                isFirstContact = message.id in firstContactMessageIds,
                category = categoryByMessageId[message.id],
                onClick = { onOpen(message.id) },
                // A fast, gentle fade for cards appearing in the list (docs/ROADMAP.md's
                // retrospective: "zero motion beyond screen transitions" was a flagged gap).
                // A tween, not a spring, to stay consistent with the plain crossfade already
                // used for navigation (docs/PRODUCT_PRINCIPLES.md: reduced motion).
                modifier = Modifier.animateItem(fadeInSpec = tween<Float>(200), fadeOutSpec = tween<Float>(150)),
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OumatjieTertiaryButton(label = "Settings", onClick = onOpenSettings)
                OumatjieTertiaryButton(label = "Get help", onClick = { showHelp = true })
            }
        }
    }
}

@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    ConfirmationDialog(
        title = "Help with Oumatjie",
        explanation = "Tap a message to read it. When you are finished, choose Done reading. " +
            "Oumatjie will keep the message in your inbox and mark it as read. " +
            "If a message has a document attached, choose Open document to view it.",
        confirmLabel = "Close help",
        onConfirm = onDismiss,
        onDismiss = onDismiss,
    )
}

@Composable
private fun MailCard(
    message: MailSummary,
    isFirstContact: Boolean,
    category: MailCategory?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        // The larger of the app's two card radii (docs/DESIGN_SYSTEM.md, "Shape") — this is a
        // primary, tappable content surface, not an informational one (see OumatjieInfoCard).
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (message.isUnread) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(message.receivedLabel, style = MaterialTheme.typography.bodyLarge)
            }
            if (message.isUnread) {
                // Unread is otherwise signalled by card color alone (see below), which is
                // invisible to TalkBack and unreliable for low-vision/colorblind readers — a
                // real gap this label closes without changing the visual design at all.
                Text(
                    text = "Unread",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            if (isFirstContact) {
                Text(
                    text = "New sender",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            if (category != null) {
                // Tier 1 (no AI) categorization — docs/AI_ASSISTANT.md, "Categorization
                // design". Neutral styling on purpose: unlike "Unread"/"New sender" above, a
                // category is routine metadata, not something that needs attention.
                Text(
                    text = category.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(message.subject, style = MaterialTheme.typography.titleLarge)
            Text(message.preview, style = MaterialTheme.typography.bodyLarge)
            if (message.attachmentCount > 0) {
                Text(
                    text = if (message.attachmentCount == 1) "Contains 1 document" else "Contains ${message.attachmentCount} documents",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun MessageScreen(
    message: MailMessage,
    errorMessage: String?,
    downloadingAttachmentId: String?,
    isFirstContact: Boolean,
    scamCheck: ScamCheckUiState,
    aiFeaturesEnabled: Boolean,
    hasRealAiProvider: Boolean,
    summary: SummaryUiState,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onMoveToTrash: () -> Unit,
    onOpenAttachment: (MailAttachment) -> Unit,
    onSummarize: () -> Unit,
) {
    var showTrashConfirmation by remember { mutableStateOf(false) }
    var firstContactDismissed by remember(message.summary.id) { mutableStateOf(false) }
    val (readAloudController, isSpeaking) = rememberReadAloudController()
    val readAloudText = remember(message) {
        buildString {
            append("Message from ${message.summary.senderName}. ")
            append("Subject: ${message.summary.subject}. ")
            message.bodyParagraphs.forEach { append(it); append(". ") }
        }
    }

    if (showTrashConfirmation) {
        ConfirmationDialog(
            title = "Move to Trash?",
            explanation = "This message will move to Trash. You can get it back from Trash for 30 days. " +
                "After that, Gmail deletes it.",
            confirmLabel = "Move to Trash",
            onConfirm = {
                showTrashConfirmation = false
                onMoveToTrash()
            },
            onDismiss = { showTrashConfirmation = false },
        )
    }

    BackHandler(onBack = onBack)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            OumatjieTertiaryButton(label = "Back to your mail", onClick = onBack)
        }
        if (errorMessage != null) {
            item {
                OumatjieInfoCard(tone = InfoCardTone.Problem) {
                    Text(errorMessage, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        item {
            Text(
                text = message.summary.subject,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "From ${message.summary.senderName}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(message.summary.senderAddress, style = MaterialTheme.typography.bodyLarge)
        }
        if (isFirstContact && !firstContactDismissed) {
            item {
                FirstContactBanner(onDismiss = { firstContactDismissed = true })
            }
        }
        item {
            ScamCheckBanner(
                scamCheck = scamCheck,
                aiFeaturesEnabled = aiFeaturesEnabled,
                hasRealAiProvider = hasRealAiProvider,
            )
        }
        item {
            OumatjieSecondaryButton(
                label = if (isSpeaking) "Stop reading" else "Read this message aloud",
                onClick = {
                    if (isSpeaking) readAloudController.stop() else readAloudController.speak(readAloudText)
                },
            )
        }
        if (aiFeaturesEnabled) {
            item {
                SummarySection(summary = summary, onSummarize = onSummarize)
            }
        }
        item { HorizontalDivider() }
        items(message.bodyParagraphs) { paragraph ->
            Text(paragraph, style = MaterialTheme.typography.bodyLarge)
        }
        items(message.attachments, key = { it.id }) { attachment ->
            AttachmentCard(
                attachment = attachment,
                isDownloading = downloadingAttachmentId == attachment.id,
                onOpen = { onOpenAttachment(attachment) },
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OumatjieHeroButton(label = "Done reading", onClick = onDone)
                OumatjieSecondaryButton(
                    label = "Move to Trash",
                    onClick = { showTrashConfirmation = true },
                )
            }
        }
    }
}

/**
 * A calm, dismissible note on the first message ever received from a sender
 * (docs/AI_ASSISTANT.md, feature 1; docs/PRODUCT_PRINCIPLES.md: "Unknown senders... receive
 * calm, factual warnings"). Neutral styling on purpose — this is informational, not an alarm.
 */
@Composable
private fun FirstContactBanner(onDismiss: () -> Unit) {
    OumatjieInfoCard(tone = InfoCardTone.Highlight, contentSpacing = 12.dp) {
        Text(
            "You haven't received mail from this address before.",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Take a moment before clicking links or replying with personal information.",
            style = MaterialTheme.typography.bodyLarge,
        )
        OumatjieTertiaryButton(label = "Got it", onClick = onDismiss)
    }
}

@Composable
private fun ScamCheckBanner(
    scamCheck: ScamCheckUiState,
    aiFeaturesEnabled: Boolean,
    hasRealAiProvider: Boolean,
) {
    when {
        !aiFeaturesEnabled -> Unit // App behaves identically to one without this feature at all.
        !hasRealAiProvider -> Text(
            "Add an AI provider key in Settings to turn on scam checks for messages you open.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        scamCheck is ScamCheckUiState.Checking -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator(modifier = Modifier.height(24.dp))
            Text("Checking this message…", style = MaterialTheme.typography.bodyLarge)
        }
        scamCheck is ScamCheckUiState.Result -> when (val assessment = scamCheck.assessment) {
            ScamAssessment.NoConcernsFound -> Unit // A calm absence, not a "you're safe" badge.
            is ScamAssessment.WorthACloserLook -> OumatjieInfoCard(tone = InfoCardTone.Highlight) {
                Text(
                    "Worth a closer look",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(assessment.reason, style = MaterialTheme.typography.bodyLarge)
            }
            is ScamAssessment.CheckFailed -> Text(
                assessment.message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        else -> Unit
    }
}

@Composable
private fun SummarySection(summary: SummaryUiState, onSummarize: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (summary) {
            SummaryUiState.Idle -> OumatjieSecondaryButton(label = "Summarize this", onClick = onSummarize)
            SummaryUiState.Summarizing -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.height(24.dp))
                Text("Summarizing…", style = MaterialTheme.typography.bodyLarge)
            }
            is SummaryUiState.Result -> OumatjieInfoCard(tone = InfoCardTone.Neutral) {
                Text(
                    if (summary.isDemo) "Summary (demo)" else "Summary",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(summary.text, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun AttachmentCard(
    attachment: MailAttachment,
    isDownloading: Boolean,
    onOpen: () -> Unit,
) {
    OumatjieInfoCard(tone = InfoCardTone.Highlight, contentSpacing = 10.dp) {
        Text("Document", style = MaterialTheme.typography.labelLarge)
        Text(
            attachment.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(attachment.sizeLabel, style = MaterialTheme.typography.bodyLarge)
        if (attachment.isPasswordProtected) {
            Text("This document needs a password.", style = MaterialTheme.typography.bodyLarge)
        }
        if (isDownloading) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(modifier = Modifier.height(28.dp))
                Text("Opening…", style = MaterialTheme.typography.titleLarge)
            }
        } else {
            OumatjieHeroButton(label = "Open document", onClick = onOpen)
        }
    }
}

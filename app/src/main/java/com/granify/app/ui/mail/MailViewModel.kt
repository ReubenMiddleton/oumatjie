package com.granify.app.ui.mail

import com.granify.app.ai.AiProvider
import com.granify.app.ai.ScamAssessment
import com.granify.app.data.MailAttachment
import com.granify.app.data.MailAuthException
import com.granify.app.data.MailMessage
import com.granify.app.data.MailRepository
import com.granify.app.data.MailSummary
import com.granify.app.data.attachments.AttachmentDownloader
import com.granify.app.data.categories.CategoryAssigner
import com.granify.app.data.categories.MailCategory
import com.granify.app.data.senders.KnownSendersRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ScamCheckUiState {
    data object Idle : ScamCheckUiState
    data object Checking : ScamCheckUiState
    data class Result(val assessment: ScamAssessment) : ScamCheckUiState
}

sealed interface SummaryUiState {
    data object Idle : SummaryUiState
    data object Summarizing : SummaryUiState
    data class Result(val text: String, val isDemo: Boolean) : SummaryUiState
}

data class MailUiState(
    val isLoading: Boolean = false,
    val inbox: List<MailSummary> = emptyList(),
    val selectedMessage: MailMessage? = null,
    val errorMessage: String? = null,
    val downloadingAttachmentId: String? = null,
    /** Message ids whose sender had never been seen before this inbox load — see
     * docs/AI_ASSISTANT.md, "First-contact sender flagging". */
    val firstContactMessageIds: Set<String> = emptySet(),
    /** Tier 1 (no AI) category, by message id, for messages a local rule confidently matched —
     * see docs/AI_ASSISTANT.md, "Categorization design" and `data/categories/CategoryAssigner`.
     * A message with no entry here is simply uncategorized, not an error. */
    val categoryByMessageId: Map<String, MailCategory> = emptyMap(),
    val scamCheck: ScamCheckUiState = ScamCheckUiState.Idle,
    val summary: SummaryUiState = SummaryUiState.Idle,
)

class MailViewModel(
    private val repository: MailRepository,
    private val attachmentDownloader: AttachmentDownloader,
    private val knownSendersRepository: KnownSendersRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(MailUiState())
    val state: StateFlow<MailUiState> = _state.asStateFlow()

    private val _openDocumentEvents = Channel<String>(Channel.BUFFERED)
    val openDocumentEvents: Flow<String> = _openDocumentEvents.receiveAsFlow()

    private val _snackbarMessages = Channel<String>(Channel.BUFFERED)
    val snackbarMessages: Flow<String> = _snackbarMessages.receiveAsFlow()

    // Tracked so leaving the message screen (back, Done reading, Move to Trash) cancels an
    // in-flight download rather than letting the PDF viewer pop open after the user has
    // already navigated away.
    private var attachmentDownloadJob: Job? = null
    private var scamCheckJob: Job? = null
    private var summarizeJob: Job? = null

    fun loadInbox() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { repository.loadInbox() }
                .onSuccess { inbox ->
                    val firstContactIds = inbox
                        .filter { knownSendersRepository.isFirstContact(it.senderAddress) }
                        .map { it.id }
                        .toSet()
                    knownSendersRepository.recordSeen(inbox.map { it.senderAddress })
                    _state.update {
                        it.copy(
                            isLoading = false,
                            inbox = inbox,
                            selectedMessage = null,
                            firstContactMessageIds = firstContactIds,
                            categoryByMessageId = CategoryAssigner.assignAll(inbox),
                        )
                    }
                }
                .onFailureIgnoringCancellation { failure ->
                    _state.update { state ->
                        state.copy(isLoading = false, errorMessage = errorMessageFor(failure, "We could not load your mail."))
                    }
                }
        }
    }

    fun openMessage(id: String) {
        viewModelScope.launch {
            // Loading state only, not the full-screen errorMessage: a failure here should
            // leave the user on the inbox they already have, not blow it away (that field
            // is reserved for "there is nothing else to show").
            _state.update { it.copy(isLoading = true) }
            runCatching { repository.loadMessage(id) }
                .onSuccess { message ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            selectedMessage = message,
                            scamCheck = ScamCheckUiState.Idle,
                            summary = SummaryUiState.Idle,
                        )
                    }
                }
                .onFailureIgnoringCancellation { failure ->
                    _state.update { it.copy(isLoading = false) }
                    _snackbarMessages.send(errorMessageFor(failure, "We could not open this message. Please try again."))
                }
        }
    }

    fun finishMessage() {
        val message = _state.value.selectedMessage ?: return
        attachmentDownloadJob?.cancel()
        scamCheckJob?.cancel()
        summarizeJob?.cancel()
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { repository.markDone(message.summary.id) }
                .onSuccess {
                    _state.update { state ->
                        state.copy(
                            isLoading = false,
                            inbox = state.inbox.map { summary ->
                                if (summary.id == message.summary.id) summary.copy(isUnread = false) else summary
                            },
                            selectedMessage = null,
                            scamCheck = ScamCheckUiState.Idle,
                            summary = SummaryUiState.Idle,
                        )
                    }
                }
                .onFailureIgnoringCancellation { failure ->
                    _state.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = errorMessageFor(failure, "We could not finish this message. Please try again."),
                        )
                    }
                }
        }
    }

    fun moveSelectedMessageToTrash() {
        val message = _state.value.selectedMessage ?: return
        attachmentDownloadJob?.cancel()
        scamCheckJob?.cancel()
        summarizeJob?.cancel()
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { repository.moveToTrash(message.summary.id) }
                .onSuccess {
                    _state.update { state ->
                        state.copy(
                            isLoading = false,
                            inbox = state.inbox.filterNot { it.id == message.summary.id },
                            selectedMessage = null,
                            scamCheck = ScamCheckUiState.Idle,
                            summary = SummaryUiState.Idle,
                        )
                    }
                    _snackbarMessages.send("Moved to Trash. You can get it back from Trash for 30 days.")
                }
                .onFailureIgnoringCancellation { failure ->
                    _state.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = errorMessageFor(failure, "We could not move this message to Trash. Please try again."),
                        )
                    }
                }
        }
    }

    fun openAttachment(attachment: MailAttachment) {
        val messageId = _state.value.selectedMessage?.summary?.id ?: return
        attachmentDownloadJob = viewModelScope.launch {
            _state.update { it.copy(downloadingAttachmentId = attachment.id, errorMessage = null) }
            runCatching { attachmentDownloader.download(messageId, attachment) }
                .onSuccess { uriString ->
                    _state.update { it.copy(downloadingAttachmentId = null) }
                    _openDocumentEvents.send(uriString)
                }
                .onFailureIgnoringCancellation { failure ->
                    _state.update { state ->
                        state.copy(
                            downloadingAttachmentId = null,
                            errorMessage = errorMessageFor(failure, "We could not open this document. Please try again."),
                        )
                    }
                }
        }
    }

    /**
     * Checks the currently open message for scam/phishing signals (docs/AI_ASSISTANT.md,
     * feature 2). [provider] is passed in by the caller rather than stored, since it depends
     * on Settings state (whether AI features are on, and which key) that can change while the
     * app is running — see ui/mail/MailScreens.kt for how it's chosen. Never called with a
     * [com.granify.app.ai.DemoAiProvider] in production code — see that class for why.
     */
    fun checkForScamSignals(provider: AiProvider) {
        val message = _state.value.selectedMessage ?: return
        if (_state.value.scamCheck !is ScamCheckUiState.Idle) return
        scamCheckJob = viewModelScope.launch {
            _state.update { it.copy(scamCheck = ScamCheckUiState.Checking) }
            val assessment = provider.checkForScamSignals(
                subject = message.summary.subject,
                senderAddress = message.summary.senderAddress,
                bodyText = message.bodyParagraphs.joinToString("\n\n"),
            )
            _state.update { it.copy(scamCheck = ScamCheckUiState.Result(assessment)) }
        }
    }

    /** Summarizes the currently open message on an explicit tap (docs/AI_ASSISTANT.md,
     * feature 4 — "e.g. a 'Summarize this' button", unlike the scam check above which fires
     * automatically on open). [isDemo] only affects how the result is labelled in the UI. */
    fun summarizeSelectedMessage(provider: AiProvider, isDemo: Boolean) {
        val message = _state.value.selectedMessage ?: return
        summarizeJob?.cancel()
        summarizeJob = viewModelScope.launch {
            _state.update { it.copy(summary = SummaryUiState.Summarizing) }
            val text = provider.summarize(
                subject = message.summary.subject,
                bodyText = message.bodyParagraphs.joinToString("\n\n"),
            )
            _state.update { it.copy(summary = SummaryUiState.Result(text, isDemo)) }
        }
    }

    fun closeMessage() {
        attachmentDownloadJob?.cancel()
        scamCheckJob?.cancel()
        summarizeJob?.cancel()
        _state.update {
            it.copy(
                selectedMessage = null,
                errorMessage = null,
                downloadingAttachmentId = null,
                scamCheck = ScamCheckUiState.Idle,
                summary = SummaryUiState.Idle,
            )
        }
    }

    private fun errorMessageFor(failure: Throwable, fallback: String): String =
        (failure as? MailAuthException)?.message ?: fallback

    /**
     * Like [Result.onFailure], but re-throws [CancellationException] instead of treating it as
     * a reportable error. Without this, cancelling [attachmentDownloadJob] (or any other
     * in-flight call) after the user has already navigated away would still run the failure
     * branch and could write a stale error message onto whatever screen they're on now.
     */
    private inline fun <T> Result<T>.onFailureIgnoringCancellation(action: (Throwable) -> Unit): Result<T> =
        onFailure { failure ->
            if (failure is CancellationException) throw failure
            action(failure)
        }

    companion object {
        fun factory(
            repository: MailRepository,
            attachmentDownloader: AttachmentDownloader,
            knownSendersRepository: KnownSendersRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                MailViewModel(repository, attachmentDownloader, knownSendersRepository) as T
        }
    }
}

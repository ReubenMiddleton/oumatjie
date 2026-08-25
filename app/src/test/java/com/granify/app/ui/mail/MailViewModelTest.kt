package com.granify.app.ui.mail

import com.granify.app.ai.AiProvider
import com.granify.app.ai.ScamAssessment
import com.granify.app.data.MailAttachment
import com.granify.app.data.MailMessage
import com.granify.app.data.MailRepository
import com.granify.app.data.MailSummary
import com.granify.app.data.attachments.AttachmentDownloader
import com.granify.app.data.categories.StarterCategories
import com.granify.app.data.senders.KnownSendersRepository
import com.granify.app.ui.MainDispatcherRule
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun finishMessage_marksItReadAndReturnsToInbox() = runTest {
        val repository = FakeMailRepository()
        val viewModel = MailViewModel(repository, FakeAttachmentDownloader(), FakeKnownSendersRepository())

        viewModel.loadInbox()
        viewModel.openMessage(repository.message.summary.id)
        viewModel.finishMessage()

        assertEquals(listOf(repository.message.summary.id), repository.finishedIds)
        assertNull(viewModel.state.value.selectedMessage)
        assertFalse(viewModel.state.value.inbox.single().isUnread)
    }

    @Test
    fun finishMessage_failureKeepsTheMessageOpenForRetry() = runTest {
        val repository = FakeMailRepository(failWhenFinishing = true)
        val viewModel = MailViewModel(repository, FakeAttachmentDownloader(), FakeKnownSendersRepository())

        viewModel.loadInbox()
        viewModel.openMessage(repository.message.summary.id)
        viewModel.finishMessage()

        assertNotNull(viewModel.state.value.selectedMessage)
        assertNotNull(viewModel.state.value.errorMessage)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun moveSelectedMessageToTrash_removesItFromTheInboxAndNotifies() = runTest {
        val repository = FakeMailRepository()
        val viewModel = MailViewModel(repository, FakeAttachmentDownloader(), FakeKnownSendersRepository())

        viewModel.loadInbox()
        viewModel.openMessage(repository.message.summary.id)
        viewModel.moveSelectedMessageToTrash()

        assertEquals(listOf(repository.message.summary.id), repository.trashedIds)
        assertNull(viewModel.state.value.selectedMessage)
        assertTrue(viewModel.state.value.inbox.isEmpty())
        assertEquals("Moved to Trash. You can get it back from Trash for 30 days.", viewModel.snackbarMessages.first())
    }

    @Test
    fun moveSelectedMessageToTrash_failureKeepsTheMessageOpen() = runTest {
        val repository = FakeMailRepository(failWhenTrashing = true)
        val viewModel = MailViewModel(repository, FakeAttachmentDownloader(), FakeKnownSendersRepository())

        viewModel.loadInbox()
        viewModel.openMessage(repository.message.summary.id)
        viewModel.moveSelectedMessageToTrash()

        assertNotNull(viewModel.state.value.selectedMessage)
        assertNotNull(viewModel.state.value.errorMessage)
    }

    @Test
    fun openAttachment_emitsTheDownloadedDocumentUri() = runTest {
        val repository = FakeMailRepository()
        val downloader = FakeAttachmentDownloader(uriToReturn = "content://oumatjie/sample.pdf")
        val viewModel = MailViewModel(repository, downloader, FakeKnownSendersRepository())

        viewModel.loadInbox()
        viewModel.openMessage(repository.message.summary.id)
        viewModel.openAttachment(repository.attachment)

        assertEquals("content://oumatjie/sample.pdf", viewModel.openDocumentEvents.first())
        assertNull(viewModel.state.value.downloadingAttachmentId)
    }

    @Test
    fun openMessage_failureShowsASnackbarAndStaysOnTheInbox() = runTest {
        val repository = FakeMailRepository(failWhenOpeningMessage = true)
        val viewModel = MailViewModel(repository, FakeAttachmentDownloader(), FakeKnownSendersRepository())

        viewModel.loadInbox()
        viewModel.openMessage(repository.message.summary.id)

        assertNull(viewModel.state.value.selectedMessage)
        assertNull(viewModel.state.value.errorMessage)
        assertEquals("We could not open this message. Please try again.", viewModel.snackbarMessages.first())
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun closeMessage_cancelsAnInFlightAttachmentDownloadInsteadOfOpeningItLate() = runTest {
        val repository = FakeMailRepository()
        val downloader = FakeAttachmentDownloader(delayMillis = 1_000)
        val viewModel = MailViewModel(repository, downloader, FakeKnownSendersRepository())

        viewModel.loadInbox()
        viewModel.openMessage(repository.message.summary.id)
        viewModel.openAttachment(repository.attachment)
        assertEquals(repository.attachment.id, viewModel.state.value.downloadingAttachmentId)

        viewModel.closeMessage()
        advanceUntilIdle()

        assertNull(viewModel.state.value.selectedMessage)
        assertNull(viewModel.state.value.errorMessage)
        assertNull(viewModel.state.value.downloadingAttachmentId)
    }

    @Test
    fun openAttachment_failureSurfacesAnErrorMessage() = runTest {
        val repository = FakeMailRepository()
        val downloader = FakeAttachmentDownloader(shouldFail = true)
        val viewModel = MailViewModel(repository, downloader, FakeKnownSendersRepository())

        viewModel.loadInbox()
        viewModel.openMessage(repository.message.summary.id)
        viewModel.openAttachment(repository.attachment)

        assertNotNull(viewModel.state.value.errorMessage)
        assertNull(viewModel.state.value.downloadingAttachmentId)
    }

    @Test
    fun loadInbox_flagsAMessageFromANeverSeenSenderAsFirstContact() = runTest {
        val repository = FakeMailRepository()
        val viewModel = MailViewModel(repository, FakeAttachmentDownloader(), FakeKnownSendersRepository())

        viewModel.loadInbox()

        assertTrue(repository.message.summary.id in viewModel.state.value.firstContactMessageIds)
    }

    @Test
    fun loadInbox_doesNotFlagAMessageFromAnAlreadyKnownSender() = runTest {
        val repository = FakeMailRepository()
        val knownSenders = FakeKnownSendersRepository(alreadyKnown = setOf(repository.message.summary.senderAddress))
        val viewModel = MailViewModel(repository, FakeAttachmentDownloader(), knownSenders)

        viewModel.loadInbox()

        assertTrue(viewModel.state.value.firstContactMessageIds.isEmpty())
    }

    @Test
    fun loadInbox_recordsEverySenderSeenSoALaterLoadWouldNotReflagThem() = runTest {
        val repository = FakeMailRepository()
        val knownSenders = FakeKnownSendersRepository()
        val viewModel = MailViewModel(repository, FakeAttachmentDownloader(), knownSenders)

        viewModel.loadInbox()

        assertTrue(repository.message.summary.senderAddress in knownSenders.recorded)
    }

    @Test
    fun loadInbox_assignsATier1CategoryWhenTheSubjectMatchesAKnownPattern() = runTest {
        val repository = FakeMailRepository(subject = "Your March statement is ready")
        val viewModel = MailViewModel(repository, FakeAttachmentDownloader(), FakeKnownSendersRepository())

        viewModel.loadInbox()

        assertEquals(
            StarterCategories.Bills,
            viewModel.state.value.categoryByMessageId[repository.message.summary.id],
        )
    }

    @Test
    fun loadInbox_leavesAMessageUncategorizedWhenNoRuleMatches() = runTest {
        val repository = FakeMailRepository() // default "Subject"/"Preview" match nothing
        val viewModel = MailViewModel(repository, FakeAttachmentDownloader(), FakeKnownSendersRepository())

        viewModel.loadInbox()

        assertTrue(viewModel.state.value.categoryByMessageId.isEmpty())
    }

    @Test
    fun openMessage_resetsScamCheckAndSummaryFromAPreviousMessage() = runTest {
        val repository = FakeMailRepository()
        val viewModel = MailViewModel(repository, FakeAttachmentDownloader(), FakeKnownSendersRepository())

        viewModel.loadInbox()
        viewModel.openMessage(repository.message.summary.id)
        viewModel.checkForScamSignals(FakeAiProvider(scamAssessment = ScamAssessment.NoConcernsFound))
        viewModel.summarizeSelectedMessage(FakeAiProvider(summaryText = "A summary"), isDemo = false)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.scamCheck is ScamCheckUiState.Result)
        assertTrue(viewModel.state.value.summary is SummaryUiState.Result)

        viewModel.openMessage(repository.message.summary.id)

        assertEquals(ScamCheckUiState.Idle, viewModel.state.value.scamCheck)
        assertEquals(SummaryUiState.Idle, viewModel.state.value.summary)
    }

    @Test
    fun checkForScamSignals_surfacesAWorthACloserLookResult() = runTest {
        val repository = FakeMailRepository()
        val viewModel = MailViewModel(repository, FakeAttachmentDownloader(), FakeKnownSendersRepository())
        val provider = FakeAiProvider(
            scamAssessment = ScamAssessment.WorthACloserLook("This asks for your password."),
        )

        viewModel.loadInbox()
        viewModel.openMessage(repository.message.summary.id)
        viewModel.checkForScamSignals(provider)
        advanceUntilIdle()

        val result = viewModel.state.value.scamCheck
        assertTrue(result is ScamCheckUiState.Result)
        assertEquals(
            ScamAssessment.WorthACloserLook("This asks for your password."),
            (result as ScamCheckUiState.Result).assessment,
        )
    }

    @Test
    fun checkForScamSignals_doesNotRunTwiceForTheSameMessage() = runTest {
        val repository = FakeMailRepository()
        val viewModel = MailViewModel(repository, FakeAttachmentDownloader(), FakeKnownSendersRepository())
        val provider = FakeAiProvider(scamAssessment = ScamAssessment.NoConcernsFound)

        viewModel.loadInbox()
        viewModel.openMessage(repository.message.summary.id)
        viewModel.checkForScamSignals(provider)
        advanceUntilIdle()
        viewModel.checkForScamSignals(provider)
        advanceUntilIdle()

        assertEquals(1, provider.scamCheckCallCount)
    }

    @Test
    fun summarizeSelectedMessage_surfacesTheSummaryAndDemoFlag() = runTest {
        val repository = FakeMailRepository()
        val viewModel = MailViewModel(repository, FakeAttachmentDownloader(), FakeKnownSendersRepository())
        val provider = FakeAiProvider(summaryText = "In short: this is a test.")

        viewModel.loadInbox()
        viewModel.openMessage(repository.message.summary.id)
        viewModel.summarizeSelectedMessage(provider, isDemo = true)
        advanceUntilIdle()

        val result = viewModel.state.value.summary
        assertTrue(result is SummaryUiState.Result)
        assertEquals("In short: this is a test.", (result as SummaryUiState.Result).text)
        assertTrue(result.isDemo)
    }

    private class FakeMailRepository(
        private val failWhenFinishing: Boolean = false,
        private val failWhenTrashing: Boolean = false,
        private val failWhenOpeningMessage: Boolean = false,
        subject: String = "Subject",
    ) : MailRepository {
        val attachment = MailAttachment(
            id = "attachment-1",
            name = "Sample.pdf",
            mimeType = "application/pdf",
            sizeLabel = "10 KB",
        )
        val message = MailMessage(
            summary = MailSummary(
                id = "message-1",
                senderName = "Sender",
                senderAddress = "sender@example.test",
                subject = subject,
                preview = "Preview",
                receivedLabel = "Today",
                isUnread = true,
                attachmentCount = 1,
            ),
            bodyParagraphs = listOf("Body"),
            attachments = listOf(attachment),
        )
        val finishedIds = mutableListOf<String>()
        val trashedIds = mutableListOf<String>()

        override suspend fun loadInbox() = listOf(message.summary)

        override suspend fun loadMessage(id: String): MailMessage {
            if (failWhenOpeningMessage) error("Could not load message")
            return message
        }

        override suspend fun markDone(id: String) {
            if (failWhenFinishing) error("Could not update message")
            finishedIds += id
        }

        override suspend fun moveToTrash(id: String) {
            if (failWhenTrashing) error("Could not move message to Trash")
            trashedIds += id
        }
    }

    private class FakeAttachmentDownloader(
        private val uriToReturn: String = "content://oumatjie/attachment",
        private val shouldFail: Boolean = false,
        private val delayMillis: Long = 0,
    ) : AttachmentDownloader {
        override suspend fun download(messageId: String, attachment: MailAttachment): String {
            if (delayMillis > 0) delay(delayMillis)
            if (shouldFail) error("Could not download attachment")
            return uriToReturn
        }
    }

    private class FakeKnownSendersRepository(
        alreadyKnown: Set<String> = emptySet(),
    ) : KnownSendersRepository {
        val recorded = mutableSetOf<String>().apply { addAll(alreadyKnown.map { it.lowercase() }) }

        override suspend fun isFirstContact(address: String): Boolean =
            address.isNotBlank() && address.lowercase() !in recorded

        override suspend fun recordSeen(addresses: Collection<String>) {
            recorded += addresses.filter { it.isNotBlank() }.map { it.lowercase() }
        }
    }

    private class FakeAiProvider(
        private val scamAssessment: ScamAssessment = ScamAssessment.NoConcernsFound,
        private val summaryText: String = "Summary",
    ) : AiProvider {
        var scamCheckCallCount = 0
            private set

        override suspend fun checkForScamSignals(subject: String, senderAddress: String, bodyText: String): ScamAssessment {
            scamCheckCallCount++
            return scamAssessment
        }

        override suspend fun summarize(subject: String, bodyText: String): String = summaryText
    }
}

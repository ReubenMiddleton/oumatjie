package com.granify.app.data.gmail

import android.content.Intent
import com.granify.app.auth.AuthManager
import com.granify.app.auth.AuthorizeOutcome
import com.granify.app.data.MailAuthException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GmailMailRepositoryTest {
    private val sampleMessage = GmailMessage(
        id = "msg-1",
        labelIds = listOf("INBOX", "UNREAD"),
        snippet = "See you Sunday",
        payload = GmailMessagePart(
            headers = listOf(
                GmailHeader("Subject", "Lunch on Sunday"),
                GmailHeader("From", "Sarah <sarah@example.test>"),
            ),
        ),
    )

    @Test
    fun loadInbox_mapsGmailMessagesToSummaries() = runTest {
        val api = FakeGmailApiService(messages = mutableMapOf("msg-1" to sampleMessage))
        val repository = GmailMailRepository(api, FakeAuthManager())

        val inbox = repository.loadInbox()

        assertEquals(1, inbox.size)
        val summary = inbox.single()
        assertEquals("Sarah", summary.senderName)
        assertEquals("Lunch on Sunday", summary.subject)
        assertTrue(summary.isUnread)
    }

    @Test
    fun markDone_removesTheUnreadLabel() = runTest {
        val api = FakeGmailApiService(messages = mutableMapOf("msg-1" to sampleMessage))
        val repository = GmailMailRepository(api, FakeAuthManager())

        repository.markDone("msg-1")

        assertEquals(listOf("UNREAD"), api.modifyRequests.single().second.removeLabelIds)
    }

    @Test
    fun moveToTrash_callsTheTrashEndpoint() = runTest {
        val api = FakeGmailApiService(messages = mutableMapOf("msg-1" to sampleMessage))
        val repository = GmailMailRepository(api, FakeAuthManager())

        repository.moveToTrash("msg-1")

        assertEquals(listOf("msg-1"), api.trashedIds)
    }

    @Test
    fun loadInbox_dropsAMessageThatFailsToFetchInsteadOfFailingTheWholeInbox() = runTest {
        val okMessage = sampleMessage.copy(id = "msg-ok")
        val api = FakeGmailApiService(
            messages = mutableMapOf("msg-ok" to okMessage, "msg-broken" to sampleMessage.copy(id = "msg-broken")),
            listedIds = listOf("msg-ok", "msg-broken"),
            failingIds = setOf("msg-broken"),
        )
        val repository = GmailMailRepository(api, FakeAuthManager())

        val inbox = repository.loadInbox()

        assertEquals(listOf("msg-ok"), inbox.map { it.id })
    }

    @Test
    fun loadInbox_wrapsAuthFailuresAsMailAuthException() = runTest {
        val api = FakeGmailApiService(messages = mutableMapOf("msg-1" to sampleMessage))
        val authManager = FakeAuthManager(AuthorizeOutcome.Failed("Access was revoked."))
        val repository = GmailMailRepository(api, authManager)

        val failure = runCatching { repository.loadInbox() }.exceptionOrNull()

        assertTrue(failure is MailAuthException)
        assertEquals("Access was revoked.", failure?.message)
    }

    private class FakeGmailApiService(
        private val messages: MutableMap<String, GmailMessage>,
        private val listedIds: List<String> = messages.keys.toList(),
        private val failingIds: Set<String> = emptySet(),
    ) : GmailApiService {
        val modifyRequests = mutableListOf<Pair<String, GmailModifyRequest>>()
        val trashedIds = mutableListOf<String>()

        override suspend fun getProfile(token: String) = GmailProfile(emailAddress = "demo@example.test")

        override suspend fun listMessages(token: String, labelIds: String, maxResults: Int) =
            GmailMessageListResponse(messages = listedIds.map { GmailMessageRef(it) })

        override suspend fun getMessage(token: String, id: String, format: String): GmailMessage {
            if (id in failingIds) error("Simulated fetch failure for $id")
            return messages.getValue(id)
        }

        override suspend fun getAttachment(token: String, messageId: String, attachmentId: String) =
            GmailAttachmentData(attachmentId = attachmentId, size = 0, data = null)

        override suspend fun modifyMessage(token: String, id: String, request: GmailModifyRequest): GmailMessage {
            modifyRequests += id to request
            return messages.getValue(id)
        }

        override suspend fun trashMessage(token: String, id: String): GmailMessage {
            trashedIds += id
            return messages.getValue(id)
        }
    }

    private class FakeAuthManager(
        private val outcome: AuthorizeOutcome = AuthorizeOutcome.Granted("test-token"),
    ) : AuthManager {
        override suspend fun authorize(scopes: List<String>): AuthorizeOutcome = outcome

        override fun resolveAuthorizationResult(data: Intent?): AuthorizeOutcome = outcome
    }
}

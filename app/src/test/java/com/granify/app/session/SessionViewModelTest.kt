package com.granify.app.session

import android.content.Intent
import com.granify.app.auth.AuthManager
import com.granify.app.auth.AuthorizeOutcome
import com.granify.app.data.gmail.GmailApiService
import com.granify.app.data.gmail.GmailAttachmentData
import com.granify.app.data.gmail.GmailMailRepository
import com.granify.app.data.gmail.GmailMessage
import com.granify.app.data.gmail.GmailMessageListResponse
import com.granify.app.data.gmail.GmailModifyRequest
import com.granify.app.data.gmail.GmailProfile
import com.granify.app.ui.MainDispatcherRule
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Covers the silent-reauth logic added for session persistence (docs/ROADMAP.md's
 * retrospective, "Session persistence was deferred too readily"). This ViewModel had no test
 * coverage before this change; see docs/DECISIONS.md for why [SessionRepository] is an
 * interface specifically so this could be tested with a plain in-memory fake, the same
 * reasoning already applied to [AuthManager] and [com.granify.app.data.MailRepository].
 */
class SessionViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun init_withNoPreviousSignIn_neverCallsAuthorizeAndStaysSignedOut() = runTest {
        val authManager = FakeAuthManager(AuthorizeOutcome.Granted("token"))
        val viewModel = sessionViewModel(authManager = authManager, hasSignedInBefore = false)
        advanceUntilIdle()

        assertEquals(0, authManager.authorizeCallCount)
        assertEquals(SessionState.SignedOut, viewModel.session.value)
        assertFalse(viewModel.signInState.value.isLoading)
    }

    @Test
    fun init_withPreviousSignIn_andGrantedOutcome_signsInSilentlyWithNoLoadingLeftOver() = runTest {
        val authManager = FakeAuthManager(AuthorizeOutcome.Granted("token"))
        val viewModel = sessionViewModel(authManager = authManager, hasSignedInBefore = true)
        advanceUntilIdle()

        assertEquals(1, authManager.authorizeCallCount)
        val session = viewModel.session.value
        assertTrue(session is SessionState.SignedIn)
        assertEquals("demo@example.test", (session as SessionState.SignedIn).email)
        assertFalse(viewModel.signInState.value.isLoading)
        assertNull(viewModel.signInState.value.errorMessage)
    }

    @Test
    fun init_withPreviousSignIn_andFailedOutcome_clearsTheFlagAndShowsNoError() = runTest {
        val authManager = FakeAuthManager(AuthorizeOutcome.Failed("Access was revoked."))
        val sessionRepository = FakeSessionRepository(initiallySignedIn = true)
        val viewModel = sessionViewModel(authManager = authManager, sessionRepository = sessionRepository)
        advanceUntilIdle()

        assertEquals(SessionState.SignedOut, viewModel.session.value)
        assertNull(viewModel.signInState.value.errorMessage) // silent — the user never asked
        assertFalse(sessionRepository.hasSignedInBefore())
    }

    // Note: there is no test here for the AuthorizeOutcome.ResolutionRequired branch of the
    // silent-reauth path — ResolutionRequired carries a real android.app.PendingIntent, which
    // cannot be safely constructed in a plain JVM unit test without Robolectric or a mocking
    // library (neither is a dependency of this project — see docs/DECISIONS.md,
    // "AttachmentDownloader returns String, not android.net.Uri" for the same constraint
    // applied elsewhere). The branch is implemented (see SessionViewModel.attemptSilentSignIn)
    // and reasoned about in its own comment, but is a documented gap, not a verified one.

    @Test
    fun signInWithGoogle_onSuccess_recordsSignedInForNextColdStart() = runTest {
        val authManager = FakeAuthManager(AuthorizeOutcome.Granted("token"))
        val sessionRepository = FakeSessionRepository(initiallySignedIn = false)
        val viewModel = sessionViewModel(authManager = authManager, sessionRepository = sessionRepository)
        advanceUntilIdle()

        viewModel.signInWithGoogle()
        advanceUntilIdle()

        assertTrue(sessionRepository.hasSignedInBefore())
        assertTrue(viewModel.session.value is SessionState.SignedIn)
    }

    @Test
    fun signOut_clearsTheRememberedSession() = runTest {
        val sessionRepository = FakeSessionRepository(initiallySignedIn = true)
        val viewModel = sessionViewModel(
            authManager = FakeAuthManager(AuthorizeOutcome.Granted("token")),
            sessionRepository = sessionRepository,
        )
        advanceUntilIdle()

        viewModel.signOut()
        advanceUntilIdle()

        assertEquals(SessionState.SignedOut, viewModel.session.value)
        assertFalse(sessionRepository.hasSignedInBefore())
    }

    private fun sessionViewModel(
        authManager: AuthManager,
        sessionRepository: SessionRepository = FakeSessionRepository(initiallySignedIn = false),
        hasSignedInBefore: Boolean? = null,
    ): SessionViewModel {
        val repo = if (hasSignedInBefore != null) FakeSessionRepository(hasSignedInBefore) else sessionRepository
        val api = FakeGmailApiService()
        val gmailMailRepository = GmailMailRepository(api, authManager)
        return SessionViewModel(authManager, gmailMailRepository, repo)
    }

    private class FakeAuthManager(private val outcome: AuthorizeOutcome) : AuthManager {
        var authorizeCallCount = 0
            private set

        override suspend fun authorize(scopes: List<String>): AuthorizeOutcome {
            authorizeCallCount++
            return outcome
        }

        override fun resolveAuthorizationResult(data: Intent?): AuthorizeOutcome = outcome
    }

    private class FakeSessionRepository(initiallySignedIn: Boolean) : SessionRepository {
        private var signedIn = initiallySignedIn

        override suspend fun hasSignedInBefore(): Boolean = signedIn
        override suspend fun recordSignedIn() {
            signedIn = true
        }
        override suspend fun clear() {
            signedIn = false
        }
    }

    private class FakeGmailApiService(private val email: String = "demo@example.test") : GmailApiService {
        override suspend fun getProfile(token: String) = GmailProfile(emailAddress = email)

        override suspend fun listMessages(token: String, labelIds: String, maxResults: Int): GmailMessageListResponse =
            GmailMessageListResponse()

        override suspend fun getMessage(token: String, id: String, format: String): GmailMessage =
            throw UnsupportedOperationException("Not used by these tests")

        override suspend fun getAttachment(token: String, messageId: String, attachmentId: String): GmailAttachmentData =
            throw UnsupportedOperationException("Not used by these tests")

        override suspend fun modifyMessage(token: String, id: String, request: GmailModifyRequest): GmailMessage =
            throw UnsupportedOperationException("Not used by these tests")

        override suspend fun trashMessage(token: String, id: String): GmailMessage =
            throw UnsupportedOperationException("Not used by these tests")
    }
}

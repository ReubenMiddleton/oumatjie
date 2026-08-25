package com.granify.app.data.gmail

import com.granify.app.auth.AuthManager
import com.granify.app.auth.AuthorizeOutcome
import com.granify.app.auth.GmailScopes
import com.granify.app.data.MailAuthException
import com.granify.app.data.MailMessage
import com.granify.app.data.MailRepository
import com.granify.app.data.MailSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Reads and updates Gmail through the real REST API. [authManager] is asked for a token right
 * before every call, which doubles as a silent refresh: once scopes are granted, re-requesting
 * them normally returns immediately without showing the user anything (see
 * [com.granify.app.auth.GoogleAuthManager]).
 */
class GmailMailRepository(
    private val api: GmailApiService,
    private val authManager: AuthManager,
) : MailRepository {

    override suspend fun loadInbox(): List<MailSummary> = coroutineScope {
        val token = authHeader()
        val refs = api.listMessages(token).messages
        // Each message is fetched independently and a single failure (a transient blip, or a
        // message deleted between listing and fetching) is dropped rather than failing the
        // whole inbox — showing the 24 that loaded beats a blank error screen over 1 that
        // didn't, and there is nothing actionable a user could do about it anyway.
        refs
            .map { ref -> async { fetchMessageOrNull(token, ref.id) } }
            .mapNotNull { it.await() }
            .map { it.toSummary() }
    }

    private suspend fun fetchMessageOrNull(token: String, id: String): GmailMessage? = try {
        api.getMessage(token, id)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    override suspend fun loadMessage(id: String): MailMessage {
        val token = authHeader()
        return api.getMessage(token, id).toMailMessage()
    }

    override suspend fun markDone(id: String) {
        val token = authHeader()
        api.modifyMessage(token, id, GmailModifyRequest(removeLabelIds = listOf("UNREAD")))
    }

    override suspend fun moveToTrash(id: String) {
        val token = authHeader()
        api.trashMessage(token, id)
    }

    /** The signed-in address, for "Signed in as …" display copy. */
    /**
     * [accessToken] is passed in rather than fetched via [authHeader] here, deliberately: every
     * caller of this already just received a fresh [AuthorizeOutcome.Granted] token a moment
     * ago (see [com.granify.app.session.SessionViewModel.handleOutcome]) — re-deriving it via
     * another [authHeader] call would silently re-invoke [AuthManager.authorize] a second time
     * per sign-in, which is exactly the bug `SessionViewModelTest` caught (the test asserts
     * `authorize()` is called exactly once for a silent re-auth).
     */
    suspend fun fetchAccountEmail(accessToken: String): String =
        api.getProfile("Bearer $accessToken").emailAddress

    private suspend fun authHeader(): String {
        return when (val outcome = authManager.authorize(REQUIRED_SCOPES)) {
            is AuthorizeOutcome.Granted -> "Bearer ${outcome.accessToken}"
            is AuthorizeOutcome.ResolutionRequired ->
                throw MailAuthException("Please sign in again to continue.")
            is AuthorizeOutcome.Failed -> throw MailAuthException(outcome.message)
        }
    }

    companion object {
        // Both scopes are requested together at sign-in (docs/SETUP.md step 7: gmail.modify
        // is added once Done/Trash behavior exists, which it now does), so this should
        // resolve silently here.
        val REQUIRED_SCOPES = listOf(GmailScopes.READONLY, GmailScopes.MODIFY)
    }
}

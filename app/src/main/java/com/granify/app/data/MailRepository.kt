package com.granify.app.data

/**
 * One inbox page from [MailRepository.loadInbox].
 *
 * [isComplete] is false when the source listed messages it could not turn into summaries.
 * An empty [messages] list with [isComplete] true is a genuinely empty inbox. Callers that
 * already have a usable inbox must not replace it with an incomplete page.
 */
data class InboxLoad(val messages: List<MailSummary>, val isComplete: Boolean)

interface MailRepository {
    suspend fun loadInbox(): InboxLoad
    suspend fun loadMessage(id: String): MailMessage
    suspend fun markDone(id: String)
    suspend fun moveToTrash(id: String)
}

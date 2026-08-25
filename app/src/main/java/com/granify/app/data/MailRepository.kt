package com.granify.app.data

interface MailRepository {
    suspend fun loadInbox(): List<MailSummary>
    suspend fun loadMessage(id: String): MailMessage
    suspend fun markDone(id: String)
    suspend fun moveToTrash(id: String)
}


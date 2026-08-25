package com.granify.app.data

import kotlinx.coroutines.delay

class MockMailRepository : MailRepository {
    private val messages = listOf(
        MailMessage(
            summary = MailSummary(
                id = "statement",
                senderName = "Example Bank",
                senderAddress = "statements@examplebank.test",
                subject = "Your monthly statement is ready",
                preview = "Your statement for this month is attached.",
                receivedLabel = "Today",
                isUnread = true,
                attachmentCount = 1,
            ),
            bodyParagraphs = listOf(
                "Hello Mary,",
                "Your monthly account statement is attached to this email.",
                "For this demonstration, the sender and account details are fictional.",
            ),
            attachments = listOf(
                MailAttachment(
                    id = "statement-pdf",
                    name = "Monthly statement.pdf",
                    mimeType = "application/pdf",
                    sizeLabel = "420 KB",
                    isPasswordProtected = true,
                ),
            ),
        ),
        MailMessage(
            summary = MailSummary(
                id = "family",
                senderName = "Sarah",
                senderAddress = "sarah@example.test",
                subject = "Lunch on Sunday",
                preview = "We are looking forward to seeing you on Sunday.",
                receivedLabel = "Yesterday",
                isUnread = true,
                attachmentCount = 0,
            ),
            bodyParagraphs = listOf(
                "Hi Gran,",
                "We are looking forward to seeing you for lunch on Sunday at 12:30.",
                "Love, Sarah",
            ),
            attachments = emptyList(),
        ),
    ).associateByTo(linkedMapOf()) { it.summary.id }

    override suspend fun loadInbox(): List<MailSummary> {
        delay(250)
        return messages.values.map { it.summary }
    }

    override suspend fun loadMessage(id: String): MailMessage {
        delay(150)
        return messages.getValue(id)
    }

    override suspend fun markDone(id: String) {
        delay(100)
        val message = messages.getValue(id)
        messages[id] = message.copy(summary = message.summary.copy(isUnread = false))
    }

    override suspend fun moveToTrash(id: String) {
        delay(100)
        messages.remove(id)
    }
}

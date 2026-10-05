package com.granify.app.data

data class MailSummary(
    val id: String,
    val senderName: String,
    val senderAddress: String,
    val subject: String,
    val preview: String,
    val receivedLabel: String,
    val isUnread: Boolean,
    val attachmentCount: Int,
)

data class MailMessage(
    val summary: MailSummary,
    val bodyParagraphs: List<String>,
    /**
     * Validated https destinations only. Anchor labels live in [MailLink.messageLabels] and are
     * never opened. Empty for a message with no supported link.
     */
    val links: List<MailLink> = emptyList(),
    val attachments: List<MailAttachment>,
)

/**
 * One address the reader may open, after confirmation.
 *
 * [destination] is the only string that may be shown as the address, spoken as the address,
 * confirmed, and handed to Android. [messageLabels] is display text from the message, kept so a
 * label that differs from the address can be read, and is never parsed as a URL.
 */
data class MailLink(val destination: String, val messageLabels: List<String> = emptyList())

data class MailAttachment(
    val id: String,
    val name: String,
    val mimeType: String,
    val sizeLabel: String,
    val isPasswordProtected: Boolean = false,
)

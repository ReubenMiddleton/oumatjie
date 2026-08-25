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
    val attachments: List<MailAttachment>,
)

data class MailAttachment(
    val id: String,
    val name: String,
    val mimeType: String,
    val sizeLabel: String,
    val isPasswordProtected: Boolean = false,
)


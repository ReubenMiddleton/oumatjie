package com.granify.app.data.gmail

import com.granify.app.data.Base64Url
import com.granify.app.data.MailAttachment
import com.granify.app.data.MailMessage
import com.granify.app.data.MailSummary
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Turns a raw Gmail API [GmailMessage] into Oumatjie's own [MailSummary]/[MailMessage] models.
 * Kept as pure functions (no Android or network types) so the MIME-tree walking and date/size
 * formatting can be unit tested directly against hand-built fixtures.
 */

fun GmailMessage.toSummary(now: ZonedDateTime = ZonedDateTime.now()): MailSummary {
    val headers = payload?.headers.orEmpty()
    val (senderName, senderAddress) = parseSender(findHeader(headers, "From"))
    val attachments = mutableListOf<MailAttachment>()
    payload?.let { collectAttachments(it, attachments) }
    return MailSummary(
        id = id,
        senderName = senderName,
        senderAddress = senderAddress,
        subject = findHeader(headers, "Subject")?.takeIf { it.isNotBlank() } ?: "(No subject)",
        preview = snippet,
        receivedLabel = receivedLabel(internalDate?.toLongOrNull(), now),
        isUnread = labelIds.contains("UNREAD"),
        attachmentCount = attachments.size,
    )
}

fun GmailMessage.toMailMessage(now: ZonedDateTime = ZonedDateTime.now()): MailMessage {
    val root = payload ?: GmailMessagePart()
    val attachments = mutableListOf<MailAttachment>()
    collectAttachments(root, attachments)

    val bodyText = extractBodyText(root).ifBlank { snippet }
    val paragraphs = bodyText
        .split(Regex("\n\\s*\n"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .ifEmpty { listOf("This message has no text to show.") }

    return MailMessage(
        summary = toSummary(now),
        bodyParagraphs = paragraphs,
        attachments = attachments,
    )
}

private fun findHeader(headers: List<GmailHeader>, name: String): String? =
    headers.firstOrNull { it.name.equals(name, ignoreCase = true) }?.value

private val NAME_ADDRESS = Regex("""^\s*"?([^"<]*)"?\s*<([^>]+)>\s*$""")

internal fun parseSender(fromHeader: String?): Pair<String, String> {
    if (fromHeader.isNullOrBlank()) return "Unknown sender" to ""
    val match = NAME_ADDRESS.find(fromHeader) ?: return fromHeader.trim() to fromHeader.trim()
    val address = match.groupValues[2].trim()
    val name = match.groupValues[1].trim().ifEmpty { address }
    return name to address
}

internal fun receivedLabel(internalDateMillis: Long?, now: ZonedDateTime = ZonedDateTime.now()): String {
    if (internalDateMillis == null) return ""
    val then = Instant.ofEpochMilli(internalDateMillis).atZone(now.zone)
    val today = now.toLocalDate()
    val thenDate = then.toLocalDate()
    return when {
        thenDate.isEqual(today) -> "Today"
        thenDate.isEqual(today.minusDays(1)) -> "Yesterday"
        thenDate.isAfter(today.minusDays(6)) ->
            then.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        else -> then.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
    }
}

private fun findPart(part: GmailMessagePart, mimeType: String): GmailMessagePart? {
    if (part.mimeType == mimeType && part.body?.data != null) return part
    for (child in part.parts) {
        findPart(child, mimeType)?.let { return it }
    }
    return null
}

internal fun extractBodyText(root: GmailMessagePart): String {
    findPart(root, "text/plain")?.body?.data?.let { return Base64Url.decodeText(it).normalizeLineEndings() }
    findPart(root, "text/html")?.body?.data?.let {
        return stripHtml(Base64Url.decodeText(it)).normalizeLineEndings()
    }
    return ""
}

private fun collectAttachments(part: GmailMessagePart, into: MutableList<MailAttachment>) {
    val attachmentId = part.body?.attachmentId
    if (!part.filename.isNullOrBlank() && attachmentId != null) {
        into += MailAttachment(
            id = attachmentId,
            name = part.filename,
            mimeType = part.mimeType?.takeIf { it.isNotBlank() } ?: "application/octet-stream",
            sizeLabel = humanReadableSize(part.body.size),
            // Gmail's API has no "is this encrypted" field; the PDF viewer discovers this
            // itself (and shows its own prompt) when the document is opened.
            isPasswordProtected = false,
        )
    }
    part.parts.forEach { collectAttachments(it, into) }
}

private val HTML_ENTITIES = mapOf(
    "&amp;" to "&", "&lt;" to "<", "&gt;" to ">", "&quot;" to "\"",
    "&#39;" to "'", "&apos;" to "'", "&nbsp;" to " ",
)

internal fun stripHtml(html: String): String {
    var text = html
        .replace(Regex("(?is)<(script|style)[^>]*>.*?</\\1>"), "")
        .replace(Regex("(?is)<br\\s*/?>"), "\n")
        .replace(Regex("(?is)</(p|div|tr|li|h[1-6])>"), "\n")
        .replace(Regex("(?is)<.*?>"), "")
    for ((entity, replacement) in HTML_ENTITIES) {
        text = text.replace(entity, replacement)
    }
    return text.replace(Regex("[ \\t]+"), " ").trim()
}

private fun String.normalizeLineEndings(): String = replace("\r\n", "\n").replace("\r", "\n")

internal fun humanReadableSize(bytes: Int): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${Math.round(bytes / 1024.0)} KB"
    else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
}

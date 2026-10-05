package com.granify.app.data.gmail

import com.granify.app.data.Base64Url
import com.granify.app.data.MailAttachment
import com.granify.app.data.MailLink
import com.granify.app.data.MailMessage
import com.granify.app.data.MailSummary
import java.net.URI
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Turns a raw Gmail API [GmailMessage] into Oumatjie's own [MailSummary]/[MailMessage] models.
 * Kept as pure functions (no Android or network types) so the MIME-tree walking and date/size
 * formatting can be unit tested directly against hand-built fixtures.
 *
 * The reading body and the link list are separate. [extractBodyText] still prefers text/plain.
 * [extractMessageLinks] keeps https hrefs, and raw https text that is not an anchor label.
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
        links = extractMessageLinks(root),
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

/**
 * Https destinations for [root]. Does not read the stripped body or the Gmail snippet: stripped
 * HTML still contains anchor labels, and a label that looks like a URL must not become a second
 * destination. A text/plain part is the raw-URL source when one exists. Otherwise raw URLs come
 * only from HTML text that is outside anchors. Hrefs are collected either way, and only a
 * validated https href is kept.
 */
internal fun extractMessageLinks(root: GmailMessagePart): List<MailLink> {
    val htmlParts = mutableListOf<GmailMessagePart>()
    collectParts(root, "text/html", htmlParts)
    val fromHrefs = htmlParts.flatMap { part ->
        extractAnchorLinks(Base64Url.decodeText(part.body?.data.orEmpty()).normalizeLineEndings())
    }
    val plain = findPart(root, "text/plain")?.body?.data
    val rawText = if (plain != null) {
        Base64Url.decodeText(plain).normalizeLineEndings()
    } else {
        htmlParts.joinToString("\n") { part ->
            htmlOutsideAnchors(Base64Url.decodeText(part.body?.data.orEmpty()).normalizeLineEndings())
        }
    }
    return mergeLinks(fromHrefs, extractRawHttpsLinks(rawText))
}

/**
 * The single https string Oumatjie will show and open, or null when [raw] is not a supported
 * destination. The result round-trips, so a later check can require the same characters.
 */
internal fun authoritativeHttpsUrl(raw: String): String? {
    val canonical = canonicalHttpsOrNull(raw)
    return canonical?.takeIf { canonicalHttpsOrNull(it) == it }
}

private fun canonicalHttpsOrNull(raw: String): String? {
    val candidate = raw.trim()
    val uri = if (candidate.isSafeUrlText()) runCatching { URI(candidate) }.getOrNull() else null
    val ascii = if (uri != null && uri.isAuthoritativeHttps()) {
        runCatching { uri.toASCIIString() }.getOrNull()
    } else {
        null
    }
    return ascii?.takeIf { it.startsWith("https://") }
}

private fun String.isSafeUrlText(): Boolean =
    isNotEmpty() && none { it.isISOControl() || it.isWhitespace() || it in INVISIBLE_URL_CHARACTERS }

private fun URI.isAuthoritativeHttps(): Boolean {
    val https = scheme == "https"
    val hostPresent = !host.isNullOrBlank()
    return isAbsolute && https && rawUserInfo == null && hostPresent
}

private fun collectParts(part: GmailMessagePart, mimeType: String, into: MutableList<GmailMessagePart>) {
    if (part.mimeType == mimeType && part.body?.data != null) into += part
    part.parts.forEach { collectParts(it, mimeType, into) }
}

private fun extractAnchorLinks(html: String): List<MailLink> {
    val prepared = htmlPreparedForLinks(html)
    return ANCHOR.findAll(prepared).mapNotNull { match ->
        val href = HREF_ATTRIBUTE.find(match.groupValues[1])
            ?.groupValues
            ?.drop(1)
            ?.firstOrNull { it.isNotEmpty() }
        val destination = href?.let { authoritativeHttpsUrl(decodeEntities(it)) }
        if (destination == null) {
            null
        } else {
            val label = stripHtml(match.groupValues[2]).trim()
            val labels = if (label.isNotEmpty() && label != destination) listOf(label) else emptyList()
            MailLink(destination, labels)
        }
    }.toList()
}

/** HTML with anchors, including their labels, removed, then reduced to text for a raw-URL scan. */
private fun htmlOutsideAnchors(html: String): String {
    var text = htmlPreparedForLinks(html).replace(ANCHOR, "")
    val unclosed = UNCLOSED_ANCHOR.find(text)
    if (unclosed != null) text = text.substring(0, unclosed.range.first)
    return stripHtml(text)
}

private fun htmlPreparedForLinks(html: String): String = html
    .replace(SCRIPT_OR_STYLE, "")
    .replace(HTML_COMMENT, "")

private fun extractRawHttpsLinks(text: String): List<MailLink> {
    return RAW_HTTPS.findAll(text).mapNotNull { match ->
        val token = match.value.replace(TRAILING_URL_PUNCTUATION, "")
        val destination = authoritativeHttpsUrl(token) ?: return@mapNotNull null
        MailLink(destination)
    }.toList()
}

private fun mergeLinks(hrefs: List<MailLink>, raw: List<MailLink>): List<MailLink> {
    val labelsByDestination = linkedMapOf<String, MutableList<String>>()
    for (link in hrefs + raw) {
        val labels = labelsByDestination.getOrPut(link.destination) { mutableListOf() }
        for (label in link.messageLabels) {
            if (label !in labels) labels += label
        }
    }
    return labelsByDestination.map { (destination, labels) -> MailLink(destination, labels) }
}

private fun decodeEntities(text: String): String {
    var decoded = text
    for ((entity, replacement) in HTML_ENTITIES) {
        decoded = decoded.replace(entity, replacement)
    }
    return decoded
}

private val ANCHOR = Regex("""(?is)<a\b([^>]*)>(.*?)</a>""")
private val UNCLOSED_ANCHOR = Regex("""(?is)<a\b""")
private val HREF_ATTRIBUTE = Regex("""(?is)\bhref\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s"'=<>`]+))""")
private val SCRIPT_OR_STYLE = Regex("""(?is)<(script|style)[^>]*>.*?</\1>""")
private val HTML_COMMENT = Regex("""(?s)<!--.*?-->""")
private val RAW_HTTPS = Regex("""https://\S+""")
private val TRAILING_URL_PUNCTUATION = Regex("""[].,;:!?)}'"]+$""")

/** Bidirectional and zero-width characters that can make a shown address disagree with the host. */
private const val INVISIBLE_URL_CHARACTERS =
    "\u200B\u200C\u200D\u200E\u200F\u202A\u202B\u202C\u202D\u202E\u2060\u2066\u2067\u2068\u2069\uFEFF"

private fun String.normalizeLineEndings(): String = replace("\r\n", "\n").replace("\r", "\n")

internal fun humanReadableSize(bytes: Int): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${Math.round(bytes / 1024.0)} KB"
    else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
}

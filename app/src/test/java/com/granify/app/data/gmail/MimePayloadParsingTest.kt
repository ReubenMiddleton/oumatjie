package com.granify.app.data.gmail

import com.granify.app.data.Base64Url
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class MimePayloadParsingTest {
    private val now = ZonedDateTime.of(2026, 8, 17, 12, 0, 0, 0, ZoneOffset.UTC)

    @Test
    fun base64Url_decodesUnpaddedUrlSafeInput() {
        // "Hello, Oumatjie!" base64-encoded is "SGVsbG8sIE91bWF0amllIQ" (no '+', '/', or padding
        // needed here, so this mainly proves the round trip; the '-'/'_' swap is exercised by
        // decodeBytes always normalizing them even when absent).
        assertEquals("Hello, Oumatjie!", Base64Url.decodeText("SGVsbG8sIE91bWF0amllIQ"))
    }

    @Test
    fun base64Url_decodesInputMissingPadding() {
        // "Hi" -> "SGk=" normally; Gmail omits the trailing '='.
        assertEquals("Hi", Base64Url.decodeText("SGk"))
    }

    @Test
    fun extractBodyText_prefersPlainTextWhenPresent() {
        val plain = base64UrlOf("Hello from plain text.")
        val html = base64UrlOf("<p>Hello from HTML.</p>")
        val root = GmailMessagePart(
            mimeType = "multipart/alternative",
            parts = listOf(
                GmailMessagePart(mimeType = "text/plain", body = GmailMessagePartBody(data = plain)),
                GmailMessagePart(mimeType = "text/html", body = GmailMessagePartBody(data = html)),
            ),
        )

        assertEquals("Hello from plain text.", extractBodyText(root))
    }

    @Test
    fun extractBodyText_fallsBackToStrippedHtmlWhenNoPlainTextExists() {
        val html = base64UrlOf("<p>Hello</p><p>Second line</p>")
        val root = GmailMessagePart(mimeType = "text/html", body = GmailMessagePartBody(data = html))

        assertEquals("Hello\nSecond line", extractBodyText(root))
    }

    @Test
    fun extractBodyText_findsPlainTextNestedInsideMultipartMixedWithAnAttachment() {
        val plain = base64UrlOf("Nested body text.")
        val root = GmailMessagePart(
            mimeType = "multipart/mixed",
            parts = listOf(
                GmailMessagePart(
                    mimeType = "multipart/alternative",
                    parts = listOf(GmailMessagePart(mimeType = "text/plain", body = GmailMessagePartBody(data = plain))),
                ),
                GmailMessagePart(
                    mimeType = "application/pdf",
                    filename = "statement.pdf",
                    body = GmailMessagePartBody(attachmentId = "att-1", size = 2048),
                ),
            ),
        )

        assertEquals("Nested body text.", extractBodyText(root))
    }

    @Test
    fun toMailMessage_collectsAttachmentsFromAnyDepth() {
        val message = GmailMessage(
            id = "msg-1",
            snippet = "snippet",
            payload = GmailMessagePart(
                mimeType = "multipart/mixed",
                headers = listOf(GmailHeader("Subject", "Statement")),
                parts = listOf(
                    GmailMessagePart(mimeType = "text/plain", body = GmailMessagePartBody(data = base64UrlOf("Hi"))),
                    GmailMessagePart(
                        filename = "statement.pdf",
                        mimeType = "application/pdf",
                        body = GmailMessagePartBody(attachmentId = "att-1", size = 430_000),
                    ),
                ),
            ),
        )

        val result = message.toMailMessage(now)

        assertEquals(1, result.attachments.size)
        assertEquals("att-1", result.attachments.single().id)
        assertEquals("statement.pdf", result.attachments.single().name)
        assertEquals("420 KB", result.attachments.single().sizeLabel)
    }

    @Test
    fun parseSender_splitsDisplayNameAndAddress() {
        assertEquals("Sarah" to "sarah@example.test", parseSender("Sarah <sarah@example.test>"))
    }

    @Test
    fun parseSender_handlesQuotedDisplayNames() {
        assertEquals("Bank Support" to "support@bank.test", parseSender("\"Bank Support\" <support@bank.test>"))
    }

    @Test
    fun parseSender_fallsBackToTheAddressWhenThereIsNoDisplayName() {
        assertEquals("someone@example.test" to "someone@example.test", parseSender("someone@example.test"))
    }

    @Test
    fun receivedLabel_recognizesTodayAndYesterday() {
        val today = now.toInstant().toEpochMilli()
        val yesterday = now.minusDays(1).toInstant().toEpochMilli()

        assertEquals("Today", receivedLabel(today, now))
        assertEquals("Yesterday", receivedLabel(yesterday, now))
    }

    @Test
    fun receivedLabel_usesAShortDateForOlderMessages() {
        // Computed the same way as the production code rather than a hardcoded literal, so
        // this does not depend on the test JVM's default locale.
        val monthAgo = now.minusDays(40)
        val expected = monthAgo.format(java.time.format.DateTimeFormatter.ofPattern("MMM d", java.util.Locale.getDefault()))

        assertEquals(expected, receivedLabel(monthAgo.toInstant().toEpochMilli(), now))
    }

    @Test
    fun humanReadableSize_formatsBytesKilobytesAndMegabytes() {
        assertEquals("512 B", humanReadableSize(512))
        assertEquals("420 KB", humanReadableSize(430_000))
        assertEquals("2.5 MB", humanReadableSize(2_621_440))
    }

    private fun base64UrlOf(text: String): String =
        java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(text.toByteArray(Charsets.UTF_8))
}

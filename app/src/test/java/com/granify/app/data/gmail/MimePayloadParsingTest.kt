package com.granify.app.data.gmail

import com.granify.app.data.Base64Url
import com.granify.app.data.MailMessage
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        assertEquals("application/pdf", result.attachments.single().mimeType)
        assertEquals(emptyList<String>(), result.links.map { it.destination })
    }

    @Test
    fun toMailMessage_preservesListedAttachmentMimeTypes() {
        val message = messageWithAttachments(
            attachment(id = "pdf", filename = "statement.pdf", mimeType = "Application/PDF; charset=binary"),
            attachment(id = "jpg", filename = "photo.jpg", mimeType = "image/jpeg"),
            attachment(id = "png", filename = "photo.png", mimeType = "image/png"),
            attachment(id = "bin", filename = "mystery.bin", mimeType = null),
            attachment(id = "blank", filename = "blank.bin", mimeType = "  "),
        )

        assertEquals(
            listOf(
                "pdf" to "Application/PDF; charset=binary",
                "jpg" to "image/jpeg",
                "png" to "image/png",
                "bin" to "application/octet-stream",
                "blank" to "application/octet-stream",
            ),
            message.attachments.map { it.id to it.mimeType },
        )
        assertEquals(5, message.summary.attachmentCount)
    }

    @Test
    fun toMailMessage_doesNotListPartsThatAreNotNamedAttachments() {
        val message = messageWithAttachments(
            GmailMessagePart(
                mimeType = "image/jpeg",
                filename = "",
                headers = listOf(GmailHeader("Content-Disposition", "inline")),
                body = GmailMessagePartBody(attachmentId = "inline-img", size = 500),
            ),
            GmailMessagePart(
                filename = "ghost.pdf",
                mimeType = "application/pdf",
                body = GmailMessagePartBody(size = 10),
            ),
            attachment(id = "pdf", filename = "statement.pdf", mimeType = "application/pdf"),
        )

        assertEquals(listOf("pdf"), message.attachments.map { it.id })
        assertEquals(listOf("application/pdf"), message.attachments.map { it.mimeType })
        assertEquals(1, message.summary.attachmentCount)
    }

    @Test
    fun toMailMessage_ordinaryAnchorLabel_keepsTheHrefAndTheReadingBody() {
        val message = messageFrom(
            html = """<p>Please <a href="https://phish.example/login">reset your password</a> today.</p>""",
        )

        assertEquals(listOf("Please reset your password today."), message.bodyParagraphs)
        assertEquals(listOf("https://phish.example/login"), message.links.map { it.destination })
        assertEquals(listOf("reset your password"), message.links.single().messageLabels)
    }

    @Test
    fun toMailMessage_urlLookingAnchorLabel_isNotASecondDestination() {
        val message = messageFrom(
            html = """<a href="https://phish.example/login">https://bank.example</a>""",
        )

        assertEquals(listOf("https://bank.example"), message.bodyParagraphs)
        assertEquals(listOf("https://phish.example/login"), message.links.map { it.destination })
        assertEquals(listOf("https://bank.example"), message.links.single().messageLabels)
    }

    @Test
    fun toMailMessage_rawHttpsOutsideAnAnchor_staysActionableBesideAMismatchedLabel() {
        val message = messageFrom(
            html = """
                <p>See https://real.example/info for details.</p>
                <a href="https://phish.example/login">https://bank.example</a>
            """.trimIndent(),
        )

        assertEquals(
            listOf("https://phish.example/login", "https://real.example/info"),
            message.links.map { it.destination },
        )
        assertEquals(listOf("https://bank.example"), message.links.first().messageLabels)
        assertEquals(emptyList<String>(), message.links.last().messageLabels)
    }

    @Test
    fun toMailMessage_javascriptHref_doesNotMakeAnHttpsLabelActionable() {
        val message = messageFrom(
            html = """<a href="javascript:alert(1)">https://bank.example</a>""",
        )

        assertEquals(listOf("https://bank.example"), message.bodyParagraphs)
        assertTrue(message.links.isEmpty())
    }

    @Test
    fun toMailMessage_rawHttpsInPlainText_isADestinationAndTheBodyIsUnchanged() {
        val plain = "Visit https://example.com/path today."
        val message = messageFrom(plain = plain, html = "<p>HTML only https://ignored.example/no</p>")

        assertEquals(listOf(plain), message.bodyParagraphs)
        assertEquals(listOf("https://example.com/path"), message.links.map { it.destination })
        assertEquals(emptyList<String>(), message.links.single().messageLabels)
    }

    @Test
    fun toMailMessage_htmlOnlyRawHttpsOutsideAnchors_isADestination() {
        val message = messageFrom(html = "<p>Your receipt is at https://receipts.example/r/1 today.</p>")

        assertEquals(listOf("Your receipt is at https://receipts.example/r/1 today."), message.bodyParagraphs)
        assertEquals(listOf("https://receipts.example/r/1"), message.links.map { it.destination })
        assertEquals(emptyList<String>(), message.links.single().messageLabels)
    }

    @Test
    fun toMailMessage_multipartPlainBody_keepsHtmlHrefAndDropsUrlLookingLabel() {
        val message = messageFrom(
            plain = "Hello from plain text.",
            html = """<a href="https://phish.example/login">https://bank.example</a>""",
        )

        assertEquals(listOf("Hello from plain text."), message.bodyParagraphs)
        assertEquals(listOf("https://phish.example/login"), message.links.map { it.destination })
        assertEquals(listOf("https://bank.example"), message.links.single().messageLabels)
    }

    @Test
    fun toMailMessage_plainTextOutsideAnchorIsNotScannedWhenPlainPartExists() {
        val message = messageFrom(
            plain = "Hello from plain text.",
            html = "<p>https://outside.example/x</p>",
        )

        assertEquals(listOf("Hello from plain text."), message.bodyParagraphs)
        assertTrue(message.links.isEmpty())
    }

    @Test
    fun toMailMessage_sameDestinationInPlainAndHtml_isOneLinkAndKeepsADifferingLabel() {
        val message = messageFrom(
            plain = "Go to https://example.com/same",
            html = """<a href="https://example.com/same">Open the site</a>""",
        )

        assertEquals(listOf("https://example.com/same"), message.links.map { it.destination })
        assertEquals(listOf("Open the site"), message.links.single().messageLabels)
    }

    @Test
    fun toMailMessage_httpMailtoAndTel_areNotDestinations() {
        val message = messageFrom(
            plain = "Visit http://example.com/insecure",
            html = """
                <a href="http://example.com">web</a>
                <a href="mailto:a@b.test">mail</a>
                <a href="tel:123">call</a>
            """.trimIndent(),
        )

        assertTrue(message.bodyParagraphs.single().contains("http://example.com/insecure"))
        assertTrue(message.links.isEmpty())
    }

    @Test
    fun toMailMessage_userinfoAndBidiOverride_areRejected() {
        val bidi = "https://example.com/\u202Elogin"
        val message = messageFrom(
            html = """
                <a href="https://bank.example@evil.example/login">go</a>
                <a href="$bidi">override</a>
            """.trimIndent(),
        )

        assertTrue(message.links.isEmpty())
    }

    @Test
    fun toMailMessage_encodedAmpersandInHref_isDecodedIntoTheDestination() {
        val message = messageFrom(
            html = """<a href="https://example.com/search?a=1&amp;b=2">results</a>""",
        )

        assertEquals(listOf("https://example.com/search?a=1&b=2"), message.links.map { it.destination })
        assertEquals(
            message.links.single().destination,
            authoritativeHttpsUrl(message.links.single().destination),
        )
    }

    @Test
    fun toMailMessage_snippetIsNotALinkSource() {
        val message = GmailMessage(
            id = "msg",
            snippet = "See https://snippet.example/x",
            payload = GmailMessagePart(mimeType = "text/plain"),
        ).toMailMessage(now)

        assertEquals(listOf("See https://snippet.example/x"), message.bodyParagraphs)
        assertTrue(message.links.isEmpty())
    }

    @Test
    fun toMailMessage_unclosedAnchor_doesNotPromoteItsLabel() {
        val message = messageFrom(
            html = """<a href="javascript:alert(1)">https://bank.example <p>https://real.example/ok</p>""",
        )

        assertTrue(message.links.none { it.destination.contains("bank.example") })
        assertTrue(message.links.isEmpty())
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

    private fun attachment(id: String, filename: String, mimeType: String?): GmailMessagePart = GmailMessagePart(
        filename = filename,
        mimeType = mimeType,
        body = GmailMessagePartBody(attachmentId = id, size = 2048),
    )

    private fun messageWithAttachments(vararg parts: GmailMessagePart): MailMessage {
        val message = GmailMessage(
            id = "msg-1",
            snippet = "snippet",
            payload = GmailMessagePart(
                mimeType = "multipart/mixed",
                headers = listOf(GmailHeader("Subject", "Statement")),
                parts = listOf(
                    GmailMessagePart(mimeType = "text/plain", body = GmailMessagePartBody(data = base64UrlOf("Hi"))),
                ) + parts,
            ),
        )
        return message.toMailMessage(now)
    }

    private fun messageFrom(plain: String? = null, html: String? = null): MailMessage {
        val parts = buildList {
            if (plain != null) {
                add(GmailMessagePart(mimeType = "text/plain", body = GmailMessagePartBody(data = base64UrlOf(plain))))
            }
            if (html != null) {
                add(GmailMessagePart(mimeType = "text/html", body = GmailMessagePartBody(data = base64UrlOf(html))))
            }
        }
        val payload = if (parts.size == 1) {
            parts.single()
        } else {
            GmailMessagePart(mimeType = "multipart/alternative", parts = parts)
        }
        return GmailMessage(id = "msg", snippet = "", payload = payload).toMailMessage(now)
    }

    private fun base64UrlOf(text: String): String =
        java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(text.toByteArray(Charsets.UTF_8))
}

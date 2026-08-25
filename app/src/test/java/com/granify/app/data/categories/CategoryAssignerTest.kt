package com.granify.app.data.categories

import com.granify.app.data.MailSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategoryAssignerTest {
    private fun summary(subject: String, preview: String = "") = MailSummary(
        id = "id",
        senderName = "Sender",
        senderAddress = "sender@example.com",
        subject = subject,
        preview = preview,
        receivedLabel = "Today",
        isUnread = true,
        attachmentCount = 0,
    )

    @Test
    fun assign_matchesBillsKeywordsInSubject() {
        assertEquals(StarterCategories.Bills, CategoryAssigner.assign(summary("Your March statement is ready")))
        assertEquals(StarterCategories.Bills, CategoryAssigner.assign(summary("Payment due March 1")))
    }

    @Test
    fun assign_matchesReceiptsKeywordsInSubjectOrPreview() {
        assertEquals(StarterCategories.Receipts, CategoryAssigner.assign(summary("Your order confirmation")))
        assertEquals(
            StarterCategories.Receipts,
            CategoryAssigner.assign(summary("Thanks for shopping with us", preview = "Here is your receipt for order #4821.")),
        )
    }

    @Test
    fun assign_matchesNewslettersKeywords() {
        assertEquals(StarterCategories.Newsletters, CategoryAssigner.assign(summary("This week's newsletter")))
        assertEquals(
            StarterCategories.Newsletters,
            CategoryAssigner.assign(summary("Community update", preview = "Click here to unsubscribe at any time.")),
        )
    }

    @Test
    fun assign_returnsNullWhenNothingMatches() {
        assertNull(CategoryAssigner.assign(summary("Hi Gran, just checking in")))
    }

    @Test
    fun assign_neverReturnsFamily_noReliableTextSignalExists() {
        // Deliberate: see CategoryAssigner's own doc comment for why Family is never
        // auto-assigned by keyword rules.
        val messages = listOf(
            summary("Hi Gran, thinking of you"),
            summary("Call me when you get a chance"),
            summary("Family dinner this weekend?"),
        )
        messages.forEach { message ->
            assertNull("expected no category for: ${message.subject}", CategoryAssigner.assign(message))
        }
    }

    @Test
    fun assign_prefersBillsWhenSubjectMatchesMultipleCategories() {
        // "statement" (Bills) appears alongside "order" (Receipts, but only as part of a
        // stricter multi-word phrase that doesn't match here) — Bills is checked first by
        // design since mischaracterizing a bill is the more consequential mistake.
        assertEquals(StarterCategories.Bills, CategoryAssigner.assign(summary("Your account statement and order history")))
    }

    @Test
    fun assignAll_mapsOnlyMatchedMessagesById() {
        val bill = summary("Your March statement is ready").copy(id = "1")
        val receipt = summary("Your order confirmation").copy(id = "2")
        val uncategorized = summary("Hi Gran, just checking in").copy(id = "3")

        val result = CategoryAssigner.assignAll(listOf(bill, receipt, uncategorized))

        assertEquals(StarterCategories.Bills, result["1"])
        assertEquals(StarterCategories.Receipts, result["2"])
        assertNull(result["3"])
        assertEquals(2, result.size)
    }
}

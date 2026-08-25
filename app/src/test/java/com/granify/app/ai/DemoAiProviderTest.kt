package com.granify.app.ai

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoAiProviderTest {
    private val provider = DemoAiProvider()

    @Test
    fun checkForScamSignals_flagsAKnownSuspiciousPhrase() = runTest {
        val result = provider.checkForScamSignals(
            subject = "Your account has been suspended",
            senderAddress = "noreply@example.test",
            bodyText = "Please verify your account immediately.",
        )

        assertTrue(result is ScamAssessment.WorthACloserLook)
        assertTrue((result as ScamAssessment.WorthACloserLook).reason.contains("demo check", ignoreCase = true))
    }

    @Test
    fun checkForScamSignals_findsNoConcernsInOrdinaryText() = runTest {
        val result = provider.checkForScamSignals(
            subject = "Lunch on Sunday",
            senderAddress = "sarah@example.test",
            bodyText = "We are looking forward to seeing you for lunch on Sunday.",
        )

        assertEquals(ScamAssessment.NoConcernsFound, result)
    }

    @Test
    fun summarize_usesTheFirstNonBlankLine() = runTest {
        val summary = provider.summarize(
            subject = "Monthly statement",
            bodyText = "\n\nYour statement is ready.\nSecond line.",
        )

        assertEquals("Demo summary: Your statement is ready.", summary)
    }

    @Test
    fun summarize_handlesBlankBodyText() = runTest {
        val summary = provider.summarize(subject = "Empty", bodyText = "   \n  ")

        assertEquals("This message does not have enough text to summarize.", summary)
    }
}

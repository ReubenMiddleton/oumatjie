package com.granify.app

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.granify.app.ui.mail.ConfirmedOpenResult
import com.granify.app.ui.mail.startConfirmedDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The demo bank message carries one https link. Cancel is the last step so this test never
 * leaves Oumatjie. The intent itself is asserted in [ConfirmedDestinationHandOffTest].
 */
@RunWith(AndroidJUnit4::class)
class MessageLinkTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val isHeading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)

    @Test
    fun openLink_showsTheDestinationAndCancelStaysOnTheMessage() {
        composeRule.onNodeWithText("Try the demo inbox").performClick()
        composeRule.waitUntil(TIMEOUT_MS) {
            composeRule.onAllNodesWithText("Your mail").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(DEMO_SUBJECT).performClick()
        composeRule.waitUntil(TIMEOUT_MS) {
            composeRule.onAllNodesWithText("Back to your mail").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Links"))
        composeRule.onNodeWithText("Links").assert(isHeading)
        composeRule.onNodeWithText(DESTINATION).assertIsDisplayed()
        composeRule.onNodeWithText("The message shows this as: View your statement").assertIsDisplayed()
        composeRule.onNodeWithText("Open link").performClick()

        composeRule.onNodeWithText("Leave this message?").assertIsDisplayed()
        composeRule.onNode(
            hasText("leave this message", substring = true, ignoreCase = true) and
                hasText(DESTINATION, substring = true),
        ).assertIsDisplayed()

        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.waitUntil(TIMEOUT_MS) {
            composeRule.onAllNodesWithText("Leave this message?").fetchSemanticsNodes().isEmpty()
        }
        // The link card is below the fold, so the back control is still on this message
        // but not in the viewport until we scroll back to it.
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Back to your mail"))
        composeRule.onNodeWithText("Back to your mail").assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(DESTINATION))
        composeRule.onNodeWithText(DESTINATION).assertIsDisplayed()
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L
        const val DEMO_SUBJECT = "Your monthly statement is ready"
        const val DESTINATION = "https://example.com/statement"
    }
}

/** Runs on a device because [android.net.Uri] is not available to JVM unit tests. */
@RunWith(AndroidJUnit4::class)
class ConfirmedDestinationHandOffTest {

    @Test
    fun startConfirmedDestination_putsTheExactDestinationOnTheViewIntent() {
        val destination = "https://example.com/statement"
        var recorded: Intent? = null

        val result = startConfirmedDestination(destination) { intent -> recorded = intent }

        assertEquals(ConfirmedOpenResult.Opened, result)
        val intent = checkNotNull(recorded)
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(destination, intent.dataString)
        assertTrue(intent.hasCategory(Intent.CATEGORY_BROWSABLE))
    }

    @Test
    fun startConfirmedDestination_reportsNoHandlerWithoutReplacingTheDestination() {
        val destination = "https://example.com/statement"
        var recorded: Intent? = null

        val result = startConfirmedDestination(destination) { intent ->
            recorded = intent
            throw ActivityNotFoundException()
        }

        assertEquals(ConfirmedOpenResult.NoHandler, result)
        assertEquals(destination, recorded?.dataString)
    }

    @Test
    fun startConfirmedDestination_doesNotStartRejectedAddresses() {
        var calls = 0
        val startActivity: (Intent) -> Unit = { calls += 1 }

        assertEquals(ConfirmedOpenResult.NotHttps, startConfirmedDestination("http://example.com", startActivity))
        assertEquals(
            ConfirmedOpenResult.NotHttps,
            startConfirmedDestination("https://bank.example@evil.example/login", startActivity),
        )
        assertEquals(0, calls)
    }
}

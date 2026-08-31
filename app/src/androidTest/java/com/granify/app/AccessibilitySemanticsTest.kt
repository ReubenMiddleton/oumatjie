package com.granify.app

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Locks down the accessibility guarantees this project has committed to but could never verify.
 *
 * Why this exists: the 2026-08-25 static audit added heading semantics and an explicit "Unread"
 * text label, and a later session confirmed on an emulator that TalkBack reads this app — but
 * *not* that headings are actually exposed as headings. That could not be checked from outside
 * the app: `uiautomator dump` does not expose `isHeading`, and release TalkBack does not log the
 * text it speaks. See docs/DECISIONS.md, "First emulator run since 2026-08-17".
 *
 * These assertions run against the real `MainActivity` and the real demo inbox rather than
 * composables in isolation, deliberately — `InboxScreen`/`MessageScreen` are file-private, and
 * asserting on what a user actually reaches is a stronger guarantee than asserting on a
 * composable invoked with hand-made arguments.
 *
 * This does not replace a human listening to TalkBack. It replaces *guessing* that the semantics
 * are right.
 */
@RunWith(AndroidJUnit4::class)
class AccessibilitySemanticsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    // `Modifier.semantics { heading() }` sets exactly this key. If the modifier is dropped from a
    // screen, the matching assertion below fails.
    private val isHeading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)

    // ---- Sign-in screen -----------------------------------------------------------------

    @Test
    fun signInScreen_appTitleIsAHeading() {
        // Regression test for a real gap: this title had no heading semantics, so a TalkBack user
        // navigating by heading skipped the page title entirely and landed on "Just exploring?".
        composeRule.onNodeWithText("Oumatjie").assertIsDisplayed().assert(isHeading)
    }

    @Test
    fun signInScreen_demoSectionIsAHeading() {
        composeRule.onNodeWithText("Just exploring?").assertIsDisplayed().assert(isHeading)
    }

    @Test
    fun signInScreen_bodyTextIsNotMarkedAsAHeading() {
        // Guards the other direction: marking everything a heading is as useless as marking
        // nothing, because "next heading" stops being a shortcut.
        composeRule.onNodeWithText("A simple, safe way to read your Gmail.")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Heading))
    }

    // ---- Inbox --------------------------------------------------------------------------

    @Test
    fun inbox_titleIsAHeading() {
        openDemoInbox()
        composeRule.onNodeWithText("Your mail").assertIsDisplayed().assert(isHeading)
    }

    @Test
    fun inbox_unreadIsSignalledByTextNotColourAlone() {
        // The WCAG 1.4.1 "Use of Color" fix from the 2026-08-25 static audit. Unread mail used to
        // be distinguishable only by card background colour, which TalkBack cannot convey at all.
        openDemoInbox()
        composeRule.onAllNodesWithText("Unread").onFirst().assertIsDisplayed()
    }

    // Deliberately NOT tested here: the "New sender" first-contact label. It looks like it belongs
    // beside the "Unread" assertion above, and an earlier draft of this file asserted it — but it
    // is not a semantics guarantee, it is persisted product state, and the assertion is
    // unstable by construction. `DataStoreKnownSendersRepository` writes every sender it has shown
    // to disk, so `isFirstContact` returns false forever afterwards: the label is present on a
    // freshly-installed app and absent on every subsequent run, including the second CI run on a
    // reused emulator. Testing it properly needs a test that clears app data first (or injects a
    // fake KnownSendersRepository), which is a different kind of test from this file's.
    // "Unread" above is safe by contrast: MockMailRepository holds that state in memory only.

    // ---- Message screen -----------------------------------------------------------------

    @Test
    fun message_subjectIsAHeading() {
        openDemoInbox()
        composeRule.onNodeWithText(DEMO_SUBJECT).performClick()
        composeRule.waitUntil(TIMEOUT_MS) {
            composeRule.onAllNodesWithText(BACK_TO_MAIL).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(DEMO_SUBJECT).assert(isHeading)
    }

    // ---- Settings -----------------------------------------------------------------------

    @Test
    fun settings_titleAndSectionHeadingsAreHeadings() {
        openDemoInbox()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(SETTINGS))
        composeRule.onNodeWithText(SETTINGS).performClick()
        composeRule.waitUntil(TIMEOUT_MS) {
            composeRule.onAllNodesWithText(TEXT_SIZE).fetchSemanticsNodes().isNotEmpty()
        }
        // The screen title, and at least one section heading — section headings are what make
        // "next heading" useful on the longest screen in the app.
        composeRule.onAllNodesWithText(SETTINGS).onFirst().assert(isHeading)
        composeRule.onNodeWithText(TEXT_SIZE).assert(isHeading)
    }

    // ---- Helpers ------------------------------------------------------------------------

    /** Enters the offline demo inbox, which needs no credentials or network. */
    private fun openDemoInbox() {
        composeRule.onNodeWithText("Try the demo inbox").performClick()
        composeRule.waitUntil(TIMEOUT_MS) {
            composeRule.onAllNodesWithText("Your mail").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L

        // Fixtures from data/MockMailRepository. If the demo inbox copy changes, these change.
        const val DEMO_SUBJECT = "Your monthly statement is ready"
        const val BACK_TO_MAIL = "Back to your mail"
        const val SETTINGS = "Settings"
        const val TEXT_SIZE = "Text size"
    }
}

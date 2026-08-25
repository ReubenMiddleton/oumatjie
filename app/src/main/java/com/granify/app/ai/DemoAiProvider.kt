package com.granify.app.ai

import kotlinx.coroutines.delay

/**
 * A canned, fully offline [AiProvider] — mirrors [com.granify.app.data.MockMailRepository]'s
 * role for mail: development, screenshots, and unit tests can exercise every AI-dependent
 * screen and flow without spending a real API call or requiring a configured key
 * (docs/NEEDS_YOUR_INPUT.md, "AI provider account and API key").
 *
 * [summarize] is used as a real, user-facing fallback when AI features are turned on but no
 * provider key has been configured yet — its output is honestly labelled as a demo in the UI
 * (see ui/mail/MailScreens.kt), never presented as if it came from a real model.
 *
 * [checkForScamSignals] is deliberately **not** wired into the live, automatic scam-check
 * trigger regardless of whether a key is configured (see ui/mail/MailViewModel.kt) — this is a
 * simple keyword heuristic, not a real safety check, and silently standing in for one on a
 * message that actually is a scam would be actively harmful for exactly the audience this app
 * is built for. It still fully implements the interface so it can be used in tests and
 * previews. See docs/DECISIONS.md for the full reasoning.
 */
class DemoAiProvider : AiProvider {
    override suspend fun checkForScamSignals(
        subject: String,
        senderAddress: String,
        bodyText: String,
    ): ScamAssessment {
        delay(DEMO_DELAY_MILLIS) // mimics a network round trip so loading states get exercised too
        val haystack = "$subject $bodyText".lowercase()
        val matched = SUSPICIOUS_PHRASES.firstOrNull { haystack.contains(it) }
        return if (matched != null) {
            ScamAssessment.WorthACloserLook(
                "This is a demo check, not a real one. It noticed the phrase \"$matched\", which " +
                    "sometimes appears in scam messages. Add an AI provider key in Settings to turn " +
                    "on a real check.",
            )
        } else {
            ScamAssessment.NoConcernsFound
        }
    }

    override suspend fun summarize(subject: String, bodyText: String): String {
        delay(DEMO_DELAY_MILLIS)
        val firstSentence = bodyText.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
        return if (firstSentence.isNotEmpty()) {
            "Demo summary: $firstSentence"
        } else {
            "This message does not have enough text to summarize."
        }
    }

    private companion object {
        const val DEMO_DELAY_MILLIS = 400L
        val SUSPICIOUS_PHRASES = listOf(
            "verify your account", "confirm your password", "account has been suspended",
            "click here immediately", "act now", "wire transfer", "gift card",
            "urgent action required", "your account will be closed", "unusual activity",
        )
    }
}

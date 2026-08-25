package com.granify.app.ai

/**
 * A single provider-agnostic boundary for every AI-assisted feature (docs/AI_ASSISTANT.md,
 * "Provider abstraction") — mirrors the existing [com.granify.app.auth.AuthManager] /
 * [com.granify.app.auth.GoogleAuthManager] split for the same reason: the concrete provider
 * stays swappable and testable, and a demo implementation ([DemoAiProvider]) can exist from
 * day one.
 *
 * Every method takes only the text of the one message the user is already looking at — never
 * the inbox as a whole, never other messages (docs/AI_ASSISTANT.md, "Trigger model"). Calendar
 * awareness and the chat panel (items 5 and 8 in docs/AI_ASSISTANT.md's feature order) are
 * intentionally not part of this interface yet — see docs/DECISIONS.md for why this session
 * scoped down to the two highest-priority AI features (scam warnings and summarization) rather
 * than building all of it at once.
 */
interface AiProvider {
    /**
     * Never returns a raw model response — always a verdict plus, when there's something worth
     * a closer look, a short, calm, plain-language reason (docs/PRODUCT_PRINCIPLES.md: "calm,
     * factual warnings").
     */
    suspend fun checkForScamSignals(subject: String, senderAddress: String, bodyText: String): ScamAssessment

    suspend fun summarize(subject: String, bodyText: String): String
}

sealed interface ScamAssessment {
    /** Nothing notable found. Rendered as no banner at all — a calm absence, not a green
     * "you're safe" badge, since a clean result is not a guarantee. */
    data object NoConcernsFound : ScamAssessment

    /** A calm, plain-language reason to take a moment before acting on this message. */
    data class WorthACloserLook(val reason: String) : ScamAssessment

    /** The check itself could not complete (network error, provider outage, and so on). */
    data class CheckFailed(val message: String) : ScamAssessment
}

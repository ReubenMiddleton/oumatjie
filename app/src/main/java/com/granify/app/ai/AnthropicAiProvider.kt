package com.granify.app.ai

import kotlinx.coroutines.CancellationException

/**
 * Calls Anthropic's Messages API (Claude Haiku 4.5) directly — the recommended default per
 * docs/AI_ASSISTANT.md's "Provider recommendation": no free tier, but cheap enough at
 * single-user scale that cost isn't a real constraint, and Anthropic's standard API terms do
 * not train on customer prompts by default (unlike Gemini's free tier).
 *
 * [apiKey] is always a value the user typed into Settings — see AppContainer and
 * data/settings/SettingsRepository. Never bundled with the app, never logged.
 */
class AnthropicAiProvider(
    private val api: AnthropicApiService,
    private val apiKey: String,
) : AiProvider {
    override suspend fun checkForScamSignals(
        subject: String,
        senderAddress: String,
        bodyText: String,
    ): ScamAssessment = try {
        val reply = createMessage(
            system = SCAM_SYSTEM_PROMPT,
            maxTokens = 300,
            userText = "Sender: $senderAddress\nSubject: $subject\n\n$bodyText",
        )
        parseScamReply(reply)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ScamAssessment.CheckFailed("We could not check this message right now. Please try again later.")
    }

    override suspend fun summarize(subject: String, bodyText: String): String = try {
        createMessage(
            system = SUMMARY_SYSTEM_PROMPT,
            maxTokens = 200,
            userText = "Subject: $subject\n\n$bodyText",
        ).ifBlank { "This message does not have enough text to summarize." }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        "We could not summarize this message right now. Please try again later."
    }

    private suspend fun createMessage(system: String, maxTokens: Int, userText: String): String {
        val response = api.createMessage(
            apiKey = apiKey,
            request = AnthropicRequest(
                model = MODEL,
                maxTokens = maxTokens,
                system = system,
                messages = listOf(AnthropicMessage(role = "user", content = userText)),
            ),
        )
        return response.content.firstOrNull { it.type == "text" }?.text?.trim().orEmpty()
    }

    /**
     * The model is instructed (see [SCAM_SYSTEM_PROMPT]) to reply with exactly one of two
     * shapes. Any reply that doesn't match either is treated as [ScamAssessment.WorthACloserLook]
     * rather than silently discarded — a false-positive "take a moment" costs the user a few
     * seconds; a false-negative "all clear" from an unparsed reply could cost far more.
     */
    private fun parseScamReply(raw: String): ScamAssessment {
        val trimmed = raw.trim()
        return when {
            trimmed.isEmpty() -> ScamAssessment.CheckFailed("We could not check this message right now.")
            trimmed.startsWith("SAFE", ignoreCase = true) -> ScamAssessment.NoConcernsFound
            trimmed.startsWith("CONCERN", ignoreCase = true) -> {
                val reason = trimmed.substringAfter(":", missingDelimiterValue = trimmed).trim()
                ScamAssessment.WorthACloserLook(reason.ifBlank { trimmed })
            }
            else -> ScamAssessment.WorthACloserLook(trimmed)
        }
    }

    private companion object {
        // An alias, not a dated snapshot, so this keeps resolving to a current Haiku 4.5
        // build as Anthropic ages out old snapshots, without needing an app update — see
        // docs/DECISIONS.md.
        const val MODEL = "claude-haiku-4-5"

        val SCAM_SYSTEM_PROMPT = """
            You are helping an older adult who may find email confusing decide whether a
            message deserves a closer look before they act on it. You will be shown the
            sender address, subject, and text of one email. Reply with exactly one of these
            two shapes and nothing else, no other text:

            SAFE

            or

            CONCERN: <one or two short, calm, plain-English sentences explaining specifically
            what about this message is worth a closer look>

            Only reply CONCERN for real phishing or scam signals — urgency or pressure to act
            fast, requests for a password, PIN, or payment, a sender address that does not
            match who the message claims to be from, or a suspicious link. Do not reply
            CONCERN just because a message is commercial, unfamiliar, or poorly written. Never
            use alarming language, all caps, or exclamation points; keep the tone calm and
            factual either way.
        """.trimIndent()

        val SUMMARY_SYSTEM_PROMPT = """
            Summarize the following email for an older adult who prefers short, plain
            language and no jargon. Reply with two or three short sentences: what the message
            is about, and what action (if any) it asks the reader to take. Do not add
            information that is not in the message, and do not include a greeting or preamble
            — reply with the summary itself only.
        """.trimIndent()
    }
}

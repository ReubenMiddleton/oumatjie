package com.granify.app.data.categories

import com.granify.app.data.MailSummary

/**
 * Tier 1 (no AI) category assignment — docs/AI_ASSISTANT.md, "Categorization design". Simple,
 * explainable local rules over text already present in a [MailSummary] (subject and preview;
 * no extra fetch needed, so this can run for the whole inbox list cheaply). Returns `null`
 * rather than a forced guess when nothing matches — an unconfident category is worse than an
 * honest "not categorized," especially for the audience this app is built for
 * (docs/PRODUCT_PRINCIPLES.md). Never called with message body text the user hasn't already
 * loaded (the inbox list only ever has the summary), matching this app's general "don't fetch
 * more than the screen needs" pattern.
 *
 * Deliberately does not attempt [StarterCategories.Family]: there is no text signal in a
 * message that reliably indicates "this sender is a family member," unlike Bills/Receipts/
 * Newsletters, which have real, common, checkable vocabulary. Guessing via something like "the
 * sender's name matches a common first name" would be unreliable and occasionally embarrassing
 * rather than genuinely useful — the honest path to populating Family is Tier 2 (AI-assisted,
 * not yet built) or a future manual "assign to category" action, not a fabricated keyword rule.
 */
object CategoryAssigner {
    private val billsKeywords = listOf(
        "bill", "billing", "statement", "invoice", "payment due", "amount due", "balance due",
        "autopay",
    )
    private val receiptsKeywords = listOf(
        "receipt", "order confirmation", "your order", "purchase confirmation", "order #",
        "shipping confirmation", "your order has shipped",
    )
    private val newslettersKeywords = listOf(
        "newsletter", "digest", "weekly update", "unsubscribe", "view in browser",
    )

    /**
     * Returns the single best-matching category for [message], or `null` if none of the rules
     * matched. Checked in a fixed order (Bills, then Receipts, then Newsletters) so a message
     * that happens to match more than one keyword list still gets exactly one category rather
     * than an ambiguous result — Bills is checked first since a mischaracterized bill (e.g. as
     * a receipt) is the more consequential mistake for this audience.
     */
    fun assign(message: MailSummary): MailCategory? {
        val haystack = "${message.subject} ${message.preview}".lowercase()
        return when {
            billsKeywords.any { haystack.contains(it) } -> StarterCategories.Bills
            receiptsKeywords.any { haystack.contains(it) } -> StarterCategories.Receipts
            newslettersKeywords.any { haystack.contains(it) } -> StarterCategories.Newsletters
            else -> null
        }
    }

    /** Convenience for computing categories for a whole inbox load at once — see
     * `ui/mail/MailViewModel.kt`'s `loadInbox()`, which mirrors the same shape as first-contact
     * flagging's `firstContactMessageIds`. */
    fun assignAll(messages: List<MailSummary>): Map<String, MailCategory> =
        messages.mapNotNull { message -> assign(message)?.let { message.id to it } }.toMap()
}

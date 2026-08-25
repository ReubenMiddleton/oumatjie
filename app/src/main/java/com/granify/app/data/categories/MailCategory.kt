package com.granify.app.data.categories

/**
 * A single mail category (docs/AI_ASSISTANT.md, "Categorization design", Tier 1). [id] is a
 * stable identity that never changes and is never shown to the user; [label] is what's actually
 * displayed. Splitting these apart now — even though nothing yet lets the user edit [label] —
 * is what makes a future "rename this category" screen a UI-only addition later rather than a
 * data-model change; see docs/DECISIONS.md for why renaming/merging itself isn't built yet.
 */
data class MailCategory(
    val id: String,
    val label: String,
)

/**
 * The fixed starter set every install ships with. Not user-editable yet — see
 * docs/DECISIONS.md's Tier 1 categorization entry for what "yet" means here and why.
 */
object StarterCategories {
    val Bills = MailCategory(id = "bills", label = "Bills")
    val Receipts = MailCategory(id = "receipts", label = "Receipts")
    val Newsletters = MailCategory(id = "newsletters", label = "Newsletters")
    val Family = MailCategory(id = "family", label = "Family")

    /** All starter categories, in the order they should be presented anywhere they're listed
     * together (e.g. a future manage-categories screen). */
    val all: List<MailCategory> = listOf(Bills, Receipts, Newsletters, Family)
}

package com.granify.app.data.senders

/**
 * A local, on-device record of sender addresses Oumatjie has ever shown the user, so a message
 * screen can tell them "you haven't received mail from this address before" — modeled on
 * Microsoft Exchange's "First Contact Safety Tip" (see docs/ROADMAP.md's research findings).
 *
 * Deliberately not AI, not a network call, and not scoped to the current inbox fetch alone —
 * addresses are remembered across app restarts and beyond whatever page of the inbox is
 * currently loaded, which is what makes "first contact" mean *first ever*, not merely
 * "haven't seen it in the last 25 messages" (see docs/DECISIONS.md for the full reasoning).
 * Works identically for the demo inbox and a real Gmail account, since it only ever looks at
 * [com.granify.app.data.MailSummary.senderAddress], never at which repository produced it.
 */
interface KnownSendersRepository {
    /** True if [address] has never been recorded before. Does not itself record it. */
    suspend fun isFirstContact(address: String): Boolean

    /** Records every address in [addresses] as now known, for future [isFirstContact] checks. */
    suspend fun recordSeen(addresses: Collection<String>)
}

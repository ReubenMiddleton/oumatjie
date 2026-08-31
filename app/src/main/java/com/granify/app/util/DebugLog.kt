package com.granify.app.util

import android.util.Log
import com.granify.app.BuildConfig

/**
 * Debug-build-only diagnostic logging for failures the app deliberately swallows.
 *
 * This app has several places where an exception is caught and turned into a calm, user-facing
 * fallback rather than an error screen — a failed AI scam check, a failed summary, a single Gmail
 * message that could not be fetched. Those are deliberate product decisions (see
 * `docs/DECISIONS.md`) and must not start throwing. But discarding the cause entirely made them
 * undiagnosable: if the first real Gmail run returns an empty inbox, or the scam check always
 * reports "we could not check this message", there was previously nothing anywhere to say why.
 *
 * Two deliberate constraints:
 *
 * 1. **Debug builds only.** Guarded on [BuildConfig.DEBUG], so release builds carry no logging at
 *    all. Nothing an end user runs writes any of this to logcat.
 * 2. **No message content, ever.** Callers pass a short static description of *what* failed, never
 *    subject lines, sender addresses, or body text. Oumatjie's privacy rules
 *    (`docs/PRODUCT_PRINCIPLES.md`) keep email processing on the device; this keeps the failure
 *    trail on the device too, and content out of it entirely. Note that a `Throwable` from Retrofit
 *    or OkHttp can still carry the request URL, which for Gmail includes a message id — that is an
 *    identifier, not content, and it stays in debug builds only.
 */
internal fun logSwallowed(tag: String, what: String, cause: Throwable) {
    if (BuildConfig.DEBUG) {
        Log.w(tag, "Swallowed a failure: $what", cause)
    }
}

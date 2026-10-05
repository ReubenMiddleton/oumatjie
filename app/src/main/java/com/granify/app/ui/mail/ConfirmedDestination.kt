package com.granify.app.ui.mail

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import com.granify.app.data.gmail.authoritativeHttpsUrl

internal fun leaveMessageExplanation(destination: String): String =
    "Oumatjie will leave this message and open this address:\n\n$destination"

internal const val LINK_COULD_NOT_BE_OPENED = "This link could not be opened on this device."

internal sealed interface ConfirmedOpenResult {
    data object Opened : ConfirmedOpenResult
    data object NotHttps : ConfirmedOpenResult
    data object NoHandler : ConfirmedOpenResult
}

/**
 * Hands [destination] to [startActivity] only when it is still the canonical https address.
 * The intent data is that same string. A missing handler does not substitute another address.
 */
internal fun startConfirmedDestination(destination: String, startActivity: (Intent) -> Unit): ConfirmedOpenResult {
    val uri = Uri.parse(destination)
    val accepted = authoritativeHttpsUrl(destination) == destination && uri.scheme == "https"
    return if (!accepted) {
        ConfirmedOpenResult.NotHttps
    } else {
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        try {
            startActivity(intent)
            ConfirmedOpenResult.Opened
        } catch (_: ActivityNotFoundException) {
            ConfirmedOpenResult.NoHandler
        }
    }
}

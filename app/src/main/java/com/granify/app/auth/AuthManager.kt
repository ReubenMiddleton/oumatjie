package com.granify.app.auth

import android.app.PendingIntent
import android.content.Intent

/**
 * Requests Gmail access on the user's behalf. Implementations must never see or store the
 * user's Google password (see docs/PRODUCT_PRINCIPLES.md, "Privacy rules").
 */
interface AuthManager {
    /** Requests the given scopes. May succeed silently if they were already granted. */
    suspend fun authorize(scopes: List<String>): AuthorizeOutcome

    /** Finishes an [AuthorizeOutcome.ResolutionRequired] flow after the user responds. */
    fun resolveAuthorizationResult(data: Intent?): AuthorizeOutcome
}

sealed interface AuthorizeOutcome {
    data class Granted(val accessToken: String) : AuthorizeOutcome

    /** The user must approve access; launch [pendingIntent] and resolve the result. */
    data class ResolutionRequired(val pendingIntent: PendingIntent) : AuthorizeOutcome

    data class Failed(val message: String) : AuthorizeOutcome
}

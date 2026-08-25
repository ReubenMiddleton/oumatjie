package com.granify.app.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/**
 * Requests Gmail scopes through Google Play services' Authorization API. This identifies the
 * app by its package name and signing certificate (see docs/SETUP.md), so no client ID or
 * secret is embedded here.
 *
 * [AuthorizeOutcome] is meant to be a total description of how this can end — callers should
 * never need to catch an exception from these two methods — so failures broader than the
 * documented [ApiException] (Play services missing/outdated, etc.) are still turned into
 * [AuthorizeOutcome.Failed] rather than left to crash the caller.
 */
class GoogleAuthManager(context: Context) : AuthManager {
    private val authorizationClient = Identity.getAuthorizationClient(context.applicationContext)

    override suspend fun authorize(scopes: List<String>): AuthorizeOutcome {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(scopes.map { Scope(it) })
            .build()
        return try {
            toOutcome(authorizationClient.authorize(request).await())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AuthorizeOutcome.Failed(describe(e))
        }
    }

    override fun resolveAuthorizationResult(data: Intent?): AuthorizeOutcome {
        return try {
            toOutcome(authorizationClient.getAuthorizationResultFromIntent(data))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AuthorizeOutcome.Failed(describe(e))
        }
    }

    private fun toOutcome(result: AuthorizationResult): AuthorizeOutcome {
        val pendingIntent = result.pendingIntent
        if (result.hasResolution() && pendingIntent != null) {
            return AuthorizeOutcome.ResolutionRequired(pendingIntent)
        }
        val token = result.accessToken
        return if (token != null) {
            AuthorizeOutcome.Granted(token)
        } else {
            AuthorizeOutcome.Failed("Google did not grant access to your mail.")
        }
    }

    // Never surfaces e.message: ApiException's messages are things like
    // "16: [16] Cancelled by user." — accurate for a developer, not for this audience.
    private fun describe(e: Exception): String {
        if (e is ApiException && e.statusCode == CommonStatusCodes.CANCELED) {
            return "You closed the Google sign-in screen before finishing. " +
                "Tap Continue with Google whenever you're ready to try again."
        }
        return "Please try again, or use the demo inbox below."
    }
}

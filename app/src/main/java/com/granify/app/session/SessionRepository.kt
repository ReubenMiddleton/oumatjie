package com.granify.app.session

/**
 * Remembers only *that* the user previously completed Google sign-in — never a token, never
 * an email address, just a boolean. [com.granify.app.auth.GoogleAuthManager.authorize] already
 * silently re-grants already-approved scopes with no UI, so this boolean is enough for
 * [SessionViewModel] to attempt a silent re-authorize on cold start and skip the sign-in
 * screen, without persisting anything sensitive.
 *
 * See docs/ROADMAP.md's retrospective, "Session persistence was deferred too readily", and
 * docs/DECISIONS.md for why this exists as an interface (a plain DataStore-backed class would
 * need a real [android.content.Context] to unit test, which this project's JVM unit tests
 * deliberately avoid — see docs/DECISIONS.md, "AttachmentDownloader returns String" for the
 * same reasoning applied elsewhere). [DataStoreSessionRepository] is the real implementation;
 * tests use a plain in-memory fake.
 */
interface SessionRepository {
    suspend fun hasSignedInBefore(): Boolean
    suspend fun recordSignedIn()
    suspend fun clear()
}

package com.granify.app.session

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.sessionDataStore by preferencesDataStore(name = "oumatjie_session")

/** DataStore-backed [SessionRepository] — see that interface for why this is split out. */
class DataStoreSessionRepository(private val context: Context) : SessionRepository {
    override suspend fun hasSignedInBefore(): Boolean =
        context.sessionDataStore.data.first()[HAS_SIGNED_IN_KEY] ?: false

    override suspend fun recordSignedIn() {
        context.sessionDataStore.edit { prefs -> prefs[HAS_SIGNED_IN_KEY] = true }
    }

    override suspend fun clear() {
        context.sessionDataStore.edit { prefs -> prefs.remove(HAS_SIGNED_IN_KEY) }
    }

    private companion object {
        val HAS_SIGNED_IN_KEY = booleanPreferencesKey("has_signed_in_before")
    }
}

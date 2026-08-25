package com.granify.app.data.senders

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.knownSendersDataStore by preferencesDataStore(name = "oumatjie_known_senders")

/** DataStore-backed [KnownSendersRepository] — see that interface for the design reasoning. */
class DataStoreKnownSendersRepository(private val context: Context) : KnownSendersRepository {
    override suspend fun isFirstContact(address: String): Boolean {
        if (address.isBlank()) return false
        val known = context.knownSendersDataStore.data.first()[KNOWN_SENDERS_KEY] ?: emptySet()
        return address.lowercase() !in known
    }

    override suspend fun recordSeen(addresses: Collection<String>) {
        val normalized = addresses.filter { it.isNotBlank() }.map { it.lowercase() }.toSet()
        if (normalized.isEmpty()) return
        context.knownSendersDataStore.edit { prefs ->
            prefs[KNOWN_SENDERS_KEY] = (prefs[KNOWN_SENDERS_KEY] ?: emptySet()) + normalized
        }
    }

    private companion object {
        val KNOWN_SENDERS_KEY = stringSetPreferencesKey("known_sender_addresses")
    }
}

package com.omaroid.tasky.core.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.omaroid.tasky.core.domain.crypto.Cryptography
import com.omaroid.tasky.core.domain.session.SessionLocalDataSource
import com.omaroid.tasky.core.domain.session.UserSession
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.security.GeneralSecurityException

class DataStoreSessionLocalDataSource(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
    private val sessionCrypto: Cryptography,
) : SessionLocalDataSource {
    override suspend fun getSession(): UserSession? {
        val raw = dataStore.data.first()[SESSION_KEY] ?: return null
        return try {
            val decrypted = sessionCrypto.decrypt(raw)
            json.decodeFromString<UserSessionDto>(decrypted).toDomain()
        } catch (_: GeneralSecurityException) {
            tryReadLegacyPlainJson(raw)
        } catch (_: IllegalArgumentException) {
            tryReadLegacyPlainJson(raw)
        }
    }

    private fun tryReadLegacyPlainJson(raw: String): UserSession? {
        return try {
            // Backward compatibility: previously persisted plain JSON.
            json.decodeFromString<UserSessionDto>(raw).toDomain()
        } catch (_: SerializationException) {
            null
        }
    }

    override suspend fun saveSession(session: UserSession) {
        val plainJson = json.encodeToString(session.toDto())
        val encrypted = sessionCrypto.encrypt(plainJson)
        dataStore.edit { preferences ->
            preferences[SESSION_KEY] = encrypted
        }
    }

    override suspend fun clearSession() {
        dataStore.edit { preferences ->
            preferences.remove(SESSION_KEY)
        }
    }

    private companion object {
        val SESSION_KEY = stringPreferencesKey("session_json")
    }
}

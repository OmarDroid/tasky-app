package com.omaroid.tasky.core.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.omaroid.tasky.core.domain.auth.Credentials
import com.omaroid.tasky.core.domain.auth.CredentialsStorage
import com.omaroid.tasky.core.domain.crypto.Cryptography
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Mirrors the session storage exactly: encrypted JSON through the Step 9 Keystore-backed
 * [Cryptography], under its OWN key in the shared DataStore file — `clearSession()` never touches it,
 * so remembered credentials survive logout (the feature's purpose).
 */
class DataStoreCredentialsStorage(
    private val dataStore: DataStore<Preferences>,
    private val crypto: Cryptography,
    private val json: Json,
) : CredentialsStorage {

    override val credentials: Flow<Credentials?> = dataStore.data.map { prefs ->
        val raw = prefs[CREDENTIALS_KEY] ?: return@map null
        try {
            json.decodeFromString<CredentialsDto>(crypto.decrypt(raw)).toDomain()
        } catch (e: CancellationException) {
            throw e // never swallow cancellation
        } catch (e: Exception) {
            null    // corrupt/unreadable ciphertext = nothing saved
        }
    }

    override suspend fun save(credentials: Credentials) {
        val encrypted = crypto.encrypt(json.encodeToString(credentials.toDto()))
        dataStore.edit { it[CREDENTIALS_KEY] = encrypted }
    }

    override suspend fun clear() {
        dataStore.edit { it.remove(CREDENTIALS_KEY) } // this key only — session untouched
    }

    private companion object {
        val CREDENTIALS_KEY = stringPreferencesKey("remembered_credentials")
    }
}

// Serialization is the DTO's job — the domain model stays annotation-free.
@Serializable
private data class CredentialsDto(val email: String, val password: String)

private fun CredentialsDto.toDomain() = Credentials(email = email, password = password)
private fun Credentials.toDto() = CredentialsDto(email = email, password = password)

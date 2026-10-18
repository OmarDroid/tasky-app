package com.omaroid.tasky.core.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.omaroid.tasky.core.domain.crypto.Cryptography
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.time.Instant
import org.junit.Test
import java.io.File
import java.security.GeneralSecurityException

class DataStoreSessionLocalDataSourceTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val sessionKey = stringPreferencesKey("session_json")

    @Test
    fun `save session stores encrypted payload and get session returns decrypted domain model`() = runTest {
        val dataStore = createDataStore(backgroundScope)
        val fakeCrypto = FakeSessionCrypto()
        val dataSource = DataStoreSessionLocalDataSource(
            dataStore = dataStore,
            json = json,
            sessionCrypto = fakeCrypto,
        )
        val session = session(expiresAtMs = 2_000L)

        dataSource.saveSession(session)

        val storedRaw = dataStore.data.first()[sessionKey]
        val expectedPlainJson = json.encodeToString(session.toDto())
        assertThat(storedRaw).isEqualTo("enc:$expectedPlainJson")

        val restored = dataSource.getSession()

        assertThat(fakeCrypto.encryptCalls).isEqualTo(1)
        assertThat(fakeCrypto.decryptCalls).isEqualTo(1)
        assertThat(restored).isEqualTo(session)
    }

    @Test
    fun `get session supports backward compatible plain json entries`() = runTest {
        val dataStore = createDataStore(backgroundScope)
        val fakeCrypto = FakeSessionCrypto(throwOnDecrypt = true)
        val dataSource = DataStoreSessionLocalDataSource(
            dataStore = dataStore,
            json = json,
            sessionCrypto = fakeCrypto,
        )
        val session = session(expiresAtMs = 2_000L)
        val plainJson = json.encodeToString(session.toDto())

        dataStore.edit { prefs ->
            prefs[sessionKey] = plainJson
        }

        val restored = dataSource.getSession()

        assertThat(fakeCrypto.decryptCalls).isEqualTo(1)
        assertThat(restored).isEqualTo(session)
    }

    @Test
    fun `get session returns null when encrypted payload and legacy payload are both invalid`() = runTest {
        val dataStore = createDataStore(backgroundScope)
        val fakeCrypto = FakeSessionCrypto(throwOnDecrypt = true)
        val dataSource = DataStoreSessionLocalDataSource(
            dataStore = dataStore,
            json = json,
            sessionCrypto = fakeCrypto,
        )

        dataStore.edit { prefs ->
            prefs[sessionKey] = "not-json"
        }

        val restored = dataSource.getSession()

        assertThat(fakeCrypto.decryptCalls).isEqualTo(1)
        assertThat(restored).isNull()
    }

    private fun createDataStore(scope: CoroutineScope): DataStore<Preferences> {
        val file = File.createTempFile("tasky-session-test", ".preferences_pb").apply {
            deleteOnExit()
        }
        return PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { file },
        )
    }

    private fun session(expiresAtMs: Long) = com.omaroid.tasky.core.domain.session.UserSession(
        userId = "user-1",
        username = "tasky-user",
        accessToken = "access-token",
        refreshToken = "refresh-token",
        expiresAt = Instant.fromEpochMilliseconds(expiresAtMs),
    )

    private class FakeSessionCrypto(
        private val throwOnDecrypt: Boolean = false,
    ) : Cryptography {

        var encryptCalls: Int = 0
            private set

        var decryptCalls: Int = 0
            private set

        override suspend fun encrypt(plainText: String): String {
            encryptCalls += 1
            return "enc:$plainText"
        }

        override suspend fun decrypt(cipherText: String): String {
            decryptCalls += 1
            if (throwOnDecrypt) {
                throw GeneralSecurityException("simulated decrypt failure")
            }
            require(cipherText.startsWith("enc:"))
            return cipherText.removePrefix("enc:")
        }
    }
}

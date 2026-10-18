package com.omaroid.tasky.core.util

import com.omaroid.tasky.core.domain.auth.Credentials
import com.omaroid.tasky.core.domain.auth.CredentialsStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

class FakeCredentialsStorage : CredentialsStorage {
    val stored = MutableStateFlow<Credentials?>(null)

    override val credentials: Flow<Credentials?> = stored

    /** Set > 0 to keep a save in-flight so survivability tests can cancel around it. */
    var saveDelayMs = 0L

    override suspend fun save(credentials: Credentials) {
        if (saveDelayMs > 0) delay(saveDelayMs)
        stored.value = credentials
    }

    override suspend fun clear() {
        stored.value = null
    }
}

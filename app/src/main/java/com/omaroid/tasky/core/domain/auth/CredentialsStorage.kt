package com.omaroid.tasky.core.domain.auth

import kotlinx.coroutines.flow.Flow

/**
 * Persists the user's credentials for the opt-in "Remember me" prefill. Null means nothing saved.
 * Deliberately independent of the session: logout keeps these; unchecking the box clears them.
 */
interface CredentialsStorage {
    val credentials: Flow<Credentials?>
    suspend fun save(credentials: Credentials)
    suspend fun clear()
}

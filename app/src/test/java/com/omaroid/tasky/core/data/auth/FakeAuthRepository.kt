package com.omaroid.tasky.core.data.auth

import com.omaroid.tasky.core.domain.auth.AuthError
import com.omaroid.tasky.core.domain.auth.AuthRepository
import com.omaroid.tasky.core.domain.util.EmptyResult
import com.omaroid.tasky.core.domain.util.Result
import kotlinx.coroutines.delay

class FakeAuthRepository : AuthRepository {
    var loginResult: EmptyResult<AuthError> = Result.Success(Unit)
    var registerResult: EmptyResult<AuthError> = Result.Success(Unit)
    var logoutResult: EmptyResult<AuthError> = Result.Success(Unit)
    var logoutDelayMs: Long = 0L
    var logoutCalls: Int = 0

    val savedSessions = mutableListOf<Pair<String, String>>() // email to password

    override suspend fun login(email: String, password: String): EmptyResult<AuthError> {
        savedSessions.add(email to password)
        return loginResult
    }

    override suspend fun register(
        fullName: String,
        email: String,
        password: String,
    ): EmptyResult<AuthError> = registerResult

    override suspend fun logout(): EmptyResult<AuthError> {
        logoutCalls++
        if (logoutDelayMs > 0L) delay(logoutDelayMs)
        return logoutResult
    }
}

package com.omaroid.tasky.core.data.auth

import com.omaroid.tasky.core.domain.auth.AuthError
import com.omaroid.tasky.core.domain.auth.AuthRepository
import com.omaroid.tasky.core.domain.session.SessionLocalDataSource
import com.omaroid.tasky.core.domain.session.UserSession
import com.omaroid.tasky.core.domain.util.DataError
import com.omaroid.tasky.core.domain.util.EmptyResult
import com.omaroid.tasky.core.domain.util.Result
import com.omaroid.tasky.core.domain.util.asEmptyResult
import com.omaroid.tasky.core.domain.util.mapError
import com.omaroid.tasky.core.domain.util.onSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlin.time.Instant

class RemoteAuthRepository(
    private val remote: AuthRemoteDataSource,
    private val sessionLocalDataSource: SessionLocalDataSource,
    private val applicationScope: CoroutineScope,
) : AuthRepository {

    override suspend fun login(email: String, password: String): EmptyResult<AuthError> =
        remote.login(email, password)
            .onSuccess { dto ->
                sessionLocalDataSource.saveSession(
                    UserSession(
                        userId = dto.userId,
                        username = dto.username,
                        accessToken = dto.accessToken,
                        refreshToken = dto.refreshToken,
                        expiresAt = Instant.fromEpochMilliseconds(dto.accessTokenExpirationTimestamp),
                    )
                )
            }
            .mapError { it.toAuthError() }
            .asEmptyResult()

    override suspend fun register(
        fullName: String,
        email: String,
        password: String,
    ): EmptyResult<AuthError> =
        remote.register(fullName, email, password)
            .mapError { it.toAuthError() }
            .asEmptyResult()

    override suspend fun logout(): EmptyResult<AuthError> = applicationScope.async {
        val session = sessionLocalDataSource.getSession()
            ?: return@async Result.Success(Unit)
        remote.logout(session.refreshToken)
            .onSuccess { sessionLocalDataSource.clearSession() }
            .mapError { it.toAuthError() }
            .asEmptyResult()
    }.await()

    private fun DataError.Network.toAuthError(): AuthError = when (this) {
        DataError.Network.UNAUTHORIZED -> AuthError.INVALID_CREDENTIALS
        DataError.Network.CONFLICT -> AuthError.EMAIL_ALREADY_EXISTS
        else -> AuthError.UNKNOWN
    }
}
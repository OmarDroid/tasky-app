package com.omaroid.tasky.core.domain.session

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlinx.coroutines.test.runTest
import kotlin.time.Instant
import org.junit.Test

class CheckSessionValidUseCaseTest {

    @Test
    fun `returns false when no session exists`() = runTest {
        val useCase = CheckSessionValidUseCase(
            sessionLocalDataSource = FakeSessionLocalDataSource(session = null),
        )

        val result = useCase(now = Instant.fromEpochMilliseconds(1_000))

        assertThat(result).isFalse()
    }

    @Test
    fun `returns false when session is expired at current time`() = runTest {
        val useCase = CheckSessionValidUseCase(
            sessionLocalDataSource = FakeSessionLocalDataSource(
                session = session(expiresAtMs = 1_000),
            ),
        )

        val result = useCase(now = Instant.fromEpochMilliseconds(1_000))

        assertThat(result).isFalse()
    }

    @Test
    fun `returns true when session expiration is in the future`() = runTest {
        val useCase = CheckSessionValidUseCase(
            sessionLocalDataSource = FakeSessionLocalDataSource(
                session = session(expiresAtMs = 1_001),
            ),
        )

        val result = useCase(now = Instant.fromEpochMilliseconds(1_000))

        assertThat(result).isTrue()
    }

    private fun session(expiresAtMs: Long) = UserSession(
        userId = "user-1",
        username = "tasky-user",
        accessToken = "access-token",
        refreshToken = "refresh-token",
        expiresAt = Instant.fromEpochMilliseconds(expiresAtMs),
    )

    private class FakeSessionLocalDataSource(
        private val session: UserSession?,
    ) : SessionLocalDataSource {

        override suspend fun getSession(): UserSession? = session

        override suspend fun saveSession(session: UserSession) = Unit

        override suspend fun clearSession() = Unit
    }
}

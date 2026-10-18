package com.omaroid.tasky.core.data.auth

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.omaroid.tasky.app.startup.StartupViewModelTest
import com.omaroid.tasky.core.domain.auth.AuthError
import com.omaroid.tasky.core.domain.session.UserSession
import com.omaroid.tasky.core.domain.util.DataError
import com.omaroid.tasky.core.domain.util.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.time.Instant

class RemoteAuthRepositoryTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeSession: StartupViewModelTest.FakeSessionLocalDataSource
    private lateinit var fakeRemote: FakeAuthRemoteDataSource
    private lateinit var applicationScope: CoroutineScope
    private lateinit var repo: RemoteAuthRepository

    @Before
    fun setUp() {
        fakeSession = StartupViewModelTest.FakeSessionLocalDataSource(
            session = null
        )
        fakeRemote = FakeAuthRemoteDataSource()
        applicationScope = CoroutineScope(SupervisorJob() + testDispatcher)
        repo = RemoteAuthRepository(fakeRemote, fakeSession, applicationScope)
    }

    @Test
    fun `login success saves session and returns Success`() = runTest(testDispatcher) {
        fakeRemote.loginResponse = Result.Success(
            LoginResponseDto(
                accessToken = "token",
                refreshToken = "refresh",
                username = "Test",
                userId = "u1",
                accessTokenExpirationTimestamp = 9_999_999_000L,
            )
        )

        val result = repo.login("Test@example.com", "Password1!")

        assertThat(result).isInstanceOf(Result.Success::class)
        assertThat(fakeSession.session).isNotNull()
        assertThat(fakeSession.session!!.username).isEqualTo("Test")
        assertThat(fakeSession.session!!.expiresAt)
            .isEqualTo(Instant.fromEpochMilliseconds(9_999_999_000L))
    }

    @Test
    fun `login 401 maps to INVALID_CREDENTIALS`() = runTest(testDispatcher) {
        fakeRemote.loginResponse = Result.Error(DataError.Network.UNAUTHORIZED)

        val result = repo.login("Test@example.com", "wrong")

        assertThat(result).isInstanceOf(Result.Error::class)
        assertThat((result as Result.Error).error).isEqualTo(AuthError.INVALID_CREDENTIALS)
        assertThat(fakeSession.session).isNull()
    }

    @Test
    fun `login generic error maps to UNKNOWN`() = runTest(testDispatcher) {
        fakeRemote.loginResponse = Result.Error(DataError.Network.SERVER_ERROR)

        val result = repo.login("Test@example.com", "Password1!")

        assertThat(result).isInstanceOf(Result.Error::class)
        assertThat((result as Result.Error).error).isEqualTo(AuthError.UNKNOWN)
        assertThat(fakeSession.session).isNull()
    }

    @Test
    fun `register success returns Success`() = runTest(testDispatcher) {
        fakeRemote.registerResponse = Result.Success(Unit)

        val result = repo.register("Test", "Test@example.com", "Password1!")

        assertThat(result).isInstanceOf(Result.Success::class)
    }

    @Test
    fun `register 409 maps to EMAIL_ALREADY_EXISTS`() = runTest(testDispatcher) {
        fakeRemote.registerResponse = Result.Error(DataError.Network.CONFLICT)

        val result = repo.register("Test", "Test@example.com", "Password1!")

        assertThat(result).isInstanceOf(Result.Error::class)
        assertThat((result as Result.Error).error).isEqualTo(AuthError.EMAIL_ALREADY_EXISTS)
    }

    @Test
    fun `register generic error maps to UNKNOWN`() = runTest(testDispatcher) {
        fakeRemote.registerResponse = Result.Error(DataError.Network.SERIALIZATION)

        val result = repo.register("Test", "Test@example.com", "Password1!")

        assertThat(result).isInstanceOf(Result.Error::class)
        assertThat((result as Result.Error).error).isEqualTo(AuthError.UNKNOWN)
    }

    @Test
    fun `logout success clears session and returns Success`() = runTest(testDispatcher) {
        val session = UserSession(
            userId = "u1",
            username = "Test",
            accessToken = "at",
            refreshToken = "rt",
            expiresAt = Instant.fromEpochMilliseconds(0)
        )
        fakeSession.session = session
        fakeRemote.logoutResponse = Result.Success(Unit)

        val result = repo.logout()

        assertThat(result).isInstanceOf(Result.Success::class)
        assertThat(fakeSession.session).isNull()
    }

    @Test
    fun `logout with no session returns Success`() = runTest(testDispatcher) {
        fakeSession.session = null

        val result = repo.logout()

        assertThat(result).isInstanceOf(Result.Success::class)
    }

    @Test
    fun `logout failure returns error and does not clear session`() = runTest(testDispatcher) {
        val session = UserSession(
            userId = "u1",
            username = "Test",
            accessToken = "at",
            refreshToken = "rt",
            expiresAt = Instant.fromEpochMilliseconds(0)
        )
        fakeSession.session = session
        fakeRemote.logoutResponse = Result.Error(DataError.Network.SERVER_ERROR)

        val result = repo.logout()

        assertThat(result).isInstanceOf(Result.Error::class)
        assertThat(fakeSession.session).isEqualTo(session)
    }
}

package com.omaroid.tasky.app.startup

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.omaroid.tasky.core.domain.session.CheckSessionValidUseCase
import com.omaroid.tasky.core.domain.session.SessionLocalDataSource
import com.omaroid.tasky.core.domain.session.UserSession
import com.omaroid.tasky.core.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.time.Clock
import kotlin.time.Instant
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StartupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    @Test
    fun `on app launched with valid session navigates to agenda and ends loading`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val localDataSource = FakeSessionLocalDataSource(
                session = session(expiresAtMs = 1_001L),
            )
            val viewModel = createViewModel(
                localDataSource = localDataSource,
                nowMs = 1_000L,
            )

            viewModel.onAction(StartupAction.OnAppLaunched)
            advanceUntilIdle()

            assertThat(viewModel.state.value).isEqualTo(
                StartupState(
                    isLoading = false,
                    destination = StartupDestination.Agenda,
                ),
            )
        }

    @Test
    fun `on app launched with no session navigates to login and ends loading`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val localDataSource = FakeSessionLocalDataSource(session = null)
            val viewModel = createViewModel(
                localDataSource = localDataSource,
                nowMs = 1_000L,
            )

            viewModel.onAction(StartupAction.OnAppLaunched)
            advanceUntilIdle()

            assertThat(viewModel.state.value).isEqualTo(
                StartupState(
                    isLoading = false,
                    destination = StartupDestination.Login,
                ),
            )
        }

    @Test
    fun `on app launched with expired session navigates to login`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val localDataSource = FakeSessionLocalDataSource(
                session = session(expiresAtMs = 1_000L),
            )
            val viewModel = createViewModel(
                localDataSource = localDataSource,
                nowMs = 1_000L,
            )

            viewModel.onAction(StartupAction.OnAppLaunched)
            advanceUntilIdle()

            assertThat(viewModel.state.value.destination).isEqualTo(StartupDestination.Login)
        }

    private fun createViewModel(
        localDataSource: SessionLocalDataSource,
        nowMs: Long,
    ): StartupViewModel {
        val useCase = CheckSessionValidUseCase(localDataSource)
        val clock = object : Clock {
            override fun now() = Instant.fromEpochMilliseconds(nowMs)
        }
        return StartupViewModel(checkSessionValidUseCase = useCase, clock = clock)
    }

    private fun session(expiresAtMs: Long) = UserSession(
        userId = "user-1",
        username = "tasky-user",
        accessToken = "access-token",
        refreshToken = "refresh-token",
        expiresAt = Instant.fromEpochMilliseconds(expiresAtMs),
    )

    class FakeSessionLocalDataSource(
        var session: UserSession?,
    ) : SessionLocalDataSource {

        override suspend fun getSession(): UserSession? = session

        override suspend fun saveSession(session: UserSession) {
            this.session = session
        }

        override suspend fun clearSession() {
            this.session = null
        }
    }
}

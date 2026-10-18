package com.omaroid.tasky.feature.auth.login

import androidx.lifecycle.viewModelScope
import com.omaroid.tasky.core.domain.auth.Credentials
import com.omaroid.tasky.core.util.FakeCredentialsStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.advanceUntilIdle
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.omaroid.tasky.core.data.auth.FakeAuthRepository
import com.omaroid.tasky.core.domain.auth.AuthError
import com.omaroid.tasky.core.domain.auth.AuthRepository
import com.omaroid.tasky.core.domain.util.EmptyResult
import com.omaroid.tasky.core.domain.util.Result
import com.omaroid.tasky.core.domain.validation.EmailValidator
import com.omaroid.tasky.core.util.TestPatternValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeRepo = FakeAuthRepository()
    private val fakeCredentials = FakeCredentialsStorage()
    private lateinit var applicationScope: CoroutineScope
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        applicationScope = CoroutineScope(SupervisorJob() + testDispatcher)
        viewModel = LoginViewModel(
            emailValidator = EmailValidator(TestPatternValidator()),
            authRepository = fakeRepo,
            credentialsStorage = fakeCredentials,
            applicationScope = applicationScope,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @Test
    fun `initial state has empty fields and disabled submit`() = runTest {
        val state = viewModel.state.value
        assertThat(state.email).isEqualTo("")
        assertThat(state.password).isEqualTo("")
        assertThat(state.isValid).isFalse()
    }

    @Test
    fun `valid email and non-blank password enables submit`() = runTest {
        viewModel.onAction(LoginAction.OnEmailChanged("user@example.com"))
        viewModel.onAction(LoginAction.OnPasswordChanged("Secret1!"))
        assertThat(viewModel.state.value.isValid).isTrue()
        assertThat(viewModel.state.value.emailError).isNull()
        assertThat(viewModel.state.value.passwordError).isNull()
    }

    @Test
    fun `invalid email format keeps submit disabled`() = runTest {
        val vm = LoginViewModel(
            emailValidator = EmailValidator(TestPatternValidator(result = false)),
            authRepository = fakeRepo,
            credentialsStorage = fakeCredentials,
            applicationScope = applicationScope,
        )
        vm.onAction(LoginAction.OnEmailChanged("not-an-email"))
        vm.onAction(LoginAction.OnPasswordChanged("Secret1!"))
        assertThat(vm.state.value.isValid).isFalse()
        assertThat(vm.state.value.emailError).isNotNull()
    }

    @Test
    fun `blank password keeps submit disabled`() = runTest {
        viewModel.onAction(LoginAction.OnEmailChanged("user@example.com"))
        viewModel.onAction(LoginAction.OnPasswordChanged(""))
        assertThat(viewModel.state.value.isValid).isFalse()
        assertThat(viewModel.state.value.passwordError).isNotNull()
    }

    @Test
    fun `password visibility toggle flips isPasswordVisible`() = runTest {
        assertThat(viewModel.state.value.isPasswordVisible).isFalse()
        viewModel.onAction(LoginAction.OnPasswordVisibilityClicked)
        assertThat(viewModel.state.value.isPasswordVisible).isTrue()
        viewModel.onAction(LoginAction.OnPasswordVisibilityClicked)
        assertThat(viewModel.state.value.isPasswordVisible).isFalse()
    }

    @Test
    fun `OnRegisterClicked emits NavigateToRegister event`() = runTest {
        val events = mutableListOf<LoginEvent>()
        val job = launch { viewModel.events.collect { events.add(it) } }
        viewModel.onAction(LoginAction.OnRegisterClicked)
        advanceUntilIdle()
        assertThat(events).isEqualTo(listOf(LoginEvent.NavigateToRegister))
        job.cancel()
    }

    // ── Auth integration ──────────────────────────────────────────────────────

    @Test
    fun `successful login emits NavigateToAgenda`() = runTest {
        fakeRepo.loginResult = Result.Success(Unit)
        val events = mutableListOf<LoginEvent>()
        val job = launch { viewModel.events.collect { events.add(it) } }

        viewModel.onAction(LoginAction.OnEmailChanged("user@example.com"))
        viewModel.onAction(LoginAction.OnPasswordChanged("Secret1!"))
        viewModel.onAction(LoginAction.OnLoginClicked)
        advanceUntilIdle()

        assertThat(events).isEqualTo(listOf(LoginEvent.NavigateToAgenda))
        assertThat(viewModel.state.value.isSubmitting).isFalse()
        assertThat(viewModel.state.value.globalError).isNull()
        job.cancel()
    }

    @Test
    fun `invalid credentials sets globalError and clears isSubmitting`() = runTest {
        fakeRepo.loginResult = Result.Error(AuthError.INVALID_CREDENTIALS)

        viewModel.onAction(LoginAction.OnEmailChanged("user@example.com"))
        viewModel.onAction(LoginAction.OnPasswordChanged("Wrong1!"))
        viewModel.onAction(LoginAction.OnLoginClicked)
        advanceUntilIdle()

        assertThat(viewModel.state.value.globalError).isNotNull()
        assertThat(viewModel.state.value.isSubmitting).isFalse()
    }

    @Test
    fun `network error sets globalError`() = runTest {
        fakeRepo.loginResult = Result.Error(AuthError.UNKNOWN)

        viewModel.onAction(LoginAction.OnEmailChanged("user@example.com"))
        viewModel.onAction(LoginAction.OnPasswordChanged("Secret1!"))
        viewModel.onAction(LoginAction.OnLoginClicked)
        advanceUntilIdle()

        assertThat(viewModel.state.value.globalError).isNotNull()
    }

    @Test
    fun `globalError is cleared when email changes after failed login`() = runTest {
        fakeRepo.loginResult = Result.Error(AuthError.INVALID_CREDENTIALS)

        viewModel.onAction(LoginAction.OnEmailChanged("user@example.com"))
        viewModel.onAction(LoginAction.OnPasswordChanged("Wrong1!"))
        viewModel.onAction(LoginAction.OnLoginClicked)
        advanceUntilIdle()

        viewModel.onAction(LoginAction.OnEmailChanged("other@example.com"))

        assertThat(viewModel.state.value.globalError).isNull()
    }

    // ── Remember me ──────────────────────────────────────────────────────────

    @Test
    fun `OnScreenEntered prefills saved credentials and checks the box`() = runTest(testDispatcher.scheduler) {
        fakeCredentials.stored.value = Credentials("saved@example.com", "Secret1!")
        viewModel.onAction(LoginAction.OnScreenEntered)
        advanceUntilIdle()
        val state = viewModel.state.value
        assertThat(state.email).isEqualTo("saved@example.com")
        assertThat(state.password).isEqualTo("Secret1!")
        assertThat(state.rememberMe).isTrue()
    }

    @Test
    fun `successful login with remember me checked saves the exact credentials`() = runTest(testDispatcher.scheduler) {
        viewModel.onAction(LoginAction.OnEmailChanged("user@example.com"))
        viewModel.onAction(LoginAction.OnPasswordChanged("Secret1!"))
        viewModel.onAction(LoginAction.OnRememberMeToggled)
        viewModel.onAction(LoginAction.OnLoginClicked)
        advanceUntilIdle()
        // assert the captured VALUES, not just that something was saved
        assertThat(fakeCredentials.stored.value).isEqualTo(Credentials("user@example.com", "Secret1!"))
    }

    @Test
    fun `successful login with remember me unchecked clears previously saved credentials`() = runTest(testDispatcher.scheduler) {
        // seed the OPPOSITE of the fake's default so the assertion proves the write
        fakeCredentials.stored.value = Credentials("old@example.com", "OldSecret1!")
        viewModel.onAction(LoginAction.OnEmailChanged("user@example.com"))
        viewModel.onAction(LoginAction.OnPasswordChanged("Secret1!"))
        viewModel.onAction(LoginAction.OnLoginClicked) // rememberMe stays false
        advanceUntilIdle()
        assertThat(fakeCredentials.stored.value).isNull()
    }

    @Test
    fun `credential save survives ViewModel cancellation`() = runTest(testDispatcher.scheduler) {
        // Keep the save in-flight: login runs, the applicationScope save suspends in its delay,
        // THEN the ViewModel dies — the write must still complete.
        fakeCredentials.saveDelayMs = 500
        viewModel.onAction(LoginAction.OnEmailChanged("user@example.com"))
        viewModel.onAction(LoginAction.OnPasswordChanged("Secret1!"))
        viewModel.onAction(LoginAction.OnRememberMeToggled)
        viewModel.onAction(LoginAction.OnLoginClicked)
        testDispatcher.scheduler.runCurrent() // login completes; save launched, parked in delay
        viewModel.viewModelScope.cancel()     // screen gone
        advanceUntilIdle()                    // applicationScope finishes the save anyway
        assertThat(fakeCredentials.stored.value).isEqualTo(Credentials("user@example.com", "Secret1!"))
    }
}

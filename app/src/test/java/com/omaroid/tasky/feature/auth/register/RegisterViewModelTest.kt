package com.omaroid.tasky.feature.auth.register

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
import com.omaroid.tasky.core.domain.validation.NameValidator
import com.omaroid.tasky.core.domain.validation.PasswordValidator
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
class RegisterViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeRepo = FakeAuthRepository()
    private lateinit var viewModel: RegisterViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = RegisterViewModel(
            nameValidator = NameValidator(),
            emailValidator = EmailValidator(TestPatternValidator()),
            passwordValidator = PasswordValidator(),
            authRepository = fakeRepo,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @Test
    fun `initial state is invalid`() = runTest {
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()
        assertThat(viewModel.state.value.isValid).isFalse()
    }

    @Test
    fun `name shorter than 4 chars keeps form invalid`() = runTest {
        backgroundScope.launch { viewModel.state.collect {} }
        viewModel.onAction(RegisterAction.OnNameChanged("Al"))
        viewModel.onAction(RegisterAction.OnEmailChanged("al@example.com"))
        viewModel.onAction(RegisterAction.OnPasswordChanged("Password1!"))
        advanceUntilIdle()
        assertThat(viewModel.state.value.isValid).isFalse()
        assertThat(viewModel.state.value.nameError).isNotNull()
    }

    @Test
    fun `name longer than 50 chars keeps form invalid`() = runTest {
        backgroundScope.launch { viewModel.state.collect {} }
        viewModel.onAction(RegisterAction.OnNameChanged("A".repeat(51)))
        viewModel.onAction(RegisterAction.OnEmailChanged("al@example.com"))
        viewModel.onAction(RegisterAction.OnPasswordChanged("Password1!"))
        advanceUntilIdle()
        assertThat(viewModel.state.value.isValid).isFalse()
        assertThat(viewModel.state.value.nameError).isNotNull()
    }

    @Test
    fun `password without uppercase keeps form invalid`() = runTest {
        backgroundScope.launch { viewModel.state.collect {} }
        viewModel.onAction(RegisterAction.OnNameChanged("Alice"))
        viewModel.onAction(RegisterAction.OnEmailChanged("alice@example.com"))
        viewModel.onAction(RegisterAction.OnPasswordChanged("password1!"))
        advanceUntilIdle()
        assertThat(viewModel.state.value.isValid).isFalse()
        assertThat(viewModel.state.value.passwordError).isNotNull()
    }

    @Test
    fun `password without digit keeps form invalid`() = runTest {
        backgroundScope.launch { viewModel.state.collect {} }
        viewModel.onAction(RegisterAction.OnNameChanged("Alice"))
        viewModel.onAction(RegisterAction.OnEmailChanged("alice@example.com"))
        viewModel.onAction(RegisterAction.OnPasswordChanged("Password!!"))
        advanceUntilIdle()
        assertThat(viewModel.state.value.isValid).isFalse()
        assertThat(viewModel.state.value.passwordError).isNotNull()
    }

    @Test
    fun `password shorter than 8 chars keeps form invalid`() = runTest {
        backgroundScope.launch { viewModel.state.collect {} }
        viewModel.onAction(RegisterAction.OnNameChanged("Alice"))
        viewModel.onAction(RegisterAction.OnEmailChanged("alice@example.com"))
        viewModel.onAction(RegisterAction.OnPasswordChanged("Pass1!"))
        advanceUntilIdle()
        assertThat(viewModel.state.value.isValid).isFalse()
        assertThat(viewModel.state.value.passwordError).isNotNull()
    }

    @Test
    fun `all fields valid enables submit`() = runTest {
        backgroundScope.launch { viewModel.state.collect {} }
        viewModel.onAction(RegisterAction.OnNameChanged("Alice"))
        viewModel.onAction(RegisterAction.OnEmailChanged("alice@example.com"))
        viewModel.onAction(RegisterAction.OnPasswordChanged("Password1!"))
        advanceUntilIdle()
        assertThat(viewModel.state.value.isValid).isTrue()
        assertThat(viewModel.state.value.nameError).isNull()
        assertThat(viewModel.state.value.emailError).isNull()
        assertThat(viewModel.state.value.passwordError).isNull()
    }

    // ── Auth integration ──────────────────────────────────────────────────────

    @Test
    fun `successful register followed by auto-login emits NavigateToAgenda`() = runTest {
        fakeRepo.registerResult = Result.Success(Unit)
        fakeRepo.loginResult = Result.Success(Unit)
        val events = mutableListOf<RegisterEvent>()
        val job = launch { viewModel.events.collect { events.add(it) } }

        viewModel.onAction(RegisterAction.OnNameChanged("Alice"))
        viewModel.onAction(RegisterAction.OnEmailChanged("alice@example.com"))
        viewModel.onAction(RegisterAction.OnPasswordChanged("Password1!"))
        viewModel.onAction(RegisterAction.OnRegisterClicked)
        advanceUntilIdle()

        assertThat(events).isEqualTo(listOf(RegisterEvent.NavigateToAgenda))
        job.cancel()
    }

    @Test
    fun `register succeeds but auto-login fails emits NavigateToLogin`() = runTest {
        fakeRepo.registerResult = Result.Success(Unit)
        fakeRepo.loginResult = Result.Error(AuthError.UNKNOWN)
        val events = mutableListOf<RegisterEvent>()
        val job = launch { viewModel.events.collect { events.add(it) } }

        viewModel.onAction(RegisterAction.OnNameChanged("Alice"))
        viewModel.onAction(RegisterAction.OnEmailChanged("alice@example.com"))
        viewModel.onAction(RegisterAction.OnPasswordChanged("Password1!"))
        viewModel.onAction(RegisterAction.OnRegisterClicked)
        advanceUntilIdle()

        assertThat(events).isEqualTo(listOf(RegisterEvent.NavigateToLogin))
        job.cancel()
    }

    @Test
    fun `duplicate email sets globalError and clears isSubmitting`() = runTest {
        fakeRepo.registerResult = Result.Error(AuthError.EMAIL_ALREADY_EXISTS)

        viewModel.onAction(RegisterAction.OnNameChanged("Alice"))
        viewModel.onAction(RegisterAction.OnEmailChanged("alice@example.com"))
        viewModel.onAction(RegisterAction.OnPasswordChanged("Password1!"))
        viewModel.onAction(RegisterAction.OnRegisterClicked)
        advanceUntilIdle()

        assertThat(viewModel.state.value.globalError).isNotNull()
        assertThat(viewModel.state.value.isSubmitting).isFalse()
    }
}

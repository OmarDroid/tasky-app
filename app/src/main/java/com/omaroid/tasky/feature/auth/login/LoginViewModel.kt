package com.omaroid.tasky.feature.auth.login

import com.omaroid.tasky.core.domain.auth.Credentials
import com.omaroid.tasky.core.domain.auth.CredentialsStorage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omaroid.tasky.R
import com.omaroid.tasky.core.domain.auth.AuthRepository
import com.omaroid.tasky.core.domain.util.onFailure
import com.omaroid.tasky.core.domain.util.onSuccess
import com.omaroid.tasky.core.domain.validation.EmailValidator
import com.omaroid.tasky.core.presentation.util.UiText
import com.omaroid.tasky.core.presentation.util.toUiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LoginViewModel(
    private val emailValidator: EmailValidator,
    private val authRepository: AuthRepository,
    private val credentialsStorage: CredentialsStorage,
    private val applicationScope: CoroutineScope,
) : ViewModel() {

    private var hasPrefilled = false

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    private val _events = Channel<LoginEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: LoginAction) {
        when (action) {
            is LoginAction.OnEmailChanged -> onEmailChanged(action.value)
            is LoginAction.OnPasswordChanged -> onPasswordChanged(action.value)
            LoginAction.OnPasswordVisibilityClicked -> {
                _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            }
            LoginAction.OnRememberMeToggled -> {
                _state.update { it.copy(rememberMe = !it.rememberMe) }
            }
            // Dispatched once from LoginRoot (the StartupViewModel OnAppLaunched precedent) — no
            // construction-time side effects; the prefill stays testable via a plain action.
            LoginAction.OnScreenEntered -> prefillSavedCredentials()
            LoginAction.OnLoginClicked -> onLoginClicked()
            LoginAction.OnRegisterClicked -> {
                viewModelScope.launch { _events.send(LoginEvent.NavigateToRegister) }
            }
            LoginAction.OnErrorConsumed -> {
                _state.update { it.copy(globalError = null) }
            }
        }
    }

    private fun onEmailChanged(value: String) {
        _state.update {
            it.copy(
                email = value,
                emailError = emailError(value),
                globalError = null,
            )
        }
    }

    private fun onPasswordChanged(value: String) {
        _state.update {
            it.copy(
                password = value,
                passwordError = validatePassword(value),
                globalError = null,
            )
        }
    }

    private fun onLoginClicked() {
        val current = _state.value
        val emailError = emailError(current.email)
        val passwordError = validatePassword(current.password)

        if (emailError != null || passwordError != null) {
            _state.update {
                it.copy(
                    emailError = emailError,
                    passwordError = passwordError,
                )
            }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, globalError = null) }
            authRepository.login(_state.value.email, _state.value.password)
                .onSuccess {
                    _state.update { it.copy(isSubmitting = false) }
                    // The write must survive immediate navigation → applicationScope. The navigate
                    // Event stays on viewModelScope (never send Events from applicationScope).
                    val remember = _state.value.rememberMe
                    val credentials = Credentials(_state.value.email, _state.value.password)
                    applicationScope.launch {
                        if (remember) credentialsStorage.save(credentials)
                        else credentialsStorage.clear()
                    }
                    _events.send(LoginEvent.NavigateToAgenda)
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(isSubmitting = false, globalError = error.toUiText())
                    }
                }
        }
    }

    private fun prefillSavedCredentials() {
        if (hasPrefilled) return // once per ViewModel — don't clobber user edits on re-entry
        hasPrefilled = true
        viewModelScope.launch {
            credentialsStorage.credentials.first()?.let { saved ->
                _state.update {
                    it.copy(email = saved.email, password = saved.password, rememberMe = true)
                }
            }
        }
    }

    private fun emailError(email: String): UiText? {
        if (email.isBlank()) return UiText.StringResource(R.string.error_email_required)
        if (!emailValidator(email)) return UiText.StringResource(R.string.error_email_invalid)
        return null
    }

    private fun validatePassword(password: String): UiText? {
        if (password.isBlank()) return UiText.StringResource(R.string.error_password_required)
        return null
    }
}

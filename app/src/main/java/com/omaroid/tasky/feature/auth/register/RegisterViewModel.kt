package com.omaroid.tasky.feature.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omaroid.tasky.R
import com.omaroid.tasky.core.domain.auth.AuthRepository
import com.omaroid.tasky.core.domain.util.onFailure
import com.omaroid.tasky.core.domain.util.onSuccess
import com.omaroid.tasky.core.domain.validation.EmailValidator
import com.omaroid.tasky.core.domain.validation.NameValidator
import com.omaroid.tasky.core.domain.validation.PasswordValidator
import com.omaroid.tasky.core.presentation.util.UiText
import com.omaroid.tasky.core.presentation.util.toUiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val nameValidator: NameValidator,
    private val emailValidator: EmailValidator,
    private val passwordValidator: PasswordValidator,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private var hasLoadedInitialData = false

    private val _events = Channel<RegisterEvent>()
    val events = _events.receiveAsFlow()

    private val _state = MutableStateFlow(RegisterState())
    val state = _state.asStateFlow()

    init {
        if (!hasLoadedInitialData) {
            /** Load initial data here **/
            hasLoadedInitialData = true
        }
    }

    fun onAction(action: RegisterAction) {
        when (action) {
            is RegisterAction.OnEmailChanged -> _state.update {
                it.copy(
                    email = action.value,
                    emailError = validateEmail(action.value),
                    globalError = null
                )
            }

            RegisterAction.OnLoginClicked -> viewModelScope.launch {
                _events.send(
                    RegisterEvent.NavigateToLogin
                )
            }

            is RegisterAction.OnNameChanged -> _state.update {
                it.copy(
                    name = action.value,
                    nameError = validateName(action.value),
                    globalError = null
                )
            }

            is RegisterAction.OnPasswordChanged -> _state.update {
                it.copy(
                    password = action.value,
                    passwordError = validatePassword(action.value),
                    globalError = null
                )
            }

            RegisterAction.OnPasswordVisibilityClicked -> _state.update {
                it.copy(
                    isPasswordVisible = !it.isPasswordVisible
                )
            }

            RegisterAction.OnRegisterClicked -> onRegisterClicked()
            RegisterAction.OnErrorConsumed -> _state.update { it.copy(globalError = null) }
        }
    }

    private fun onRegisterClicked() {
        val current = _state.value
        val nameError = validateName(current.name)
        val emailError = validateEmail(current.email)
        val passwordError = validatePassword(current.password)
        if (nameError != null || emailError != null || passwordError != null) {
            _state.update {
                it.copy(
                    nameError = nameError, emailError = emailError,
                    passwordError = passwordError, hasAttemptedSubmit = true
                )
            }

            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, globalError = null) }
            val current = _state.value
            authRepository.register(current.name, current.email, current.password)
                .onSuccess {
                    // Auto-login after successful registration
                    authRepository.login(current.email, current.password)
                        .onSuccess { _events.send(RegisterEvent.NavigateToAgenda) }
                        .onFailure { _events.send(RegisterEvent.NavigateToLogin) }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(isSubmitting = false, globalError = error.toUiText())
                    }
                }
        }
    }

    private fun validateName(name: String): UiText? {
        if (name.isBlank()) return UiText.StringResource(R.string.error_name_required)
        if (!nameValidator(name)) return UiText.StringResource(R.string.error_name_invalid)
        return null
    }

    private fun validateEmail(email: String): UiText? {
        if (email.isBlank()) return UiText.StringResource(R.string.error_email_required)
        if (!emailValidator(email)) return UiText.StringResource(R.string.error_email_invalid)
        return null
    }

    private fun validatePassword(password: String): UiText? {
        if (password.isBlank()) return UiText.StringResource(R.string.error_password_required)
        if (!passwordValidator(password).isValid) return UiText.StringResource(R.string.error_password_too_short)
        return null
    }

}

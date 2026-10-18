package com.omaroid.tasky.feature.auth.register
import com.omaroid.tasky.core.presentation.util.UiText

data class RegisterState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isSubmitting: Boolean = false,
    val nameError: UiText? = null,
    val emailError: UiText? = null,
    val passwordError: UiText? = null,
    val globalError: UiText? = null,
    val hasAttemptedSubmit: Boolean = false,
) {
    val isValid: Boolean
        get() = nameError == null && emailError == null && passwordError == null
                && name.isNotBlank() && email.isNotBlank() && password.isNotBlank()
}

package com.omaroid.tasky.feature.auth.login

import com.omaroid.tasky.core.presentation.util.UiText

data class LoginState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val rememberMe: Boolean = false,
    val isSubmitting: Boolean = false,
    val emailError: UiText? = null,
    val passwordError: UiText? = null,
    val globalError: UiText? = null,
) {
    val isValid: Boolean
        get() = emailError == null && passwordError == null && email.isNotBlank() && password.isNotBlank()
}

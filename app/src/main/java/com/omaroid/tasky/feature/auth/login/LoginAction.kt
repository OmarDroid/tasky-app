package com.omaroid.tasky.feature.auth.login

sealed interface LoginAction {
    data class OnEmailChanged(val value: String) : LoginAction
    data class OnPasswordChanged(val value: String) : LoginAction
    data object OnPasswordVisibilityClicked : LoginAction
    data object OnRememberMeToggled : LoginAction
    data object OnScreenEntered : LoginAction
    data object OnLoginClicked : LoginAction
    data object OnRegisterClicked : LoginAction
    data object OnErrorConsumed : LoginAction
}

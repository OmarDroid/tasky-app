package com.omaroid.tasky.feature.auth.register

sealed interface RegisterAction {
    data class OnNameChanged(val value: String) : RegisterAction
    data class OnEmailChanged(val value: String) : RegisterAction
    data class OnPasswordChanged(val value: String) : RegisterAction
    data object OnPasswordVisibilityClicked : RegisterAction
    data object OnRegisterClicked : RegisterAction
    data object OnLoginClicked : RegisterAction
    data object OnErrorConsumed : RegisterAction
}

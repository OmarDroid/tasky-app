package com.omaroid.tasky.feature.auth.login

sealed interface LoginEvent {
    data object NavigateToRegister : LoginEvent
    data object NavigateToAgenda : LoginEvent
}

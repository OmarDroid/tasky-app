package com.omaroid.tasky.feature.auth.register

sealed interface RegisterEvent {
    data object NavigateToLogin : RegisterEvent
    data object NavigateToAgenda : RegisterEvent
}

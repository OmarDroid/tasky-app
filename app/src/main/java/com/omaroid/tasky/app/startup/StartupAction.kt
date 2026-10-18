package com.omaroid.tasky.app.startup

sealed interface StartupAction {
    data object OnAppLaunched : StartupAction
}
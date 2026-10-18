package com.omaroid.tasky.app.startup

data class StartupState(
    val isLoading: Boolean = true,
    val destination: StartupDestination = StartupDestination.Login,
)

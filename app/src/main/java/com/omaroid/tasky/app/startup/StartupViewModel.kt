package com.omaroid.tasky.app.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omaroid.tasky.core.domain.session.CheckSessionValidUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

class StartupViewModel(
    private val checkSessionValidUseCase: CheckSessionValidUseCase,
    private val clock: Clock,
) : ViewModel() {

    private var hasStarted = false

    private val _state = MutableStateFlow(StartupState())
    val state: StateFlow<StartupState> = _state.asStateFlow()

    fun onAction(action: StartupAction) {
        when (action) {
            StartupAction.OnAppLaunched -> {
                if (hasStarted) return
                hasStarted = true

                viewModelScope.launch {
                    val now = clock.now()
                    val hasValidSession = checkSessionValidUseCase(now)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            destination = if (hasValidSession) {
                                StartupDestination.Agenda
                            } else {
                                StartupDestination.Login
                            },
                        )
                    }
                }
            }
        }
    }
}

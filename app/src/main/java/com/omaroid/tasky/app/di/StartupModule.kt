package com.omaroid.tasky.app.di

import com.omaroid.tasky.app.startup.StartupViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import kotlin.time.Clock

val startupPresentationModule = module {
    single<Clock> { Clock.System}
    viewModel { StartupViewModel(checkSessionValidUseCase = get(), clock = get()) }
}

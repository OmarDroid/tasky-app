package com.omaroid.tasky.app.di

import com.omaroid.tasky.feature.auth.login.LoginViewModel
import com.omaroid.tasky.feature.auth.register.RegisterViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val authPresentationModule = module {
    viewModelOf(::LoginViewModel)
    viewModelOf(::RegisterViewModel)
}

package com.omaroid.tasky.app.di

import com.omaroid.tasky.feature.detail.AgendaDetailViewModel
import com.omaroid.tasky.feature.detail.PhotoDetailViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val agendaDetailModule = module {
    viewModelOf(::AgendaDetailViewModel)
    viewModelOf(::PhotoDetailViewModel)
}

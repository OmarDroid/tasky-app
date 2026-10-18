package com.omaroid.tasky.app.di

import com.omaroid.tasky.feature.agenda.AgendaViewModel
import com.omaroid.tasky.feature.calendar.CalendarViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val agendaPresentationModule = module {
    viewModelOf(::AgendaViewModel)
    viewModelOf(::CalendarViewModel)
}
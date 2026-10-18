package com.omaroid.tasky.feature.calendar

import kotlinx.datetime.LocalDate

sealed interface CalendarAction {
    data object OnPreviousMonth : CalendarAction
    data object OnNextMonth : CalendarAction
    data object OnTodayClicked : CalendarAction
    data class OnDaySelected(val date: LocalDate) : CalendarAction
}

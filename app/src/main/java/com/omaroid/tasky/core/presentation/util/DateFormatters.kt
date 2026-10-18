package com.omaroid.tasky.core.presentation.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char

private val displayDateFormat = LocalDate.Format {
    monthName(MonthNames.ENGLISH_ABBREVIATED)
    char(' ')
    day()
    char(' ')
    year()
}

private val displayTimeFormat = LocalTime.Format {
    hour()
    char(':')
    minute()
}

fun LocalDate.toDisplayDate(): String = displayDateFormat.format(this)

fun LocalTime.toDisplayTime(): String = displayTimeFormat.format(this)

// Agenda list card label, e.g. "Mar 5, 10:00" — abbreviated month, un-padded day, then time.
// Built with the format DSL (like the formatters above) so all date/time shaping stays in one
// place and stays localizable, rather than hand-concatenating from the enum name.
private val agendaCardFormat = LocalDateTime.Format {
    monthName(MonthNames.ENGLISH_ABBREVIATED)
    char(' ')
    day(padding = Padding.NONE)
    chars(", ")
    hour()
    char(':')
    minute()
}

fun LocalDateTime.toAgendaCardLabel(): String = agendaCardFormat.format(this)

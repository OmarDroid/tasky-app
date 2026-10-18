package com.omaroid.tasky.feature.calendar

import com.omaroid.tasky.feature.agenda.YearMonth
import com.omaroid.tasky.feature.agenda.model.AgendaItemUi
import com.omaroid.tasky.feature.agenda.yearMonth
import com.omaroid.tasky.feature.detail.AgendaDetailType
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

data class CalendarState(
    val today: LocalDate,                       // required — captured once in the VM from its clock
    val selectedDate: LocalDate = today,
    val yearMonth: YearMonth = today.yearMonth,
    val cells: List<LocalDate> = generateCalendarCells(today.yearMonth), // 42 grid days, Monday-first
    val markers: Map<LocalDate, Set<AgendaDetailType>> = emptyMap(),     // dot types per day
    val dayItems: List<AgendaItemUi> = emptyList(),                      // selected day's items
)

/**
 * 6×7 Monday-first grid: lead-in days from the previous month, tail from the next. A PURE top-level
 * function beside the state (the `generateVisibleDates` precedent) — list building never happens in
 * a composable.
 */
fun generateCalendarCells(yearMonth: YearMonth): List<LocalDate> {
    val first = LocalDate(yearMonth.year, yearMonth.month, 1)
    val gridStart = first.minus(first.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
    return (0 until 42).map { gridStart.plus(it, DateTimeUnit.DAY) }
}

fun YearMonth.shiftedBy(months: Int): YearMonth =
    LocalDate(year, month, 1).plus(months, DateTimeUnit.MONTH).yearMonth

/** The item's calendar day — Task/Reminder carry a display date; an Event belongs to its start day. */
val AgendaItemUi.calendarDate: LocalDate
    get() = when (this) {
        is AgendaItemUi.Task -> displayDate
        is AgendaItemUi.Reminder -> displayDate
        is AgendaItemUi.Event -> from.date
    }

/** Reuses the EXISTING detail-type enum for markers and navigation — no new type enum. */
val AgendaItemUi.detailType: AgendaDetailType
    get() = when (this) {
        is AgendaItemUi.Task -> AgendaDetailType.TASK
        is AgendaItemUi.Reminder -> AgendaDetailType.REMINDER
        is AgendaItemUi.Event -> AgendaDetailType.EVENT
    }

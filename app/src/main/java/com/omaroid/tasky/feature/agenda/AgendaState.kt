package com.omaroid.tasky.feature.agenda

import com.omaroid.tasky.R
import com.omaroid.tasky.core.domain.preferences.ThemePreference
import com.omaroid.tasky.core.presentation.util.UiText
import com.omaroid.tasky.feature.agenda.model.AgendaItemUi
import com.omaroid.tasky.feature.agenda.model.AgendaListEntry
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

data class YearMonth(val year: Int, val month: Month)

val LocalDate.yearMonth: YearMonth get() = YearMonth(year, month)

data class AgendaState(
    val today: LocalDate,
    val selectedDate: LocalDate = today,
    val displayedMonth: YearMonth = today.yearMonth,
    val visibleDates: List<LocalDate> = generateVisibleDates(today),
    val selectedDateLabel: UiText = UiText.StringResource(R.string.today),
    val needleTime: LocalTime? = null,
    val entries: List<AgendaListEntry> = emptyList(),
    val isLoading: Boolean = false,
    val username: String = "",
    val userInitials: String = "",
    val isOnline: Boolean = true,
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val isProfileMenuVisible: Boolean = false,
    val isTypePickerVisible: Boolean = false,
    val pendingDeleteItem: AgendaItemUi? = null,
)

fun generateVisibleDates(center: LocalDate): List<LocalDate> =
    (-15..15).map { center.plus(it, DateTimeUnit.DAY) }

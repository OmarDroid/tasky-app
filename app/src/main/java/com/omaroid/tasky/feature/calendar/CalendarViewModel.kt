package com.omaroid.tasky.feature.calendar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omaroid.tasky.core.domain.agenda.repository.EventRepository
import com.omaroid.tasky.core.domain.agenda.repository.ReminderRepository
import com.omaroid.tasky.core.domain.agenda.repository.TaskRepository
import com.omaroid.tasky.core.domain.session.SessionLocalDataSource
import com.omaroid.tasky.feature.agenda.model.toUi
import com.omaroid.tasky.feature.agenda.yearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
    private val reminderRepository: ReminderRepository,
    private val eventRepository: EventRepository,
    private val sessionLocalDataSource: SessionLocalDataSource,
    clock: Clock = Clock.System,
) : ViewModel() {

    private val zone = TimeZone.currentSystemDefault()
    private val today = clock.todayIn(zone)
    private val initialDate = savedStateHandle.get<Long>("initialDateEpochDay")
        ?.let { LocalDate.fromEpochDays(it.toInt()) } ?: today

    private var currentUserId: String? = null

    // _state is INPUT-only (month + day selection); DB data folds into `state` below, never back in.
    private val _state = MutableStateFlow(
        CalendarState(
            today = today,
            selectedDate = initialDate,
            yearMonth = initialDate.yearMonth,
            cells = generateCalendarCells(initialDate.yearMonth),
        ),
    )

    val state = _state
        .map { it.yearMonth }
        .distinctUntilChanged()                 // unrelated _state updates must NOT restart Room flows
        .flatMapLatest { ym ->                  // month switch restarts the range; never collect{} here
            val startMs = LocalDate(ym.year, ym.month, 1)
                .atStartOfDayIn(zone).toEpochMilliseconds()
            val endMs = LocalDate(ym.year, ym.month, 1).plus(1, DateTimeUnit.MONTH)
                .atStartOfDayIn(zone).toEpochMilliseconds() // [monthStart, nextMonthStart)
            combine(
                taskRepository.observeTasksForDay(startMs, endMs),
                reminderRepository.observeRemindersForDay(startMs, endMs),
                eventRepository.observeEventsForDay(startMs, endMs),
            ) { tasks, reminders, events ->
                (tasks + reminders + events).map { it.toUi(currentUserId) }
            }
        }
        .combine(_state) { monthItems, s ->     // fold the DB flow INTO the state declaration
            s.copy(
                markers = monthItems
                    .groupBy(keySelector = { it.calendarDate }, valueTransform = { it.detailType })
                    .mapValues { (_, types) -> types.toSet() },
                dayItems = monthItems
                    .filter { it.calendarDate == s.selectedDate }
                    .sortedBy { it.startTime },
            )
        }
        .onStart { currentUserId = sessionLocalDataSource.getSession()?.userId }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), _state.value)

    fun onAction(action: CalendarAction) {
        when (action) {
            CalendarAction.OnPreviousMonth -> _state.update {
                val ym = it.yearMonth.shiftedBy(-1)
                it.copy(yearMonth = ym, cells = generateCalendarCells(ym))
            }
            CalendarAction.OnNextMonth -> _state.update {
                val ym = it.yearMonth.shiftedBy(+1)
                it.copy(yearMonth = ym, cells = generateCalendarCells(ym))
            }
            CalendarAction.OnTodayClicked -> _state.update {
                it.copy(
                    selectedDate = it.today,
                    yearMonth = it.today.yearMonth,
                    cells = generateCalendarCells(it.today.yearMonth),
                )
            }
            is CalendarAction.OnDaySelected -> _state.update { it.copy(selectedDate = action.date) }
        }
    }
}

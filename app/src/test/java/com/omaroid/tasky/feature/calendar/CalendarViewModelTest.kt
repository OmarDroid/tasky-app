package com.omaroid.tasky.feature.calendar

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import com.omaroid.tasky.core.data.agenda.FakeEventRepository
import com.omaroid.tasky.core.data.agenda.FakeReminderRepository
import com.omaroid.tasky.core.data.agenda.FakeTaskRepository
import com.omaroid.tasky.core.data.session.FakeSessionLocalDataSource
import com.omaroid.tasky.core.domain.agenda.models.AgendaItem
import com.omaroid.tasky.core.domain.session.UserSession
import com.omaroid.tasky.core.util.FakeClock
import com.omaroid.tasky.core.util.MainDispatcherRule
import com.omaroid.tasky.feature.detail.AgendaDetailType
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import org.junit.Rule
import org.junit.Test
import kotlin.time.Instant

class CalendarViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val zone = TimeZone.currentSystemDefault()

    // Fixed, NON-midnight "now" (2026-05-15 09:00 local) per the FakeClock rule.
    private val baseDate = LocalDate(2026, 5, 15)
    private val fakeClock = FakeClock(baseDate.atTime(9, 0).toInstant(zone))

    private val fakeTaskRepo = FakeTaskRepository()
    private val fakeReminderRepo = FakeReminderRepository()
    private val fakeEventRepo = FakeEventRepository()
    private val session = UserSession(
        userId = "user1", username = "Alice Bob",
        accessToken = "token", refreshToken = "refresh",
        expiresAt = Instant.parse("2099-01-01T00:00:00Z"),
    )

    private fun vm(initialDate: LocalDate = baseDate) = CalendarViewModel(
        savedStateHandle = SavedStateHandle(mapOf("initialDateEpochDay" to initialDate.toEpochDays().toLong())),
        taskRepository = fakeTaskRepo,
        reminderRepository = fakeReminderRepo,
        eventRepository = fakeEventRepo,
        sessionLocalDataSource = FakeSessionLocalDataSource().apply { session = this@CalendarViewModelTest.session },
        clock = fakeClock,
    )

    private fun task(id: String, date: LocalDate, time: LocalTime = LocalTime(10, 0)) = AgendaItem.Task(
        id = id, title = "T-$id", description = "",
        time = date.atTime(time).toInstant(zone),
        remindAt = date.atTime(time).toInstant(zone),
        isDone = false,
    )

    private fun reminder(id: String, date: LocalDate) = AgendaItem.Reminder(
        id = id, title = "R-$id", description = "",
        time = date.atTime(14, 0).toInstant(zone),
        remindAt = date.atTime(13, 30).toInstant(zone),
    )

    @Test
    fun `markers group the month's items by day and type`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeTaskRepo.upsertTask(task("t1", LocalDate(2026, 5, 3)))
            fakeReminderRepo.upsertReminder(reminder("r1", LocalDate(2026, 5, 3)))
            fakeTaskRepo.upsertTask(task("t2", LocalDate(2026, 5, 20)))
            vm().state.test {
                // Unconfined main: the pipeline may run during subscription, so the StateFlow
                // conflates — await by PREDICATE from the first item instead of blind skipping.
                var loaded = awaitItem()
                while (loaded.markers.size < 2) loaded = awaitItem()
                assertThat(loaded.markers[LocalDate(2026, 5, 3)]!!)
                    .containsExactlyInAnyOrder(AgendaDetailType.TASK, AgendaDetailType.REMINDER)
                assertThat(loaded.markers[LocalDate(2026, 5, 20)]!!)
                    .containsExactlyInAnyOrder(AgendaDetailType.TASK)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `OnDaySelected swaps the day list to that day's items`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeTaskRepo.upsertTask(task("t-selected", LocalDate(2026, 5, 3)))
            fakeTaskRepo.upsertTask(task("t-other", LocalDate(2026, 5, 20)))
            val viewModel = vm()
            viewModel.state.test {
                var s = awaitItem()
                while (s.markers.isEmpty()) s = awaitItem() // month loaded
                viewModel.onAction(CalendarAction.OnDaySelected(LocalDate(2026, 5, 3)))
                var selected = awaitItem()
                while (selected.dayItems.isEmpty()) selected = awaitItem()
                // assert the IDENTITY of the listed item, not a count
                assertThat(selected.dayItems.map { it.id }).containsExactlyInAnyOrder("t-selected")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `OnNextMonth restarts the range - new month's items appear, old month's disappear`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeTaskRepo.upsertTask(task("may-task", LocalDate(2026, 5, 10)))
            fakeTaskRepo.upsertTask(task("june-task", LocalDate(2026, 6, 10)))
            val viewModel = vm()
            viewModel.state.test {
                var may = awaitItem()
                while (!may.markers.keys.contains(LocalDate(2026, 5, 10))) may = awaitItem()
                viewModel.onAction(CalendarAction.OnNextMonth)
                // the restarted Room flow then delivers June's data
                var next = awaitItem()
                while (next.markers.keys.contains(LocalDate(2026, 5, 10)) ||
                    !next.markers.keys.contains(LocalDate(2026, 6, 10))
                ) {
                    next = awaitItem()
                }
                assertThat(next.markers.keys).contains(LocalDate(2026, 6, 10))
                assertThat(next.markers.keys).doesNotContain(LocalDate(2026, 5, 10))
                cancelAndIgnoreRemainingEvents()
            }
        }

    // Boundary tests per the range-query rule: mid-range greens don't catch >= / < typos.

    @Test
    fun `item at exactly month start is included`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val monthStart = LocalDate(2026, 5, 1)
            fakeTaskRepo.upsertTask(task("t-boundary", monthStart, LocalTime(0, 0)))
            vm().state.test {
                var s = awaitItem()
                while (!s.markers.keys.contains(monthStart)) s = awaitItem()
                assertThat(s.markers.keys).contains(monthStart)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `item at exactly next month start is excluded`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val nextMonthStart = LocalDate(2026, 6, 1)
            fakeTaskRepo.upsertTask(task("t-next", nextMonthStart, LocalTime(0, 0)))
            vm().state.test {
                // exclusion can't be awaited — with the unconfined main the first item is the
                // settled state; assert directly on it
                assertThat(awaitItem().markers.keys).doesNotContain(nextMonthStart)
                cancelAndIgnoreRemainingEvents()
            }
        }
}

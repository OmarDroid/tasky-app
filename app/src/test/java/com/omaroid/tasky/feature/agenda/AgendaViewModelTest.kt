package com.omaroid.tasky.feature.agenda

import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.omaroid.tasky.app.startup.StartupViewModelTest
import com.omaroid.tasky.core.data.agenda.FakeAgendaSyncRepository
import com.omaroid.tasky.core.data.agenda.FakeEventRepository
import com.omaroid.tasky.core.data.agenda.FakeReminderRepository
import com.omaroid.tasky.core.data.agenda.FakeTaskRepository
import com.omaroid.tasky.core.data.agenda.remote.dto.TaskDto
import com.omaroid.tasky.core.data.auth.FakeAuthRepository
import com.omaroid.tasky.core.domain.session.UserSession
import com.omaroid.tasky.R
import com.omaroid.tasky.core.presentation.util.UiText
import com.omaroid.tasky.core.util.FakeClock
import com.omaroid.tasky.core.domain.preferences.ThemePreference
import com.omaroid.tasky.core.util.FakeConnectivityMonitor
import com.omaroid.tasky.core.util.FakeReminderScheduler
import com.omaroid.tasky.core.util.FakeThemePreferenceStorage
import com.omaroid.tasky.core.util.MainDispatcherRule
import com.omaroid.tasky.core.util.event
import com.omaroid.tasky.core.util.reminder
import com.omaroid.tasky.core.util.task
import com.omaroid.tasky.feature.agenda.model.AgendaItemUi
import com.omaroid.tasky.feature.agenda.model.AgendaListEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class AgendaViewModelTest {

    // StandardTestDispatcher is required for Turbine's state.test {} to work correctly.
    // With UnconfinedTestDispatcher, WhileSubscribed's SharingCoroutine runs outside the
    // scheduler's queue, so Turbine's awaitItem() times out instead of advancing to it.
    // StandardTestDispatcher schedules all coroutines through the TestCoroutineScheduler,
    // which Turbine advances internally on each awaitItem() call.
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private lateinit var fakeTaskRepo: FakeTaskRepository
    private lateinit var fakeReminderRepo: FakeReminderRepository
    private lateinit var fakeEventRepo: FakeEventRepository
    private lateinit var fakeAuthRepo: FakeAuthRepository
    private lateinit var fakeSession: StartupViewModelTest.FakeSessionLocalDataSource
    private lateinit var fakeAgendaSyncRepo: FakeAgendaSyncRepository
    private lateinit var viewModel: AgendaViewModel
    private lateinit var applicationScope: CoroutineScope
    private val fakeConnectivity = FakeConnectivityMonitor(online = true)
    private val fakeReminderScheduler = FakeReminderScheduler()
    private val fakeThemeStorage = FakeThemePreferenceStorage()

    private val zone = TimeZone.currentSystemDefault()
    private val today = LocalDate(2026, 5, 6)
    private val fakeClock = FakeClock(today.atStartOfDayIn(zone) + 9.hours)

    val session: UserSession = UserSession(
        userId = "user1",
        username = "Alice Bob",
        accessToken = "token",
        refreshToken = "refresh",
        expiresAt = Instant.parse("2099-01-01T00:00:00Z"),
    )

    @Before
    fun setUp() {
        applicationScope = CoroutineScope(SupervisorJob() + mainDispatcherRule.testDispatcher)
        fakeTaskRepo = FakeTaskRepository()
        fakeReminderRepo = FakeReminderRepository()
        fakeEventRepo = FakeEventRepository()
        fakeAuthRepo = FakeAuthRepository()
        fakeSession = StartupViewModelTest.FakeSessionLocalDataSource(session)
        fakeAgendaSyncRepo = FakeAgendaSyncRepository()
        viewModel = AgendaViewModel(
            taskRepository = fakeTaskRepo,
            reminderRepository = fakeReminderRepo,
            eventRepository = fakeEventRepo,
            authRepository = fakeAuthRepo,
            sessionLocalDataSource = fakeSession,
            connectivityMonitor = fakeConnectivity,
            reminderScheduler = fakeReminderScheduler,
            themePreferenceStorage = fakeThemeStorage,
            applicationScope = applicationScope,
            clock = fakeClock,
            agendaSyncRepository = fakeAgendaSyncRepo,
        )
    }

    @After
    fun tearDown() {
        applicationScope.cancel()
    }

    // Emission sequence for viewModel.state.test {}:
    //   Item 1: stateIn initial value — AgendaState(today) with empty username, selectedDate=today
    //   Item 2: first real emission — Turbine's awaitItem() advances the scheduler, which runs
    //            WhileSubscribed's SharingCoroutine → onStart → loadUsername() →
    //            _state updates → combine emits with username + current DB contents
    // Tests use skipItems(1) + awaitItem() to land on the loaded state,
    // unless they explicitly test the initial stateIn value.

    // ── Session ───────────────────────────────────────────────────────────────────

    @Test
    fun `state shows today as selected date on start`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            viewModel.state.test {
                val initial = awaitItem() // stateIn initial value — selectedDate is already today
                assertThat(initial.selectedDate).isEqualTo(today)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `username and initials are loaded from session before first agenda emission`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            viewModel.state.test {
                skipItems(1) // stateIn initial value (empty username)
                val loaded = awaitItem()
                assertThat(loaded.username).isEqualTo("Alice Bob")
                assertThat(loaded.userInitials).isEqualTo("AB")
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ── Database ──────────────────────────────────────────────────────────────────

    @Test
    fun `items for today are shown immediately after start`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeTaskRepo.upsertTask(task("t1", today.atStartOfDayIn(zone) + 1.hours))
            viewModel.state.test {
                skipItems(1)
                val loaded = awaitItem()
                val items = loaded.entries.filterIsInstance<AgendaListEntry.Item>()
                assertThat(items).hasSize(1)
                assertThat(items.first().item.id).isEqualTo("t1")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `adding an item after start reactively updates entries`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            viewModel.state.test {
                skipItems(1)
                val initial = awaitItem()
                assertThat(initial.entries.filterIsInstance<AgendaListEntry.Item>()).isEmpty()

                fakeTaskRepo.upsertTask(task("t1", today.atStartOfDayIn(zone) + 1.hours))

                val updated = awaitItem()
                assertThat(updated.entries.filterIsInstance<AgendaListEntry.Item>()).hasSize(1)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `switching date shows only that date's items`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val tomorrow = today.plus(1, DateTimeUnit.DAY)
            fakeTaskRepo.upsertTask(task("today-task", today.atStartOfDayIn(zone) + 1.hours))
            fakeTaskRepo.upsertTask(task("tomorrow-task", tomorrow.atStartOfDayIn(zone) + 1.hours))
            viewModel.state.test {
                skipItems(1)
                awaitItem() // initial: today's items loaded

                viewModel.onAction(AgendaAction.OnDateSelected(tomorrow))

                // One intermediate emission occurs: combine(_state) fires immediately when
                // _state changes (selectedDate=tomorrow), but flatMapLatest hasn't switched yet
                // so items are still from today's query. Skip it and await the final emission
                // where flatMapLatest has re-subscribed for tomorrow's date range.
                skipItems(1)
                val updated = awaitItem()
                val items = updated.entries.filterIsInstance<AgendaListEntry.Item>()
                assertThat(items).hasSize(1)
                assertThat(items.first().item.id).isEqualTo("tomorrow-task")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `items from all three types appear together in entries`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeTaskRepo.upsertTask(task("t1", today.atStartOfDayIn(zone) + 1.hours))
            fakeReminderRepo.upsertReminder(reminder("r1", today.atStartOfDayIn(zone) + 2.hours))
            fakeEventRepo.upsertEvent(event("e1", today.atStartOfDayIn(zone) + 3.hours, hostId = "user1"))
            viewModel.state.test {
                skipItems(1)
                val loaded = awaitItem()
                assertThat(loaded.entries.filterIsInstance<AgendaListEntry.Item>()).hasSize(3)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `items outside today's date range are not shown`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val yesterday = today.minus(1, DateTimeUnit.DAY)
            fakeTaskRepo.upsertTask(task("old", yesterday.atStartOfDayIn(zone) + 1.hours))
            fakeTaskRepo.upsertTask(task("current", today.atStartOfDayIn(zone) + 1.hours))
            viewModel.state.test {
                skipItems(1)
                val loaded = awaitItem()
                val items = loaded.entries.filterIsInstance<AgendaListEntry.Item>()
                assertThat(items).hasSize(1)
                assertThat(items.first().item.id).isEqualTo("current")
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ── isCreator / currentUserId ─────────────────────────────────────────────────

    @Test
    fun `event hosted by current user renders with isCreator true`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeEventRepo.upsertEvent(event("e1", today.atStartOfDayIn(zone) + 1.hours, hostId = "user1"))
            viewModel.state.test {
                skipItems(1)
                val loaded = awaitItem()
                val uiEvent = loaded.entries
                    .filterIsInstance<AgendaListEntry.Item>()
                    .map { it.item }
                    .filterIsInstance<AgendaItemUi.Event>()
                    .first()
                assertThat(uiEvent.isCreator).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `event hosted by another user renders with isCreator false`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeEventRepo.upsertEvent(event("e1", today.atStartOfDayIn(zone) + 1.hours, hostId = "other"))
            viewModel.state.test {
                skipItems(1)
                val loaded = awaitItem()
                val uiEvent = loaded.entries
                    .filterIsInstance<AgendaListEntry.Item>()
                    .map { it.item }
                    .filterIsInstance<AgendaItemUi.Event>()
                    .first()
                assertThat(uiEvent.isCreator).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `event renders with isCreator false when session is missing`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeSession.session = null // no session → currentUserId stays null
            fakeEventRepo.upsertEvent(event("e1", today.atStartOfDayIn(zone) + 1.hours, hostId = "user1"))
            viewModel.state.test {
                skipItems(1)
                val loaded = awaitItem()
                val uiEvent = loaded.entries
                    .filterIsInstance<AgendaListEntry.Item>()
                    .map { it.item }
                    .filterIsInstance<AgendaItemUi.Event>()
                    .first()
                assertThat(uiEvent.isCreator).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ── Needle ────────────────────────────────────────────────────────────────────

    @Test
    fun `needle appears in entries when today is selected`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            viewModel.state.test {
                skipItems(1)
                val loaded = awaitItem()
                assertThat(loaded.entries.filterIsInstance<AgendaListEntry.Needle>()).hasSize(1)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `needle is absent when a past date is selected`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            viewModel.state.test {
                skipItems(1)
                awaitItem() // loaded state with today selected
                viewModel.onAction(AgendaAction.OnDateSelected(today.minus(1, DateTimeUnit.DAY)))
                val updated = awaitItem()
                assertThat(updated.entries.filterIsInstance<AgendaListEntry.Needle>()).isEmpty()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `needle is inserted before items that start after the current time`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeTaskRepo.upsertTask(task("early", today.atStartOfDayIn(zone) + 8.hours))  // 08:00
            fakeTaskRepo.upsertTask(task("late",  today.atStartOfDayIn(zone) + 11.hours)) // 11:00
            viewModel.state.test {
                skipItems(1)
                val loaded = awaitItem()
                val entries = loaded.entries
                val earlyIndex  = entries.indexOfFirst { it is AgendaListEntry.Item && it.item.id == "early" }
                val needleIndex = entries.indexOfFirst { it is AgendaListEntry.Needle }
                val lateIndex   = entries.indexOfFirst { it is AgendaListEntry.Item && it.item.id == "late" }
                assertThat(earlyIndex < needleIndex).isTrue()
                assertThat(needleIndex < lateIndex).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ── FAB / Picker / Profile ────────────────────────────────────────────────────

    @Test
    fun `OnFabClicked shows type picker`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            viewModel.state.test {
                skipItems(1)
                awaitItem() // loaded
                viewModel.onAction(AgendaAction.OnFabClicked)
                assertThat(awaitItem().isTypePickerVisible).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `OnTypePickerDismissed hides type picker`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            viewModel.state.test {
                skipItems(1)
                awaitItem() // loaded
                viewModel.onAction(AgendaAction.OnFabClicked)
                awaitItem() // picker shown
                viewModel.onAction(AgendaAction.OnTypePickerDismissed)
                assertThat(awaitItem().isTypePickerVisible).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `OnProfileClicked shows profile menu`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            viewModel.state.test {
                skipItems(1)
                awaitItem() // loaded
                viewModel.onAction(AgendaAction.OnProfileClicked)
                assertThat(awaitItem().isProfileMenuVisible).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `OnProfileMenuDismissed hides profile menu`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            viewModel.state.test {
                skipItems(1)
                awaitItem() // loaded
                viewModel.onAction(AgendaAction.OnProfileClicked)
                awaitItem() // menu shown
                viewModel.onAction(AgendaAction.OnProfileMenuDismissed)
                assertThat(awaitItem().isProfileMenuVisible).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ── Delete ────────────────────────────────────────────────────────────────────

    @Test
    fun `OnItemDeleteClicked sets pendingDeleteItem`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            viewModel.state.test {
                skipItems(1)
                awaitItem() // loaded
                val item = AgendaItemUi.Task("t1", "T", "D", LocalTime(9, 0), today, false)
                viewModel.onAction(AgendaAction.OnItemDeleteClicked(item))
                assertThat(awaitItem().pendingDeleteItem).isEqualTo(item)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `OnItemDeleteDismissed clears pendingDeleteItem`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            viewModel.state.test {
                skipItems(1)
                awaitItem() // loaded
                val item = AgendaItemUi.Task("t1", "T", "D", LocalTime(9, 0), today, false)
                viewModel.onAction(AgendaAction.OnItemDeleteClicked(item))
                awaitItem() // pending set
                viewModel.onAction(AgendaAction.OnItemDeleteDismissed)
                assertThat(awaitItem().pendingDeleteItem).isNull()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `OnItemDeleteConfirmed removes task from repository and clears pending`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeTaskRepo.upsertTask(task("t1", today.atStartOfDayIn(zone) + 1.hours))
            viewModel.state.test {
                skipItems(1)
                awaitItem() // loaded with task
                val uiTask = AgendaItemUi.Task("t1", "T", "", LocalTime(1, 0), today, false)
                viewModel.onAction(AgendaAction.OnItemDeleteClicked(uiTask))
                awaitItem() // pending set
                viewModel.onAction(AgendaAction.OnItemDeleteConfirmed)
                cancelAndIgnoreRemainingEvents()
            }
            // OnItemDeleteConfirmed launches a viewModelScope coroutine (delete + pending clear).
            advanceUntilIdle()
            assertThat(fakeTaskRepo.tasks).isEmpty()
            assertThat(viewModel.state.value.pendingDeleteItem).isNull()
        }

    // ── Task toggle ───────────────────────────────────────────────────────────────

    @Test
    fun `OnTaskToggled flips isDone from the domain task, not the UI snapshot`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeTaskRepo.upsertTask(task("t1", today.atStartOfDayIn(zone) + 1.hours, isDone = false))
            viewModel.state.test {
                skipItems(1)
                val loaded = awaitItem()
                val uiTask = loaded.entries
                    .filterIsInstance<AgendaListEntry.Item>()
                    .map { it.item }
                    .filterIsInstance<AgendaItemUi.Task>()
                    .first()
                viewModel.onAction(AgendaAction.OnTaskToggled(uiTask))
                cancelAndIgnoreRemainingEvents()
            }
            // OnTaskToggled launches a viewModelScope coroutine; advance to let it run.
            advanceUntilIdle()
            assertThat(fakeTaskRepo.tasks.find { it.id == "t1" }?.isDone).isEqualTo(true)
        }

    // ── Navigation events ─────────────────────────────────────────────────────────

    @Test
    fun `OnCreateTask emits OpenTask with null id and isEditing true`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            viewModel.events.test {
                viewModel.onAction(AgendaAction.OnFabClicked)
                viewModel.onAction(AgendaAction.OnCreateTask)
                val event = awaitItem()
                assertThat(event).isInstanceOf(AgendaEvent.OpenTask::class)
                assertThat((event as AgendaEvent.OpenTask).id).isNull()
                assertThat(event.isEditing).isTrue()
            }
        }

    @Test
    fun `OnCreateReminder emits OpenReminder with null id and isEditing true`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            viewModel.events.test {
                viewModel.onAction(AgendaAction.OnFabClicked)
                viewModel.onAction(AgendaAction.OnCreateReminder)
                val event = awaitItem()
                assertThat(event).isInstanceOf(AgendaEvent.OpenReminder::class)
                assertThat((event as AgendaEvent.OpenReminder).id).isNull()
                assertThat(event.isEditing).isTrue()
            }
        }

    @Test
    fun `OnCreateEvent emits OpenEvent with null id and isEditing true`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            viewModel.events.test {
                viewModel.onAction(AgendaAction.OnFabClicked)
                viewModel.onAction(AgendaAction.OnCreateEvent)
                val event = awaitItem()
                assertThat(event).isInstanceOf(AgendaEvent.OpenEvent::class)
                assertThat((event as AgendaEvent.OpenEvent).id).isNull()
                assertThat(event.isEditing).isTrue()
            }
        }

    // ── Logout ────────────────────────────────────────────────────────────────────

    // Date-picker jump (25.1)

    @Test
    fun `OnDateJumped selects the date, recenters the strip and formats the label`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val target = LocalDate(2026, 5, 20)
            viewModel.state.test {
                var s = awaitItem()
                viewModel.onAction(AgendaAction.OnDateJumped(target))
                while (s.selectedDate != target) s = awaitItem()
                assertThat(s.displayedMonth).isEqualTo(target.yearMonth)
                assertThat(s.visibleDates).contains(target) // strip recentered around the jump
                // the HUMAN-READABLE label, not just the date — this assertion catches broken
                // interpolation (a literal template string once shipped without it)
                assertThat(s.selectedDateLabel).isEqualTo(UiText.DynamicString("20 May"))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `OnDateJumped to today restores the Today label`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            viewModel.state.test {
                var s = awaitItem()
                viewModel.onAction(AgendaAction.OnDateJumped(LocalDate(2026, 5, 20)))
                while (s.selectedDate != LocalDate(2026, 5, 20)) s = awaitItem()
                viewModel.onAction(AgendaAction.OnDateJumped(today)) // the Calendar screen's Today path
                while (s.selectedDate != today) s = awaitItem()
                assertThat(s.selectedDate).isEqualTo(LocalDate(2026, 5, 6)) // FakeClock's fixed date
                assertThat(s.selectedDateLabel).isEqualTo(UiText.StringResource(R.string.today))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `OnLogoutClicked emits NavigateToLogin`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            viewModel.events.test {
                viewModel.onAction(AgendaAction.OnLogoutClicked)
                assertThat(awaitItem()).isInstanceOf(AgendaEvent.NavigateToLogin::class)
            }
        }

    @Test
    fun `logout survives ViewModel cancellation`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeAuthRepo.logoutDelayMs = 500L
            viewModel.onAction(AgendaAction.OnLogoutClicked)
            viewModel.viewModelScope.cancel()
            advanceUntilIdle()
            assertThat(fakeAuthRepo.logoutCalls).isEqualTo(1)
            // Requirement: remove all reminders on logout — runs on the same applicationScope job.
            assertThat(fakeReminderScheduler.cancelAllCalls).isEqualTo(1)
        }

    // ── Theme ─────────────────────────────────────────────────────────────────────

    @Test
    fun `OnThemeToggled cycles the preference and persists it`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            viewModel.state.test {
                skipItems(1)
                awaitItem() // onStart ran — theme observer live at SYSTEM
                viewModel.onAction(AgendaAction.OnThemeToggled)
                assertThat(awaitItem().theme).isEqualTo(ThemePreference.LIGHT)
                cancelAndIgnoreRemainingEvents()
            }
            // The write hit persistent storage, not just UI state.
            assertThat(fakeThemeStorage.stored.value).isEqualTo(ThemePreference.LIGHT)
        }

    // ── fetchAndSync ──────────────────────────────────────────────────────────────

    @Test
    fun fetchAndSync_populatesEntriesFromRemoteOnStart() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            // FakeAgendaSyncRepository.onSync seeds the fake repo as if AgendaSyncRepositoryImpl
            // had written to the DAO. Room Flows in FakeTaskRepository react via MutableStateFlow.
            fakeAgendaSyncRepo.onSync = {
                fakeTaskRepo.upsertTask(task("remote-t1", today.atStartOfDayIn(zone) + 1.hours))
            }
            viewModel.state.test {
                skipItems(1)
                awaitItem() // loaded state; fetchAndSync is in-flight as a separate launch
                cancelAndIgnoreRemainingEvents()
            }
            advanceUntilIdle()
            assertThat(
                viewModel.state.value.entries.filterIsInstance<AgendaListEntry.Item>()
            ).hasSize(1)
            assertThat(
                viewModel.state.value.entries.filterIsInstance<AgendaListEntry.Item>().first().item.id
            ).isEqualTo("remote-t1")
        }

    @Test
    fun fetchAndSync_failureLeavesCachedRoomDataIntact() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeTaskRepo.upsertTask(task("cached-t1", today.atStartOfDayIn(zone) + 1.hours))
            fakeAgendaSyncRepo.error = RuntimeException("network error")
            viewModel.state.test {
                skipItems(1)
                awaitItem() // loaded state with cached task already in entries
                cancelAndIgnoreRemainingEvents()
            }
            advanceUntilIdle() // fetchAndSync throws, exception is swallowed
            assertThat(
                viewModel.state.value.entries.filterIsInstance<AgendaListEntry.Item>()
            ).hasSize(1)
            assertThat(
                viewModel.state.value.entries.filterIsInstance<AgendaListEntry.Item>().first().item.id
            ).isEqualTo("cached-t1")
        }

    @Test
    fun fetchAndSync_runsAgainWhenSubscriberReAttaches() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            viewModel.state.test {
                skipItems(1)
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }
            advanceUntilIdle()
            assertThat(fakeAgendaSyncRepo.syncCallCount).isEqualTo(1)

            // let WhileSubscribed(5_000) timeout elapse so the upstream fully stops
            advanceTimeBy(5_001L)
            advanceUntilIdle() // drain upstream cancellation before re-subscribing

            // re-subscribe: WhileSubscribed restarts the upstream, onStart re-fires,
            // fetchAndSync() launches again
            val job = launch { viewModel.state.collect {} }
            advanceUntilIdle()
            job.cancel()

            assertThat(fakeAgendaSyncRepo.syncCallCount).isEqualTo(2)
        }
}

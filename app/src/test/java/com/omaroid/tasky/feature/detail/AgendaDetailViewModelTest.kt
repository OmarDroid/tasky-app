package com.omaroid.tasky.feature.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import com.omaroid.tasky.core.data.agenda.FakeEventRepository
import com.omaroid.tasky.core.data.agenda.FakeReminderRepository
import com.omaroid.tasky.core.data.agenda.FakeTaskRepository
import com.omaroid.tasky.core.data.agenda.FakeAttendeeRepository
import com.omaroid.tasky.core.data.photo.FakeLocalPhotoStorage
import com.omaroid.tasky.core.data.session.FakeSessionLocalDataSource
import com.omaroid.tasky.core.domain.agenda.models.AgendaItem
import com.omaroid.tasky.core.domain.agenda.models.Attendee
import com.omaroid.tasky.core.domain.agenda.models.Photo
import com.omaroid.tasky.feature.detail.model.PhotoUi
import com.omaroid.tasky.core.domain.session.UserSession
import com.omaroid.tasky.core.domain.util.PatternValidator
import com.omaroid.tasky.core.domain.validation.EmailValidator
import com.omaroid.tasky.core.util.FakeClock
import com.omaroid.tasky.core.util.FakeConnectivityMonitor
import com.omaroid.tasky.core.util.FakeReminderScheduler
import com.omaroid.tasky.core.util.MainDispatcherRule
import com.omaroid.tasky.core.util.reminder
import com.omaroid.tasky.core.util.task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import android.app.Application
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * One test class for the single [AgendaDetailViewModel]. The route's `type` selects Task vs
 * Reminder vs Event; everything else is shared, so the same ViewModel logic is exercised once per
 * type instead of duplicated across three test classes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AgendaDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private lateinit var fakeTaskRepo: FakeTaskRepository
    private lateinit var fakeReminderRepo: FakeReminderRepository
    private lateinit var fakeEventRepo: FakeEventRepository
    private lateinit var fakeSession: FakeSessionLocalDataSource
    private lateinit var fakeAttendeeRepository: FakeAttendeeRepository
    private lateinit var fakeLocalPhotoStorage: FakeLocalPhotoStorage
    private lateinit var applicationScope: CoroutineScope

    // Simple format validator: good enough to exercise the valid/invalid email paths.
    private val emailValidator = EmailValidator(object : PatternValidator {
        override fun matches(value: String) = value.contains("@") && value.contains(".")
    })

    private val zone = TimeZone.currentSystemDefault()
    private val today = LocalDate(2026, 5, 6)
    private val fixedInstant = today.atStartOfDayIn(zone) + 9.hours
    private val fakeClock = FakeClock(fixedInstant)
    private val fakeConnectivity = FakeConnectivityMonitor(online = true)
    private val fakeReminderScheduler = FakeReminderScheduler()
    private val testSession = UserSession(
        userId = "user1", username = "Alice Bob",
        accessToken = "tok", refreshToken = "ref",
        expiresAt = Instant.parse("2099-01-01T00:00:00Z"),
    )

    @Before
    fun setUp() {
        applicationScope = CoroutineScope(SupervisorJob() + mainDispatcherRule.testDispatcher)
        fakeTaskRepo = FakeTaskRepository()
        fakeReminderRepo = FakeReminderRepository()
        fakeEventRepo = FakeEventRepository()
        fakeSession = FakeSessionLocalDataSource().also { it.session = testSession }
        fakeAttendeeRepository = FakeAttendeeRepository()
        fakeLocalPhotoStorage = FakeLocalPhotoStorage()
    }

    @After
    fun tearDown() { applicationScope.cancel() }

    private fun vm(handle: SavedStateHandle) = AgendaDetailViewModel(
        savedStateHandle = handle,
        taskRepository = fakeTaskRepo,
        reminderRepository = fakeReminderRepo,
        eventRepository = fakeEventRepo,
        sessionLocalDataSource = fakeSession,
        attendeeRepository = fakeAttendeeRepository,
        localPhotoStorage = fakeLocalPhotoStorage,
        emailValidator = emailValidator,
        connectivityMonitor = fakeConnectivity,
        reminderScheduler = fakeReminderScheduler,
        applicationScope = applicationScope,
        clock = fakeClock,
    )

    private fun newReminderVm() =
        vm(SavedStateHandle(mapOf("type" to AgendaDetailType.REMINDER, "isEditing" to true)))

    private fun existingReminderVm(id: String = "r1") =
        vm(SavedStateHandle(mapOf("type" to AgendaDetailType.REMINDER, "itemId" to id, "isEditing" to false)))

    private fun newTaskVm() =
        vm(SavedStateHandle(mapOf("type" to AgendaDetailType.TASK, "isEditing" to true)))

    private fun existingTaskVm(id: String = "t1") =
        vm(SavedStateHandle(mapOf("type" to AgendaDetailType.TASK, "itemId" to id, "isEditing" to false)))

    private fun newEventVm() =
        vm(SavedStateHandle(mapOf("type" to AgendaDetailType.EVENT, "isEditing" to true)))

    private fun existingEventVm(id: String = "e1") =
        vm(SavedStateHandle(mapOf("type" to AgendaDetailType.EVENT, "itemId" to id, "isEditing" to false)))

    private fun event(id: String = "e1", hostId: String = "user1") = AgendaItem.Event(
        id = id, title = "Meeting", description = "Discuss",
        time = fixedInstant,
        to = fixedInstant + 1.hours,
        remindAt = fixedInstant - 30.minutes,
        hostId = hostId, attendees = emptyList(), photos = emptyList(),
    )

    private fun eventDetails(state: AgendaDetailState) = state.details as AgendaItemDetails.Event

    // ── Reminder ────────────────────────────────────────────────────────────────────

    @Test
    fun `new reminder starts in edit mode with Reminder details seeded from the clock`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            newReminderVm().state.test {
                val initial = awaitItem()
                assertThat(initial.isNew).isTrue()
                assertThat(initial.isEditMode).isTrue()
                assertThat(initial.details).isInstanceOf(AgendaItemDetails.Reminder::class)
                assertThat(initial.dateTime.date).isEqualTo(LocalDate(2026, 5, 6))
                assertThat(initial.dateTime.time.hour).isEqualTo(9)
                assertThat(initial.dateTime.time.minute).isEqualTo(0)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `loadReminder populates title, description and clears edit mode`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeReminderRepo.upsertReminder(
                reminder("r1", fixedInstant).copy(title = "Doctor appointment", description = "Building C")
            )
            existingReminderVm().state.test {
                skipItems(1)
                val loaded = awaitItem()
                assertThat(loaded.title).isEqualTo("Doctor appointment")
                assertThat(loaded.description).isEqualTo("Building C")
                assertThat(loaded.isEditMode).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `onSaveClicked creates reminder in repo and emits NavigateBack`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val vm = newReminderVm()
            vm.onAction(AgendaDetailAction.OnTitleEdited("Team standup"))
            vm.events.test {
                vm.onAction(AgendaDetailAction.OnSaveClicked)
                assertThat(awaitItem()).isInstanceOf(AgendaDetailEvent.NavigateBack::class)
            }
            assertThat(fakeReminderRepo.reminders.first().title).isEqualTo("Team standup")
            // Saving also schedules the reminder notification (same applicationScope job).
            // the reminder we actually saved is the one that got scheduled — match by id, not title
            val savedId = fakeReminderRepo.reminders.first().id
            assertThat(fakeReminderScheduler.scheduledItems.map { it.id }).contains(savedId)
        }

    @Test
    fun `onDeleteConfirmed removes reminder and emits NavigateBack`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeReminderRepo.upsertReminder(reminder("r1", fixedInstant))
            val vm = existingReminderVm()
            vm.state.test { skipItems(1); awaitItem(); cancelAndIgnoreRemainingEvents() }
            vm.events.test {
                vm.onAction(AgendaDetailAction.OnDeleteConfirmed)
                assertThat(awaitItem()).isInstanceOf(AgendaDetailEvent.NavigateBack::class)
            }
            assertThat(fakeReminderRepo.reminders).isEmpty()
            // Deleting also cancels the item's scheduled notification.
            assertThat(fakeReminderScheduler.cancelledIds).contains("r1")
        }

    @Test
    fun `onDeleteDismissed hides delete confirm dialog`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val vm = newReminderVm()
            vm.state.test {
                val initial = awaitItem()
                assertThat(initial.isDeleteConfirmVisible).isFalse()
                vm.onAction(AgendaDetailAction.OnDeleteClicked)
                assertThat(awaitItem().isDeleteConfirmVisible).isTrue()
                vm.onAction(AgendaDetailAction.OnDeleteDismissed)
                assertThat(awaitItem().isDeleteConfirmVisible).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `onTitleClicked emits NavigateToEditTitle with current title`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val vm = newReminderVm()
            vm.onAction(AgendaDetailAction.OnTitleEdited("My reminder"))
            vm.events.test {
                vm.onAction(AgendaDetailAction.OnTitleClicked)
                val event = awaitItem() as AgendaDetailEvent.NavigateToEditTitle
                assertThat(event.currentTitle).isEqualTo("My reminder")
            }
        }

    @Test
    fun `reminder save survives viewModelScope cancellation`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val vm = newReminderVm()
            vm.onAction(AgendaDetailAction.OnTitleEdited("Survived"))
            vm.onAction(AgendaDetailAction.OnSaveClicked)
            vm.viewModelScope.cancel()
            advanceUntilIdle()
            assertThat(fakeReminderRepo.reminders.first().title).isEqualTo("Survived")
        }

    @Test
    fun `reminder delete survives viewModelScope cancellation`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeReminderRepo.upsertReminder(reminder("r1", fixedInstant))
            val vm = existingReminderVm()
            vm.state.test { skipItems(1); awaitItem(); cancelAndIgnoreRemainingEvents() }
            vm.onAction(AgendaDetailAction.OnDeleteConfirmed)
            vm.viewModelScope.cancel()
            advanceUntilIdle()
            assertThat(fakeReminderRepo.reminders).isEmpty()
        }

    // ── Task ────────────────────────────────────────────────────────────────────────

    @Test
    fun `new task starts in edit mode with isDone false`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            newTaskVm().state.test {
                val initial = awaitItem()
                assertThat(initial.isNew).isTrue()
                assertThat(initial.isEditMode).isTrue()
                assertThat((initial.details as AgendaItemDetails.Task).isDone).isFalse()
                assertThat(initial.dateTime.date).isEqualTo(LocalDate(2026, 5, 6))
                assertThat(initial.dateTime.time.hour).isEqualTo(9)
                assertThat(initial.dateTime.time.minute).isEqualTo(0)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `loadTask populates title, description and isDone from repository`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeTaskRepo.upsertTask(
                task(id = "t1", time = fixedInstant).copy(
                    title = "Buy groceries", description = "Milk and eggs", isDone = true,
                )
            )
            existingTaskVm().state.test {
                skipItems(1)
                val loaded = awaitItem()
                assertThat(loaded.title).isEqualTo("Buy groceries")
                assertThat(loaded.description).isEqualTo("Milk and eggs")
                assertThat((loaded.details as AgendaItemDetails.Task).isDone).isTrue()
                assertThat(loaded.isEditMode).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `onSaveClicked creates task in repo and emits NavigateBack`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val vm = newTaskVm()
            vm.onAction(AgendaDetailAction.OnTitleEdited("My new task"))
            vm.events.test {
                vm.onAction(AgendaDetailAction.OnSaveClicked)
                assertThat(awaitItem()).isInstanceOf(AgendaDetailEvent.NavigateBack::class)
            }
            assertThat(fakeTaskRepo.tasks.first().title).isEqualTo("My new task")
        }

    @Test
    fun `onSaveClicked updates existing task with changed title`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeTaskRepo.upsertTask(task("t1", fixedInstant).copy(title = "Old title"))
            val vm = existingTaskVm()
            vm.state.test { skipItems(1); awaitItem(); cancelAndIgnoreRemainingEvents() }
            vm.onAction(AgendaDetailAction.OnTitleEdited("New title"))
            vm.events.test {
                vm.onAction(AgendaDetailAction.OnSaveClicked)
                assertThat(awaitItem()).isInstanceOf(AgendaDetailEvent.NavigateBack::class)
            }
            assertThat(fakeTaskRepo.tasks.find { it.id == "t1" }?.title).isEqualTo("New title")
        }

    @Test
    fun `onDeleteConfirmed removes task and emits NavigateBack`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeTaskRepo.upsertTask(task("t1", fixedInstant))
            val vm = existingTaskVm()
            vm.state.test { skipItems(1); awaitItem(); cancelAndIgnoreRemainingEvents() }
            vm.events.test {
                vm.onAction(AgendaDetailAction.OnDeleteConfirmed)
                assertThat(awaitItem()).isInstanceOf(AgendaDetailEvent.NavigateBack::class)
            }
            assertThat(fakeTaskRepo.tasks).isEmpty()
        }

    @Test
    fun `task save survives viewModelScope cancellation`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val vm = newTaskVm()
            vm.onAction(AgendaDetailAction.OnTitleEdited("Survived"))
            vm.onAction(AgendaDetailAction.OnSaveClicked)
            vm.viewModelScope.cancel()
            advanceUntilIdle()
            assertThat(fakeTaskRepo.tasks.first().title).isEqualTo("Survived")
        }

    // ── Event ─────────────────────────────────────────────────────────────────────

    @Test
    fun `new event starts in edit mode after session loads currentUserId`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            newEventVm().state.test {
                skipItems(1) // stateIn initial value
                val loaded = awaitItem() // onStart: session loaded → currentUserId set
                assertThat(loaded.isNew).isTrue()
                assertThat(loaded.isEditMode).isTrue()
                assertThat(loaded.title).isEqualTo("")
                assertThat(eventDetails(loaded).currentUserId).isEqualTo("user1")
                // Regression: a new event's creator is the current user, so hostId is seeded to the
                // current user → isUserCreator (hostId == currentUserId) is true and the screen's
                // edit affordances (dates, attendees, photos, save, delete) are enabled.
                assertThat(eventDetails(loaded).hostId).isEqualTo("user1")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `loadEvent populates title, description and marks user as creator`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeEventRepo.upsertEvent(event())
            existingEventVm().state.test {
                skipItems(1)
                var loaded = awaitItem()
                while (loaded.title.isEmpty()) loaded = awaitItem()
                assertThat(loaded.title).isEqualTo("Meeting")
                assertThat(loaded.description).isEqualTo("Discuss")
                val d = eventDetails(loaded)
                assertThat(d.hostId == d.currentUserId).isTrue() // isUserCreator
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `onSaveClicked creates event in repo and emits NavigateBack`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val vm = newEventVm()
            vm.state.test { skipItems(1); awaitItem(); cancelAndIgnoreRemainingEvents() }
            vm.onAction(AgendaDetailAction.OnTitleEdited("Kickoff"))
            vm.events.test {
                vm.onAction(AgendaDetailAction.OnSaveClicked)
                assertThat(awaitItem()).isInstanceOf(AgendaDetailEvent.NavigateBack::class)
            }
            assertThat(fakeEventRepo.events.first().title).isEqualTo("Kickoff")
        }

    @Test
    fun `onDeleteConfirmed removes event and emits NavigateBack`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeEventRepo.upsertEvent(event())
            val vm = existingEventVm()
            vm.state.test { skipItems(1); awaitItem(); cancelAndIgnoreRemainingEvents() }
            vm.events.test {
                vm.onAction(AgendaDetailAction.OnDeleteConfirmed)
                assertThat(awaitItem()).isInstanceOf(AgendaDetailEvent.NavigateBack::class)
            }
            assertThat(fakeEventRepo.events).isEmpty()
        }

    @Test
    fun `onToDateChanged updates toTime date`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val vm = newEventVm()
            vm.state.test {
                skipItems(1); awaitItem()
                vm.onAction(AgendaDetailAction.OnToDateChanged(LocalDate(2027, 6, 16)))
                assertThat(eventDetails(awaitItem()).toTime.date).isEqualTo(LocalDate(2027, 6, 16))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `onAttendeeAddConfirmed adds attendee and flags success when the email resolves`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeAttendeeRepository.attendee = Attendee(
                email = "bob@example.com", username = "Bob Stone", userId = "u2",
                eventId = "", isGoing = true, remindAt = Instant.fromEpochMilliseconds(0),
            )
            val vm = newEventVm()
            vm.state.test {
                skipItems(1); awaitItem()
                vm.onAction(AgendaDetailAction.OnAttendeeEmailChanged("bob@example.com"))
                awaitItem()
                vm.onAction(AgendaDetailAction.OnAttendeeAddConfirmed)
                var loaded = awaitItem()
                while (eventDetails(loaded).attendees.isEmpty()) loaded = awaitItem()
                assertThat(eventDetails(loaded).attendees.size).isEqualTo(1) // real invited list (no creator)
                // Assert *who* was added, not just the count — the resolved attendee's id/name must
                // survive the lookup → add flow, otherwise the wrong person could be invited silently.
                val added = eventDetails(loaded).attendees.first()
                assertThat(added.userId).isEqualTo("u2")
                assertThat(added.fullName).isEqualTo("Bob Stone")
                // goingAttendees also includes the synthesised creator row → creator + added = 2.
                assertThat(eventDetails(loaded).goingAttendees.size).isEqualTo(2)
                assertThat(eventDetails(loaded).goingAttendees.first().isCreator).isTrue()
                // Sheet stays in composition; success is flagged so it can animate itself out.
                assertThat(eventDetails(loaded).isAttendeeAdded).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `onAttendeeEmailChanged marks a well-formed email valid`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val vm = newEventVm()
            vm.state.test {
                skipItems(1); awaitItem()
                vm.onAction(AgendaDetailAction.OnAttendeeEmailChanged("nope"))
                assertThat(eventDetails(awaitItem()).isAttendeeEmailValid).isFalse()
                vm.onAction(AgendaDetailAction.OnAttendeeEmailChanged("good@example.com"))
                assertThat(eventDetails(awaitItem()).isAttendeeEmailValid).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `onAttendeeAddConfirmed shows error when the email does not resolve`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeAttendeeRepository.attendee = null
            val vm = newEventVm()
            vm.state.test {
                skipItems(1); awaitItem()
                vm.onAction(AgendaDetailAction.OnAttendeeEmailChanged("ghost@example.com"))
                awaitItem()
                vm.onAction(AgendaDetailAction.OnAttendeeAddConfirmed)
                var loaded = awaitItem()
                while (eventDetails(loaded).attendeeEmailError == null) loaded = awaitItem()
                assertThat(eventDetails(loaded).attendeeEmailError).isEqualTo("This email does not exist")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `onLocalPhotoAdded respects the 10-photo limit`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val vm = newEventVm()
            vm.state.test {
                skipItems(1); awaitItem()
                repeat(10) { i ->
                    vm.onAction(AgendaDetailAction.OnLocalPhotoAdded("content://p$i")); awaitItem()
                }
                cancelAndIgnoreRemainingEvents()
            }
            // The 11th pick is not added AND the user is told one photo was skipped.
            vm.events.test {
                vm.onAction(AgendaDetailAction.OnLocalPhotoAdded("content://overflow"))
                assertThat(awaitItem()).isEqualTo(AgendaDetailEvent.ShowSkippedPhotosMessage(1))
            }
            assertThat(eventDetails(vm.state.value).localPhotoUris.size).isEqualTo(10)
        }

    @Test
    fun `event save survives viewModelScope cancellation`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val vm = newEventVm()
            vm.state.test { skipItems(1); awaitItem(); cancelAndIgnoreRemainingEvents() }
            vm.onAction(AgendaDetailAction.OnTitleEdited("Survived"))
            vm.onAction(AgendaDetailAction.OnSaveClicked)
            vm.viewModelScope.cancel()
            advanceUntilIdle()
            assertThat(fakeEventRepo.events.first().title).isEqualTo("Survived")
        }

    @Test
    fun `event delete survives viewModelScope cancellation`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeEventRepo.upsertEvent(event())
            val vm = existingEventVm()
            vm.state.test { skipItems(1); awaitItem(); cancelAndIgnoreRemainingEvents() }
            vm.onAction(AgendaDetailAction.OnDeleteConfirmed)
            vm.viewModelScope.cancel()
            advanceUntilIdle()
            assertThat(fakeEventRepo.events).isEmpty()
        }

    // A non-creator attendee tapping JOIN / LEAVE flips their own isGoing and PUTs it to the repo.
    // Seeding the attendee as currently going means tapping LEAVE writes isGoing=false — asserting
    // false (not the fake's default of true) proves the flipped value actually reached the repository.
    @Test
    fun `join or leave event persists isGoing to the repository`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeEventRepo.upsertEvent(
                event(hostId = "host99").copy(
                    attendees = listOf(
                        Attendee(
                            email = "alice@example.com", username = "Alice Bob", userId = "user1",
                            eventId = "e1", isGoing = true, remindAt = Instant.fromEpochMilliseconds(0),
                        ),
                    ),
                ),
            )
            val vm = existingEventVm()
            vm.state.test {
                skipItems(1)
                var loaded = awaitItem()
                while (eventDetails(loaded).attendees.isEmpty()) loaded = awaitItem()
                cancelAndIgnoreRemainingEvents()
            }
            vm.onAction(AgendaDetailAction.OnJoinOrLeaveEvent)
            advanceUntilIdle()
            assertThat(fakeEventRepo.lastUpdateIsGoing).isFalse()
        }

    // Regression for "invited user can't see the event": the added attendee's id must reach the repo
    // (→ create_event_request.attendeeIds) so the server registers them and they receive the event.
    @Test
    fun `creating an event forwards added attendee ids to the repository`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeAttendeeRepository.attendee = Attendee(
                email = "bob@example.com", username = "Bob Stone", userId = "u2",
                eventId = "", isGoing = true, remindAt = Instant.fromEpochMilliseconds(0),
            )
            val vm = newEventVm()
            vm.state.test { skipItems(1); awaitItem(); cancelAndIgnoreRemainingEvents() }
            vm.onAction(AgendaDetailAction.OnTitleEdited("Kickoff"))
            vm.onAction(AgendaDetailAction.OnAttendeeEmailChanged("bob@example.com"))
            vm.onAction(AgendaDetailAction.OnAttendeeAddConfirmed)
            advanceUntilIdle()
            vm.onAction(AgendaDetailAction.OnSaveClicked)
            advanceUntilIdle()
            val saved = fakeEventRepo.events.first { it.title == "Kickoff" }
            assertThat(saved.attendees.map { it.userId }).contains("u2")
        }

    // Regression for "photos disappear after restart": a pick is cached, forwarded to the repo for
    // upload, AND persisted on the saved event so view mode renders it offline / after a restart.
    @Test
    fun `a locally added photo is cached, uploaded and persisted on the event`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val vm = newEventVm()
            vm.state.test { skipItems(1); awaitItem(); cancelAndIgnoreRemainingEvents() }
            vm.onAction(AgendaDetailAction.OnTitleEdited("With photo"))
            vm.onAction(AgendaDetailAction.OnLocalPhotoAdded("content://pick/1"))
            advanceUntilIdle()
            vm.onAction(AgendaDetailAction.OnSaveClicked)
            advanceUntilIdle()
            assertThat(fakeEventRepo.lastCreateLocalPhotoUris).contains("content://pick/1")
            val saved = fakeEventRepo.events.first { it.title == "With photo" }
            assertThat(saved.photos.map { it.url }).contains("content://pick/1")
        }

    @Test
    fun `removing a remote photo while editing queues it in deletedPhotoKeys on update`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeEventRepo.upsertEvent(
                event().copy(photos = listOf(Photo(key = "remote-1", url = "https://cdn/remote-1.jpg"))),
            )
            val vm = existingEventVm()
            vm.state.test {
                skipItems(1)
                var loaded = awaitItem()
                while (eventDetails(loaded).photos.isEmpty()) loaded = awaitItem()
                cancelAndIgnoreRemainingEvents()
            }
            vm.onAction(AgendaDetailAction.OnEditClicked)
            vm.onAction(
                AgendaDetailAction.OnDeletePhotoClicked(PhotoUi(key = "remote-1", url = "https://cdn/remote-1.jpg")),
            )
            vm.onAction(AgendaDetailAction.OnSaveClicked)
            advanceUntilIdle()
            assertThat(fakeEventRepo.lastUpdateDeletedPhotoKeys).contains("remote-1")
        }

    @Test
    fun `removing a local pick frees the cached file and does not queue a deleted key`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val vm = newEventVm()
            vm.state.test { skipItems(1); awaitItem(); cancelAndIgnoreRemainingEvents() }
            vm.onAction(AgendaDetailAction.OnLocalPhotoAdded("content://pick/9"))
            advanceUntilIdle()
            vm.onAction(
                AgendaDetailAction.OnDeletePhotoClicked(PhotoUi(key = "content://pick/9", url = "content://pick/9")),
            )
            advanceUntilIdle()
            assertThat(fakeLocalPhotoStorage.deleted).contains("content://pick/9")
            assertThat(eventDetails(vm.state.value).deletedPhotoKeys).isEmpty()
        }
}

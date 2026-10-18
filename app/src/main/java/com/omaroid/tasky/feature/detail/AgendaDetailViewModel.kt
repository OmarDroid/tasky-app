package com.omaroid.tasky.feature.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.omaroid.tasky.app.navigation.AgendaDetailRoute
import com.omaroid.tasky.core.domain.agenda.models.AgendaItem
import com.omaroid.tasky.core.domain.agenda.repository.AttendeeRepository
import com.omaroid.tasky.core.domain.agenda.repository.EventRepository
import com.omaroid.tasky.core.domain.agenda.repository.ReminderRepository
import com.omaroid.tasky.core.domain.agenda.repository.TaskRepository
import com.omaroid.tasky.core.domain.connectivity.ConnectivityMonitor
import com.omaroid.tasky.core.domain.notification.ReminderScheduler
import com.omaroid.tasky.core.domain.photo.LocalPhotoStorage
import com.omaroid.tasky.core.domain.session.SessionLocalDataSource
import com.omaroid.tasky.core.domain.validation.EmailValidator
import com.omaroid.tasky.core.presentation.util.InitialsFormatter
import com.omaroid.tasky.feature.detail.model.AttendeeUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

/**
 * Single detail ViewModel for every agenda item type. The route carries an [AgendaDetailType] so
 * one ViewModel + one screen serve Task, Reminder, and Event. Shared concerns (title, description,
 * "from" date/time, reminder offset) live on [AgendaDetailState]; only [AgendaItemDetails] decides
 * which domain model is built and which repository persists it. Event-only concerns (attendees,
 * photos, "to" time, join/leave) are handled in the event branches — no separate Event ViewModel.
 */
class AgendaDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
    private val reminderRepository: ReminderRepository,
    private val eventRepository: EventRepository,
    private val sessionLocalDataSource: SessionLocalDataSource,
    private val attendeeRepository: AttendeeRepository,
    private val localPhotoStorage: LocalPhotoStorage,
    private val emailValidator: EmailValidator,
    private val connectivityMonitor: ConnectivityMonitor,
    private val reminderScheduler: ReminderScheduler,
    private val applicationScope: CoroutineScope,
    private val clock: Clock = Clock.System,
) : ViewModel() {

    private val zone = TimeZone.currentSystemDefault()
    private val route = savedStateHandle.toRoute<AgendaDetailRoute>()
    private val type = route.type
    private val itemId = route.itemId
    private val isEditing = route.isEditing
    // Seeds a NEW item's date to the day the user had selected in the agenda (falls back to today
    // when absent — e.g. a deep link). An EXISTING item's real dateTime overwrites this once
    // load(itemId) resolves in onStart. Time-of-day still defaults to "now" either way.
    private val initialDate = route.initialDateEpochDay
        ?.let { LocalDate.fromEpochDays(it.toInt()) } ?: clock.todayIn(zone)
    private val defaultDateTime = LocalDateTime(initialDate, clock.now().toLocalDateTime(zone).time)
    private var hasLoadedInitialData = false
    private var currentUsername = ""

    private val initialState = AgendaDetailState(
        dateTime = defaultDateTime,
        details = initialDetailsFor(type),
        isNew = itemId == null,
        isEditMode = isEditing,
    )
    private val _state = MutableStateFlow(initialState)
    val state = _state
        .onStart {
            if (!hasLoadedInitialData) {
                hasLoadedInitialData = true
                // Events need the current user id/name to derive creator/attendee role and to show
                // the creator as a "Going" visitor.
                val session = sessionLocalDataSource.getSession()
                currentUsername = session?.username ?: ""
                _state.update { st ->
                    val d = st.details
                    if (d is AgendaItemDetails.Event) {
                        val uid = session?.userId ?: ""
                        // A new event's creator is the current user — seed hostId so isUserCreator
                        // (hostId == currentUserId) is true and the edit affordances are enabled.
                        // The server assigns the real host on create, so this local value is safe.
                        st.copy(
                            details = d.copy(
                                currentUserId = uid,
                                hostId = if (st.isNew) uid else d.hostId,
                            ).withGroups(),
                        )
                    } else st
                }
                // Fold connectivity into the event details so the UI hides the attendee/photo `+`
                // and shows the offline notice (replaces the hard-coded isOnline = true placeholder).
                connectivityMonitor.isOnline
                    .onEach { online ->
                        _state.update { st ->
                            val d = st.details
                            if (d is AgendaItemDetails.Event) st.copy(details = d.copy(isOnline = online))
                            else st
                        }
                    }
                    .launchIn(viewModelScope)
                if (itemId != null) load(itemId)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = initialState,
        )

    private val _events = Channel<AgendaDetailEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: AgendaDetailAction) {
        when (action) {
            AgendaDetailAction.OnCloseClicked -> onCloseClicked()
            is AgendaDetailAction.OnDateChanged -> onDateChanged(action.date)
            AgendaDetailAction.OnDateClicked -> _state.update { it.copy(isDatePickerVisible = true) }
            AgendaDetailAction.OnDatePickerDismissed -> _state.update { it.copy(isDatePickerVisible = false) }
            AgendaDetailAction.OnDeleteClicked -> _state.update { it.copy(isDeleteConfirmVisible = true) }
            AgendaDetailAction.OnDeleteConfirmed -> onDeleteConfirmed()
            AgendaDetailAction.OnDeleteDismissed -> _state.update { it.copy(isDeleteConfirmVisible = false) }
            AgendaDetailAction.OnDescriptionClicked -> onDescriptionClicked()
            is AgendaDetailAction.OnDescriptionEdited -> _state.update { it.copy(description = action.description) }
            AgendaDetailAction.OnEditClicked -> _state.update { it.copy(isEditMode = true) }
            is AgendaDetailAction.OnReminderChanged -> _state.update { it.copy(reminderType = action.type) }
            AgendaDetailAction.OnSaveClicked -> onSaveClicked()
            is AgendaDetailAction.OnTimeChanged -> onTimeChanged(action.time)
            AgendaDetailAction.OnTimeClicked -> _state.update { it.copy(isTimePickerVisible = true) }
            AgendaDetailAction.OnTimePickerDismissed -> _state.update { it.copy(isTimePickerVisible = false) }
            AgendaDetailAction.OnTitleClicked -> onTitleClicked()
            is AgendaDetailAction.OnTitleEdited -> _state.update { it.copy(title = action.title) }

            // ── Event: "to" date & time ────────────────────────────────────────
            AgendaDetailAction.OnToDateClicked -> updateEventDetails { copy(isToDatePickerVisible = true) }
            AgendaDetailAction.OnToTimeClicked -> updateEventDetails { copy(isToTimePickerVisible = true) }
            AgendaDetailAction.OnToDatePickerDismissed -> updateEventDetails { copy(isToDatePickerVisible = false) }
            AgendaDetailAction.OnToTimePickerDismissed -> updateEventDetails { copy(isToTimePickerVisible = false) }
            is AgendaDetailAction.OnToDateChanged ->
                updateEventDetails { copy(toTime = LocalDateTime(action.date, toTime.time)) }
            is AgendaDetailAction.OnToTimeChanged ->
                updateEventDetails { copy(toTime = LocalDateTime(toTime.date, action.time)) }

            // ── Event: attendees ───────────────────────────────────────────────
            AgendaDetailAction.OnAddAttendeeClicked -> updateEventDetails { copy(isAddAttendeeSheetVisible = true) }
            is AgendaDetailAction.OnAttendeeEmailChanged ->
                updateEventDetails {
                    copy(
                        attendeeEmailInput = action.email,
                        attendeeEmailError = null, // typing clears any displayed error
                        isAttendeeEmailValid = emailValidator(action.email),
                    )
                }
            AgendaDetailAction.OnAttendeeAddConfirmed -> onAttendeeAddConfirmed()
            AgendaDetailAction.OnAddAttendeeDismissed ->
                updateEventDetails {
                    copy(
                        isAddAttendeeSheetVisible = false,
                        isAttendeeAdded = false,
                        attendeeEmailInput = "",
                        attendeeEmailError = null,
                        isAttendeeEmailValid = false,
                    )
                }
            is AgendaDetailAction.OnRemoveAttendeeClicked ->
                updateEventDetails { copy(attendees = attendees.filter { a -> a.userId != action.userId }).withGroups() }
            is AgendaDetailAction.OnAttendeeFilterChanged ->
                updateEventDetails { copy(attendeeFilter = action.filter) }
            AgendaDetailAction.OnJoinOrLeaveEvent -> onJoinOrLeaveEvent()

            // ── Event: photos ──────────────────────────────────────────────────
            AgendaDetailAction.OnAddPhotoClicked ->
                viewModelScope.launch { _events.send(AgendaDetailEvent.OpenPhotoPicker) }
            is AgendaDetailAction.OnLocalPhotoAdded -> onLocalPhotoAdded(action.uri)
            is AgendaDetailAction.OnPhotoClicked -> onPhotoClicked(action)
            is AgendaDetailAction.OnDeletePhotoClicked -> onPhotoRemoved(action.photo.key)
            is AgendaDetailAction.OnDeletedPhotoKeyReceived -> onPhotoRemoved(action.photoKey)
        }
    }

    private fun onTitleClicked() {
        viewModelScope.launch { _events.send(AgendaDetailEvent.NavigateToEditTitle(_state.value.title)) }
    }

    private fun onDescriptionClicked() {
        viewModelScope.launch { _events.send(AgendaDetailEvent.NavigateToEditDescription(_state.value.description)) }
    }

    private fun onDateChanged(date: LocalDate) {
        _state.update { it.copy(dateTime = LocalDateTime(date, it.dateTime.time)) }
    }

    private fun onTimeChanged(time: LocalTime) {
        _state.update { it.copy(dateTime = LocalDateTime(it.dateTime.date, time)) }
    }

    private fun onCloseClicked() {
        viewModelScope.launch { _events.send(AgendaDetailEvent.NavigateBack) }
    }

    // The write runs on applicationScope so it survives the screen leaving composition before it
    // finishes. The NavigateBack send stays on viewModelScope: the events Channel is rendezvous, so
    // a send left on applicationScope would suspend forever once the collector is gone — leaking the
    // ViewModel. join() keeps navigation ordered after the write.
    private fun onSaveClicked() {
        val state = _state.value
        val isNew = state.isNew
        val item = state.toAgendaItem()
        val eventDetails = state.details as? AgendaItemDetails.Event
        val localPhotoUris = eventDetails?.localPhotoUris ?: emptyList()
        val deletedPhotoKeys = eventDetails?.deletedPhotoKeys ?: emptyList()
        val saveJob = applicationScope.launch {
            when (item) {
                is AgendaItem.Task ->
                    if (isNew) taskRepository.createTask(item) else taskRepository.updateTask(item)
                is AgendaItem.Reminder ->
                    if (isNew) reminderRepository.createReminder(item) else reminderRepository.updateReminder(item)
                is AgendaItem.Event ->
                    if (isNew) eventRepository.createEvent(item, localPhotoUris)
                    // Only the creator can reach Save (attendees are view-only), so isGoing = true.
                    else eventRepository.updateEvent(item, localPhotoUris, deletedPhotoKeys, isGoing = true)
            }
            reminderScheduler.schedule(item) // all three types; a past remindAt no-ops
        }
        viewModelScope.launch {
            saveJob.join()
            _events.send(AgendaDetailEvent.NavigateBack)
        }
    }

    private fun onDeleteConfirmed() {
        val state = _state.value
        val id = state.id
        val eventDetails = state.details as? AgendaItemDetails.Event
        val isAttendeeLeaving = eventDetails != null && eventDetails.hostId != eventDetails.currentUserId
        val deleteJob = applicationScope.launch {
            when (type) {
                AgendaDetailType.TASK -> taskRepository.deleteTask(id)
                AgendaDetailType.REMINDER -> reminderRepository.deleteReminder(id)
                // Creator deletes the event; an attendee removes the event from their own agenda.
                AgendaDetailType.EVENT ->
                    if (isAttendeeLeaving) attendeeRepository.removeAttendee(id) else eventRepository.deleteEvent(id)
            }
            // Covers the attendee-leave branch too — after leaving an event, no notifications for it.
            reminderScheduler.cancel(id)
        }
        viewModelScope.launch {
            deleteJob.join()
            _events.send(AgendaDetailEvent.NavigateBack)
        }
    }

    private fun onAttendeeAddConfirmed() {
        viewModelScope.launch {
            updateEventDetails { copy(isCheckingAttendee = true) }
            val attendee = attendeeRepository.getAttendee(eventDetails().attendeeEmailInput)
            if (attendee == null) {
                updateEventDetails { copy(isCheckingAttendee = false, attendeeEmailError = "This email does not exist") }
                return@launch
            }
            val alreadyAdded = eventDetails().attendees.any { it.userId == attendee.userId }
            updateEventDetails {
                val updated = if (alreadyAdded) {
                    attendees
                } else {
                    attendees + AttendeeUi(
                        userId = attendee.userId,
                        fullName = attendee.username,
                        initials = InitialsFormatter.format(attendee.username),
                        email = attendee.email,
                        isGoing = true,
                        isCreator = false,
                    )
                }
                // Keep the sheet in composition and flag success — the sheet animates itself out
                // (LaunchedEffect on isAttendeeAdded → sheetState.hide() → OnAddAttendeeDismissed).
                copy(
                    attendees = updated,
                    isCheckingAttendee = false,
                    isAttendeeAdded = true,
                ).withGroups()
            }
        }
    }

    private fun onJoinOrLeaveEvent() {
        val d = eventDetails()
        val userId = d.currentUserId
        if (d.hostId == userId) return // creators are always going and cannot leave their own event
        val newGoing = !d.attendees.any { it.userId == userId && it.isGoing }
        // Optimistic local toggle so the UI reflects the change immediately.
        updateEventDetails {
            copy(attendees = attendees.map { a -> if (a.userId == userId) a.copy(isGoing = newGoing) else a }).withGroups()
        }
        // Join/leave is a PUT /event that flips the attendee's own isGoing (per the API: attendees can
        // only update isGoing + remindAt). Persist on applicationScope so it survives navigation.
        val snapshot = _state.value
        applicationScope.launch {
            try {
                val domainEvent = snapshot.toAgendaItem() as AgendaItem.Event
                eventRepository.updateEvent(
                    domainEvent,
                    localPhotoUris = emptyList(),
                    deletedPhotoKeys = emptyList(),
                    isGoing = newGoing,
                )
            } catch (_: Exception) { /* best-effort; local state already reflects the toggle */ }
        }
    }

    private fun onLocalPhotoAdded(uri: String) {
        val d = eventDetails()
        if (d.photos.size + d.localPhotoUris.size >= 10) {
            viewModelScope.launch { _events.send(AgendaDetailEvent.ShowSkippedPhotosMessage(1)) }
            return
        }
        // Cache the pick into app storage first so it survives process death and stays readable for
        // the upload; only then add the stable file:// uri to the displayed list.
        viewModelScope.launch {
            val cached = localPhotoStorage.cache(uri) ?: return@launch
            updateEventDetails { copy(localPhotoUris = localPhotoUris + cached) }
        }
    }

    // A removed cell is either a remote photo (key matches a loaded PhotoUi → queue it in
    // deletedPhotoKeys so PUT /event deletes it) or a local pick (a cached file:// uri → free it).
    private fun onPhotoRemoved(key: String) {
        val wasRemote = eventDetails().photos.any { it.key == key }
        if (!wasRemote) applicationScope.launch { localPhotoStorage.delete(key) }
        updateEventDetails {
            copy(
                photos = photos.filter { it.key != key },
                localPhotoUris = localPhotoUris.filter { it != key },
                deletedPhotoKeys = if (wasRemote) deletedPhotoKeys + key else deletedPhotoKeys,
            )
        }
    }

    private fun onPhotoClicked(action: AgendaDetailAction.OnPhotoClicked) {
        val d = eventDetails()
        val isCreator = d.hostId == d.currentUserId
        viewModelScope.launch {
            _events.send(
                AgendaDetailEvent.NavigateToPhotoDetail(
                    action.photo.key, action.photo.url, _state.value.id, isCreator,
                )
            )
        }
    }

    private fun load(id: String) {
        viewModelScope.launch {
            val source = when (type) {
                AgendaDetailType.TASK -> taskRepository.observeTask(id)
                AgendaDetailType.REMINDER -> reminderRepository.observeReminder(id)
                AgendaDetailType.EVENT -> eventRepository.observeEvent(id)
            }
            source.filterNotNull().first().let { item ->
                _state.update { current ->
                    val loaded = item.toDetailState().copy(isEditMode = isEditing)
                    val loadedDetails = loaded.details
                    val currentDetails = current.details
                    if (loadedDetails is AgendaItemDetails.Event && currentDetails is AgendaItemDetails.Event) {
                        // toDetailState() rebuilds Event details with isOnline defaulting to true; carry the
                        // live connectivity value across the load so offline gating isn't reset to online
                        // (the isOnline StateFlow already emitted and won't re-emit to correct it).
                        loaded.copy(
                            details = loadedDetails.copy(
                                currentUserId = currentDetails.currentUserId,
                                isOnline = connectivityMonitor.isOnline.value,
                            ).withGroups(),
                        )
                    } else {
                        loaded
                    }
                }
            }
        }
    }

    private fun initialDetailsFor(type: AgendaDetailType): AgendaItemDetails = when (type) {
        AgendaDetailType.TASK -> AgendaItemDetails.Task(isDone = false)
        AgendaDetailType.REMINDER -> AgendaItemDetails.Reminder
        AgendaDetailType.EVENT -> AgendaItemDetails.Event(
            toTime = (clock.now() + 1.hours).toLocalDateTime(zone),
        )
    }

    private fun eventDetails() = _state.value.details as AgendaItemDetails.Event

    private fun updateEventDetails(block: AgendaItemDetails.Event.() -> AgendaItemDetails.Event) {
        _state.update {
            val d = it.details
            if (d is AgendaItemDetails.Event) it.copy(details = d.block()) else it
        }
    }

    // Re-derive the Going / Not going groups the Visitors section renders. The creator is shown as a
    // "Going" row (with the CREATOR badge) even though they are not stored in `attendees` — the
    // current user is synthesised from the session when they are the host. Partition logic lives
    // here in the ViewModel, never in a composable.
    private fun AgendaItemDetails.Event.withGroups(): AgendaItemDetails.Event {
        val going = attendees.filter { it.isGoing }
        val notGoing = attendees.filter { !it.isGoing }
        val needsCreatorRow = currentUserId.isNotEmpty() && currentUserId == hostId && going.none { it.isCreator }
        val goingWithCreator = if (needsCreatorRow) {
            listOf(
                AttendeeUi(
                    userId = currentUserId,
                    fullName = currentUsername,
                    initials = InitialsFormatter.format(currentUsername),
                    email = "",
                    isGoing = true,
                    isCreator = true,
                ),
            ) + going
        } else {
            going
        }
        return copy(goingAttendees = goingWithCreator, notGoingAttendees = notGoing)
    }
}

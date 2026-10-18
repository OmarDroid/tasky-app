package com.omaroid.tasky.feature.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omaroid.tasky.R
import com.omaroid.tasky.core.domain.agenda.repository.AgendaSyncRepository
import com.omaroid.tasky.core.domain.agenda.repository.EventRepository
import com.omaroid.tasky.core.domain.agenda.repository.ReminderRepository
import com.omaroid.tasky.core.domain.agenda.repository.TaskRepository
import com.omaroid.tasky.core.domain.auth.AuthRepository
import com.omaroid.tasky.core.domain.connectivity.ConnectivityMonitor
import com.omaroid.tasky.core.domain.notification.ReminderScheduler
import com.omaroid.tasky.core.domain.preferences.ThemePreferenceStorage
import com.omaroid.tasky.core.domain.session.SessionLocalDataSource
import com.omaroid.tasky.core.presentation.util.InitialsFormatter
import com.omaroid.tasky.core.presentation.util.UiText
import com.omaroid.tasky.feature.agenda.model.AgendaItemUi
import com.omaroid.tasky.feature.agenda.model.buildAgendaListEntries
import com.omaroid.tasky.feature.agenda.model.toUi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

class AgendaViewModel(
    private val taskRepository: TaskRepository,
    private val reminderRepository: ReminderRepository,
    private val eventRepository: EventRepository,
    private val agendaSyncRepository: AgendaSyncRepository,
    private val authRepository: AuthRepository,
    private val sessionLocalDataSource: SessionLocalDataSource,
    private val connectivityMonitor: ConnectivityMonitor,
    private val reminderScheduler: ReminderScheduler,
    private val themePreferenceStorage: ThemePreferenceStorage,
    private val applicationScope: CoroutineScope,
    private val clock: Clock = Clock.System,
) : ViewModel() {

    private val today = clock.todayIn(TimeZone.currentSystemDefault())

    private var hasLoadedInitialData = false
    private var currentUserId: String? = null

    private val _state = MutableStateFlow(AgendaState(today = today))
    private val _events = Channel<AgendaEvent>()
    val events = _events.receiveAsFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val state = _state
        .map { it.selectedDate }
        .distinctUntilChanged()
        .flatMapLatest { date ->
            val zone = TimeZone.currentSystemDefault()
            val startMs = date.atStartOfDayIn(zone).toEpochMilliseconds()
            val endMs = date.plus(1, DateTimeUnit.DAY).atStartOfDayIn(zone).toEpochMilliseconds()
            combine(
                taskRepository.observeTasksForDay(startMs, endMs),
                reminderRepository.observeRemindersForDay(startMs, endMs),
                eventRepository.observeEventsForDay(startMs, endMs),
            ) { tasks, reminders, events ->
                (tasks + reminders + events).map { it.toUi(currentUserId) }
            }
        }
        .combine(_state) { items, s ->
            val zone = TimeZone.currentSystemDefault()
            val needle = if (s.selectedDate == today) clock.now().toLocalDateTime(zone).time else null
            s.copy(
                needleTime = needle,
                entries = buildAgendaListEntries(items, needle),
            )
        }
        .onStart {
            if (!hasLoadedInitialData) {
                hasLoadedInitialData = true
                loadUsername()
                connectivityMonitor.isOnline
                    .onEach { isOnline -> _state.update { it.copy(isOnline = isOnline) } }
                    .launchIn(viewModelScope)
                themePreferenceStorage.theme
                    .onEach { theme -> _state.update { it.copy(theme = theme) } }
                    .launchIn(viewModelScope)
            }
            fetchAndSync()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue = AgendaState(today = today),
        )

    private suspend fun loadUsername() {
        val session = sessionLocalDataSource.getSession()
        currentUserId = session?.userId
        val username = session?.username ?: ""
        _state.update {
            it.copy(
                username = username,
                userInitials = InitialsFormatter.format(username),
            )
        }
    }

    private fun fetchAndSync() {
        viewModelScope.launch {
            try {
                agendaSyncRepository.syncAll()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                /* offline — Room data remains */
            }
        }
    }

    fun onAction(action: AgendaAction) {
        when (action) {
            is AgendaAction.OnDateSelected -> {
                val label = if (action.date == today) {
                    UiText.StringResource(R.string.today)
                } else {
                    UiText.DynamicString(
                        "${action.date.day} ${action.date.month.name.lowercase().replaceFirstChar { it.uppercase() }}"
                    )
                }
                _state.update {
                    it.copy(
                        selectedDate = action.date,
                        displayedMonth = action.date.yearMonth,
                        visibleDates = generateVisibleDates(action.date),
                        selectedDateLabel = label,
                    )
                }
            }
            is AgendaAction.OnDateJumped -> {
                // Same as OnDateSelected, plus: a jump can land outside the visible strip, so the
                // window is regenerated around the target.
                val label = if (action.date == today) {
                    UiText.StringResource(R.string.today)
                } else {
                    UiText.DynamicString(
                        "${action.date.day} ${action.date.month.name.lowercase().replaceFirstChar { it.uppercase() }}"
                    )
                }
                _state.update {
                    it.copy(
                        selectedDate = action.date,
                        displayedMonth = action.date.yearMonth,
                        visibleDates = generateVisibleDates(action.date),
                        selectedDateLabel = label,
                    )
                }
            }
            AgendaAction.OnProfileClicked ->
                _state.update { it.copy(isProfileMenuVisible = true) }
            AgendaAction.OnLogoutClicked -> {
                // Logout requires an active connection (the server call must succeed). When offline,
                // surface a snackbar and do nothing.
                if (!connectivityMonitor.isOnline.value) {
                    viewModelScope.launch { _events.send(AgendaEvent.ShowOfflineLogoutError) }
                } else {
                    applicationScope.launch {
                        reminderScheduler.cancelAll() // requirement: remove all reminders on logout
                        authRepository.logout()
                    }
                    viewModelScope.launch { _events.send(AgendaEvent.NavigateToLogin) }
                }
            }
            AgendaAction.OnProfileMenuDismissed ->
                _state.update { it.copy(isProfileMenuVisible = false) }
            AgendaAction.OnThemeToggled ->
                viewModelScope.launch { themePreferenceStorage.set(_state.value.theme.next()) }
            AgendaAction.OnFabClicked ->
                _state.update { it.copy(isTypePickerVisible = true) }
            AgendaAction.OnTypePickerDismissed ->
                _state.update { it.copy(isTypePickerVisible = false) }
            AgendaAction.OnCreateTask -> {
                _state.update { it.copy(isTypePickerVisible = false) }
                viewModelScope.launch {
                    _events.send(AgendaEvent.OpenTask(null, true, _state.value.selectedDate))
                }
            }
            AgendaAction.OnCreateReminder -> {
                _state.update { it.copy(isTypePickerVisible = false) }
                viewModelScope.launch {
                    _events.send(AgendaEvent.OpenReminder(null, true, _state.value.selectedDate))
                }
            }
            AgendaAction.OnCreateEvent -> {
                _state.update { it.copy(isTypePickerVisible = false) }
                viewModelScope.launch {
                    _events.send(AgendaEvent.OpenEvent(null, true, _state.value.selectedDate))
                }
            }
            is AgendaAction.OnItemOpened -> navigateToDetail(action.item, isEditing = false)
            is AgendaAction.OnItemEdited -> navigateToDetail(action.item, isEditing = true)
            is AgendaAction.OnItemDeleteClicked ->
                _state.update { it.copy(pendingDeleteItem = action.item) }
            AgendaAction.OnItemDeleteConfirmed -> applicationScope.launch {
                when (val item = _state.value.pendingDeleteItem) {
                    is AgendaItemUi.Task -> taskRepository.deleteTask(item.id)
                    is AgendaItemUi.Reminder -> reminderRepository.deleteReminder(item.id)
                    is AgendaItemUi.Event -> eventRepository.deleteEvent(item.id)
                    null -> Unit
                }
                _state.update { it.copy(pendingDeleteItem = null) }
            }
            AgendaAction.OnItemDeleteDismissed ->
                _state.update { it.copy(pendingDeleteItem = null) }
            is AgendaAction.OnTaskToggled -> viewModelScope.launch {
                val domainTask = taskRepository.observeTask(action.task.id).filterNotNull().first()
                taskRepository.updateTask(domainTask.copy(isDone = !domainTask.isDone))
            }
        }
    }

    private fun navigateToDetail(item: AgendaItemUi, isEditing: Boolean) {
        viewModelScope.launch {
            // Unused by the VM for an EXISTING item (load(id) overwrites the date once it resolves);
            // passed for signature symmetry with the create path.
            val date = _state.value.selectedDate
            when (item) {
                is AgendaItemUi.Task ->
                    _events.send(AgendaEvent.OpenTask(item.id, isEditing, date))
                is AgendaItemUi.Reminder ->
                    _events.send(AgendaEvent.OpenReminder(item.id, isEditing, date))
                is AgendaItemUi.Event ->
                    _events.send(AgendaEvent.OpenEvent(item.id, isEditing, date))
            }
        }
    }
}

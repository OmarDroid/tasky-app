package com.omaroid.tasky.feature.agenda

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.omaroid.tasky.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omaroid.tasky.core.presentation.designsystem.theme.TaskyDarkBackground
import com.omaroid.tasky.core.presentation.designsystem.theme.TaskyTheme
import com.omaroid.tasky.core.presentation.util.afterMenuExit
import com.omaroid.tasky.core.presentation.util.ObserveAsEvents
import com.omaroid.tasky.feature.agenda.components.AgendaDateStrip
import com.omaroid.tasky.feature.agenda.components.AgendaDeleteDialog
import com.omaroid.tasky.feature.agenda.components.AgendaItemList
import com.omaroid.tasky.feature.agenda.components.AgendaToolbar
import com.omaroid.tasky.feature.agenda.model.AgendaItemUi
import com.omaroid.tasky.feature.agenda.model.buildAgendaListEntries
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun AgendaRoot(
    onNavigateToLogin: () -> Unit,
    onNavigateToTask: (id: String?, isEditing: Boolean, date: LocalDate) -> Unit,
    onNavigateToReminder: (id: String?, isEditing: Boolean, date: LocalDate) -> Unit,
    onNavigateToEvent: (id: String?, isEditing: Boolean, date: LocalDate) -> Unit,
    onNavigateToCalendar: (LocalDate) -> Unit,
    // Returned by the Calendar screen's back button (savedStateHandle result — see NavGraph); folds
    // into the SAME OnDateJumped recenter/label logic the old date-picker used to drive.
    jumpedDate: LocalDate? = null,
    viewModel: AgendaViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(jumpedDate) {
        jumpedDate?.let { viewModel.onAction(AgendaAction.OnDateJumped(it)) }
    }

    // Notifications are an agenda concern (invites + reminders), so the runtime ask lives here —
    // when the user reaches their agenda — not on the root activity at cold launch. Denial is fine:
    // AndroidAgendaNotifier and ReminderReceiver both guard notify(), and the invite dedup is only
    // recorded when a notification actually posts, so invites surface once permission is granted.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* granted or not — the app degrades gracefully */ }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        // Gate on the CURRENT grant: LaunchedEffect(Unit) re-runs on every return to this screen
        // and on rotation — without the check the system dialog would reappear each time.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val offlineLogoutMessage = stringResource(R.string.error_offline_logout)
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            AgendaEvent.NavigateToLogin -> onNavigateToLogin()
            AgendaEvent.ShowOfflineLogoutError ->
                scope.launch { snackbarHostState.showSnackbar(offlineLogoutMessage) }
            is AgendaEvent.OpenTask -> onNavigateToTask(event.id, event.isEditing, event.date)
            is AgendaEvent.OpenReminder -> onNavigateToReminder(event.id, event.isEditing, event.date)
            is AgendaEvent.OpenEvent -> onNavigateToEvent(event.id, event.isEditing, event.date)
        }
    }

    AgendaScreen(
        state = state,
        onAction = viewModel::onAction,
        snackbarHostState = snackbarHostState,
        onNavigateToCalendar = onNavigateToCalendar,
    )
}

@Composable
fun AgendaScreen(
    state: AgendaState,
    onAction: (AgendaAction) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onNavigateToCalendar: (LocalDate) -> Unit = {}, // last + defaulted: positional previews unaffected
) {
    val scope = rememberCoroutineScope()

    // Mirror AgendaItemMenu.selectThenDismiss: close the type picker first, then defer the create
    // action so the dropdown's exit transition plays before navigation tears it out of composition.
    fun dismissThen(action: AgendaAction) {
        onAction(AgendaAction.OnTypePickerDismissed)
        scope.afterMenuExit { onAction(action) }
    }

    Scaffold(
        containerColor = TaskyDarkBackground,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                AgendaToolbar(
                    displayedMonth = state.displayedMonth,
                    userInitials = state.userInitials,
                    isProfileMenuVisible = state.isProfileMenuVisible,
                    isOnline = state.isOnline,
                    theme = state.theme,
                    onMonthClick = { onNavigateToCalendar(state.selectedDate) },
                    onCalendarClick = { onNavigateToCalendar(state.selectedDate) },
                    onProfileClick = { onAction(AgendaAction.OnProfileClicked) },
                    onProfileMenuDismiss = { onAction(AgendaAction.OnProfileMenuDismissed) },
                    onThemeToggle = { onAction(AgendaAction.OnThemeToggled) },
                    onLogout = { onAction(AgendaAction.OnLogoutClicked) },
                )

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        AgendaDateStrip(
                            dates = state.visibleDates,
                            selectedDate = state.selectedDate,
                            today = state.today,
                            onDateSelected = { onAction(AgendaAction.OnDateSelected(it)) },
                        )
                        Text(
                            text = state.selectedDateLabel.asString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                        AgendaItemList(
                            entries = state.entries,
                            onItemOpen = { onAction(AgendaAction.OnItemOpened(it)) },
                            onItemEdit = { onAction(AgendaAction.OnItemEdited(it)) },
                            onItemDeleteRequest = { onAction(AgendaAction.OnItemDeleteClicked(it)) },
                            onTaskToggle = { onAction(AgendaAction.OnTaskToggled(it)) },
                        )
                    }
                }
            }

            // FAB positioned manually so it clears the gesture-nav bar while the background
            // still fills edge-to-edge. Scaffold.floatingActionButton would sit against the
            // absolute bottom edge when contentWindowInsets = WindowInsets(0).
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(end = 16.dp, bottom = 16.dp),
            ) {
                FloatingActionButton(
                    onClick = { onAction(AgendaAction.OnFabClicked) },
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(16.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New item")
                }
                DropdownMenu(
                    expanded = state.isTypePickerVisible,
                    onDismissRequest = { onAction(AgendaAction.OnTypePickerDismissed) },
                ) {
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.CalendarMonth, null) },
                        text = { Text("Event") },
                        onClick = { dismissThen(AgendaAction.OnCreateEvent) },
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.CheckCircle, null) },
                        text = { Text("Task") },
                        onClick = { dismissThen(AgendaAction.OnCreateTask) },
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.Notifications, null) },
                        text = { Text("Reminder") },
                        onClick = { dismissThen(AgendaAction.OnCreateReminder) },
                    )
                }
            }
        }
    }

    state.pendingDeleteItem?.let { item ->
        val typeName = when (item) {
            is AgendaItemUi.Task -> stringResource(R.string.agenda_item_type_task)
            is AgendaItemUi.Reminder -> stringResource(R.string.agenda_item_type_reminder)
            is AgendaItemUi.Event -> stringResource(R.string.agenda_item_type_event)
        }
        AgendaDeleteDialog(
            itemTypeName = typeName,
            onConfirm = { onAction(AgendaAction.OnItemDeleteConfirmed) },
            onDismiss = { onAction(AgendaAction.OnItemDeleteDismissed) },
        )
    }
}

private val sampleState = run {
    val today = kotlinx.datetime.LocalDate(2026, 5, 6)
    val items = listOf(
        AgendaItemUi.Task("t1", "Buy groceries", "Milk, eggs", LocalTime(9, 0), today, false),
        AgendaItemUi.Reminder("r1", "Doctor appointment", "Building C", LocalTime(11, 30), today),
        AgendaItemUi.Event(
            "e1", "Team standup", "Daily sync", LocalTime(14, 0),
            LocalDateTime(today, LocalTime(14, 0)),
            LocalDateTime(today, LocalTime(14, 30)), true,
        ),
    )
    val needle = LocalTime(10, 15)
    AgendaState(
        today = today,
        username = "Alice Bob",
        userInitials = "AB",
        needleTime = needle,
        entries = buildAgendaListEntries(items, needle),
    )
}

@Preview(
    name = "Agenda · Portrait · Light",
    device = "spec:width=360dp,height=800dp,dpi=420",
    showBackground = true
)
@Composable
private fun AgendaPortraitLightPreview() {
    TaskyTheme(darkTheme = false) { AgendaScreen(sampleState, {}) }
}

@Preview(
    name = "Agenda · Portrait · Dark",
    device = "spec:width=360dp,height=800dp,dpi=420",
    showBackground = true
)
@Composable
private fun AgendaPortraitDarkPreview() {
    TaskyTheme(darkTheme = true) { AgendaScreen(sampleState, {}) }
}

@Preview(
    name = "Agenda · Landscape",
    device = "spec:width=800dp,height=360dp,dpi=420",
    showBackground = true
)
@Composable
private fun AgendaLandscapePreview() {
    TaskyTheme { AgendaScreen(sampleState, {}) }
}

@Preview(
    name = "Agenda · Tablet",
    device = "spec:width=768dp,height=1024dp,dpi=240",
    showBackground = true
)
@Composable
private fun AgendaTabletPreview() {
    TaskyTheme { AgendaScreen(sampleState, {}) }
}
package com.omaroid.tasky.feature.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omaroid.tasky.core.domain.agenda.models.ReminderType
import com.omaroid.tasky.core.presentation.designsystem.theme.TaskyTheme
import com.omaroid.tasky.core.presentation.designsystem.theme.extendedColors
import com.omaroid.tasky.core.presentation.util.ObserveAsEvents
import com.omaroid.tasky.core.presentation.util.toDisplayDate
import com.omaroid.tasky.core.presentation.util.toDisplayTime
import com.omaroid.tasky.feature.agenda.components.AgendaDeleteDialog
import com.omaroid.tasky.feature.components.DetailDateTimeRow
import com.omaroid.tasky.feature.components.DetailDescriptionRow
import com.omaroid.tasky.feature.components.DetailReminderRow
import com.omaroid.tasky.feature.components.DetailScreenScaffold
import com.omaroid.tasky.feature.detail.model.PhotoUi
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Instant

@Composable
fun AgendaDetailRoot(
    onNavigateBack: () -> Unit,
    onNavigateToEditTitle: (String) -> Unit,
    onNavigateToEditDescription: (String) -> Unit,
    // Event-only — default to no-ops so Task/Reminder callers need not pass them.
    onNavigateToPhotoDetail: (photoKey: String, photoUrl: String, eventId: String, isUserCreator: Boolean) -> Unit = { _, _, _, _ -> },
    onOpenPhotoPicker: () -> Unit = {},
    viewModel: AgendaDetailViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            AgendaDetailEvent.NavigateBack -> onNavigateBack()
            is AgendaDetailEvent.NavigateToEditTitle -> onNavigateToEditTitle(event.currentTitle)
            is AgendaDetailEvent.NavigateToEditDescription -> onNavigateToEditDescription(event.currentDescription)
            AgendaDetailEvent.OpenPhotoPicker -> onOpenPhotoPicker()
            is AgendaDetailEvent.NavigateToPhotoDetail ->
                onNavigateToPhotoDetail(event.photoKey, event.photoUrl, event.eventId, event.isUserCreator)
            is AgendaDetailEvent.ShowSkippedPhotosMessage ->
                scope.launch {
                    snackbarHostState.showSnackbar("Maximum 10 photos — ${event.count} photo(s) skipped.")
                }
        }
    }

    Box(Modifier.fillMaxSize()) {
        AgendaDetailScreen(state = state, onAction = viewModel::onAction)
        SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaDetailScreen(
    state: AgendaDetailState,
    onAction: (AgendaDetailAction) -> Unit,
) {
    val formattedDate = remember(state.dateTime) { state.dateTime.date.toDisplayDate() }
    val formattedTime = remember(state.dateTime) { state.dateTime.time.toDisplayTime() }

    val details = state.details
    val eventDetails = details as? AgendaItemDetails.Event
    val isUserCreator = eventDetails == null || eventDetails.hostId == eventDetails.currentUserId
    println("isUserCreator: $isUserCreator")
    println("eventDetails: ${eventDetails?.hostId}")
    println("currentUserId: ${eventDetails?.currentUserId}")
    // Event "to" labels — derived at the render site. remember is called unconditionally (never
    // inside a ?.let) so the Compose slot table stays stable and the label refreshes when toTime changes.
    val toTime = eventDetails?.toTime
    val formattedToDate = remember(toTime) { toTime?.date?.toDisplayDate().orEmpty() }
    val formattedToTime = remember(toTime) { toTime?.time?.toDisplayTime().orEmpty() }

    val typeLabel = when (details) {
        is AgendaItemDetails.Task -> "TASK"
        is AgendaItemDetails.Reminder -> "REMINDER"
        is AgendaItemDetails.Event -> "EVENT"
    }
    val accentColor = when (details) {
        is AgendaItemDetails.Task -> extendedColors.taskyGreen
        is AgendaItemDetails.Reminder -> MaterialTheme.colorScheme.onSurfaceVariant
        is AgendaItemDetails.Event -> extendedColors.taskyLime
    }
    val accentOutlined = details is AgendaItemDetails.Reminder
    val isDone = (details as? AgendaItemDetails.Task)?.isDone ?: false

    DetailScreenScaffold(
        title = state.title,
        typeLabel = typeLabel,
        accentColor = accentColor,
        accentOutlined = accentOutlined,
        isEditMode = state.isEditMode,
        formattedDate = formattedDate,
        // Attendees are view-only: hide the edit/save toolbar controls for them.
        showEditControls = isUserCreator,
        onClose = { onAction(AgendaDetailAction.OnCloseClicked) },
        onEdit = { onAction(AgendaDetailAction.OnEditClicked) },
        onSave = { onAction(AgendaDetailAction.OnSaveClicked) },
        onDelete = { onAction(AgendaDetailAction.OnDeleteClicked) },
        onTitleClick = { onAction(AgendaDetailAction.OnTitleClicked) },
        titleLeadingContent = if (details is AgendaItemDetails.Task) {
            {
                Icon(
                    imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isDone) extendedColors.taskyGreen else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp),
                )
            }
        } else null,
        titleContent = if (state.isNew) {
            {
                val focusRequester = remember { FocusRequester() }
                BasicTextField(
                    value = state.title,
                    onValueChange = { onAction(AgendaDetailAction.OnTitleEdited(it)) },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (state.title.isEmpty()) {
                                Text(
                                    "Title",
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
            }
        } else null,
    ) {
        // Create mode: inline description field. Edit/view mode: row that navigates to EditTextScreen.
        if (state.isNew) {
            BasicTextField(
                value = state.description,
                onValueChange = { onAction(AgendaDetailAction.OnDescriptionEdited(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(vertical = 8.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.TopStart) {
                        if (state.description.isEmpty()) {
                            Text(
                                "Description",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        innerTextField()
                    }
                },
            )
        } else {
            DetailDescriptionRow(
                description = state.description,
                isEditable = state.isEditMode && isUserCreator,
                onClick = { onAction(AgendaDetailAction.OnDescriptionClicked) },
            )
        }
        HorizontalDivider()

        if (eventDetails != null) {
            // Photos sit between the description and the From row (per the design). Local picks are
            // shown alongside remote photos as PhotoUi(key = uri) so they appear immediately.
            EventPhotosSection(
                photos = eventDetails.photos + eventDetails.localPhotoUris.map { PhotoUi(key = it, url = it) },
                isEditMode = state.isEditMode && isUserCreator,
                isOnline = eventDetails.isOnline,
                onAddPhoto = { onAction(AgendaDetailAction.OnAddPhotoClicked) },
                onPhotoClick = { onAction(AgendaDetailAction.OnPhotoClicked(it)) },
                onDeletePhoto = { onAction(AgendaDetailAction.OnDeletePhotoClicked(it)) },
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp),
            )
            HorizontalDivider()
            DetailDateTimeRow(
                label = "From",
                dateText = formattedDate,
                timeText = formattedTime,
                isEditable = state.isEditMode && isUserCreator,
                onDateClick = { onAction(AgendaDetailAction.OnDateClicked) },
                onTimeClick = { onAction(AgendaDetailAction.OnTimeClicked) },
            )
            HorizontalDivider()
            DetailDateTimeRow(
                label = "To",
                dateText = formattedToDate,
                timeText = formattedToTime,
                isEditable = state.isEditMode && isUserCreator,
                onDateClick = { onAction(AgendaDetailAction.OnToDateClicked) },
                onTimeClick = { onAction(AgendaDetailAction.OnToTimeClicked) },
            )
            HorizontalDivider()
            // Attendees can always change their personal reminder, even in view mode.
            DetailReminderRow(
                selectedType = state.reminderType,
                isEditable = state.isEditMode || !isUserCreator,
                onTypeSelected = { onAction(AgendaDetailAction.OnReminderChanged(it)) },
            )
            HorizontalDivider()
            EventVisitorsSection(
                goingAttendees = eventDetails.goingAttendees,
                notGoingAttendees = eventDetails.notGoingAttendees,
                currentUserId = eventDetails.currentUserId,
                isUserCreator = isUserCreator,
                isEditMode = state.isEditMode,
                isOnline = eventDetails.isOnline,
                attendeeFilter = eventDetails.attendeeFilter,
                onFilterChanged = { onAction(AgendaDetailAction.OnAttendeeFilterChanged(it)) },
                onAddAttendee = { onAction(AgendaDetailAction.OnAddAttendeeClicked) },
                onRemoveAttendee = { userId -> onAction(AgendaDetailAction.OnRemoveAttendeeClicked(userId)) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            if (!isUserCreator) {
                val isGoing = eventDetails.attendees.any { it.userId == eventDetails.currentUserId && it.isGoing }
                OutlinedButton(
                    onClick = { onAction(AgendaDetailAction.OnJoinOrLeaveEvent) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Text(if (isGoing) "LEAVE EVENT" else "JOIN EVENT")
                }
            }
            Spacer(Modifier.height(8.dp))
        } else {
            DetailDateTimeRow(
                label = "At",
                dateText = formattedDate,
                timeText = formattedTime,
                isEditable = state.isEditMode,
                onDateClick = { onAction(AgendaDetailAction.OnDateClicked) },
                onTimeClick = { onAction(AgendaDetailAction.OnTimeClicked) },
            )
            HorizontalDivider()
            DetailReminderRow(
                selectedType = state.reminderType,
                isEditable = state.isEditMode,
                onTypeSelected = { onAction(AgendaDetailAction.OnReminderChanged(it)) },
            )
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
        }
    }

    if (state.isDeleteConfirmVisible) {
        AgendaDeleteDialog(
            itemTypeName = if (eventDetails != null && !isUserCreator) "attendance" else typeLabel.lowercase(),
            onConfirm = { onAction(AgendaDetailAction.OnDeleteConfirmed) },
            onDismiss = { onAction(AgendaDetailAction.OnDeleteDismissed) },
        )
    }

    if (state.isDatePickerVisible) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.dateTime.date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
        )
        DatePickerDialog(
            onDismissRequest = { onAction(AgendaDetailAction.OnDatePickerDismissed) },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { ms ->
                        onAction(AgendaDetailAction.OnDateChanged(
                            Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.UTC).date
                        ))
                    }
                    onAction(AgendaDetailAction.OnDatePickerDismissed)
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { onAction(AgendaDetailAction.OnDatePickerDismissed) }) { Text("Cancel") }
            },
        ) { DatePicker(state = datePickerState) }
    }

    if (state.isTimePickerVisible) {
        val timePickerState = rememberTimePickerState(
            initialHour = state.dateTime.time.hour, initialMinute = state.dateTime.time.minute,
        )
        AlertDialog(
            onDismissRequest = { onAction(AgendaDetailAction.OnTimePickerDismissed) },
            confirmButton = {
                TextButton(onClick = {
                    onAction(AgendaDetailAction.OnTimeChanged(LocalTime(timePickerState.hour, timePickerState.minute)))
                    onAction(AgendaDetailAction.OnTimePickerDismissed)
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { onAction(AgendaDetailAction.OnTimePickerDismissed) }) { Text("Cancel") }
            },
            text = { TimePicker(state = timePickerState) },
        )
    }

    if (eventDetails != null && eventDetails.isToDatePickerVisible) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = eventDetails.toTime.date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
        )
        DatePickerDialog(
            onDismissRequest = { onAction(AgendaDetailAction.OnToDatePickerDismissed) },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { ms ->
                        onAction(AgendaDetailAction.OnToDateChanged(
                            Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.UTC).date
                        ))
                    }
                    onAction(AgendaDetailAction.OnToDatePickerDismissed)
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { onAction(AgendaDetailAction.OnToDatePickerDismissed) }) { Text("Cancel") }
            },
        ) { DatePicker(state = datePickerState) }
    }

    if (eventDetails != null && eventDetails.isToTimePickerVisible) {
        val timePickerState = rememberTimePickerState(
            initialHour = eventDetails.toTime.time.hour, initialMinute = eventDetails.toTime.time.minute,
        )
        AlertDialog(
            onDismissRequest = { onAction(AgendaDetailAction.OnToTimePickerDismissed) },
            confirmButton = {
                TextButton(onClick = {
                    onAction(AgendaDetailAction.OnToTimeChanged(LocalTime(timePickerState.hour, timePickerState.minute)))
                    onAction(AgendaDetailAction.OnToTimePickerDismissed)
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { onAction(AgendaDetailAction.OnToTimePickerDismissed) }) { Text("Cancel") }
            },
            text = { TimePicker(state = timePickerState) },
        )
    }

    if (eventDetails != null && eventDetails.isAddAttendeeSheetVisible) {
        AddAttendeeSheet(
            email = eventDetails.attendeeEmailInput,
            emailError = eventDetails.attendeeEmailError,
            isEmailValid = eventDetails.isAttendeeEmailValid,
            isChecking = eventDetails.isCheckingAttendee,
            isAdded = eventDetails.isAttendeeAdded,
            onEmailChanged = { onAction(AgendaDetailAction.OnAttendeeEmailChanged(it)) },
            onAddClicked = { onAction(AgendaDetailAction.OnAttendeeAddConfirmed) },
            onDismiss = { onAction(AgendaDetailAction.OnAddAttendeeDismissed) },
        )
    }
}

private val sampleNewTaskState = AgendaDetailState(
    dateTime = LocalDateTime(2022, 7, 21, 8, 0),
    details = AgendaItemDetails.Task(isDone = false),
    reminderType = ReminderType.THIRTY_MINUTES,
    isNew = true, isEditMode = true,
)

private val sampleExistingTaskState = AgendaDetailState(
    title = "Project X",
    description = "Weekly plan\nRole distribution",
    dateTime = LocalDateTime(2022, 7, 21, 8, 0),
    details = AgendaItemDetails.Task(isDone = false),
    reminderType = ReminderType.THIRTY_MINUTES,
    isNew = false,
)

private val sampleNewReminderState = AgendaDetailState(
    dateTime = LocalDateTime(2025, 3, 5, 11, 30),
    details = AgendaItemDetails.Reminder,
    reminderType = ReminderType.ONE_HOUR,
    isNew = true, isEditMode = true,
)

private val sampleExistingReminderState = AgendaDetailState(
    title = "Doctor appointment",
    description = "Building C, 3rd floor",
    dateTime = LocalDateTime(2025, 3, 5, 11, 30),
    details = AgendaItemDetails.Reminder,
    reminderType = ReminderType.ONE_HOUR,
    isNew = false,
)

private val sampleNewEventState = AgendaDetailState(
    dateTime = LocalDateTime(2025, 3, 5, 10, 0),
    details = AgendaItemDetails.Event(toTime = LocalDateTime(2025, 3, 5, 12, 0)),
    isNew = true, isEditMode = true,
)

// isUserCreator is derived from hostId == currentUserId — same value here for the creator preview.
private val sampleExistingEventState = AgendaDetailState(
    title = "Team meeting",
    description = "Weekly sync",
    dateTime = LocalDateTime(2025, 3, 5, 10, 0),
    details = AgendaItemDetails.Event(
        toTime = LocalDateTime(2025, 3, 5, 12, 0),
        hostId = "user1", currentUserId = "user1",
    ),
    isNew = false, isEditMode = false,
)

@Preview(name = "AgendaDetail · Task · Create · Light", device = "spec:width=360dp,height=800dp,dpi=420", showBackground = true)
@Composable private fun AgendaDetailTaskCreateLightPreview() {
    TaskyTheme(darkTheme = false) { AgendaDetailScreen(sampleNewTaskState, {}) }
}

@Preview(name = "AgendaDetail · Task · View · Dark", device = "spec:width=360dp,height=800dp,dpi=420", showBackground = true)
@Composable private fun AgendaDetailTaskViewDarkPreview() {
    TaskyTheme(darkTheme = true) { AgendaDetailScreen(sampleExistingTaskState.copy(isEditMode = false), {}) }
}

@Preview(name = "AgendaDetail · Reminder · Create · Light", device = "spec:width=360dp,height=800dp,dpi=420", showBackground = true)
@Composable private fun AgendaDetailReminderCreateLightPreview() {
    TaskyTheme(darkTheme = false) { AgendaDetailScreen(sampleNewReminderState, {}) }
}

@Preview(name = "AgendaDetail · Reminder · View · Dark", device = "spec:width=360dp,height=800dp,dpi=420", showBackground = true)
@Composable private fun AgendaDetailReminderViewDarkPreview() {
    TaskyTheme(darkTheme = true) { AgendaDetailScreen(sampleExistingReminderState.copy(isEditMode = false), {}) }
}

@Preview(name = "AgendaDetail · Event · Create · Light", device = "spec:width=360dp,height=800dp,dpi=420", showBackground = true)
@Composable private fun AgendaDetailEventCreateLightPreview() {
    TaskyTheme(darkTheme = false) { AgendaDetailScreen(sampleNewEventState, {}) }
}

@Preview(name = "AgendaDetail · Event · View · Dark", device = "spec:width=360dp,height=800dp,dpi=420", showBackground = true)
@Composable private fun AgendaDetailEventViewDarkPreview() {
    TaskyTheme(darkTheme = true) { AgendaDetailScreen(sampleExistingEventState, {}) }
}

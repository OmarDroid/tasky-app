package com.omaroid.tasky.feature.detail

import com.omaroid.tasky.core.domain.agenda.models.ReminderType
import com.omaroid.tasky.feature.detail.model.AttendeeFilter
import com.omaroid.tasky.feature.detail.model.AttendeeUi
import com.omaroid.tasky.feature.detail.model.PhotoUi
import kotlinx.datetime.LocalDateTime
import java.util.UUID

data class AgendaDetailState(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val description: String = "",
    val dateTime: LocalDateTime,
    val reminderType: ReminderType = ReminderType.THIRTY_MINUTES,
    val details: AgendaItemDetails,
    val isEditMode: Boolean = true,
    val isNew: Boolean = true,
    val isLoading: Boolean = false,
    val isDeleteConfirmVisible: Boolean = false,
    val isDatePickerVisible: Boolean = false,
    val isTimePickerVisible: Boolean = false,
)

sealed interface AgendaItemDetails {
    data class Task(val isDone: Boolean) : AgendaItemDetails

    // All event-specific UI state. The "from" time is the shared AgendaDetailState.dateTime; only
    // the event-only fields are below. attendees is the full list; goingAttendees/notGoingAttendees
    // are the pre-computed groups the Visitors section renders (never partition in a composable).
    data class Event(
        val toTime: LocalDateTime,
        val attendees: List<AttendeeUi> = emptyList(),
        val goingAttendees: List<AttendeeUi> = emptyList(),
        val notGoingAttendees: List<AttendeeUi> = emptyList(),
        val photos: List<PhotoUi> = emptyList(),
        // Stable file:// uris of newly-picked photos (cached to app storage) not yet uploaded.
        val localPhotoUris: List<String> = emptyList(),
        // Remote photo keys the user removed while editing — sent as deletedPhotoKeys on PUT /event.
        val deletedPhotoKeys: List<String> = emptyList(),
        val currentUserId: String = "",
        val hostId: String = "",
        val attendeeFilter: AttendeeFilter = AttendeeFilter.ALL,
        val attendeeEmailInput: String = "",
        val attendeeEmailError: String? = null,
        val isAttendeeEmailValid: Boolean = false,
        val isCheckingAttendee: Boolean = false,
        val isAddAttendeeSheetVisible: Boolean = false,
        val isAttendeeAdded: Boolean = false,
        val isToDatePickerVisible: Boolean = false,
        val isToTimePickerVisible: Boolean = false,
        val isOnline: Boolean = true,
    ) : AgendaItemDetails

    data object Reminder : AgendaItemDetails
}

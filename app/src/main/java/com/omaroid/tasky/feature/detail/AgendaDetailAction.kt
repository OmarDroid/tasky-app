package com.omaroid.tasky.feature.detail

import com.omaroid.tasky.core.domain.agenda.models.ReminderType
import com.omaroid.tasky.feature.detail.model.AttendeeFilter
import com.omaroid.tasky.feature.detail.model.PhotoUi
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

sealed interface AgendaDetailAction {
    data object OnSaveClicked : AgendaDetailAction
    data object OnCloseClicked : AgendaDetailAction
    data object OnEditClicked : AgendaDetailAction
    data object OnDeleteClicked : AgendaDetailAction
    data object OnDeleteConfirmed : AgendaDetailAction
    data object OnDeleteDismissed : AgendaDetailAction
    data object OnTitleClicked : AgendaDetailAction
    data object OnDescriptionClicked : AgendaDetailAction

    // Shared "from"/"at" date & time (Task, Reminder, and the Event "From" row)
    data object OnDateClicked : AgendaDetailAction
    data object OnTimeClicked : AgendaDetailAction
    data object OnDatePickerDismissed : AgendaDetailAction
    data object OnTimePickerDismissed : AgendaDetailAction
    data class OnDateChanged(val date: LocalDate) : AgendaDetailAction
    data class OnTimeChanged(val time: LocalTime) : AgendaDetailAction

    data class OnReminderChanged(val type: ReminderType) : AgendaDetailAction
    data class OnTitleEdited(val title: String) : AgendaDetailAction
    data class OnDescriptionEdited(val description: String) : AgendaDetailAction

    // ── Event-only ────────────────────────────────────────────────────────────
    data object OnToDateClicked : AgendaDetailAction
    data object OnToTimeClicked : AgendaDetailAction
    data object OnToDatePickerDismissed : AgendaDetailAction
    data object OnToTimePickerDismissed : AgendaDetailAction
    data class OnToDateChanged(val date: LocalDate) : AgendaDetailAction
    data class OnToTimeChanged(val time: LocalTime) : AgendaDetailAction

    data object OnAddAttendeeClicked : AgendaDetailAction
    data class OnAttendeeEmailChanged(val email: String) : AgendaDetailAction
    data object OnAttendeeAddConfirmed : AgendaDetailAction
    data object OnAddAttendeeDismissed : AgendaDetailAction
    data class OnRemoveAttendeeClicked(val userId: String) : AgendaDetailAction
    data class OnAttendeeFilterChanged(val filter: AttendeeFilter) : AgendaDetailAction
    data object OnJoinOrLeaveEvent : AgendaDetailAction

    data object OnAddPhotoClicked : AgendaDetailAction
    data class OnPhotoClicked(val photo: PhotoUi) : AgendaDetailAction
    data class OnDeletePhotoClicked(val photo: PhotoUi) : AgendaDetailAction
    data class OnLocalPhotoAdded(val uri: String) : AgendaDetailAction
    data class OnDeletedPhotoKeyReceived(val photoKey: String) : AgendaDetailAction
}

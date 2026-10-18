package com.omaroid.tasky.feature.agenda

import kotlinx.datetime.LocalDate

sealed interface AgendaEvent {
    data object NavigateToLogin : AgendaEvent
    data object ShowOfflineLogoutError : AgendaEvent
    // date = the agenda's currently selected day — seeds a NEW item's date/time; a no-op for an
    // existing item, since AgendaDetailViewModel.load(id) overwrites it once the real item loads.
    data class OpenTask(val id: String?, val isEditing: Boolean, val date: LocalDate) : AgendaEvent
    data class OpenReminder(val id: String?, val isEditing: Boolean, val date: LocalDate) : AgendaEvent
    data class OpenEvent(val id: String?, val isEditing: Boolean, val date: LocalDate) : AgendaEvent
}
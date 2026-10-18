package com.omaroid.tasky.feature.agenda

import com.omaroid.tasky.feature.agenda.model.AgendaItemUi
import kotlinx.datetime.LocalDate

sealed interface AgendaAction {
    data class OnDateSelected(val date: LocalDate) : AgendaAction
    data class OnDateJumped(val date: LocalDate) : AgendaAction
    data object OnProfileClicked : AgendaAction
    data object OnLogoutClicked : AgendaAction
    data object OnProfileMenuDismissed : AgendaAction
    data object OnThemeToggled : AgendaAction
    data object OnFabClicked : AgendaAction
    data object OnTypePickerDismissed : AgendaAction
    data object OnCreateEvent : AgendaAction
    data object OnCreateTask : AgendaAction
    data object OnCreateReminder : AgendaAction
    data class OnItemOpened(val item: AgendaItemUi) : AgendaAction
    data class OnItemEdited(val item: AgendaItemUi) : AgendaAction
    data class OnItemDeleteClicked(val item: AgendaItemUi) : AgendaAction
    data class OnTaskToggled(val task: AgendaItemUi.Task) : AgendaAction
    data object OnItemDeleteConfirmed : AgendaAction
    data object OnItemDeleteDismissed : AgendaAction
}
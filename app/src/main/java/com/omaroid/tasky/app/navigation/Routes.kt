package com.omaroid.tasky.app.navigation

import com.omaroid.tasky.feature.detail.AgendaDetailType
import com.omaroid.tasky.feature.edittext.EditTextType
import kotlinx.serialization.Serializable

@Serializable
data object AuthGraphRoute

@Serializable
data object LoginRoute

@Serializable
data object RegisterRoute

@Serializable
data object AgendaRoute

// One route for every agenda item type — [type] tells the single AgendaDetailViewModel which
// item it is editing. Property names (type/itemId/isEditing) become SavedStateHandle keys.
@Serializable
data class AgendaDetailRoute(
    val type: AgendaDetailType,
    val itemId: String? = null,
    val isEditing: Boolean = true,
    // Seeds a NEW item's date to the day the user had selected in the agenda when they tapped
    // "create" (null = fall back to today — see AgendaDetailViewModel.initialDate).
    val initialDateEpochDay: Long? = null,
)

@Serializable
data class EditTextRoute(val type: EditTextType, val initialText: String)

@Serializable
data class PhotoDetailRoute(
    val photoKey: String,
    val photoUrl: String,
    val eventId: String,
    val isUserCreator: Boolean = false,
)

@Serializable
data class CalendarRoute(val initialDateEpochDay: Long) // opens on the agenda's selected date's month

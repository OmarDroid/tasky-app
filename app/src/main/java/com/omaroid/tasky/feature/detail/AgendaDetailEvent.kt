package com.omaroid.tasky.feature.detail

sealed interface AgendaDetailEvent {
    data object NavigateBack : AgendaDetailEvent
    data class NavigateToEditTitle(val currentTitle: String) : AgendaDetailEvent
    data class NavigateToEditDescription(val currentDescription: String) : AgendaDetailEvent
    data object OpenPhotoPicker : AgendaDetailEvent
    data class NavigateToPhotoDetail(
        val photoKey: String,
        val photoUrl: String,
        val eventId: String,
        val isUserCreator: Boolean,
    ) : AgendaDetailEvent
    data class ShowSkippedPhotosMessage(val count: Int) : AgendaDetailEvent
}

package com.omaroid.tasky.feature.detail

import kotlinx.serialization.Serializable

/**
 * Which agenda item the single [AgendaDetailViewModel] is editing. Carried on the navigation
 * route and read back from [androidx.lifecycle.SavedStateHandle] so one screen + one ViewModel
 * serve Task, Reminder, and Event instead of a separate copy per type.
 */
@Serializable
enum class AgendaDetailType { TASK, REMINDER, EVENT }

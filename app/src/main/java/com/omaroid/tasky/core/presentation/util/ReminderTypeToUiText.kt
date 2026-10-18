package com.omaroid.tasky.core.presentation.util

import com.omaroid.tasky.R
import com.omaroid.tasky.core.domain.agenda.models.ReminderType
import com.omaroid.tasky.core.presentation.util.UiText.StringResource

fun ReminderType.toUiText(): UiText = when (this) {
    ReminderType.TEN_MINUTES -> StringResource(R.string.reminder_type_ten_minutes)
    ReminderType.THIRTY_MINUTES -> StringResource(R.string.reminder_type_thirty_minutes)
    ReminderType.ONE_HOUR -> StringResource(R.string.reminder_type_one_hour)
    ReminderType.SIX_HOURS -> StringResource(R.string.reminder_type_six_hours)
    ReminderType.ONE_DAY -> StringResource(R.string.reminder_type_one_day)
}

package com.omaroid.tasky.core.domain.agenda.models

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

enum class ReminderType(val offset: Duration) {
    TEN_MINUTES(10.minutes),
    THIRTY_MINUTES(30.minutes),
    ONE_HOUR(1.hours),
    SIX_HOURS(6.hours),
    ONE_DAY(24.hours);

    companion object {
        fun fromOffset(diff: Duration): ReminderType =
            entries.minByOrNull { kotlin.math.abs((it.offset - diff).inWholeMinutes) }
                ?: THIRTY_MINUTES
    }
}
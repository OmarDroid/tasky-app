package com.omaroid.tasky.core.domain.notification

import com.omaroid.tasky.core.domain.agenda.models.AgendaItem

/**
 * Schedules local reminder notifications for agenda items. A domain port (like [AgendaNotifier])
 * so ViewModels and repositories never depend on the WorkManager-backed implementation.
 */
interface ReminderScheduler {
    fun schedule(item: AgendaItem)
    fun cancel(itemId: String)

    /** Suspends: "all" is derived from the stored items (alarms carry no queryable tag). */
    suspend fun cancelAll()
}

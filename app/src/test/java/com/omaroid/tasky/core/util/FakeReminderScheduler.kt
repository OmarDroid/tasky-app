package com.omaroid.tasky.core.util

import com.omaroid.tasky.core.domain.agenda.models.AgendaItem
import com.omaroid.tasky.core.domain.notification.ReminderScheduler

class FakeReminderScheduler : ReminderScheduler {
    val scheduledItems = mutableListOf<AgendaItem>()
    val cancelledIds = mutableListOf<String>()
    var cancelAllCalls = 0
        private set

    override fun schedule(item: AgendaItem) {
        scheduledItems += item
    }

    override fun cancel(itemId: String) {
        cancelledIds += itemId
    }

    override suspend fun cancelAll() {
        cancelAllCalls++
    }
}

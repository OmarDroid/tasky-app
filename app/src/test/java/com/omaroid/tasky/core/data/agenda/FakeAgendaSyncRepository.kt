package com.omaroid.tasky.core.data.agenda

import com.omaroid.tasky.core.domain.agenda.repository.AgendaSyncRepository

class FakeAgendaSyncRepository : AgendaSyncRepository {
    /** Set to a non-null exception to simulate network failure in [syncAll]. */
    var error: Exception? = null

    /** Incremented each time [syncAll] is called without error. */
    var syncCallCount: Int = 0

    /**
     * Optional lambda run at the end of each successful [syncAll].
     * Use this in tests that verify sync populates repositories:
     *
     * ```kotlin
     * fakeAgendaSyncRepo.onSync = {
     *     fakeTaskRepo.upsertTask(task("remote-t1", today.atStartOfDayIn(zone) + 1.hours))
     * }
     * ```
     */
    var onSync: (suspend () -> Unit)? = null

    override suspend fun syncAll() {
        error?.let { throw it }
        syncCallCount++
        onSync?.invoke()
    }
}

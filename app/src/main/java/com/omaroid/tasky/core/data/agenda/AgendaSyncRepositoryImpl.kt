package com.omaroid.tasky.core.data.agenda

import com.omaroid.tasky.core.data.agenda.local.dao.EventDao
import com.omaroid.tasky.core.data.agenda.local.dao.NotifiedEventDao
import com.omaroid.tasky.core.data.agenda.local.dao.ReminderDao
import com.omaroid.tasky.core.data.agenda.local.dao.TaskDao
import com.omaroid.tasky.core.data.agenda.local.entities.NotifiedEventEntity
import com.omaroid.tasky.core.data.agenda.remote.AgendaRemoteDataSource
import com.omaroid.tasky.core.data.agenda.remote.mappers.toDomain
import com.omaroid.tasky.core.data.agenda.remote.mappers.toEntity
import com.omaroid.tasky.core.domain.agenda.repository.AgendaSyncRepository
import com.omaroid.tasky.core.domain.notification.AgendaNotifier
import com.omaroid.tasky.core.domain.notification.ReminderScheduler
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.time.Clock

/**
 * Fetches the full agenda from the server and writes every item directly to the local DAOs (bypassing
 * the per-item create endpoints to avoid re-uploading server data). Room Flows emit automatically and
 * the ViewModel pipeline reacts.
 *
 * It is also where **event invites** are surfaced. The Tasky API has no push channel, so an invite is
 * detected here: a fetched event the server marks `isUserEventCreator = false` that we have not
 * already notified for is a new invite → a local notification via [AgendaNotifier]. The
 * `notified_event_ids` table makes this **exactly-once** and survives an events-cache clear
 * (logout/login); it is pruned to the current agenda each sync so a re-invite notifies again.
 */
class AgendaSyncRepositoryImpl(
    private val agendaRemote: AgendaRemoteDataSource,
    private val taskDao: TaskDao,
    private val reminderDao: ReminderDao,
    private val eventDao: EventDao,
    private val notifiedEventDao: NotifiedEventDao,
    private val notifier: AgendaNotifier,
    private val reminderScheduler: ReminderScheduler,
    private val clock: Clock = Clock.System,
) : AgendaSyncRepository {

    override suspend fun syncAll() {
        val now = clock.now().toEpochMilliseconds()
        val full = agendaRemote.getFullAgenda()

        val alreadyNotified = notifiedEventDao.getAllIds().toSet()

        // The three loops are independent (different DAOs, no shared state), so they run
        // concurrently. Rescheduling each pulled item means a remindAt changed on another device
        // replaces the stale local alarm (schedule() no-ops for past times and is idempotent, so
        // the 30-minute periodic sync re-running this never duplicates notifications).
        coroutineScope {
            val tasks = async {
                full.tasks.forEach {
                    val entity = it.toEntity(now)
                    taskDao.upsert(entity)
                    reminderScheduler.schedule(entity.toDomain())
                }
            }
            val reminders = async {
                full.reminders.forEach {
                    val entity = it.toEntity(now)
                    reminderDao.upsert(entity)
                    reminderScheduler.schedule(entity.toDomain())
                }
            }
            val events = async {
                full.events.forEach { event ->
                    val entity = event.toEntity(now)
                    eventDao.upsert(entity)
                    reminderScheduler.schedule(entity.toDomain()) // includes events the user attends
                    // A new invite = an event you didn't create that hasn't been notified yet.
                    // Record it ONLY if the notification was actually posted — notifyEventInvite
                    // returns false when POST_NOTIFICATIONS isn't granted, and marking it then
                    // would silence the invite forever once the user grants the permission.
                    if (!event.isUserEventCreator && event.id !in alreadyNotified) {
                        if (notifier.notifyEventInvite(event.id, event.title)) {
                            notifiedEventDao.upsert(NotifiedEventEntity(event.id))
                        }
                    }
                }
            }
            awaitAll(tasks, reminders, events)
        }

        // AFTER awaitAll so the prune can't race the event upserts: drop notified ids for events no
        // longer in the agenda (left/removed) so a re-invite notifies again and the table stays bounded.
        val currentEventIds = full.events.map { it.id }
        if (currentEventIds.isEmpty()) notifiedEventDao.clearAll()
        else notifiedEventDao.retainOnly(currentEventIds)
    }
}

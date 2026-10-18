package com.omaroid.tasky.core.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.omaroid.tasky.core.data.agenda.local.dao.EventDao
import com.omaroid.tasky.core.data.agenda.local.dao.ReminderDao
import com.omaroid.tasky.core.data.agenda.local.dao.TaskDao
import com.omaroid.tasky.core.data.agenda.remote.mappers.toDomain
import com.omaroid.tasky.core.domain.notification.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * AlarmManager alarms do not survive a reboot (WorkManager's one advantage we gave up for exact
 * timing), so this re-schedules every stored item's reminder on BOOT_COMPLETED. `schedule()` no-ops
 * for past times, so replaying everything is idempotent and cheap.
 */
class BootCompletedReceiver : BroadcastReceiver(), KoinComponent {

    private val taskDao: TaskDao by inject()
    private val reminderDao: ReminderDao by inject()
    private val eventDao: EventDao by inject()
    private val reminderScheduler: ReminderScheduler by inject()
    private val applicationScope: CoroutineScope by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        applicationScope.launch {
            try {
                taskDao.getAll().forEach { reminderScheduler.schedule(it.toDomain()) }
                reminderDao.getAll().forEach { reminderScheduler.schedule(it.toDomain()) }
                eventDao.getAll().forEach { reminderScheduler.schedule(it.toDomain()) }
            } finally {
                pending.finish()
            }
        }
    }
}

package com.omaroid.tasky.core.data.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import com.omaroid.tasky.core.data.agenda.local.dao.EventDao
import com.omaroid.tasky.core.data.agenda.local.dao.ReminderDao
import com.omaroid.tasky.core.data.agenda.local.dao.TaskDao
import com.omaroid.tasky.core.domain.agenda.models.AgendaItem
import com.omaroid.tasky.core.domain.notification.ReminderScheduler
import kotlin.time.Clock

/**
 * AlarmManager-backed [ReminderScheduler]. Reminders must fire at a specific minute, which is the
 * one thing WorkManager is not built for (deferrable work gets batched and Doze-delayed by design) —
 * `setExactAndAllowWhileIdle` fires on time even in Doze. Requires `USE_EXACT_ALARM` in the manifest
 * (granted install-time for calendar/alarm apps; no runtime prompt).
 *
 * Trade-off vs WorkManager: alarms do NOT survive a reboot, so [BootCompletedReceiver] re-runs
 * [schedule] for every stored item on BOOT_COMPLETED (past times no-op).
 *
 * Cancellation matches by PendingIntent identity (request code = item id hash + same component);
 * extras don't participate in matching, so a bare recreated intent cancels the scheduled one.
 */
class AlarmManagerReminderScheduler(
    private val context: Context,
    private val clock: Clock,
    private val taskDao: TaskDao,
    private val reminderDao: ReminderDao,
    private val eventDao: EventDao,
) : ReminderScheduler {

    private val alarmManager: AlarmManager
        get() = context.getSystemService<AlarmManager>()!!

    override fun schedule(item: AgendaItem) {
        val triggerAtMs = item.remindAt.toEpochMilliseconds()
        if (triggerAtMs <= clock.now().toEpochMilliseconds()) {
            return // reminder time already passed (smart-scheduling requirement)
        }
        val pendingIntent = reminderPendingIntent(item.id) {
            // Identity ONLY — no title/description snapshot. ReminderReceiver reads the CURRENT
            // item from its DAO at fire time, so cross-device edits show fresh text and
            // deleted/done items simply don't notify.
            putExtra("itemId", item.id)
            putExtra("itemType", item.typeName())
        }
        // Exact alarms are gated differently per API level: 28-30 need nothing; 31-32 need
        // SCHEDULE_EXACT_ALARM (default-granted but USER-REVOCABLE); 33+ get USE_EXACT_ALARM at
        // install. If the 31/32 grant was revoked, fall back to an inexact alarm instead of letting
        // setExactAndAllowWhileIdle throw SecurityException — which would also crash-loop
        // BootCompletedReceiver on every reboot.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
        }
    }

    override fun cancel(itemId: String) {
        alarmManager.cancel(reminderPendingIntent(itemId))
    }

    // Alarms carry no queryable tag (unlike WorkManager), so "all" is derived from the source of
    // truth: every stored item's id. cancel() is a no-op for ids with no pending alarm.
    override suspend fun cancelAll() {
        taskDao.getAll().forEach { cancel(it.id) }
        reminderDao.getAll().forEach { cancel(it.id) }
        eventDao.getAll().forEach { cancel(it.id) }
    }

    private fun reminderPendingIntent(itemId: String, build: Intent.() -> Unit = {}): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            itemId.hashCode(),
            Intent(context, ReminderReceiver::class.java).apply(build),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}

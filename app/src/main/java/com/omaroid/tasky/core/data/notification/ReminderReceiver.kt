package com.omaroid.tasky.core.data.notification

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.omaroid.tasky.R
import com.omaroid.tasky.app.MainActivity
import com.omaroid.tasky.core.data.agenda.local.dao.EventDao
import com.omaroid.tasky.core.data.agenda.local.dao.ReminderDao
import com.omaroid.tasky.core.data.agenda.local.dao.TaskDao
import com.omaroid.tasky.core.data.agenda.remote.mappers.toDomain
import com.omaroid.tasky.core.domain.agenda.models.AgendaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Fires a reminder notification at its exact alarm time. The intent carries identity only — the
 * receiver reads the CURRENT item from Room, so a frozen title never goes stale after a cross-device
 * edit, and an item deleted (locally or via sync) or a completed task simply doesn't notify.
 *
 * A manifest-registered receiver is constructed by the system, not Koin, so dependencies come via
 * [KoinComponent]. The DB read outlives onReceive via [goAsync] (bounded, well under its ~10s budget).
 */
class ReminderReceiver : BroadcastReceiver(), KoinComponent {

    private val taskDao: TaskDao by inject()
    private val reminderDao: ReminderDao by inject()
    private val eventDao: EventDao by inject()
    private val applicationScope: CoroutineScope by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getStringExtra("itemId") ?: return
        val itemType = intent.getStringExtra("itemType") ?: return
        val pending = goAsync()

        applicationScope.launch {
            try {
                // Fresh read — deleted (locally or via sync) or completed items don't notify.
                val item: AgendaItem = when (itemType) {
                    "TASK" -> taskDao.getById(itemId)?.toDomain()?.takeIf { !it.isDone }
                    "REMINDER" -> reminderDao.getById(itemId)?.toDomain()
                    "EVENT" -> eventDao.getById(itemId)?.toDomain()
                    else -> null
                } ?: return@launch

                postReminderNotification(context, item)
            } finally {
                pending.finish()
            }
        }
    }

    private fun postReminderNotification(context: Context, item: AgendaItem) {
        // Same guard as AndroidAgendaNotifier: on Android 13+ notify() silently no-ops (and lint
        // errors) without the runtime POST_NOTIFICATIONS grant.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("itemId", item.id)
            putExtra("itemType", item.typeName())
        }
        val pendingIntent = PendingIntent.getActivity(
            context, item.id.hashCode(), tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, "reminders")
            .setSmallIcon(R.drawable.tasky_logo_splash)
            .setContentTitle(item.title)
            .setContentText(item.description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // "reminder:" namespace — AndroidAgendaNotifier uses eventId.hashCode() for invites, so a
        // bare itemId.hashCode() would let an event's reminder overwrite its invite notification.
        NotificationManagerCompat.from(context)
            .notify("reminder:${item.id}".hashCode(), notification)
    }
}

internal fun AgendaItem.typeName() = when (this) {
    is AgendaItem.Task -> "TASK"
    is AgendaItem.Reminder -> "REMINDER"
    is AgendaItem.Event -> "EVENT"
}

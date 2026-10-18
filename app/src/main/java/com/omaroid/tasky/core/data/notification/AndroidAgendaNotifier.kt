package com.omaroid.tasky.core.data.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.omaroid.tasky.R
import com.omaroid.tasky.app.MainActivity
import com.omaroid.tasky.core.domain.notification.AgendaNotifier

class AndroidAgendaNotifier(private val context: Context) : AgendaNotifier {

    private val manager = NotificationManagerCompat.from(context)

    init {
        // Idempotent — safe to call on every construction; a no-op below API 26.
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName("Event invites")
                .setDescription("Notifies you when someone invites you to an event")
                .build(),
        )
    }

    override fun notifyEventInvite(eventId: String, title: String): Boolean {
        // On Android 13+ posting silently no-ops without the runtime POST_NOTIFICATIONS permission.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false // suppressed — tell the caller so the invite isn't marked as notified
        }

        // Tap opens the event's detail screen via MainActivity's deep-link path (same extras as
        // ReminderWorker). Request code is namespaced so it never collides with a reminder's.
        val contentIntent = PendingIntent.getActivity(
            context,
            "invite:$eventId".hashCode(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("itemId", eventId)
                putExtra("itemType", "EVENT")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.tasky_logo)
            .setContentTitle("New event invitation")
            .setContentText("You've been invited to \"$title\"")
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        // Stable id per event so re-syncing the same invite updates rather than stacks.
        manager.notify(eventId.hashCode(), notification)
        return true
    }

    private companion object {
        const val CHANNEL_ID = "event_invites"
    }
}

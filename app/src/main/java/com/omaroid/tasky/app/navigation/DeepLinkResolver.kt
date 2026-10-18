package com.omaroid.tasky.app.navigation

import android.content.Intent
import com.omaroid.tasky.feature.detail.AgendaDetailType

/**
 * Turns a notification tap [Intent] into a navigable destination — the single place that knows the
 * extras contract (`itemId`/`itemType`). Returns null when the intent carries no deep link, so
 * callers can treat "no link" and "malformed link" identically.
 */
class DeepLinkResolver {
    fun resolve(intent: Intent?): AgendaDetailRoute? {
        val itemId = intent?.getStringExtra("itemId") ?: return null
        val type = when (intent.getStringExtra("itemType")) {
            "TASK" -> AgendaDetailType.TASK
            "REMINDER" -> AgendaDetailType.REMINDER
            "EVENT" -> AgendaDetailType.EVENT
            else -> return null
        }
        return AgendaDetailRoute(type = type, itemId = itemId, isEditing = false)
    }
}

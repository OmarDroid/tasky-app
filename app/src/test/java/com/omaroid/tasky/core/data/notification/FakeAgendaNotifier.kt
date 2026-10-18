package com.omaroid.tasky.core.data.notification

import com.omaroid.tasky.core.domain.notification.AgendaNotifier

class FakeAgendaNotifier : AgendaNotifier {
    val invites = mutableListOf<Pair<String, String>>() // (eventId, title)

    /** Set false to simulate POST_NOTIFICATIONS being denied (notification suppressed). */
    var shouldPost = true

    override fun notifyEventInvite(eventId: String, title: String): Boolean {
        invites += eventId to title
        return shouldPost
    }
}

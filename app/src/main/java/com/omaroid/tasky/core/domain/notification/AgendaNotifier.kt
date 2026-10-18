package com.omaroid.tasky.core.domain.notification

/**
 * Posts local notifications for agenda events. The Tasky backend has no push channel, so an "invite"
 * is detected at sync time (an event the server reports the user did not create, newly arriving in
 * the local cache) and surfaced as a local notification.
 */
interface AgendaNotifier {
    /**
     * Notify the current user that they have been invited to an event.
     * @return true only if the notification was actually posted — false when it was suppressed
     * (e.g. POST_NOTIFICATIONS not granted), so callers don't record an invite the user never saw.
     */
    fun notifyEventInvite(eventId: String, title: String): Boolean
}

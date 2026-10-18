package com.omaroid.tasky.core.domain.agenda.repository

import com.omaroid.tasky.core.domain.agenda.models.Attendee

/**
 * Domain port for attendee lookups so the presentation layer never depends on the remote data source
 * or its DTOs. The returned [Attendee] only carries the identity fields known at lookup time
 * (email/username/userId); the caller supplies `eventId`/`remindAt` when persisting.
 */
interface AttendeeRepository {
    // GET /attendee?email= → the matching user, or null if no such user exists.
    suspend fun getAttendee(email: String): Attendee?

    // DELETE /attendee?eventId= → the current user leaves the event.
    suspend fun removeAttendee(eventId: String)
}

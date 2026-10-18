package com.omaroid.tasky.core.data.agenda

import com.omaroid.tasky.core.domain.agenda.models.Attendee
import com.omaroid.tasky.core.domain.agenda.repository.AttendeeRepository

class FakeAttendeeRepository : AttendeeRepository {
    var attendee: Attendee? = null
    var removeAttendeeCallCount = 0

    override suspend fun getAttendee(email: String): Attendee? = attendee
    override suspend fun removeAttendee(eventId: String) { removeAttendeeCallCount++ }
}

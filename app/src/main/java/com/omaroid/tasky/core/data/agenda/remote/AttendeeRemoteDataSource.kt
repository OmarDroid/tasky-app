package com.omaroid.tasky.core.data.agenda.remote

import com.omaroid.tasky.core.data.agenda.remote.dto.CheckAttendeeResponseDto

interface AttendeeRemoteDataSource {
    // GET /attendee?email= → the attendee (200) or null when the user does not exist (404).
    suspend fun getAttendeeByEmail(email: String): CheckAttendeeResponseDto?

    // DELETE /attendee?eventId= → leave the event as an attendee.
    suspend fun deleteAttendee(eventId: String)
}

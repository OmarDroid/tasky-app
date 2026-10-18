package com.omaroid.tasky.core.data.agenda.remote

import com.omaroid.tasky.core.data.agenda.remote.dto.CheckAttendeeResponseDto

class FakeAttendeeRemoteDataSource : AttendeeRemoteDataSource {
    var attendeeResponse: CheckAttendeeResponseDto? = null
    var deleteAttendeeCallCount = 0

    override suspend fun getAttendeeByEmail(email: String): CheckAttendeeResponseDto? = attendeeResponse
    override suspend fun deleteAttendee(eventId: String) { deleteAttendeeCallCount++ }
}

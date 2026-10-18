package com.omaroid.tasky.core.data.agenda

import com.omaroid.tasky.core.data.agenda.remote.AttendeeRemoteDataSource
import com.omaroid.tasky.core.data.agenda.remote.mappers.toDomain
import com.omaroid.tasky.core.domain.agenda.models.Attendee
import com.omaroid.tasky.core.domain.agenda.repository.AttendeeRepository

class AttendeeRepositoryImpl(
    private val remote: AttendeeRemoteDataSource,
) : AttendeeRepository {

    override suspend fun getAttendee(email: String): Attendee? =
        remote.getAttendeeByEmail(email)?.toDomain()

    override suspend fun removeAttendee(eventId: String) =
        remote.deleteAttendee(eventId)
}

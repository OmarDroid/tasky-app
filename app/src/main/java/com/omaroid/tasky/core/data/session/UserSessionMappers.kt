package com.omaroid.tasky.core.data.session

import com.omaroid.tasky.core.domain.session.UserSession
import kotlin.time.Instant

fun UserSessionDto.toDomain(): UserSession {
    return UserSession(
        userId = userId,
        username = username,
        accessToken = accessToken,
        refreshToken = refreshToken,
        expiresAt = Instant.fromEpochMilliseconds(expiresAtEpochMilliseconds),
    )
}

fun UserSession.toDto(): UserSessionDto {
    return UserSessionDto(
        userId = userId,
        username = username,
        accessToken = accessToken,
        refreshToken = refreshToken,
        expiresAtEpochMilliseconds = expiresAt.toEpochMilliseconds(),
    )
}

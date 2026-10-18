package com.omaroid.tasky.core.data.session

import kotlinx.serialization.Serializable

@Serializable
data class UserSessionDto(
    val userId: String,
    val username: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochMilliseconds: Long,
)

package com.omaroid.tasky.core.domain.session

import kotlin.time.Instant

data class UserSession(
    val userId: String,
    val username: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Instant,
)
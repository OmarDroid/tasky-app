package com.omaroid.tasky.core.domain.session

import kotlin.time.Instant

class CheckSessionValidUseCase(
    private val sessionLocalDataSource: SessionLocalDataSource,
) {
    suspend operator fun invoke(now: Instant): Boolean {
        val session = sessionLocalDataSource.getSession() ?: return false
        return session.expiresAt > now
    }
}
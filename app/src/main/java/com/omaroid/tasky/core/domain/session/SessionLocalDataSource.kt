package com.omaroid.tasky.core.domain.session

interface SessionLocalDataSource {
    suspend fun getSession(): UserSession?
    suspend fun saveSession(session: UserSession)
    suspend fun clearSession()
}
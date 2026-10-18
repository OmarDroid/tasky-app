package com.omaroid.tasky.core.data.session

import com.omaroid.tasky.core.domain.session.SessionLocalDataSource
import com.omaroid.tasky.core.domain.session.UserSession

class FakeSessionLocalDataSource : SessionLocalDataSource {
    var session: UserSession? = null
    override suspend fun getSession(): UserSession? = session
    override suspend fun saveSession(session: UserSession) { this.session = session }
    override suspend fun clearSession() { session = null }
}

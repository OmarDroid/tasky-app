package com.omaroid.tasky.core.data.auth

import com.omaroid.tasky.core.domain.session.SessionLocalDataSource
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

/**
 * Handles the OAuth bearer-token refresh flow.
 *
 * Uses the *unauthenticated* [HttpClient] (no Auth plugin), so there is no risk of an
 * infinite 401-refresh loop and [markAsRefreshTokenRequest] is not needed.
 *
 * Injected into Koin via [singleOf] — Koin resolves the unqualified [HttpClient] binding
 * automatically, which is always the unauthenticated client.
 */
class TokenRefreshHandler(
    private val sessionSource: SessionLocalDataSource,
    private val httpClient: HttpClient,
) {
    /**
     * POSTs to `/auth/refresh`, persists the new tokens to [SessionLocalDataSource],
     * and returns [BearerTokens] for Ktor's Auth plugin to retry the original request.
     * Returns null if there is no session or the server rejects the refresh token.
     */
    suspend fun refresh(): BearerTokens? {
        val session = sessionSource.getSession() ?: return null
        val response = httpClient.post("/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("refreshToken" to session.refreshToken))
        }
        if (!response.status.isSuccess()) return null
        // The refresh endpoint returns only a new access token — the refresh token is not reissued,
        // so we keep the one already in the session.
        val body = response.body<AccessTokenResponseDto>()
        val newAccess = body.accessToken
        val newRefresh = session.refreshToken
        sessionSource.saveSession(session.copy(accessToken = newAccess, refreshToken = newRefresh))
        return BearerTokens(newAccess, newRefresh)
    }
}

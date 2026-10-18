package com.omaroid.tasky.core.data.auth

import com.omaroid.tasky.core.domain.util.DataError
import com.omaroid.tasky.core.domain.util.Result
import com.omaroid.tasky.core.domain.util.safeCall
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class KtorAuthRemoteDataSource(private val httpClient: HttpClient): AuthRemoteDataSource {

    override suspend fun login(email: String, password: String): Result<LoginResponseDto, DataError.Network> =
        safeCall {
            httpClient.post("/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequestDto(email = email, password = password))
            }
        }

    override suspend fun register(
        fullName: String,
        email: String,
        password: String,
    ): Result<Unit, DataError.Network> = safeCall {
        httpClient.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequestDto(fullName = fullName, email = email, password = password))
        }
    }

    override suspend fun logout(refreshToken: String): Result<Unit, DataError.Network> = safeCall {
        httpClient.post("/auth/logout") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("refreshToken" to refreshToken))
        }
    }
}
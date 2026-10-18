package com.omaroid.tasky.core.domain.auth

import com.omaroid.tasky.core.domain.util.EmptyResult

interface AuthRepository {
    suspend fun login(email: String, password: String): EmptyResult<AuthError>
    suspend fun register(fullName: String, email: String, password: String): EmptyResult<AuthError>
    suspend fun logout(): EmptyResult<AuthError>
}
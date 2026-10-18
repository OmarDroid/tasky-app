package com.omaroid.tasky.core.data.auth

import com.omaroid.tasky.core.domain.util.DataError
import com.omaroid.tasky.core.domain.util.Result

interface AuthRemoteDataSource {
    suspend fun login(email: String, password: String): Result<LoginResponseDto, DataError.Network>
    suspend fun register(fullName: String, email: String, password: String): Result<Unit, DataError.Network>
    suspend fun logout(refreshToken: String): Result<Unit, DataError.Network>
}
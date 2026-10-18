package com.omaroid.tasky.core.data.auth

import com.omaroid.tasky.core.domain.util.DataError
import com.omaroid.tasky.core.domain.util.Result

class FakeAuthRemoteDataSource : AuthRemoteDataSource {
    var loginResponse: Result<LoginResponseDto, DataError.Network> = Result.Success(
        LoginResponseDto("", "", "", "", 0L)
    )
    var registerResponse: Result<Unit, DataError.Network> = Result.Success(Unit)
    var logoutResponse: Result<Unit, DataError.Network> = Result.Success(Unit)

    override suspend fun login(email: String, password: String) = loginResponse
    override suspend fun register(fullName: String, email: String, password: String) = registerResponse
    override suspend fun logout(refreshToken: String) = logoutResponse
}
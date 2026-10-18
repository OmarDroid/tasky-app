package com.omaroid.tasky.core.data.auth

import kotlinx.serialization.Serializable

// LoginRequestDto.kt
@Serializable
data class LoginRequestDto(val email: String, val password: String)

// RegisterRequestDto.kt
@Serializable
data class RegisterRequestDto(val fullName: String, val email: String, val password: String)

// LoginResponseDto.kt
@Serializable
data class LoginResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val username: String,
    val userId: String,
    val accessTokenExpirationTimestamp: Long,
)

// AccessTokenResponseDto.kt — the /auth/refresh response. Carries only a fresh access token (and its
// expiry); the refresh token is NOT reissued, so the caller keeps reusing the stored one.
@Serializable
data class AccessTokenResponseDto(
    val accessToken: String,
    val accessTokenExpirationTimestamp: Long = 0,
)


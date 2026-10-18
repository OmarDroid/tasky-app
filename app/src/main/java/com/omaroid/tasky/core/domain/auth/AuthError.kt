package com.omaroid.tasky.core.domain.auth

import com.omaroid.tasky.core.domain.util.Error

enum class AuthError : Error {
    INVALID_CREDENTIALS,
    EMAIL_ALREADY_EXISTS,
    UNKNOWN
}
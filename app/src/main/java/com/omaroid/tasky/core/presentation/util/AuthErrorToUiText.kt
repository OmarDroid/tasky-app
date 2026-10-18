package com.omaroid.tasky.core.presentation.util

import com.omaroid.tasky.R
import com.omaroid.tasky.core.domain.auth.AuthError

fun AuthError.toUiText(): UiText = when (this) {
    AuthError.INVALID_CREDENTIALS -> UiText.StringResource(R.string.error_invalid_credentials)
    AuthError.EMAIL_ALREADY_EXISTS -> UiText.StringResource(R.string.error_email_already_exists)
    AuthError.UNKNOWN -> UiText.StringResource(R.string.error_unknown)
}
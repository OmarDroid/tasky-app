package com.omaroid.tasky.core.domain.validation

data class PasswordValidationState(
    val hasMinLength: Boolean = false,
    val hasDigit: Boolean = false,
    val hasLowerCaseLetter: Boolean = false,
    val hasUpperCaseLetter: Boolean = false,
) {
    val isValid: Boolean
        get() = hasMinLength && hasDigit && hasLowerCaseLetter && hasUpperCaseLetter
}

package com.omaroid.tasky.core.domain.validation

class PasswordValidator {
    operator fun invoke(password: String) = PasswordValidationState(
        hasMinLength = password.length >= 9,
        hasDigit = password.any { it.isDigit() },
        hasLowerCaseLetter = password.any { it.isLowerCase() },
        hasUpperCaseLetter = password.any { it.isUpperCase() },
    )
}

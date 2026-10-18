package com.omaroid.tasky.core.domain.validation

import com.omaroid.tasky.core.domain.util.PatternValidator

class EmailValidator(private val patternValidator: PatternValidator) {
    operator fun invoke(email: String): Boolean = patternValidator.matches(email.trim())
}

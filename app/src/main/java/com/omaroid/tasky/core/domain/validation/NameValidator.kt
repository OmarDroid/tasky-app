package com.omaroid.tasky.core.domain.validation

class NameValidator {
    operator fun invoke(name: String): Boolean = name.trim().length in 4..50
}

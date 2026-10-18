package com.omaroid.tasky.core.domain.util

interface PatternValidator {
    fun matches(value: String): Boolean
}
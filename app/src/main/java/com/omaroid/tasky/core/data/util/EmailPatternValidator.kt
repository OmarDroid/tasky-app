package com.omaroid.tasky.core.data.util

import android.util.Patterns
import com.omaroid.tasky.core.domain.util.PatternValidator

class EmailPatternValidator : PatternValidator {
    override fun matches(value: String): Boolean =
        Patterns.EMAIL_ADDRESS.matcher(value).matches()
}
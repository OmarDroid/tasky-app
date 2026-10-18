package com.omaroid.tasky.core.presentation.util

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource


sealed interface UiText {
    data class DynamicString(val value: String) : UiText
    data class StringResource(
        @field:StringRes val id: Int,
        val args: List<Any> = listOf(),
    ) : UiText

    @Composable
    fun asString(): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> stringResource(id, *args.toTypedArray())
        }
    }
}
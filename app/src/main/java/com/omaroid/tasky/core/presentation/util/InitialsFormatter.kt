package com.omaroid.tasky.core.presentation.util

object InitialsFormatter {
    fun format(name: String): String {
        val words = name.trim().split(" ").filter { it.isNotBlank() }
        return when {
            words.isEmpty() -> ""
            words.size == 1 -> words[0].take(2).uppercase()
            else -> "${words.first().first().uppercaseChar()}${words.last().first().uppercaseChar()}"
        }
    }
}

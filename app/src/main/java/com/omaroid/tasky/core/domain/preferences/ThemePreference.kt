package com.omaroid.tasky.core.domain.preferences

/** User theme choice. SYSTEM follows the OS setting (the requirement's default). */
enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK;

    /** Cycle order for the toolbar toggle: System → Light → Dark → System. */
    fun next(): ThemePreference = when (this) {
        SYSTEM -> LIGHT
        LIGHT -> DARK
        DARK -> SYSTEM
    }
}

package com.omaroid.tasky.core.presentation.util

import kotlinx.datetime.LocalTime

fun LocalTime.formatForAgenda(): String = "%02d:%02d".format(hour, minute)

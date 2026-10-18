package com.omaroid.tasky.core.data.agenda.remote.mappers

import kotlin.time.Instant

fun String.toEpochMs(): Long = Instant.parse(this).toEpochMilliseconds()
fun Long.toIso8601(): String = Instant.fromEpochMilliseconds(this).toString()
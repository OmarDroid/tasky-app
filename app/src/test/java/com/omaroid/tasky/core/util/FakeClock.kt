package com.omaroid.tasky.core.util

import kotlin.time.Clock
import kotlin.time.Instant

class FakeClock(private val fixedInstant: Instant) : Clock {
    override fun now() = fixedInstant
}
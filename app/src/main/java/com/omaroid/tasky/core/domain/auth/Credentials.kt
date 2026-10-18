package com.omaroid.tasky.core.domain.auth

/** Saved login credentials for the opt-in "Remember me" prefill. */
data class Credentials(val email: String, val password: String)

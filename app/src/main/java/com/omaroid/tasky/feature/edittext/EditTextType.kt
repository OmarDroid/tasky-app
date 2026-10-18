package com.omaroid.tasky.feature.edittext

import kotlinx.serialization.Serializable

// @Serializable required — used as a type-safe navigation argument in EditTextRoute.
@Serializable
enum class EditTextType { TITLE, DESCRIPTION }
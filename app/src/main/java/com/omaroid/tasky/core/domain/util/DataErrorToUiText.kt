package com.omaroid.tasky.core.domain.util

import com.omaroid.tasky.R
import com.omaroid.tasky.core.presentation.util.UiText
import com.omaroid.tasky.core.presentation.util.UiText.*

fun DataError.toUiText(): UiText = when (this) {
    DataError.Network.NO_INTERNET -> StringResource(R.string.error_no_internet)
    DataError.Network.UNAUTHORIZED -> StringResource(R.string.error_unauthorized)
    DataError.Network.CONFLICT -> StringResource(R.string.error_conflict)
    DataError.Network.SERVER_ERROR -> StringResource(R.string.error_server)
    DataError.Network.REQUEST_TIMEOUT -> StringResource(R.string.error_request_timeout)
    DataError.Network.TOO_MANY_REQUESTS -> StringResource(R.string.error_too_many_requests)
    DataError.Network.SERIALIZATION -> StringResource(R.string.error_serialization)
    DataError.Local.DISK_FULL -> StringResource(R.string.error_disk_full)
    DataError.Local.NOT_FOUND -> StringResource(R.string.error_not_found)
    DataError.Local.UNKNOWN -> StringResource(R.string.error_unknown)
    DataError.Network.BAD_REQUEST -> StringResource(R.string.error_bad_request)
    DataError.Network.FORBIDDEN -> StringResource(R.string.error_forbidden)
    DataError.Network.NOT_FOUND -> StringResource(R.string.error_not_found_network)
    DataError.Network.PAYLOAD_TOO_LARGE -> StringResource(R.string.error_payload_too_large)
    DataError.Network.SERVICE_UNAVAILABLE -> StringResource(R.string.error_service_unavailable)
    DataError.Network.UNKNOWN -> StringResource(R.string.error_network_unknown)
}

package com.omaroid.tasky.feature.detail.model

import com.omaroid.tasky.core.domain.agenda.models.Photo

data class PhotoUi(val key: String, val url: String)

fun Photo.toUi() = PhotoUi(key = key, url = url)
fun PhotoUi.toDomain() = Photo(key = key, url = url)

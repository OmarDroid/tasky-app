package com.omaroid.tasky.core.data.photo

import com.omaroid.tasky.core.domain.photo.LocalPhotoStorage

class FakeLocalPhotoStorage : LocalPhotoStorage {
    val deleted = mutableListOf<String>()

    // Echo the uri back as if it were already a stable cached file, so tests can assert on the value
    // the ViewModel stored in localPhotoUris.
    override suspend fun cache(uri: String): String? = uri

    override suspend fun delete(uri: String) {
        deleted += uri
    }
}

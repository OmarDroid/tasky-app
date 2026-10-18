package com.omaroid.tasky.core.domain.photo

interface PhotoCompressor {
    /** Returns JPEG bytes <= 1 MB for the given content uri, or null if it can't get under the limit. */
    suspend fun compress(uri: String): ByteArray?
}

package com.omaroid.tasky.core.domain.photo

/**
 * Persists a picked photo into app-private storage so it survives process death.
 *
 * The system photo picker hands back a transient `content://` uri whose read permission is revoked
 * once the app process dies — rendering it blank after a restart. Caching the bytes into the app's
 * own files dir and handing back a stable `file://` uri makes locally-added photos display reliably
 * before they have synced, and gives the upload step a readable source.
 */
interface LocalPhotoStorage {
    /** Copies [uri] into app storage, returning a stable `file://` uri, or null if it can't be read. */
    suspend fun cache(uri: String): String?

    /** Best-effort removal of a previously cached local photo (no-op for non-local uris). */
    suspend fun delete(uri: String)
}

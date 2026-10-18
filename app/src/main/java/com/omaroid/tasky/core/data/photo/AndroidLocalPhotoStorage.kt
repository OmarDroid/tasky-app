package com.omaroid.tasky.core.data.photo

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.omaroid.tasky.core.domain.photo.LocalPhotoStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class AndroidLocalPhotoStorage(private val context: Context) : LocalPhotoStorage {

    private val photosDir: File by lazy {
        File(context.filesDir, "event_photos").apply { mkdirs() }
    }

    override suspend fun cache(uri: String): String? = withContext(Dispatchers.IO) {
        // Already one of our cached files — nothing to copy.
        if (uri.startsWith("file://")) return@withContext uri
        try {
            val input = context.contentResolver.openInputStream(uri.toUri()) ?: return@withContext null
            val file = File(photosDir, "${UUID.randomUUID()}.jpg")
            input.use { source -> file.outputStream().use { sink -> source.copyTo(sink) } }
            Uri.fromFile(file).toString()
        } catch (e: CancellationException) {
            // Never swallow cancellation — if the ViewModel is cleared mid-cache it must bubble up.
            throw e
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun delete(uri: String) {
        withContext(Dispatchers.IO) {
            if (!uri.startsWith("file://")) return@withContext
            try {
                uri.toUri().path?.let { File(it) }?.takeIf { it.exists() }?.delete()
            } catch (e: CancellationException) {
                throw e // runCatching would have swallowed this
            } catch (e: Exception) {
                // best-effort delete
            }
        }
    }
}

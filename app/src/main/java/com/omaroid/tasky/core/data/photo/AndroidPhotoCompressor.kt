package com.omaroid.tasky.core.data.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.omaroid.tasky.core.domain.photo.PhotoCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.coroutines.coroutineContext
import androidx.core.net.toUri

class AndroidPhotoCompressor(private val context: Context) : PhotoCompressor {

    // Bitmap decode + JPEG compression is CPU-bound work, so it belongs on Dispatchers.Default,
    // not the I/O pool (which is sized for blocking calls, not computation).
    override suspend fun compress(uri: String): ByteArray? = withContext(Dispatchers.Default) {
        val maxBytes = 1024 * 1024 // 1 MB
        val bitmap = context.contentResolver.openInputStream(uri.toUri())?.use {
            BitmapFactory.decodeStream(it)
        } ?: return@withContext null

        var quality = 90
        var bytes: ByteArray
        do {
            // Compression can iterate several times on a large image — check cancellation each pass
            // so a cleared ViewModel actually stops the work instead of running to completion.
            coroutineContext.ensureActive()
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            bytes = stream.toByteArray()
            quality -= 10
        } while (bytes.size > maxBytes && quality > 0)

        if (bytes.size > maxBytes) null else bytes // null → caller skips this photo ("x skipped")
    }
}

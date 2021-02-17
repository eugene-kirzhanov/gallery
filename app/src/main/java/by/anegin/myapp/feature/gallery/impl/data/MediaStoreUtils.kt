package by.anegin.myapp.feature.gallery.impl.data

import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object MediaStoreUtils {

    suspend fun <T> loadMedia(context: Context, contentUri: Uri, projection: Array<String>, mapper: (Cursor) -> T): List<T> =
        suspendCancellableCoroutine { continuation ->
            val isCanceled = AtomicBoolean(false)
            continuation.invokeOnCancellation {
                isCanceled.set(true)
            }
            try {
                context.contentResolver.query(contentUri, projection, null, null, null)?.use { cursor ->
                    val items = ArrayList<T>(cursor.count)
                    while (cursor.moveToNext() && !isCanceled.get()) {
                        items.add(mapper(cursor))
                    }
                    if (!isCanceled.get()) {
                        continuation.resume(items)
                    }
                } ?: run {
                    continuation.resume(emptyList())
                }
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }

    suspend fun extractVideoDuration(context: Context, uri: Uri): Long = withContext(Dispatchers.IO) {
        val mediaMetadataRetriever = MediaMetadataRetriever()
        try {
            mediaMetadataRetriever.setDataSource(context, uri)
            ensureActive()
            mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: -1
        } catch (e: Exception) {
            Timber.e("Error extracting video duration from video `$uri`: ${e.message}")
            -1
        }
    }

}
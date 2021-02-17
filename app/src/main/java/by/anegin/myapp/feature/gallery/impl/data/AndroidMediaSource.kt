package by.anegin.myapp.feature.gallery.impl.data

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import by.anegin.myapp.feature.gallery.api.data.MediaSource
import by.anegin.myapp.feature.gallery.api.model.Media
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import javax.inject.Inject

class AndroidMediaSource @Inject constructor(
    @ApplicationContext private val context: Context
) : MediaSource {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var loadImagesJob: Job? = null
    private var loadVideosJob: Job? = null

    private val images = MutableStateFlow<List<Media.Image>?>(null)
    private val videos = MutableStateFlow<List<Media.Video>?>(null)

    private val _media = combine(images.filterNotNull(), videos.filterNotNull()) { images, videos ->
        (images + videos).sortedByDescending { it.timestamp }
    }
    override val media: Flow<List<Media>> = _media

    private val observerHandler = Handler(Looper.getMainLooper())

    private val imagesObserver = object : ContentObserver(observerHandler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            reloadImages()
        }
    }

    private val videosObserver = object : ContentObserver(observerHandler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            reloadVideos()
        }
    }

    private val videoDurationsCache = HashMap<VideoDurationCacheItem, Long?>()

    data class VideoDurationCacheItem(
        val uri: Uri,
        val size: Long,
        val createTime: Long?,
        val modifyTime: Long?
    )

    override fun init() {
        context.contentResolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, imagesObserver)
        context.contentResolver.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, videosObserver)
        if (images.value == null && loadImagesJob == null) {
            reloadImages()
        }
        if (videos.value == null && loadVideosJob == null) {
            reloadVideos()
        }
    }

    override fun destroy() {
        context.contentResolver.unregisterContentObserver(imagesObserver)
        context.contentResolver.unregisterContentObserver(videosObserver)
        loadImagesJob?.cancel()
        loadImagesJob = null
        loadVideosJob?.cancel()
        loadVideosJob = null
    }

    private fun reloadImages() {
        loadImagesJob?.cancel()
        loadImagesJob = scope.launch {
            val imagesProjection = arrayOf(
                MediaStore.Images.Media._ID,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Images.Media.DATE_TAKEN
                } else {
                    MediaStore.Images.Media.DATE_ADDED
                }
            )
            images.value = MediaStoreUtils.loadMedia(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imagesProjection) { cursor ->
                val id = cursor.getLong(0)
                val createTime = cursor.getLong(1)
                val imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                Media.Image(imageUri.toString(), createTime)
            }
        }
    }

    private fun reloadVideos() {
        loadVideosJob?.cancel()
        loadVideosJob = scope.launch {
            val notLoadedDurations = ArrayList<VideoDurationCacheItem>()

            val videosProjection = arrayOf(
                MediaStore.Video.Media._ID,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Video.Media.DATE_TAKEN
                } else {
                    MediaStore.Video.Media.DATE_ADDED
                },
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Video.Media.DURATION
                } else {
                    MediaStore.Video.Media.SIZE
                },
                MediaStore.Video.Media.DATE_MODIFIED
            )

            videos.value = MediaStoreUtils.loadMedia(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videosProjection) { cursor ->
                val id = cursor.getLong(0)
                val createTime = cursor.getLong(1)

                val videoUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                val duration = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getLong(2)
                } else {
                    val size = cursor.getLong(2)
                    val modifyTime = cursor.getLong(3)
                    val videoDurationCacheItem = VideoDurationCacheItem(videoUri, size, createTime, modifyTime)
                    videoDurationsCache[videoDurationCacheItem]
                        ?: run {
                            notLoadedDurations.add(videoDurationCacheItem)
                            null
                        }
                }

                Media.Video(videoUri.toString(), createTime, duration)
            }

            // extract durations from video files, updating videos list on each 10 loaded durations
            if (notLoadedDurations.isNotEmpty()) {
                val loadedDurationsMap = HashMap<String, Long>()
                notLoadedDurations
                    .sortedByDescending { it.createTime }
                    .forEachIndexed { index, cacheItem ->

                        MediaStoreUtils.extractVideoDuration(context, cacheItem.uri).also { duration ->
                            videoDurationsCache[cacheItem] = duration
                            loadedDurationsMap[cacheItem.uri.toString()] = duration
                        }

                        ensureActive()

                        // update videos list for each 10 loaded durations
                        if (loadedDurationsMap.size == 10 || index == notLoadedDurations.lastIndex) {
                            videos.value?.let { currentVideos ->
                                videos.value = currentVideos.map { video ->
                                    if (video.duration == null) {
                                        loadedDurationsMap[video.uri]?.let { duration ->
                                            video.copy(duration = duration)
                                        } ?: video
                                    } else {
                                        video
                                    }
                                }
                            }
                            loadedDurationsMap.clear()
                        }
                    }
            }
        }
    }

}
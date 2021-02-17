package by.anegin.myapp.feature.gallery.impl.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import by.anegin.myapp.feature.gallery.api.data.MediaSource
import by.anegin.myapp.feature.gallery.api.model.Media
import by.anegin.myapp.feature.gallery.impl.ui.model.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import java.util.*
import javax.inject.Inject

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val mediaSource: MediaSource
) : ViewModel() {

    private val isStoragePermissionGranted = MutableStateFlow(false)
    private val selectedUris = MutableStateFlow<List<Uri>>(emptyList())

    val mediaItems = combine(
        isStoragePermissionGranted.filter { it },
        mediaSource.media,
        selectedUris,
        ::makeMediaItems
    )
        .flowOn(Dispatchers.Default)
        .asLiveData(viewModelScope.coroutineContext)

    init {
        mediaSource.init()
    }

    override fun onCleared() {
        mediaSource.destroy()
        super.onCleared()
    }

    fun onStoragePermissionGranted() {
        isStoragePermissionGranted.value = true
    }

    fun toggleMediaItem(mediaItem: MediaItem): Boolean {
        val selectedUris = this.selectedUris.value.toMutableList()
        if (!selectedUris.remove(mediaItem.uri)) {
            selectedUris.add(mediaItem.uri)
        }
        this.selectedUris.value = selectedUris
        return true
    }

    private fun makeMediaItems(isStoragePermissionGranted: Boolean, mediaList: List<Media>, selectedUris: List<Uri>): List<MediaItem> {
        return if (!isStoragePermissionGranted) {
            return emptyList()
        } else {
            mediaList.map { media ->
                val uri = Uri.parse(media.uri)

                var selectionNumber: Int? = selectedUris.indexOf(uri) + 1
                if (selectionNumber == 0) selectionNumber = null

                when (media) {
                    is Media.Image -> {
                        MediaItem.Image(uri, selectionNumber)
                    }
                    is Media.Video -> {
                        val duration = media.duration?.let {
                            val totalSeconds = it / 1000
                            val minutes = totalSeconds / 60
                            val seconds = totalSeconds - minutes * 60
                            String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
                        } ?: "…"
                        MediaItem.Video(uri, duration, selectionNumber)
                    }
                }
            }
        }
    }

}
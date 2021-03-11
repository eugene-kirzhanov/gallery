package by.anegin.myapp.feature.gallery.impl.ui.gallery

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import by.anegin.myapp.feature.gallery.api.data.MediaSource
import by.anegin.myapp.feature.gallery.api.model.Media
import by.anegin.myapp.feature.gallery.impl.ui.gallery.model.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import java.util.*
import javax.inject.Inject

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val mediaSource: MediaSource
) : ViewModel() {

    private val isStoragePermissionGranted = MutableStateFlow(false)
    private val selectedUris = MutableStateFlow<List<Uri>>(emptyList())

    private val _mediaItems = combine(
        isStoragePermissionGranted.filter { it },
        mediaSource.media,
        selectedUris,
        ::makeMediaItems
    ).flowOn(Dispatchers.Default)
    val mediaItems = _mediaItems.asLiveData(viewModelScope.coroutineContext)

    val selectedCount = selectedUris.map { it.size }
        .asLiveData(viewModelScope.coroutineContext)

    private val _isInFullScreenMode = MutableStateFlow(false)
    val isInFullScreenMode = _isInFullScreenMode.asLiveData(viewModelScope.coroutineContext)

    private val _currentMediaItemUri = MutableStateFlow<Uri?>(null)
    private val _currentMediaItem = combine(_mediaItems, _currentMediaItemUri) { mediaItems, currentMediaItemUri ->
        currentMediaItemUri?.let { uri ->
            mediaItems.find { it.uri == uri }
        }
    }
    val currentMediaItem = _currentMediaItem.asLiveData(viewModelScope.coroutineContext)

    private val _toolbarCounter = combine(selectedUris, _currentMediaItemUri) { selectedUris, currentMediaItemUri ->
        if (currentMediaItemUri != null) selectedUris.size else 0
    }
    val toolbarCounter = _toolbarCounter.asLiveData(viewModelScope.coroutineContext)

    private val _toolbarCheck = _currentMediaItem.map { currentMediaItem ->
        currentMediaItem?.let { it.selectionNumber != null }
    }
    val toolbarCheck = _toolbarCheck.asLiveData(viewModelScope.coroutineContext)

    init {
        mediaSource.init()
    }

    override fun onCleared() {
        mediaSource.destroy()
        super.onCleared()
    }

    fun toggleFullScreen() {
        _isInFullScreenMode.value = _isInFullScreenMode.value != true
    }

    fun isInFullScreenMode() = _isInFullScreenMode.value

    fun onStoragePermissionGranted() {
        isStoragePermissionGranted.value = true
    }

    fun toggleMediaItem(mediaItem: MediaItem) {
        val selectedUris = this.selectedUris.value.toMutableList()
        if (!selectedUris.remove(mediaItem.uri)) {
            selectedUris.add(mediaItem.uri)
        }
        this.selectedUris.value = selectedUris
    }

    fun getSelectedUris(): List<Uri> {
        return selectedUris.value
    }

    fun onCurrentMediaItemChanged(mediaItemUri: Uri?) {
        _currentMediaItemUri.value = mediaItemUri
    }

    fun getCurrentMediaItemUri() = _currentMediaItemUri.value

    fun toggleCurrentMediaItem() {
        currentMediaItem.value?.let {
            toggleMediaItem(it)
        }
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
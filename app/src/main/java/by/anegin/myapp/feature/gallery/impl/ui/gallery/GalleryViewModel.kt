package by.anegin.myapp.feature.gallery.impl.ui.gallery

import android.graphics.Rect
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
import kotlin.math.max
import kotlin.math.min

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val mediaSource: MediaSource
) : ViewModel() {

    private val isStoragePermissionGranted = MutableStateFlow(false)

    private var prevSelectedUris: List<Uri> = emptyList()

    private val allSelectedUris = MutableStateFlow<List<Uri>>(emptyList())

    private val _mediaItems = combine(
        isStoragePermissionGranted.filter { it },
        mediaSource.media,
        allSelectedUris,
        ::makeMediaItems
    ).flowOn(Dispatchers.Default)
    val mediaItems = _mediaItems.asLiveData(viewModelScope.coroutineContext)

    val selectedCount = allSelectedUris.map { it.size }

    val isInFullScreenMode = MutableStateFlow(false)

    private val _currentMediaItemUri = MutableStateFlow<Uri?>(null)
    private val _currentMediaItem = combine(_mediaItems, _currentMediaItemUri) { mediaItems, currentMediaItemUri ->
        currentMediaItemUri?.let { uri ->
            mediaItems.find { it.uri == uri }
        }
    }.flowOn(Dispatchers.Default)
    val currentMediaItem = _currentMediaItem.asLiveData(viewModelScope.coroutineContext)

    val toolbarCounter = combine(allSelectedUris, _currentMediaItemUri) { selectedUris, currentMediaItemUri ->
        if (currentMediaItemUri != null) selectedUris.size else 0
    }

    val isToolbarCheckVisible = _currentMediaItem.map { currentMediaItem ->
        currentMediaItem?.let { it.selectionNumber != null }
    }

    val isTopSendButtonVisible = _currentMediaItemUri.map { currentMediaItemUri ->
        currentMediaItemUri != null
    }

    private val _sendButtonVisibility = combine(allSelectedUris, _currentMediaItemUri) { selectedUris, currentMediaItemUri ->
        currentMediaItemUri == null && selectedUris.isNotEmpty()
    }
    val sendButtonVisiblity = _sendButtonVisibility.asLiveData(viewModelScope.coroutineContext)

    private val _sendButtonExtended = combine(allSelectedUris, _currentMediaItem) { selectedUris, currentMediaItemUri ->
        selectedUris.isNotEmpty() && currentMediaItemUri == null
    }
    val sendButtonExtended = _sendButtonExtended.asLiveData(viewModelScope.coroutineContext)

    var insets = Rect()
    var topToolbarHeight = 0
    var bottomToolbarHeight = 0

    init {
        mediaSource.init()
    }

    override fun onCleared() {
        mediaSource.destroy()
        super.onCleared()
    }

    fun toggleFullScreen() {
        isInFullScreenMode.value = isInFullScreenMode.value != true
    }

    fun setFullScreen(fullscreen: Boolean) {
        isInFullScreenMode.value = fullscreen
    }

    fun isInFullScreenMode() = isInFullScreenMode.value

    fun onStoragePermissionGranted() {
        isStoragePermissionGranted.value = true
    }

    fun toggleMediaItem(mediaItem: MediaItem) {
        val selectedUris = this.allSelectedUris.value.toMutableList()
        if (!selectedUris.remove(mediaItem.uri)) {
            selectedUris.add(mediaItem.uri)
        }
        this.allSelectedUris.value = selectedUris
        this.prevSelectedUris = selectedUris
    }

    fun onGestureSelectionChanged(startIndex: Int, endIndex: Int) {
        val mediaItems = mediaItems.value ?: emptyList()

        val fromIndex = min(startIndex, endIndex)
        val toIndex = max(startIndex, endIndex)
        var gestureSelectedUris = (fromIndex..toIndex)
            .mapNotNull { index ->
                mediaItems.getOrNull(index)?.uri
            }
        if (startIndex > endIndex) {
            gestureSelectedUris = gestureSelectedUris.reversed()
        }

        this.allSelectedUris.value = this.prevSelectedUris + gestureSelectedUris.subtract(this.prevSelectedUris)
    }

    fun onGestureSelectionFinished() {
        this.prevSelectedUris = this.allSelectedUris.value
    }

    fun getSelectedUris(): List<Uri> {
        return this.allSelectedUris.value
    }

    fun onCurrentMediaItemChanged(mediaItem: MediaItem?) {
        _currentMediaItemUri.value = mediaItem?.uri
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
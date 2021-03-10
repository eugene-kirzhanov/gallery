package by.anegin.myapp.feature.gallery.impl.ui.viewer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import by.anegin.myapp.feature.gallery.api.data.MediaSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MediaViewerViewModel @Inject constructor(
    private val mediaSource: MediaSource
) : ViewModel() {

    private val _isInFullScreenMode = MutableLiveData(false)
    val isInFullScreenMode: LiveData<Boolean> = _isInFullScreenMode

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

}
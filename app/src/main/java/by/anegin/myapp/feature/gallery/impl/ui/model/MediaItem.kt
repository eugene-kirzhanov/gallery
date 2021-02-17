package by.anegin.myapp.feature.gallery.impl.ui.model

import android.net.Uri

sealed class MediaItem(
    open val uri: Uri,
    open val selectionNumber: Int?
) {

    data class Image(
        override val uri: Uri,
        override val selectionNumber: Int?
    ) : MediaItem(uri, selectionNumber)

    data class Video(
        override val uri: Uri,
        val duration: String,
        override val selectionNumber: Int?
    ) : MediaItem(uri, selectionNumber)

}
package by.anegin.myapp.feature.gallery.impl.ui.gallery.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class MediaItem(
    open val uri: Uri,
    open val selectionNumber: Int?
) : Parcelable {

    @Parcelize
    data class Image(
        override val uri: Uri,
        override val selectionNumber: Int?
    ) : MediaItem(uri, selectionNumber)

    @Parcelize
    data class Video(
        override val uri: Uri,
        val duration: String,
        override val selectionNumber: Int?
    ) : MediaItem(uri, selectionNumber)

}
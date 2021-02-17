package by.anegin.myapp.feature.gallery.api.model

sealed class Media(
    open val uri: String,
    open val timestamp: Long
) {

    data class Image(
        override val uri: String,
        override val timestamp: Long
    ) : Media(uri, timestamp)

    data class Video(
        override val uri: String,
        override val timestamp: Long,
        val duration: Long?
    ) : Media(uri, timestamp)

}

package by.anegin.myapp.feature.gallery.api.data

import by.anegin.myapp.feature.gallery.api.model.Media
import kotlinx.coroutines.flow.Flow

interface MediaSource {

    val media: Flow<List<Media>>

    fun init()

    fun destroy()

}
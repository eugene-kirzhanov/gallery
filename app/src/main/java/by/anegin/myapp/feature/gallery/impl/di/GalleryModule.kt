package by.anegin.myapp.feature.gallery.impl.di

import by.anegin.myapp.feature.gallery.api.data.MediaSource
import by.anegin.myapp.feature.gallery.impl.data.AndroidMediaSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
@Suppress("unused")
interface GalleryModule {

    @Binds
    fun bindImagesSource(impl: AndroidMediaSource): MediaSource

}
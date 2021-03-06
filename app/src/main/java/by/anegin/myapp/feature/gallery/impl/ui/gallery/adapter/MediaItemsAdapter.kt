package by.anegin.myapp.feature.gallery.impl.ui.gallery.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import by.anegin.myapp.feature.gallery.impl.ui.gallery.model.MediaItem
import com.bumptech.glide.RequestManager

class MediaItemsAdapter(
    private val glide: RequestManager,
    private val onMediaItemClick: (MediaItem) -> Unit,
    private val onMediaItemLongClick: (Int) -> Unit,
    private val onMediaItemToggleClick: (MediaItem) -> Unit
) : ListAdapter<MediaItem, RecyclerView.ViewHolder>(MediaItemsDiffCallback()) {

    companion object {
        private const val ITEM_IMAGE = 1
        private const val ITEM_VIDEO = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is MediaItem.Image -> ITEM_IMAGE
            is MediaItem.Video -> ITEM_VIDEO
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_IMAGE -> ImageViewHolder(parent, glide, onMediaItemClick, onMediaItemLongClick, onMediaItemToggleClick)
            ITEM_VIDEO -> VideoViewHolder(parent, glide, onMediaItemClick, onMediaItemLongClick, onMediaItemToggleClick)
            else -> throw IllegalArgumentException("Unknown viewType: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is MediaItem.Image -> (holder as? ImageViewHolder)?.bind(item)
            is MediaItem.Video -> (holder as? VideoViewHolder)?.bind(item)
        }
    }

    private class MediaItemsDiffCallback : DiffUtil.ItemCallback<MediaItem>() {
        override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
            if (oldItem is MediaItem.Image && newItem is MediaItem.Image) {
                return oldItem.uri == newItem.uri
            } else if (oldItem is MediaItem.Video && newItem is MediaItem.Video) {
                return oldItem.uri == newItem.uri
            }
            return false
        }

        override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
            if (oldItem is MediaItem.Image && newItem is MediaItem.Image) {
                return oldItem == newItem
            } else if (oldItem is MediaItem.Video && newItem is MediaItem.Video) {
                return oldItem == newItem
            }
            return false
        }
    }

}
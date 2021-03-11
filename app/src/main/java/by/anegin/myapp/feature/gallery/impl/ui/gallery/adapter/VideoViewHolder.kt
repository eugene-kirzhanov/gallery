package by.anegin.myapp.feature.gallery.impl.ui.gallery.adapter

import android.view.ViewGroup
import android.widget.ImageView
import by.anegin.myapp.R
import by.anegin.myapp.databinding.ItemVideoBinding
import by.anegin.myapp.feature.gallery.impl.ui.gallery.model.MediaItem
import by.anegin.myapp.feature.gallery.impl.ui.gallery.util.SimpleGlideRequestListener
import by.anegin.myapp.feature.gallery.impl.ui.gallery.util.ViewBindingViewHolder
import by.anegin.myapp.feature.gallery.impl.ui.gallery.util.setSingleClickListener
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade

class VideoViewHolder(
    parent: ViewGroup,
    private val glide: RequestManager,
    onClick: (ImageView, MediaItem.Video) -> Unit,
    onToggleClick: (MediaItem.Video) -> Unit
) : ViewBindingViewHolder<ItemVideoBinding>(parent, ItemVideoBinding::inflate) {

    private var video: MediaItem.Video? = null

    private val centerCropTransformation = CenterCrop()
    private val crossFadeTransition = withCrossFade(200)

    private val requestListener = SimpleGlideRequestListener {
        binding.imagePreview.setImageResource(0)
        binding.imagePreview.scaleType = ImageView.ScaleType.CENTER_CROP
        false
    }

    private val selectionHelper = MediaSelectionHelper(this, binding.layoutImageContainer)

    init {
        binding.root.setSingleClickListener {
            video?.let {
                onClick(binding.imagePreview, it)
            }
        }
        binding.textSelectionIndex.setSingleClickListener {
            video?.let(onToggleClick)
        }
    }

    fun bind(video: MediaItem.Video) {
        val isNewData = this.video != null && this.video?.uri != video.uri
        this.video = video

        binding.textDuration.text = video.duration

        binding.imagePreview.scaleType = ImageView.ScaleType.CENTER_INSIDE
        glide.load(video.uri)
            .placeholder(R.drawable.ic_video_placeholder)
            .listener(requestListener)
            .transform(centerCropTransformation)
            .transition(crossFadeTransition)
            .into(binding.imagePreview)

        video.selectionNumber?.let {
            binding.textSelectionIndex.setBackgroundResource(R.drawable.selection_index_bg_selected)
            binding.textSelectionIndex.text = it.toString()
        } ?: run {
            binding.textSelectionIndex.setBackgroundResource(R.drawable.selection_index_bg_normal)
            binding.textSelectionIndex.text = ""
        }

        selectionHelper.setSelectionState(
            isSelected = video.selectionNumber != null,
            disableAnimation = isNewData
        )
    }

}
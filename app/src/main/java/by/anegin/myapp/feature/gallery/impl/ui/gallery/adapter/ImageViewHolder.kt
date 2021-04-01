package by.anegin.myapp.feature.gallery.impl.ui.gallery.adapter

import android.view.ViewGroup
import android.widget.ImageView
import by.anegin.myapp.R
import by.anegin.myapp.databinding.GalleryItemImageBinding
import by.anegin.myapp.feature.gallery.impl.ui.gallery.model.MediaItem
import by.anegin.myapp.feature.gallery.impl.ui.gallery.util.SimpleGlideRequestListener
import by.anegin.myapp.feature.gallery.impl.ui.gallery.util.ViewBindingViewHolder
import by.anegin.myapp.feature.gallery.impl.ui.gallery.util.setSingleClickListener
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade

class ImageViewHolder(
    parent: ViewGroup,
    private val glide: RequestManager,
    onClick: (MediaItem.Image) -> Unit,
    onLongClick: (Int) -> Unit,
    onToggleClick: (MediaItem.Image) -> Unit
) : ViewBindingViewHolder<GalleryItemImageBinding>(parent, GalleryItemImageBinding::inflate) {

    private var image: MediaItem.Image? = null

    private val centerCropTransformation = CenterCrop()
    private val crossFadeTransition = withCrossFade(200)

    private val requestListener = SimpleGlideRequestListener {
        binding.imagePreview.setImageResource(0)
        binding.imagePreview.scaleType = ImageView.ScaleType.CENTER_CROP
        false
    }

    private val selectionHelper = MediaSelectionHelper(this, binding.imagePreview)

    init {
        binding.root.setSingleClickListener {
            image?.let(onClick)
        }
        binding.root.setOnLongClickListener {
            onLongClick(adapterPosition)
            true
        }
        binding.textSelectionIndex.setOnLongClickListener {
            onLongClick(adapterPosition)
            true
        }
        binding.textSelectionIndex.setSingleClickListener {
            image?.let(onToggleClick)
        }
    }

    fun bind(image: MediaItem.Image) {
        val isNewData = this.image != null && this.image?.uri != image.uri
        this.image = image

        binding.imagePreview.scaleType = ImageView.ScaleType.CENTER_INSIDE
        glide.load(image.uri)
            .placeholder(R.drawable.ic_image_placeholder)
            .listener(requestListener)
            .transform(centerCropTransformation)
            .transition(crossFadeTransition)
            .into(binding.imagePreview)

        image.selectionNumber?.let {
            binding.textSelectionIndex.setBackgroundResource(R.drawable.selection_index_bg_selected)
            binding.textSelectionIndex.text = it.toString()
        } ?: run {
            binding.textSelectionIndex.setBackgroundResource(R.drawable.selection_index_bg_normal)
            binding.textSelectionIndex.text = ""
        }

        selectionHelper.setSelectionState(
            isSelected = image.selectionNumber != null,
            disableAnimation = isNewData
        )
    }

}
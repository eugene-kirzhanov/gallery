package by.anegin.myapp.feature.gallery.impl.ui.adapter

import android.view.ViewGroup
import android.widget.ImageView
import by.anegin.myapp.R
import by.anegin.myapp.common.ui.ViewBindingViewHolder
import by.anegin.myapp.databinding.ItemImageBinding
import by.anegin.myapp.feature.gallery.impl.ui.model.MediaItem
import by.anegin.myapp.feature.gallery.impl.ui.util.SimpleGlideRequestListener
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade

class ImageViewHolder(
    parent: ViewGroup,
    private val glide: RequestManager,
    onClick: (ImageView, MediaItem.Image) -> Unit,
    onToggleClick: (MediaItem.Image) -> Unit
) : ViewBindingViewHolder<ItemImageBinding>(parent, ItemImageBinding::inflate) {

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
        binding.root.setOnClickListener { _ ->
            image?.let {
                onClick(binding.imagePreview, it)
            }
        }
        binding.textSelectionIndex.setOnClickListener {
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
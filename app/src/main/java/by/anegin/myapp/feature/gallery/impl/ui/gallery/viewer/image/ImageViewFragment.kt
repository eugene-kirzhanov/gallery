package by.anegin.myapp.feature.gallery.impl.ui.gallery.viewer.image

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.anegin.myapp.R
import by.anegin.myapp.databinding.FragmentImageViewBinding
import by.anegin.myapp.feature.gallery.impl.data.MediaStoreUtils
import by.anegin.myapp.feature.gallery.impl.ui.gallery.GalleryViewModel
import by.kirich1409.viewbindingdelegate.viewBinding
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImageViewFragment : Fragment(R.layout.fragment_image_view) {

    companion object {
        private const val ARG_IMAGE_URI = "image_uri"

        fun newInstance(imageUri: Uri) = ImageViewFragment().apply {
            arguments = bundleOf(ARG_IMAGE_URI to imageUri)
        }
    }

    private val binding by viewBinding(FragmentImageViewBinding::bind)

    private val galleryViewModel: GalleryViewModel by viewModels({ requireParentFragment() })

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.setOnApplyWindowInsetsListener { _, insets -> insets }

        val imageUri = arguments?.getParcelable(ARG_IMAGE_URI) as? Uri
            ?: return

        binding.progress.visibility = VISIBLE

        binding.image.setOnImageEventListener(object : SubsamplingScaleImageView.OnImageEventListener {
            override fun onReady() {
                binding.progress.visibility = GONE
            }

            override fun onImageLoadError(e: Exception?) {
                binding.progress.visibility = GONE
                Toast.makeText(view.context, R.string.error_loading_image, Toast.LENGTH_SHORT).show()
            }

            override fun onImageLoaded() {}

            override fun onPreviewLoadError(e: Exception?) {}

            override fun onTileLoadError(e: Exception?) {}

            override fun onPreviewReleased() {}
        })

        lifecycleScope.launchWhenCreated {
            binding.image.orientation = withContext(Dispatchers.IO) {
                MediaStoreUtils.getImageOrientation(view.context, imageUri)
            }
            binding.image.setImage(ImageSource.uri(imageUri))
        }

        binding.image.setOnClickListener {
            galleryViewModel.toggleFullScreen()
        }

        galleryViewModel.currentMediaItem.observe(viewLifecycleOwner) { mediaItem ->
            if (mediaItem?.uri != imageUri) {
                binding.image.resetScaleAndCenter()
            }
        }
    }

}
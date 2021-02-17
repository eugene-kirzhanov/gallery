package by.anegin.myapp.feature.gallery.impl.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import by.anegin.myapp.R
import by.anegin.myapp.common.ui.viewBinding
import by.anegin.myapp.databinding.FragmentGalleryBinding
import by.anegin.myapp.feature.gallery.impl.ui.adapter.MediaItemsAdapter
import by.anegin.myapp.feature.gallery.impl.ui.model.MediaItem
import by.anegin.myapp.feature.gallery.impl.ui.util.GridItemDecoration
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GalleryFragment : Fragment(R.layout.fragment_gallery) {

    companion object {
        fun newInstance() = GalleryFragment()
    }

    private val binding by viewBinding(FragmentGalleryBinding::bind)

    private val viewModel: GalleryViewModel by viewModels()

    private var permissionRequested = false

    private val requestStoragePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            viewModel.onStoragePermissionGranted()
        } else {
            Toast.makeText(requireContext(), R.string.storage_permission_denied, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recyclerViewMedia.layoutManager = GridLayoutManager(view.context, 3)
        binding.recyclerViewMedia.adapter = MediaItemsAdapter(Glide.with(this), ::onMediaItemClick, ::onMediaItemToggleClick)
        (binding.recyclerViewMedia.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        val spacing = view.context.resources.getDimension(R.dimen.gallery_media_spacing)
        binding.recyclerViewMedia.addItemDecoration(GridItemDecoration(spacing))

        binding.recyclerViewMedia.addOnScrollListener(ExtendedFloatingActionButtonScrollListener(binding.buttonSend))

        viewModel.mediaItems.observe(viewLifecycleOwner) {
            (binding.recyclerViewMedia.adapter as? MediaItemsAdapter)?.submitList(it)
        }

        viewModel.selectedCount.observe(viewLifecycleOwner) { count ->
            binding.buttonSend.text = getString(R.string.send_with_counter, count)
            if (count > 0) {
                animateSendButton(0f)
            } else {
                animateSendButton(binding.buttonSend.height * 1.5f)
            }
        }
        binding.buttonSend.doOnLayout {
            binding.buttonSend.translationY = binding.buttonSend.height * 1.5f
        }
    }

    private fun animateSendButton(targetTranslationX: Float) {
        if (binding.buttonSend.translationY != targetTranslationX) {
            binding.buttonSend.animate()
                .translationY(targetTranslationX)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .setDuration(200)
                .start()
        }
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (!permissionRequested) {
                permissionRequested = true
                requestStoragePermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            } else {
                Toast.makeText(requireContext(), R.string.storage_permission_denied, Toast.LENGTH_SHORT).show()
            }
        } else {
            viewModel.onStoragePermissionGranted()
        }
    }

    private fun onMediaItemClick(view: ImageView, media: MediaItem) {
        // todo
    }

    private fun onMediaItemToggleClick(media: MediaItem) {
        viewModel.toggleMediaItem(media)
        if (!binding.buttonSend.isExtended) {
            binding.buttonSend.extend()
        }
    }

    class ExtendedFloatingActionButtonScrollListener(
        private val floatingActionButton: ExtendedFloatingActionButton
    ) : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            if (dy > 15 && floatingActionButton.isExtended) {
                floatingActionButton.shrink()
            } else if (dy < -5 && !floatingActionButton.isExtended) {
                floatingActionButton.extend()
            }
        }
    }

}
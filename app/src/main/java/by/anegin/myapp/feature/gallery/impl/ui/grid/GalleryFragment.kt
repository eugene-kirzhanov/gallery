package by.anegin.myapp.feature.gallery.impl.ui.grid

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import by.anegin.myapp.R
import by.anegin.myapp.databinding.FragmentGalleryBinding
import by.anegin.myapp.feature.gallery.impl.ui.common.model.MediaItem
import by.anegin.myapp.feature.gallery.impl.ui.grid.adapter.MediaItemsAdapter
import by.anegin.myapp.feature.gallery.impl.ui.grid.util.GridItemDecoration
import by.anegin.myapp.feature.gallery.impl.ui.viewer.MediaViewerFragment
import by.kirich1409.viewbindingdelegate.viewBinding
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GalleryFragment : DialogFragment(R.layout.fragment_gallery) {

    companion object {
        private const val ARG_REQUEST_KEY = "request_key"
        private const val ARG_TITLE = "title"

        const val RESULT_TYPE = "result_type"
        const val RESULT_SELECTED_URIS = "selected_uris"

        const val RESULT_TYPE_URIS = 1
        const val RESULT_TYPE_USE_EXTERNAL_APP = 2

        fun newInstance(requestKey: String, title: String) = GalleryFragment().apply {
            arguments = bundleOf(
                ARG_REQUEST_KEY to requestKey,
                ARG_TITLE to title
            )
        }
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_Gallery_FullScreenDialog)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            window?.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.setOnApplyWindowInsetsListener { _, insets ->
            val systemBarInsets = WindowInsetsCompat.toWindowInsetsCompat(insets)
                .getInsets(WindowInsetsCompat.Type.systemBars())
            binding.toolbar.setPadding(0, systemBarInsets.top, 0, 0)
            binding.bottomBar.updateLayoutParams { height = systemBarInsets.bottom }
            insets
        }

        binding.toolbar.apply {
            setNavigationIcon(R.drawable.abc_ic_ab_back_material)
            setNavigationOnClickListener {
                dismiss()
            }
        }

        binding.recyclerViewMedia.layoutManager = GridLayoutManager(view.context, 3)
        binding.recyclerViewMedia.adapter = MediaItemsAdapter(Glide.with(this), ::onMediaItemClick, ::onMediaItemToggleClick)
        (binding.recyclerViewMedia.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        val spacing = view.context.resources.getDimension(R.dimen.gallery_media_spacing)
        binding.recyclerViewMedia.addItemDecoration(GridItemDecoration(spacing))

        binding.recyclerViewMedia.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 15 && binding.buttonSend.isExtended) {
                    binding.buttonSend.shrink()
                } else if (dy < -5 && !binding.buttonSend.isExtended) {
                    binding.buttonSend.extend()
                }
            }
        })

        binding.buttonSend.doOnLayout {
            binding.buttonSend.translationY = binding.buttonSend.height * 1.5f + binding.bottomBar.height
        }
        binding.buttonSend.setOnClickListener {
            returnSelectedUris()
        }

        viewModel.mediaItems.observe(viewLifecycleOwner) {
            (binding.recyclerViewMedia.adapter as? MediaItemsAdapter)?.submitList(it)
        }

        viewModel.selectedCount.observe(viewLifecycleOwner) { count ->
            binding.buttonSend.text = getString(R.string.send_with_counter, count)
            if (count > 0) {
                animateSendButton(0f)
            } else {
                animateSendButton(binding.buttonSend.height * 1.5f + binding.bottomBar.height)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setWindowAnimations(R.style.Theme_Gallery_Slide)
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

//    @SuppressLint("RestrictedApi")
//    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
//        inflater.inflate(R.menu.gallery, menu)
//        (menu as? MenuBuilder)?.setOptionalIconsVisible(true)
//        super.onCreateOptionsMenu(menu, inflater)
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        return if (item.itemId == R.id.action_open_in) {
//            returnExternanlAppRequested()
//            true
//        } else {
//            super.onOptionsItemSelected(item)
//        }
//    }

    private fun animateSendButton(targetTranslationX: Float) {
        if (binding.buttonSend.translationY != targetTranslationX) {
            binding.buttonSend.animate()
                .translationY(targetTranslationX)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .setDuration(200)
                .start()
        }
    }

    private fun returnSelectedUris() {
        setFragmentResult(
            arguments?.getString(ARG_REQUEST_KEY) ?: return,
            bundleOf(
                RESULT_TYPE to RESULT_TYPE_URIS,
                RESULT_SELECTED_URIS to viewModel.getSelectedUris()
            )
        )
        dismiss()
    }

    private fun returnExternanlAppRequested() {
        setFragmentResult(
            arguments?.getString(ARG_REQUEST_KEY) ?: return,
            bundleOf(
                RESULT_TYPE to RESULT_TYPE_USE_EXTERNAL_APP
            )
        )
        dismiss()
    }

    private fun onMediaItemClick(view: ImageView, media: MediaItem) {
        val selectedMediaItems = viewModel.mediaItems.value?.filter { it.selectionNumber != null } ?: emptyList()
        val title = arguments?.getString(ARG_TITLE) ?: ""
        MediaViewerFragment.newInstance(selectedMediaItems, media, title)
            .show(childFragmentManager, null)
    }

    private fun onMediaItemToggleClick(media: MediaItem) {
        viewModel.toggleMediaItem(media)
        if (!binding.buttonSend.isExtended) {
            binding.buttonSend.extend()
        }
    }

}
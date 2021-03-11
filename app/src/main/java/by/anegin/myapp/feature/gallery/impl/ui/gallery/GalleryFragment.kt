package by.anegin.myapp.feature.gallery.impl.ui.gallery

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.view.menu.MenuBuilder
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
import androidx.viewpager2.widget.ViewPager2
import by.anegin.myapp.R
import by.anegin.myapp.databinding.FragmentGalleryBinding
import by.anegin.myapp.feature.gallery.impl.ui.gallery.adapter.MediaItemsAdapter
import by.anegin.myapp.feature.gallery.impl.ui.gallery.model.MediaItem
import by.anegin.myapp.feature.gallery.impl.ui.gallery.util.GridItemDecoration
import by.anegin.myapp.feature.gallery.impl.ui.gallery.viewer.MediaPagerAdapter
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

    private var menuItemOpenIn: MenuItem? = null

    private lateinit var pagerAdapter: MediaPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_Gallery_FullScreenDialog)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : Dialog(requireContext(), theme) {
            override fun onBackPressed() {
                if (viewModel.getCurrentMediaItemUri() != null) {
                    viewModel.onCurrentMediaItemChanged(null)
                } else {
                    dismiss()
                }
            }
        }.apply {
            window?.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        }
    }

    @SuppressLint("RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.setOnApplyWindowInsetsListener { _, insets ->
            val systemBarInsets = WindowInsetsCompat.toWindowInsetsCompat(insets)
                .getInsets(WindowInsetsCompat.Type.systemBars())
            binding.toolbar.setPadding(0, systemBarInsets.top, 0, 0)
            binding.bottomBar.updateLayoutParams { height = systemBarInsets.bottom }
            insets
        }

        binding.toolbar.apply {
            requireActivity().menuInflater.inflate(R.menu.gallery, menu)
            (menu as? MenuBuilder)?.setOptionalIconsVisible(true)
            menuItemOpenIn = menu.findItem(R.id.action_open_in)
            setOnMenuItemClickListener { item ->
                if (item.itemId == R.id.action_open_in) {
                    returnExternanlAppRequested()
                    true
                } else {
                    false
                }
            }

            setNavigationIcon(R.drawable.abc_ic_ab_back_material)
            setNavigationOnClickListener {
                dialog?.onBackPressed()
            }
        }

        val gridAdapter = MediaItemsAdapter(Glide.with(this), ::onMediaItemClick, ::onMediaItemToggleClick)
        pagerAdapter = MediaPagerAdapter(this)

        viewModel.mediaItems.observe(viewLifecycleOwner) {
            gridAdapter.submitList(it)
            pagerAdapter.setItems(it)
        }

        // === Grid ===

        binding.recyclerViewMedia.layoutManager = GridLayoutManager(view.context, 3)
        binding.recyclerViewMedia.adapter = gridAdapter
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

        viewModel.selectedCount.observe(viewLifecycleOwner) { count ->
            binding.buttonSend.text = getString(R.string.send_with_counter, count)
            setSendButtonVisibility(visible = count > 0)
        }

        // === Viewer ===

        binding.viewPagerMedia.adapter = pagerAdapter
        binding.viewPagerMedia.offscreenPageLimit = 1
        binding.viewPagerMedia.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    val currentPosition = binding.viewPagerMedia.currentItem
                    if (currentPosition in 0 until pagerAdapter.itemCount) {
                        val currentMedia = pagerAdapter.getItem(currentPosition)
                        viewModel.onCurrentMediaItemChanged(currentMedia.uri)
                    }
                }
            }
        })

        viewModel.currentMediaItem.observe(viewLifecycleOwner) { mediaItem ->
            val isInViewerMode = mediaItem != null
            menuItemOpenIn?.isVisible = !isInViewerMode

            val toolbarBgColor = ContextCompat.getColor(requireContext(), if (isInViewerMode) R.color.media_view_toolbar_bg else R.color.bg)
            binding.toolbar.setBackgroundColor(toolbarBgColor)
            binding.bottomBar.setBackgroundColor(toolbarBgColor)

            binding.recyclerViewMedia.visibility = if (isInViewerMode) INVISIBLE else VISIBLE
            binding.viewPagerMedia.visibility = if (isInViewerMode) VISIBLE else INVISIBLE

            if (!isInViewerMode && viewModel.isInFullScreenMode()) {
                viewModel.toggleFullScreen()
            }
        }
        viewModel.isInFullScreenMode.observe(viewLifecycleOwner) { isInFullscreen ->
            setToolbarsVisibility(visible = !isInFullscreen)
        }

        // =============

        binding.imageSelectionCheck.setOnClickListener {
            viewModel.toggleCurrentMediaItem()
        }

        viewModel.toolbarCounter.observe(viewLifecycleOwner) { count ->
            binding.textSelectedCount.text = count.toString()
            binding.textSelectedCount.visibility = if (count > 0) VISIBLE else INVISIBLE
        }
        viewModel.toolbarCheck.observe(viewLifecycleOwner) { check ->
            binding.imageSelectionCheck.isSelected = check == true
            binding.imageSelectionCheck.visibility = if (check != null) VISIBLE else GONE
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

    private fun onMediaItemClick(view: ImageView, mediaItem: MediaItem) {
        val itemPosition = pagerAdapter.getItemPosition(mediaItem)
        if (itemPosition == -1) return
        binding.viewPagerMedia.setCurrentItem(itemPosition, false)
        viewModel.onCurrentMediaItemChanged(mediaItem.uri)
    }

    private fun onMediaItemToggleClick(media: MediaItem) {
        viewModel.toggleMediaItem(media)
        if (!binding.buttonSend.isExtended) {
            binding.buttonSend.extend()
        }
    }

    // === Send button ===

    private fun setSendButtonVisibility(visible: Boolean) {
        if (visible) {
            animateSendButton(0f)
        } else {
            animateSendButton(binding.buttonSend.height * 1.5f + binding.bottomBar.height)
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

    // === Toolbars ===

    private fun setToolbarsVisibility(visible: Boolean) {
        if (visible) {
            animateToolbars(0f, 0f)
        } else {
            animateToolbars(-binding.toolbar.height.toFloat(), binding.bottomBar.height.toFloat())
        }
    }

    private fun animateToolbars(toolbarTranslationY: Float, bottomViewTranslationY: Float) {
        if (binding.toolbar.translationY != toolbarTranslationY) {
            binding.toolbar.animate()
                .translationY(toolbarTranslationY)
                .setDuration(250)
                .start()
        }
        if (binding.bottomBar.translationY != bottomViewTranslationY) {
            binding.bottomBar.animate()
                .translationY(bottomViewTranslationY)
                .setDuration(250)
                .start()
        }
    }

}
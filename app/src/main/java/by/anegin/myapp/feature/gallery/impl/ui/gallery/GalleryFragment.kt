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
import by.anegin.myapp.databinding.GalleryFragmentBinding
import by.anegin.myapp.feature.gallery.impl.ui.gallery.adapter.MediaItemsAdapter
import by.anegin.myapp.feature.gallery.impl.ui.gallery.model.MediaItem
import by.anegin.myapp.feature.gallery.impl.ui.gallery.util.GridItemDecoration
import by.anegin.myapp.feature.gallery.impl.ui.gallery.util.setSingleClickListener
import by.anegin.myapp.feature.gallery.impl.ui.gallery.viewer.MediaPagerAdapter
import by.kirich1409.viewbindingdelegate.viewBinding
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GalleryFragment : DialogFragment(R.layout.gallery_fragment) {

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

    private val binding by viewBinding(GalleryFragmentBinding::bind)

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

    private var wasRestored = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_Gallery_FullScreenDialog)
        wasRestored = savedInstanceState != null
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

            viewModel.insets.set(systemBarInsets.left, systemBarInsets.top, systemBarInsets.right, systemBarInsets.bottom)
            viewModel.topToolbarHeight = binding.toolbar.height
            viewModel.bottomToolbarHeight = binding.root.height - binding.buttonSend.top

            binding.toolbar.setPadding(0, systemBarInsets.top, 0, 0)
            binding.bottomBar.updateLayoutParams { height = systemBarInsets.bottom }

            val isInFullScreenMode = viewModel.isInFullScreenMode.value ?: false
            setToolbarsVisibility(visible = !isInFullScreenMode, animate = false)

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

        val columnsCount = resources.getInteger(R.integer.gallery_columns_count)
        binding.recyclerViewMedia.layoutManager = GridLayoutManager(view.context, columnsCount)
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
            val isSendButtonVisibile = viewModel.sendButtonVisiblity.value ?: false
            setSendButtonVisibility(isSendButtonVisibile, animate = false)

            val isExtended = viewModel.sendButtonExtended.value ?: false
            setSendButtonExtendState(isExtended)
        }
        binding.buttonSend.setSingleClickListener {
            returnSelectedUris()
        }
        binding.buttonSendTop.setSingleClickListener {
            returnSelectedUris()
        }

        viewModel.selectedCount.observe(viewLifecycleOwner) { count ->
            binding.buttonSend.text = getString(R.string.send_with_counter, count)
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

        binding.imageSelectionCheck.setSingleClickListener {
            viewModel.toggleCurrentMediaItem()
        }

        viewModel.currentMediaItem.observe(viewLifecycleOwner) { mediaItem ->
            if (mediaItem != null) {
                val itemPosition = pagerAdapter.getItemPosition(mediaItem)
                if (itemPosition != -1 && itemPosition != binding.viewPagerMedia.currentItem) {
                    binding.viewPagerMedia.setCurrentItem(itemPosition, false)
                }
            }

            val isInViewerMode = mediaItem != null
            menuItemOpenIn?.isVisible = !isInViewerMode

            val toolbarBgColor = ContextCompat.getColor(requireContext(), if (isInViewerMode) R.color.media_view_toolbar_bg else R.color.bg)
            binding.toolbar.setBackgroundColor(toolbarBgColor)
            binding.bottomBar.setBackgroundColor(toolbarBgColor)

            setGridVisibility(!isInViewerMode)
            setPagerVisibility(isInViewerMode)

            if (!isInViewerMode && viewModel.isInFullScreenMode()) {
                viewModel.toggleFullScreen()
            }
        }
        viewModel.toolbarCounter.observe(viewLifecycleOwner) { count ->
            binding.textSelectedCount.text = count.toString()
            binding.textSelectedCount.visibility = if (count > 0) VISIBLE else GONE
        }
        viewModel.toolbarCheck.observe(viewLifecycleOwner) { check ->
            binding.imageSelectionCheck.isSelected = check == true
            binding.imageSelectionCheck.visibility = if (check != null) VISIBLE else GONE
        }
        viewModel.topSendButtonVisiblity.observe(viewLifecycleOwner) { visible ->
            binding.buttonSendTop.visibility = if (visible) VISIBLE else GONE
        }
        viewModel.isInFullScreenMode.observe(viewLifecycleOwner) { isInFullscreen ->
            setToolbarsVisibility(visible = !isInFullscreen)
        }

        // =============

        viewModel.sendButtonVisiblity.observe(viewLifecycleOwner) { visible ->
            setSendButtonVisibility(visible, animate = true)
        }
        viewModel.sendButtonExtended.observe(viewLifecycleOwner) { extended ->
            setSendButtonExtendState(extended)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            if (!wasRestored) {
                setWindowAnimations(R.style.Theme_Gallery_Slide)
            } else {
                setWindowAnimations(R.style.Theme_Gallery_SlideExit)
            }
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
        var selectedUris = viewModel.getSelectedUris()
        if (selectedUris.isEmpty()) {
            val currentMediaItemUri = viewModel.getCurrentMediaItemUri()
            if (currentMediaItemUri != null) {
                selectedUris = arrayListOf(currentMediaItemUri)
            }
        }
        setFragmentResult(
            arguments?.getString(ARG_REQUEST_KEY) ?: return,
            bundleOf(
                RESULT_TYPE to RESULT_TYPE_URIS,
                RESULT_SELECTED_URIS to selectedUris
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
        viewModel.onCurrentMediaItemChanged(mediaItem.uri)
    }

    private fun onMediaItemToggleClick(media: MediaItem) {
        viewModel.toggleMediaItem(media)
    }

    private fun setSendButtonExtendState(extended: Boolean) {
        if (extended && !binding.buttonSend.isExtended) {
            binding.buttonSend.extend()
        } else if (!extended && binding.buttonSend.isExtended) {
            binding.buttonSend.shrink()
        }
    }

    private fun setSendButtonVisibility(visible: Boolean, animate: Boolean = true) {
        val targetTranslationY = if (visible) {
            0f
        } else {
            binding.buttonSend.height * 1.5f + binding.bottomBar.height
        }
        if (animate) {
            if (binding.buttonSend.translationY != targetTranslationY) {
                binding.buttonSend.animate()
                    .translationY(targetTranslationY)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .setDuration(200)
                    .start()
            }
        } else {
            binding.buttonSend.translationY = targetTranslationY
        }
    }

    private fun setToolbarsVisibility(visible: Boolean, animate: Boolean = true) {
        val (toolbarTranslationY, bottomViewTranslationY) = if (visible) {
            0f to 0f
        } else {
            -binding.toolbar.height.toFloat() to binding.bottomBar.height.toFloat()
        }
        if (animate) {
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
        } else {
            binding.toolbar.translationY = toolbarTranslationY
            binding.bottomBar.translationY = bottomViewTranslationY
        }
    }

    // ==========

    private fun setGridVisibility(visible: Boolean) {
        val alpha = if (visible) 1f else 0f
        animateViewAlpha(binding.recyclerViewMedia, alpha)
    }

    private fun setPagerVisibility(visible: Boolean) {
        val alpha = if (visible) 1f else 0f
        animateViewAlpha(binding.viewPagerMedia, alpha)
    }

    private fun animateViewAlpha(view: View, targetAlpha: Float) {
        view.animate()
            .alpha(targetAlpha)
            .setDuration(250)
            .withStartAction {
                if (targetAlpha == 1f && view.visibility == INVISIBLE) {
                    view.visibility = VISIBLE
                }
            }
            .withEndAction {
                if (targetAlpha == 0f && view.visibility == VISIBLE) {
                    view.visibility = INVISIBLE
                }
            }
            .start()
    }

}
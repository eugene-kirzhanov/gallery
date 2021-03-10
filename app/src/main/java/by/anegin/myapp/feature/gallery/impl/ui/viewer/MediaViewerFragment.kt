package by.anegin.myapp.feature.gallery.impl.ui.viewer

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import by.anegin.myapp.R
import by.anegin.myapp.common.ui.viewBinding
import by.anegin.myapp.databinding.FragmentMediaViewerBinding
import by.anegin.myapp.feature.gallery.impl.ui.common.model.MediaItem
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MediaViewerFragment : DialogFragment(R.layout.fragment_media_viewer) {

    companion object {
        private const val ARG_SELECTED_MEDIA_ITEMS = "selected_media_items"
        private const val ARG_MEDIA_ITEM = "media_item"
        private const val ARG_TITLE = "title"

        fun newInstance(selectedMediaItems: List<MediaItem>, mediaItem: MediaItem, title: String) = MediaViewerFragment().apply {
            arguments = Bundle().apply {
                putParcelableArrayList(ARG_SELECTED_MEDIA_ITEMS, ArrayList(selectedMediaItems))
                putParcelable(ARG_MEDIA_ITEM, mediaItem)
                putString(ARG_TITLE, title)
            }
        }
    }

    private val binding by viewBinding(FragmentMediaViewerBinding::bind)

    private val viewModel: MediaViewerViewModel by viewModels()

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
        //val selectedMediaItems = arguments?.getParcelableArrayList<MediaItem>(ARG_SELECTED_MEDIA_ITEMS) ?: return
        val mediaItem = arguments?.getParcelable<MediaItem>(ARG_MEDIA_ITEM) ?: return
        val title = arguments?.getString(ARG_TITLE) ?: ""

        view.setOnApplyWindowInsetsListener { _, insets ->
            val systemBarInsets = WindowInsetsCompat.toWindowInsetsCompat(insets)
                .getInsets(WindowInsetsCompat.Type.systemBars())
            binding.toolbar.setPadding(0, systemBarInsets.top, 0, 0)
            binding.bottomView.updateLayoutParams { height = systemBarInsets.bottom }
            insets
        }

        binding.toolbar.apply {
            this.title = title
            setNavigationIcon(R.drawable.abc_ic_ab_back_material)
            setNavigationOnClickListener {
                dismiss()
            }
        }

        val pagerAdapter = MediaPagerAdapter(this)
        binding.viewPagerMedia.adapter = pagerAdapter

        pagerAdapter.setItems(listOf(mediaItem))

        viewModel.isInFullScreenMode.observe(viewLifecycleOwner) {
            if (it) {
                animateToolbars(-binding.toolbar.height.toFloat(), binding.bottomView.height.toFloat())
            } else {
                animateToolbars(0f, 0f)
            }
        }
    }

    private fun animateToolbars(toolbarTranslationY: Float, bottomViewTranslationY: Float) {
        if (binding.toolbar.translationY != toolbarTranslationY) {
            binding.toolbar.animate()
                .translationY(toolbarTranslationY)
                .setDuration(250)
                .start()
        }
        if (binding.bottomView.translationY != bottomViewTranslationY) {
            binding.bottomView.animate()
                .translationY(bottomViewTranslationY)
                .setDuration(250)
                .start()
        }
    }

}
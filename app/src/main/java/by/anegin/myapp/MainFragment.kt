package by.anegin.myapp

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import by.anegin.myapp.databinding.FragmentMainBinding
import by.anegin.myapp.feature.gallery.impl.ui.gallery.GalleryFragment
import by.anegin.myapp.feature.gallery.impl.ui.gallery.util.setSingleClickListener
import by.kirich1409.viewbindingdelegate.viewBinding

class MainFragment : Fragment(R.layout.fragment_main) {

    companion object {
        private const val REQUEST_KEY_PICK_MEDIA = "pick_media"
    }

    private val binding by viewBinding(FragmentMainBinding::bind)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        childFragmentManager.setFragmentResultListener(REQUEST_KEY_PICK_MEDIA, this) { _, bundle ->
            when (bundle.getInt(GalleryFragment.RESULT_TYPE)) {
                GalleryFragment.RESULT_TYPE_URIS -> {
                    bundle.getParcelableArrayList<Uri>(GalleryFragment.RESULT_SELECTED_URIS)?.let { selectedUris ->
                        onMediaSelected(selectedUris)
                    }
                }
                GalleryFragment.RESULT_TYPE_USE_EXTERNAL_APP -> {
                    // todo use external Gallery app
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.buttonGallery.setSingleClickListener {
            showGallery()
        }
    }

    private fun showGallery() {
        GalleryFragment.newInstance(REQUEST_KEY_PICK_MEDIA, "Username")
            .show(childFragmentManager, null)
    }

    private fun onMediaSelected(uris: List<Uri>) {
        binding.textResult.text = uris.joinToString(separator = "\n")
    }

}
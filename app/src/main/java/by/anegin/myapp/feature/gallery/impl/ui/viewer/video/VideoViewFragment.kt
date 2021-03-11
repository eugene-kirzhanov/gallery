package by.anegin.myapp.feature.gallery.impl.ui.viewer.video

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import by.anegin.myapp.R

class VideoViewFragment : Fragment(R.layout.fragment_video_view) {

    companion object {
        private const val ARG_VIDEO_URI = "video_uri"

        fun newInstance(videoUri: Uri) = VideoViewFragment().apply {
            arguments = bundleOf(ARG_VIDEO_URI to videoUri)
        }
    }

//    private val binding by viewBinding(FragmentVideoViewBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        val videoUri = arguments?.getParcelable(ARG_VIDEO_URI) as? Uri
//            ?: return
    }

}
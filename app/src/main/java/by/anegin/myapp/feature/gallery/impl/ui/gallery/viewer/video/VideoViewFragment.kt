package by.anegin.myapp.feature.gallery.impl.ui.gallery.viewer.video

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.media.AudioAttributesCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import by.anegin.myapp.R
import by.anegin.myapp.databinding.GalleryFragmentVideoViewBinding
import by.anegin.myapp.feature.gallery.impl.ui.gallery.GalleryViewModel
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory

class VideoViewFragment : Fragment(R.layout.gallery_fragment_video_view) {

    companion object {
        private const val ARG_VIDEO_URI = "video_uri"

        fun newInstance(videoUri: Uri) = VideoViewFragment().apply {
            arguments = bundleOf(ARG_VIDEO_URI to videoUri)
        }
    }

    private val binding by viewBinding(GalleryFragmentVideoViewBinding::bind)

    private val galleryViewModel: GalleryViewModel by viewModels({ requireParentFragment() })

    private lateinit var audioManager: AudioManager
    private lateinit var audioFocusRequest: AudioFocusRequestCompat

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.root.setPadding(0, galleryViewModel.insets.top, 0, galleryViewModel.insets.bottom)

        val videoUri = arguments?.getParcelable(ARG_VIDEO_URI) as? Uri ?: return

        galleryViewModel.currentMediaItem.observe(viewLifecycleOwner) { mediaItem ->
            if (mediaItem?.uri != videoUri) {
                binding.playerView.player?.pause()
                binding.playerView.player?.seekTo(0)
                binding.playerView.showController()
            }
        }

        val player = SimpleExoPlayer.Builder(view.context).build()
        binding.playerView.player = player

        val dataSourceFactory = DefaultDataSourceFactory(requireContext(), "ringl")
        val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(videoUri))
        player.playWhenReady = false
        player.setMediaSource(videoSource)
        player.prepare()

        binding.playerView.setControllerVisibilityListener {
            galleryViewModel.setFullScreen(it == View.GONE)
        }

        audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioFocusRequest = AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
            .setOnAudioFocusChangeListener { focusChange ->
                if (focusChange != AudioManagerCompat.AUDIOFOCUS_GAIN) {
                    if (player.isPlaying) {
                        player.playWhenReady = false
                    }
                }
            }
            .setAudioAttributes(
                AudioAttributesCompat.Builder()
                    .setUsage(AudioAttributesCompat.USAGE_MEDIA)
                    .setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .build()
        AudioManagerCompat.requestAudioFocus(audioManager, audioFocusRequest)
    }

    override fun onResume() {
        super.onResume()
        binding.playerView.onResume()
    }

    override fun onPause() {
        binding.playerView.player?.playWhenReady = false
        binding.playerView.onPause()
        super.onPause()
    }

    override fun onDestroyView() {
        AudioManagerCompat.abandonAudioFocusRequest(audioManager, audioFocusRequest)
        binding.playerView.player?.release()
        super.onDestroyView()
    }

}
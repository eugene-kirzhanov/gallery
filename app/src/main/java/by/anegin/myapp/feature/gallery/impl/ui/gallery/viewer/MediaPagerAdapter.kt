package by.anegin.myapp.feature.gallery.impl.ui.gallery.viewer

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import by.anegin.myapp.feature.gallery.impl.ui.gallery.model.MediaItem
import by.anegin.myapp.feature.gallery.impl.ui.gallery.viewer.image.ImageViewFragment
import by.anegin.myapp.feature.gallery.impl.ui.gallery.viewer.video.VideoViewFragment

class MediaPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    private val items = ArrayList<MediaItem>()

    fun setItems(items: List<MediaItem>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    fun getItem(position: Int): MediaItem {
        return items[position]
    }

    fun getItemPosition(item: MediaItem): Int {
        return items.indexOfFirst { it == item }
    }

    override fun getItemCount(): Int = items.size

    override fun createFragment(position: Int): Fragment {
        return when (val item = items[position]) {
            is MediaItem.Image -> ImageViewFragment.newInstance(item.uri)
            is MediaItem.Video -> VideoViewFragment.newInstance(item.uri)
        }
    }

}
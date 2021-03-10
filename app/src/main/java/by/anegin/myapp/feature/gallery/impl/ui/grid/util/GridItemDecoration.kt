package by.anegin.myapp.feature.gallery.impl.ui.grid.util

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class GridItemDecoration(private val spacing: Float) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)
        val gridLayoutManager = parent.layoutManager as? GridLayoutManager ?: return
        val spanCount = gridLayoutManager.spanCount
        val position = parent.getChildAdapterPosition(view)
        val column = position % spanCount
        val sp = spacing / spanCount
        outRect.left = (spacing - column * sp).toInt()
        outRect.right = ((column + 1) * sp).toInt()
        if (position < spanCount) {
            outRect.top = spacing.toInt()
        }
        outRect.bottom = spacing.toInt()
    }

}
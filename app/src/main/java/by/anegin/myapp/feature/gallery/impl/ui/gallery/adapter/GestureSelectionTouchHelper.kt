package by.anegin.myapp.feature.gallery.impl.ui.gallery.adapter

import android.view.MotionEvent
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener
import kotlin.math.max
import kotlin.math.min

class GestureSelectionTouchHelper(
    private val recyclerView: RecyclerView,
    private val onSelectionChanged: (startIndex: Int, endIndex: Int) -> Unit,
    private val onSelectionFinished: () -> Unit
) : OnItemTouchListener {

    companion object {
        private const val MAX_SCROLL_DISTANCE = 16
    }

    private val autoScrollDistance = recyclerView.resources.displayMetrics.density * 56f

    private var isDragging = false

    private var start = -1
    private var end = -1

    private var isScrolling = false
    private var scrollDistance = 0
    private var scrollSpeedFactor = 0f
    private var lastX = Float.MIN_VALUE
    private var lastY = Float.MIN_VALUE
    private var inTopSpot = false
    private var inBottomSpot = false

    fun startGestureSelection(position: Int) {
        isDragging = true
        start = position
        end = position
        onSelectionChanged(position, position)
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        if (!isDragging || rv.adapter == null || rv.adapter?.itemCount == 0) return false
        when (e.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN,
            MotionEvent.ACTION_DOWN -> reset()
        }
        return true
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
        if (!isDragging) return
        when (e.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                if (!inTopSpot && !inBottomSpot) {
                    updateSelectedRange(rv, e.x, e.y)
                }
                processAutoScroll(rv, e)
            }
            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP -> reset()
        }
    }

    private fun reset() {
        isDragging = false
        onSelectionFinished()
        start = -1
        end = -1
        inTopSpot = false
        inBottomSpot = false
        lastX = Float.MIN_VALUE
        lastY = Float.MIN_VALUE
        stopAutoScroll()
    }

    // === Selection ===

    private fun updateSelectedRange(rv: RecyclerView, x: Float, y: Float) {
        val child = rv.findChildViewUnder(x, y) ?: return
        val position = rv.getChildAdapterPosition(child)
        if (position != -1 && end != position) {
            end = position
            if (start == -1 || end == -1) return
            onSelectionChanged(start, end)
        }
    }

    // === AutoScroll ===

    private fun processAutoScroll(rv: RecyclerView, event: MotionEvent) {
        val y = event.y
        val mBottomBoundFrom = rv.height - autoScrollDistance
        val mBottomBoundTo = rv.height.toFloat()
        if (y in 0f..autoScrollDistance) {
            lastX = event.x
            lastY = event.y
            scrollSpeedFactor = (autoScrollDistance - y) / autoScrollDistance
            scrollDistance = (MAX_SCROLL_DISTANCE.toFloat() * scrollSpeedFactor * -1f).toInt()
            if (!inTopSpot) {
                inTopSpot = true
                startAutoScroll()
            }
        } else if (y < 0) {
            lastX = event.x
            lastY = event.y
            scrollDistance = MAX_SCROLL_DISTANCE * -1
            if (!inTopSpot) {
                inTopSpot = true
                startAutoScroll()
            }
        } else if (y in mBottomBoundFrom..mBottomBoundTo) {
            lastX = event.x
            lastY = event.y
            scrollSpeedFactor = (y - mBottomBoundFrom) / (mBottomBoundTo - mBottomBoundFrom)
            scrollDistance = (MAX_SCROLL_DISTANCE.toFloat() * scrollSpeedFactor).toInt()
            if (!inBottomSpot) {
                inBottomSpot = true
                startAutoScroll()
            }
        } else if (y > mBottomBoundTo) {
            lastX = event.x
            lastY = event.y
            scrollDistance = MAX_SCROLL_DISTANCE
            if (!inTopSpot) {
                inTopSpot = true
                startAutoScroll()
            }
        } else {
            inBottomSpot = false
            inTopSpot = false
            lastX = Float.MIN_VALUE
            lastY = Float.MIN_VALUE
            stopAutoScroll()
        }
    }

    private fun startAutoScroll() {
        if (!isScrolling) {
            isScrolling = true
            recyclerView.removeCallbacks(scrollRunnable)
            ViewCompat.postOnAnimation(recyclerView, scrollRunnable)
        }
    }

    private fun stopAutoScroll() {
        if (isScrolling) {
            isScrolling = false
            recyclerView.removeCallbacks(scrollRunnable)
        }
    }

    private fun scrollBy(distance: Int) {
        val scrollDistance = if (distance > 0) {
            min(distance, MAX_SCROLL_DISTANCE)
        } else {
            max(distance, -MAX_SCROLL_DISTANCE)
        }
        recyclerView.scrollBy(0, scrollDistance)
        if (lastX != Float.MIN_VALUE && lastY != Float.MIN_VALUE) {
            updateSelectedRange(recyclerView, lastX, lastY)
        }
    }

    private val scrollRunnable = object : Runnable {
        override fun run() {
            if (isScrolling) {
                scrollBy(scrollDistance)
                recyclerView.postOnAnimation(this)
            }
        }
    }

}
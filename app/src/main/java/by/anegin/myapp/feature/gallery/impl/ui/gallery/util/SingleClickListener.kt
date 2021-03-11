package by.anegin.myapp.feature.gallery.impl.ui.gallery.util

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View

fun View.setSingleClickListener(onClick: ((View) -> Unit)?) {
    if (onClick != null) {
        setOnClickListener(SingleClickListener(onClick))
    } else {
        setOnClickListener(null)
        isClickable = false
    }
}

class SingleClickListener(
    private val onClick: ((View) -> Unit)?,
    private val clickInterval: Long = DEFAULT_CLICK_INTERVAL
) : View.OnClickListener {

    companion object {
        private const val DEFAULT_CLICK_INTERVAL = 500L
    }

    private var isViewClicked = false
    private var lastClickTime = 0L

    override fun onClick(v: View) {
        val currentClickTime = SystemClock.elapsedRealtime()
        if (currentClickTime - lastClickTime <= clickInterval) return
        lastClickTime = currentClickTime
        if (!isViewClicked) {
            isViewClicked = true
            Handler(Looper.getMainLooper()).postDelayed({
                isViewClicked = false
            }, clickInterval)
        } else {
            return
        }
        onClick?.invoke(v)
    }

}
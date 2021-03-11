package by.anegin.myapp.feature.gallery.impl.ui.gallery.adapter

import android.animation.ValueAnimator
import android.view.View
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.recyclerview.widget.RecyclerView

class MediaSelectionHelper(
    private val viewHolder: RecyclerView.ViewHolder,
    private val imageView: View
) {

    companion object {
        private const val SELECTED_STATE_SCALE = 0.8f
        private const val SELECTION_ANIMATION_DURATION = 200
    }

    private var state: State = State.NORMAL
    private var anim: ValueAnimator? = null

    enum class State {
        NORMAL, ANIMATING_TO_SELECTED, SELECTED, ANIMATING_TO_NORMAL
    }

    fun setSelectionState(isSelected: Boolean, disableAnimation: Boolean) {
        if (disableAnimation) {
            anim?.cancel()
            anim = null
            if (isSelected) {
                state = State.SELECTED
                imageView.scaleX = SELECTED_STATE_SCALE
                imageView.scaleY = SELECTED_STATE_SCALE
            } else {
                state = State.NORMAL
                imageView.scaleX = 1f
                imageView.scaleY = 1f
            }
        } else {
            if (isSelected) {
                if (state == State.SELECTED || state == State.ANIMATING_TO_SELECTED) return

                state = State.ANIMATING_TO_SELECTED

                val currentScale = imageView.scaleX
                val duration = SELECTION_ANIMATION_DURATION * (currentScale - SELECTED_STATE_SCALE) / (1f - SELECTED_STATE_SCALE)
                anim = ValueAnimator.ofFloat(currentScale, SELECTED_STATE_SCALE)
                    .setDuration(duration.toLong())
                    .apply {
                        addUpdateListener {
                            val scale = it.animatedValue as Float
                            imageView.scaleX = scale
                            imageView.scaleY = scale
                        }
                        doOnStart {
                            viewHolder.setIsRecyclable(false)
                        }
                        doOnEnd {
                            state = State.SELECTED
                            viewHolder.setIsRecyclable(true)
                        }
                        start()
                    }

            } else {
                if (state == State.NORMAL || state == State.ANIMATING_TO_NORMAL) return

                state = State.ANIMATING_TO_NORMAL

                val currentScale = imageView.scaleX
                val duration = SELECTION_ANIMATION_DURATION * (1f - currentScale) / (1f - SELECTED_STATE_SCALE)
                anim = ValueAnimator.ofFloat(currentScale, 1f)
                    .setDuration(duration.toLong())
                    .apply {
                        addUpdateListener {
                            val scale = it.animatedValue as Float
                            imageView.scaleX = scale
                            imageView.scaleY = scale
                        }
                        doOnStart {
                            viewHolder.setIsRecyclable(false)
                        }
                        doOnEnd {
                            state = State.NORMAL
                            viewHolder.setIsRecyclable(true)
                        }
                        start()
                    }
            }
        }
    }

}

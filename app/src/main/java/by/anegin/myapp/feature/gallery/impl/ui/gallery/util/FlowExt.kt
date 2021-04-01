package by.anegin.myapp.feature.gallery.impl.ui.gallery.util

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

fun <T> Fragment.observeFlow(
    source: Flow<T>,
    state: Lifecycle.State = Lifecycle.State.STARTED,
    observer: (T) -> Unit
) {
    lifecycleScope.launch {
        source
            .flowWithLifecycle(lifecycle, state)
            .collect { value ->
                observer(value)
            }
    }
}
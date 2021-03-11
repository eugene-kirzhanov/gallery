package by.anegin.myapp.feature.gallery.impl.ui.gallery.util

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

abstract class ViewBindingViewHolder<B : ViewBinding>(
    parent: ViewGroup,
    bindingInflater: (inflater: LayoutInflater, container: ViewGroup, attach: Boolean) -> B,
    protected val binding: B = bindingInflater(LayoutInflater.from(parent.context), parent, false),
) : RecyclerView.ViewHolder(binding.root) {

    protected val context: Context = itemView.context

    protected fun getString(@StringRes resId: Int): String = context.getString(resId)

    protected fun getString(@StringRes resId: Int, vararg args: Any?): String = context.getString(resId, *args)

}
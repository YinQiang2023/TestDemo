package com.smartwear.publicwatch.ui.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

abstract class CommonAdapter<T, V : ViewBinding>(var mData: MutableList<T>) :
    RecyclerView.Adapter<ViewHolder<V>>() {
    protected var bind: V? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder<V> {
        bind = createBinding(parent, viewType)
        return ViewHolder(bind!!)
    }

    protected abstract fun createBinding(parent: ViewGroup?, viewType: Int): V
    override fun onBindViewHolder(holder: ViewHolder<V>, position: Int) {
        convert(holder.t, mData[position], position)
    }

    abstract fun convert(v: V, t: T, position: Int)
    override fun getItemCount(): Int {
        return mData.size
    }
}

package com.jwei.publicone.ui.adapter

import android.view.ViewGroup
import androidx.viewbinding.ViewBinding

abstract class MultiItemCommonAdapter<T, V : ViewBinding>(mData: MutableList<T>) :
    CommonAdapter<T, V>(mData) {
    override fun getItemViewType(position: Int): Int {
        return if (mData.isNotEmpty()) getItemType(mData[position]) else 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder<V> {
        bind = createBinding(parent, viewType)
        return ViewHolder(bind!!)
    }

    protected abstract fun getItemType(t: T): Int
}

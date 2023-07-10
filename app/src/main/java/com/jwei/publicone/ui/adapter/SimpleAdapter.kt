package com.jwei.publicone.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding


abstract class SimpleAdapter<T, VB : ViewBinding>(
    val inflater: (LayoutInflater, parent: ViewGroup, attachToRoot: Boolean) -> VB,
    var data: MutableList<T>,
) : RecyclerView.Adapter<ViewHolder<VB>>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder<VB> {
        return ViewHolder(inflater(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: ViewHolder<VB>, position: Int) {
        onBindingData(holder.t, data[position], position);
    }

    protected abstract fun onBindingData(binding: VB?, t: T, position: Int)

}
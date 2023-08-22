package com.smartwear.publicwatch.ui.adapter

import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

class ViewHolder<T : ViewBinding>(binding: ViewBinding) : RecyclerView.ViewHolder(binding.root) {
    var t: T = binding as T
}

package com.jwei.publicone.ui.healthy.drag

import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.jwei.publicone.ui.adapter.CommonAdapter
import com.jwei.publicone.ui.adapter.ViewHolder
import com.jwei.publicone.ui.healthy.bean.DragBean
import java.util.*

abstract class DragAdapter<T : DragBean, V : ViewBinding>(mData: MutableList<T>) :
    CommonAdapter<T, V>(mData) {

    var hideCount = 0
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder<V> {
        bind = createBinding(parent, viewType)
        return ViewHolder(bind!!)
    }

    override fun getItemViewType(position: Int): Int {
        return if (mData.size > 0) getItemType(mData[position]) else 0
    }

    override fun onBindViewHolder(holder: ViewHolder<V>, position: Int) {
        convert(holder, holder.t, mData[position], position, hideCount)
    }

    abstract fun convert(holder: ViewHolder<V>, v: V, t: T, position: Int, hiddenCount: Int)

    protected abstract fun getItemType(t: T): Int

    /**
     * 对拖拽的元素进行排序
     * @param fromPosition 得到当拖拽的viewHolder的Position  //拖动的position
     * @param toPosition 拿到当前拖拽到的item的viewHolder  //释放的position
     */
    fun isMove(fromPosition: Int, toPosition: Int, position: Int): Boolean {
        return if (mData[position].isTitle) {
            false
        } else {
            if (fromPosition < toPosition) {
                for (i in fromPosition until toPosition) {
                    if (i < 0) {
                        return false
                    }
                    Collections.swap(mData, i, i + 1)
                }
            } else {
                for (i in fromPosition downTo toPosition + 1) {
                    if (i < 1) {
                        return false
                    }
                    Collections.swap(mData, i, i - 1)
                }
            }
            var isHide = false
            var isCurrentTitle = 0
            for (i in mData.indices) {
                if (mData[i].isTitle) {
                    isHide = true
                    isCurrentTitle = i
                }
//                if (i != isCurrentTitle)
                mData[i].isHide = isHide
            }
            calcHideCount()
            notifyItemMoved(fromPosition, toPosition)
//            notifyDataSetChanged()
            true
        }
    }

    fun calcHideCount() {
        var titlePostion = 0
        hideCount = 0
        for (i in mData.indices) {
            if (!mData[i].isTitle && mData[i].isHide) {
                hideCount++
            } else {
                titlePostion = i
            }
        }
//        Handler(Looper.myLooper()!!).postDelayed({notifyItemChanged(titlePostion)},200)
//        Handler(Looper.myLooper()!!).postDelayed({notifyDataSetChanged()},200)
    }

    fun isCanSelect(position: Int): Boolean {
//        var count = 0
//        for (i in mData.indices){
//            if (mData[i].isHide){
//                count++
//            }
//        }
        return mData[position].isTitle
    }
}
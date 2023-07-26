package com.jwei.xzfit.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.jwei.xzfit.R
import com.jwei.xzfit.ui.device.bean.StyleBean
import com.jwei.xzfit.utils.GlideApp

/**
 * @author YinQiang
 * @date 2023/3/21
 */
class StyleAdapter(
    private val context: Context,
    private val data: List<StyleBean>,
    var selected: (postion: Int) -> Unit,
) :
    RecyclerView.Adapter<StyleAdapter.PointerViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PointerViewHolder {
        return PointerViewHolder(
            LayoutInflater.from(context).inflate(R.layout.item_diy_photo, parent, false)
        )
    }

    override fun onBindViewHolder(holder: PointerViewHolder, position: Int) {
        val photoBean = data.get(position)

        if (photoBean.img != null) {
            GlideApp.with(context).load(photoBean.img).into(holder.ivIcon)
        } else {
            GlideApp.with(context).load(R.mipmap.sport_share_custom_camera).into(holder.ivIcon)

        }

        holder.ivSelected.visibility = if (photoBean.isSelected) View.VISIBLE else View.GONE
        holder.rootlayout.setOnClickListener {
            if (!photoBean.isSelected) {
                data.forEach { it.isSelected = false }
                data.get(position).isSelected = true
                notifyDataSetChanged()
                selected(position)
            }
        }
    }

    override fun getItemCount(): Int = data.size


    inner class PointerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var ivIcon: AppCompatImageView = view.findViewById(R.id.iv_icon)
        var ivSelected: AppCompatImageView = view.findViewById(R.id.iv_selected)
        val rootlayout: ConstraintLayout = view.findViewById(R.id.root_layout)
    }
}
//endregion
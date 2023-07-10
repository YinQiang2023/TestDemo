package com.jwei.publicone.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.ClickUtils
import com.zhapp.ble.bean.DiyWatchFaceConfigBean
import com.jwei.publicone.R
import com.jwei.publicone.ui.device.bean.diydial.MyDiyDialUtils

/**
 * @author YinQiang
 * @date 2023/3/21
 */
//region 复杂功能

class FunctionsAdapter(
    private val context: Context,
    var data: List<DiyWatchFaceConfigBean.FunctionsConfig>,
    var click: (postion: Int) -> Unit,
) :
    RecyclerView.Adapter<FunctionsAdapter.FunctionsViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FunctionsViewHolder {
        return FunctionsViewHolder(
            LayoutInflater.from(context).inflate(R.layout.item_complex, parent, false)
        )
    }

    override fun onBindViewHolder(holder: FunctionsViewHolder, position: Int) {
        val info = data.get(position)
        holder.tvLocation.text =
            MyDiyDialUtils.getFunctionsLocationNameByType(context, info.position)

        holder.tvContext.text =
            MyDiyDialUtils.getFunctionsDetailNameByType(context, info.typeChoose)

        ClickUtils.applySingleDebouncing(holder.rootLayout) {
            click(position)
        }
    }

    override fun getItemCount(): Int = data.size


    inner class FunctionsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var rootLayout: LinearLayout = view.findViewById(R.id.rootLayout)
        var tvLocation: TextView = view.findViewById(R.id.tvLocation)
        var tvContext: TextView = view.findViewById(R.id.tvContext)
    }
}
//endregion
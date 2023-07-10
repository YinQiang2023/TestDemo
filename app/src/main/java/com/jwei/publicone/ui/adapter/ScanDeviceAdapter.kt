package com.jwei.publicone.ui.adapter

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.jwei.publicone.R
import com.jwei.publicone.ui.data.Global
import com.jwei.publicone.viewmodel.DeviceModel
import com.zhapp.ble.bean.ScanDeviceBean
import com.jwei.publicone.utils.GlideApp
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by android
 * on 2021/7/16
 */
class ScanDeviceAdapter(private val mContext: Context, private val lists: ArrayList<ScanDeviceBean>, private val viewModel: DeviceModel) :
    RecyclerView.Adapter<RecyclerView.ViewHolder?>() {

    private var listener:ItemClickListener? = null

    fun setListener(listener:ItemClickListener?){
        this.listener = listener
    }

    override fun getItemCount(): Int {
        return lists.size
    }

    fun getData():ArrayList<ScanDeviceBean>{
        return lists
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val itemView: View = LayoutInflater.from(mContext).inflate(R.layout.device_item_layout, parent, false)
        return NormalHolder(itemView)
    }

    override fun onBindViewHolder(@NonNull holder: RecyclerView.ViewHolder, position: Int) {
        val normalHolder = holder as NormalHolder
        val bean = lists[position]
        normalHolder.tvName.text = bean.name
        normalHolder.tvMac.text = bean.address
        normalHolder.rvRssi.text = bean.rssi.toString()

        var productLogourl = ""
        for (j in Global.productList.indices) {
            if (bean.deviceType.equals(Global.productList[j].deviceType)) {
                productLogourl = Global.productList[j].productLogo
                break
            }
        }

        if (TextUtils.isEmpty(productLogourl)) {
            GlideApp.with(mContext).load(R.mipmap.device_no_bind_right_img).into(normalHolder.ivDevice)
        } else {
            GlideApp.with(mContext).load(productLogourl).into(normalHolder.ivDevice)
        }

        normalHolder.rootLayout.setOnClickListener {
            listener?.onItemClick(position)
        }
    }

    inner class NormalHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvName: TextView = itemView.findViewById(R.id.tvName)
        var tvMac: TextView = itemView.findViewById(R.id.tvMac)
        var rvRssi: TextView = itemView.findViewById(R.id.rvRssi)
        var ivDevice: ImageView = itemView.findViewById(R.id.ivDevice)
        var rootLayout: ConstraintLayout = itemView.findViewById(R.id.rootLayout)

        init {
        }
    }

//    ScanDeviceBean


//    internal class SortByBleDevice : Comparator<Any1?> {
//        override fun compare(o1: Any1, o2: Any1): Int {
//            val mDeviceModel1 = o1 as ScanDeviceBean
//            val mDeviceModel2 = o2 as ScanDeviceBean
//
//            val num1: Int = mDeviceModel1.rssi
//            val num2: Int = mDeviceModel2.rssi
//            var aa = 0
//            if (num2 > num1) {
//                aa = num2 - num1
//            }
//            if (num2 < num1) {
//                aa = num2 - num1
//            }
//            return aa
//        }
//    }

    fun myDeviceSort() {
        //按名称排序
        Collections.sort(lists, SortByBleDevice())
    }

    internal class SortByBleDevice : Comparator<kotlin.Any?> {
        override fun compare(o1: kotlin.Any?, o2: kotlin.Any?): Int {
            val mDeviceModel1 = o1 as ScanDeviceBean
            val mDeviceModel2 = o2 as ScanDeviceBean
            val num1: Int = mDeviceModel1.rssi
            val num2: Int = mDeviceModel2.rssi
            var aa = 0
            if (num2 > num1) {
                aa = num2 - num1
            }
            if (num2 < num1) {
                aa = num2 - num1
            }
            return aa
        }
    }

    interface ItemClickListener{
        fun onItemClick(position: Int)
    }
}
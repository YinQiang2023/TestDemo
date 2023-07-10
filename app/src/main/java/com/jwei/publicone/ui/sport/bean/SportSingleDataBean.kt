package com.jwei.publicone.ui.sport.bean

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * Created by Android on 2021/10/9.
 * 运动单个数据查看item
 */
@Parcelize
data class SportSingleDataBean(
    var index: Int = -1,            // -1 顶部  0123 下边四个
    var dataType: Int = 0,          // 数据类型
    var describe: String = "",       // 数据描述
    var title: String = "",         // 数据单个选择标题
    var value: String = "",         // 数据值
    var unit: String = "",           // 数据单位
    var imgId: Int = 0,             // 数据图标
    var isChecked: Boolean = false //是否选中
) : Parcelable, Cloneable {

    override fun clone(): Any {
        return super.clone()
    }

}
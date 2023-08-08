package com.smartwear.xzfit.ui.sport.bean

import android.text.SpannableStringBuilder

/**
 * Created by Android on 2021/10/14.
 * 运动心率item
 */
data class SportHeartRangeBean(
    var iconBgId: Int = 0,
    var name: String = "",
    var timeBuilder: SpannableStringBuilder? = null,
    /*var isHour:Boolean = false,
    var minute: String = "",
    var second: String = "",*/
    var range: Int = 0  //占比
)
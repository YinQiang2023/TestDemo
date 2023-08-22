package com.smartwear.publicwatch.ui.sport.bean

/**
 * Created by Android on 2021/10/29.
 * 运动过程数据
 * list 每十秒记录一个点的数据+最后一个暂停点，暂停后重新生产list
 * 最终得到List<List<SportDataBean>>结构数据
 */
data class SportDataBean(
    var timeMillis: Long,        //运动时间戳
    var distance: Float = 0f     //运动距离
)
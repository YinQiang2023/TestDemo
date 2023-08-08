package com.smartwear.xzfit.ui.device.bean

import com.smartwear.xzfit.ui.healthy.bean.DragBean

/**
 * Created by Android on 2022/9/28.
 */
class WorldClockItem : DragBean() {
    var cityId = 0 //世界时钟
    var cityName: String = "" //城市名称
    var longitude = 0.0 //经度
    var latitude = 0.0 //纬度
    var offset = 0 //时区，刻度15分钟

    override fun toString(): String {
        return "WorldClockItem(cityId=$cityId, cityName=$cityName, longitude=$longitude, latitude=$latitude, offset=$offset)"
    }

}
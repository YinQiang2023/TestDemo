package com.smartwear.xzfit.https.params

import java.util.ArrayList

class UpLoadDailyBean {
//    var userId: Long = -1
//    var date: String = ""
//    var deviceType: String = ""
//    var deviceMac: String = ""
//    var deviceVersion: String = ""
//    var deviceSyncTimestamp: String = ""
//    var stepFrequency: String = ""
//    var distanceFrequency: String = ""
//    var calorieFrequency: String = ""
//    var stepData: String = ""
//    var totalStep: String = ""
//    var distanceData: String = ""
//    var totalDistance: String = ""
//    var calorieData: String = ""
//    var totalCalorie: String = ""

    var dataList: MutableList<Data> = ArrayList()

    class Data {
        var userId: String = "" //用户id
        var date: String = "" //日期（格式：yyyy-MM-dd）
        var deviceType: String = "" //	设备号
        var deviceMac: String = "" // 	设备MAC,
        var deviceVersion: String = "" //	设备版本号，0.0.0-255.255.255
        var deviceSyncTimestamp: String = "" //	设备同步数据给app时间戳
        var stepFrequency: String = "" //	步数频次，以分钟为单位（1/5/10/30/60）分钟(0-100)
        var distanceFrequency: String = "" //	距离频次，以分钟为单位（1/5/10/30/60）分钟(0-100)
        var calorieFrequency: String = "" //	卡路里频次，以分钟为单位（1/5/10/30/60）分钟(0-100)
        var stepData: String = "" //	步数数据
        var totalStep: String = "" //	当日总步数
        var distanceData: String = "" //	距离数据
        var totalDistance: String = "" //	当日总距离，2未小数点
        var calorieData: String = "" //	卡路里数据
        var totalCalorie: String = "" //	当日总卡路里，2位小数点
    }
}
package com.smartwear.xzfit.https.params

import java.util.ArrayList

class UpLoadHeartRateBean {
    var dataList: MutableList<UpLoadHeartRateBean.Data> = ArrayList()

    class Data {
        var userId = "" //	Y	Long	用户id
        var date = "" //	Y	String(10)	日期（格式：yyyy-MM-dd）
        var deviceType = "" //	Y	String(10)	设备号
        var deviceMac = "" //	Y	String(17) 	设备MAC,
        var deviceVersion = "" //	N	String	设备版本号，0.0.0-255.255.255
        var deviceSyncTimestamp = "" //	N	Long（10）	设备同步数据给app时间戳
        var heartRateFrequency = "" //	Y	int(4)	以分钟为单位（1/5/10/30/60）分钟(0-100)
        var heartRateData = "" //	Y	String	当日连续心率监测数据
        var maxHeartRate = "" //	N	Int(3)	最高心率
        var minHeartRate = "" //	N	Int(3)	最低心率
        var avgHeartRate = "" //	N	Int(3)	平均心率
    }
}
package com.smartwear.publicwatch.https.params

import java.util.ArrayList

class UpLoadSingleHeartRateBean {
    var dataList: MutableList<UpLoadSingleHeartRateBean.Data> = ArrayList()

    class Data {
        var userId = "" //	Y	Long	用户id
        var measureTime = "" //	Y	String(19)	测量时间（格式：yyyy-MM-dd HH:mi:ss）
        var deviceType = "" //	Y	String(10)	设备号
        var deviceMac = "" //	Y	String(17) 	设备MAC,
        var deviceVersion = "" //	N	String	设备版本号，0.0.0-255.255.255
        var deviceSyncTimestamp = "" //	N	Long（10）	设备同步数据给app时间戳
        var measureData = "" //	Y	int	心率值
    }
}
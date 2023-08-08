package com.smartwear.xzfit.https.params

import java.util.ArrayList

class UpLoadEffectiveStandBean {
    var dataList: MutableList<UpLoadEffectiveStandBean.Data> = ArrayList()

    class Data {
        var userId = "" //	Y	Long	用户id
        var date = "" //	Y	String(10)	日期（格式：yyyy-MM-dd）
        var deviceType = "" //	Y	String(10)	设备号
        var deviceMac = "" //	Y	String(17) 	设备MAC,
        var deviceVersion = "" //	N	String	设备版本号，0.0.0-255.255.255
        var deviceSyncTimestamp = "" //	N	Long	设备同步数据给app时间戳
        var effectiveStandingFrequency = "" //	N	int(4)	频次，以分钟为单位（1/5/10/30/60）分钟(0-100)
        var effectiveStandingData = "" //	Y	String	有效站立数据
        var effectiveStandingDuration = "" //	N	int	有效站立时长（单位：分钟）
    }
}
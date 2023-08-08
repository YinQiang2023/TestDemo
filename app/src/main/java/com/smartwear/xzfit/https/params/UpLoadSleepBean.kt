package com.smartwear.xzfit.https.params

import java.util.ArrayList

class UpLoadSleepBean {
    var dataList: MutableList<Data> = ArrayList()

    class Data {
        var userId = "" //	Y	Long	用户id
        var date = "" //	Y	String(10)	日期（格式：yyyy-MM-dd）
        var deviceType = "" //	Y	String(10)	设备号
        var deviceMac = "" //	Y	String(17) 	设备MAC,
        var deviceVersion = "" //	N	String	设备版本号，0.0.0-255.255.255
        var deviceSyncTimestamp = "" //	N	Long（10）	设备同步数据给app时间戳
        var startSleepTimestamp = "" //	N	Long（10）	入睡时间
        var endSleepTimestamp = "" //	N	Long（10）	起床时间
        var sleepDuration = "" //	N	int(4)	睡眠时长
        var sleepScore = "" //	N	int(2)	睡眠分数
        var awakeTime = "" //	N	int(4)	总的清醒时长
        var awakeTimePercentage = "" //	N	double(4,2)	总的清醒时长百分比
        var lightSleepTime = "" //	N	int(4)	总的浅睡时长
        var lightSleepTimePercentage = "" //	N	double(2,2)	总的浅睡时长百分比
        var deepSleepTime = "" //	N	int(4)	总的深睡时长
        var deepSleepTimePercentage = "" //	N	double(2,2)	总的深睡时长百分比
        var rapidEyeMovementTime = "" //	N	int(4)	总的快速眼动时长
        var rapidEyeMovementTimePercentage = "" //	N	double(2,2)	总的快速眼动时长百分比
        var sectionList: MutableList<BreakData> = ArrayList() // 睡眠分断数据集合
    }

    class BreakData {
        var sleepDistributionType = "" //	Y	int(2)	0:清醒, 1:浅睡, 2:深睡,3:快速眼动
        var startTimestamp = "" //	Y	Long(10)
        var sleepDuration = "" //	Y	int(4)
    }

}
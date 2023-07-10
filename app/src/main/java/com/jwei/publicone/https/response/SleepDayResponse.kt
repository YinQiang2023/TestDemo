package com.jwei.publicone.https.response

class SleepDayResponse {

    var startSleepTimestamp = ""//	Long（10）	入睡时间
    var endSleepTimestamp = "" //	Long（10）	起床时间
    var sleepDuration = "" //	int(4)	睡眠时长
    var sleepScore = "" //	int(2)	睡眠分数
    var awakeTime = "" //	int(4)	总的清醒时长
    var awakeTimePercentage = "" //	double(4,2)	总的清醒时长百分比
    var lightSleepTime = "" //	int(4)	总的浅睡时长
    var lightSleepTimePercentage = "" //	double(2,2)	总的浅睡时长百分比
    var deepSleepTime = "" //	int(4)	总的深睡时长
    var deepSleepTimePercentage = "" //	double(2,2)	总的深睡时长百分比
    var rapidEyeMovementTime = "" //	int(4)	总的快速眼动时长
    var rapidEyeMovementTimePercentage = "" //	double(2,2)	总的快速眼动时长百分比

    lateinit var sectionList: List<Data> //	睡眠分段数据

    class Data {
        var sleepDistributionType = "" //	Y	int(2)
        var startTimestamp = "" //	Y	Long(10)
        var sleepDuration = "" //	Y	int(4)

        override fun toString(): String {
            return "Data(sleepDistributionType='$sleepDistributionType', startTimestamp='$startTimestamp', sleepDuration='$sleepDuration')"
        }

    }

    override fun toString(): String {
        return "SleepDayResponse(startSleepTimestamp='$startSleepTimestamp', endSleepTimestamp='$endSleepTimestamp', sleepDuration='$sleepDuration', sleepScore='$sleepScore', awakeTime='$awakeTime', awakeTimePercentage='$awakeTimePercentage', lightSleepTime='$lightSleepTime', lightSleepTimePercentage='$lightSleepTimePercentage', deepSleepTime='$deepSleepTime', deepSleepTimePercentage='$deepSleepTimePercentage', rapidEyeMovementTime='$rapidEyeMovementTime', rapidEyeMovementTimePercentage='$rapidEyeMovementTimePercentage', sectionList=$sectionList)"
    }

}
package com.smartwear.xzfit.https.response

class SleepListResponse {
    var totalSleepDuration = ""//	int	总睡眠时长
    var avgSleepDuration = ""//	int	平均睡眠时长（当天睡眠时长为0，不加入计算）
    lateinit var dataList: List<Data>

    class Data {
        var date = ""//	String	睡眠日期
        var sleepDuration = ""//	int(4)	睡眠时长
        var awakeTime = ""//	int(4)	总的清醒时长
        var lightSleepTime = ""//	int(4)	总的浅睡时长
        var deepSleepTime = ""//int(4)	总的深睡时长
        var rapidEyeMovementTime = ""//	int(4)	总的快速眼动时长

        override fun toString(): String {
            return "Data(date='$date', sleepDuration='$sleepDuration', awakeTime='$awakeTime', lightSleepTime='$lightSleepTime', deepSleepTime='$deepSleepTime', rapidEyeMovementTime='$rapidEyeMovementTime')"
        }

    }

    override fun toString(): String {
        return "SleepListResponse(totalSleepDuration='$totalSleepDuration', avgSleepDuration='$avgSleepDuration', dataList=$dataList)"
    }

}
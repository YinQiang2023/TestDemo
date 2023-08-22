package com.smartwear.publicwatch.https.response

class HeartRateResponse {
    var id = ""//	Long	数据Id
    var heartRateFrequency = ""//	int	以分钟为单位（1/5/10/30/60）分钟(0-100)
    var heartRateData = ""//	String	当日连续心率监测数据
    var maxHeartRate = ""//	int	最高心率
    var minHeartRate = ""//	int	最低心率
    var avgHeartRate = ""//	int	平均心率

    override fun toString(): String {
        return "HeartRateResponse(id='$id', heartRateFrequency='$heartRateFrequency', heartRateData='$heartRateData', maxHeartRate='$maxHeartRate', minHeartRate='$minHeartRate', avgHeartRate='$avgHeartRate')"
    }

}
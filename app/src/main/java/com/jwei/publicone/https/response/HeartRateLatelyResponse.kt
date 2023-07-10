package com.jwei.publicone.https.response

class HeartRateLatelyResponse {
    var id = ""    //	Long	数据id
    var date = ""    //	String	日期（格式：yyyy-MM-dd）
    var heartRateFrequency = ""    //	int(4)	以分钟为单位（1/5/10/30/60）分钟(0-100)
    var heartRateData = ""    //	String	当日连续心率监测数据
    var lastHeartRate = ""    //String	最后一次监测心率值

    override fun toString(): String {
        return "HeartRateLatelyResponse(id='$id', date='$date', heartRateFrequency='$heartRateFrequency', heartRateData='$heartRateData', lastHeartRate='$lastHeartRate')"
    }

}
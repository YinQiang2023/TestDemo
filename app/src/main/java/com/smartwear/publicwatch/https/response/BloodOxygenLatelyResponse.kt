package com.smartwear.publicwatch.https.response

class BloodOxygenLatelyResponse {
    var id = ""    //	Long	数据id
    var date = ""    //	String	日期（格式：yyyy-MM-dd）
    var bloodOxygenFrequency = ""    //	int(4)	以分钟为单位（1/5/10/30/60）分钟(0-100)
    var bloodOxygenData = ""    //	String	当日连续心率监测数据
    var lastbloodOxygen = ""    //String	最后一次监测心率值

    override fun toString(): String {
        return "BloodOxygenLatelyResponse(id='$id', date='$date', bloodOxygenFrequency='$bloodOxygenFrequency', bloodOxygenData='$bloodOxygenData', lastbloodOxygen='$lastbloodOxygen')"
    }
}
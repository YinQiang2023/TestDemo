package com.jwei.xzfit.https.response

/**
 * Created by Android on 2023/2/13.
 */
class PressureLatelyResponse {
    var id = ""    //	Long	数据id
    var date = ""    //	String	日期（格式：yyyy-MM-dd）
    var pressureFrequency = ""    //	int(4)	以分钟为单位（1/5/10/30/60）分钟(0-100)
    var pressureData = ""    //	String	当日连续压力监测数据
    var lastPressure = ""    //String	最后一次监测压力值
    
    override fun toString(): String {
        return "PressureLatelyResponse(id='$id', date='$date', pressureFrequency='$pressureFrequency', pressureData='$pressureData', lastPressure='$lastPressure')"
    }

}
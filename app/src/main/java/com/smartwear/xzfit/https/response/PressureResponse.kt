package com.smartwear.xzfit.https.response

/**
 * Created by Android on 2022/10/13.
 */
class PressureResponse {
    var id = ""//	Long	数据Id
    var pressureFrequency = ""//	int	以分钟为单位（1/5/10/30/60）分钟(0-100)
    var pressureData = ""//	String	当日连续压力监测数据
    var maxPressure = ""//	int	最高压力
    var minPressure = ""//	int	最低压力
    var avgPressure = ""//  int 平均压力

    override fun toString(): String {
        return "PressureResponse(id='$id', pressureFrequency='$pressureFrequency', pressureData='$pressureData', maxPressure='$maxPressure', minPressure='$minPressure', avgPressure='$avgPressure')"
    }
}
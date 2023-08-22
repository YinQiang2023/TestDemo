package com.smartwear.publicwatch.https.response

class BloodOxygenResponse {
    var id = ""//	Long	数据Id
    var bloodOxygenFrequency = ""//	int	以分钟为单位（1/5/10/30/60）分钟(0-100)
    var bloodOxygenData = ""//	String	当日连续血氧监测数据
    var maxBloodOxygen = ""//	int	最高血氧饱和度
    var minBloodOxygen = ""//	int	最低血氧饱和度
    var avgBloodOxygen = ""//	int	平均血氧饱和度

    override fun toString(): String {
        return "BloodOxygenResponse(id='$id', bloodOxygenFrequency='$bloodOxygenFrequency', bloodOxygenData='$bloodOxygenData', maxBloodOxygen='$maxBloodOxygen', minBloodOxygen='$minBloodOxygen', avgBloodOxygen='$avgBloodOxygen')"
    }

}
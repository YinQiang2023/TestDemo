package com.smartwear.xzfit.https.response


class EcgLastDataResponse {


    var id = ""//Long	数据id，唯一，后面详细数据根据该id查询
    var userId = ""//Long	用户id
    var heart = ""//Integer	心率
    var systolic = ""//Integer	收缩压
    var diastolic = ""//Integer	舒张压
    var hrvResult = ""//Integer	测量结果（HRV）
    var healthIndex = ""//Integer	健康指数
    var fatigueIndex = ""//Integer	疲劳指数
    var bodyLoad = ""//Integer	身心负荷
    var bodyQuality = ""//Integer	身体素质
    var cardiacFunction = ""//Integer	心脏功能
    var healthMeasuringTime = ""//String	测量时间（ECG/PPG）：yyyy-MM-dd hh:mi:ss
    var deviceSensorType = ""//Integer	设备传感类型   最长为2，数值
    var bpStatus = ""//Integer	是否支持血压  0：支持 1：不支持 默认为0
    var initiator = ""//Integer	发起者  0：离线 1：APP发起

    override fun toString(): String {
        return "Data(id='$id', healthMeasuringTime='$healthMeasuringTime')"
    }
}
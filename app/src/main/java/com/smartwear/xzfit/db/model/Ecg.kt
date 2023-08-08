package com.smartwear.xzfit.db.model


class Ecg : BaseData() {
    //运动记录ID  上传服务器后查询时赋值
    var DataId: Long = 0L

    var userId: String = ""
    var healthMeasuringTime = "" //测量时间（ECG/PPG）：yyyy-MM-dd hh:mi:ss
    var initiator = "" //0：离线 1：APP发起
    var deviceSensorType = "" //设备传感类型   最长为2，数值
    var bpStatus = "" //	是否支持血压  0：支持 1：不支持 默认为0

    var ecgData = "" //ECG原始数据

    var heart = "" //心率
    var systolic = "" //收缩压
    var diastolic = "" //舒张压

    var hrvResult = "" //测量结果（HRV）
    var healthIndex = "" //健康指数
    var fatigueIndex = "" //疲劳指数
    var bodyLoad = "" //身心负荷
    var bodyQuality = "" //身体素质
    var cardiacFunction = "" //心脏功能

}
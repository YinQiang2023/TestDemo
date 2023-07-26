package com.jwei.xzfit.db.model

class HeartRate : BaseData() {
    //连续心率数据
    var userId: String = ""
    var continuousHeartRateFrequency: String = ""
    var heartRateData: String = ""
    var maxHeartRate = "" //	N	Int(3)	最高心率
    var minHeartRate = "" //	N	Int(3)	最低心率
    var avgHeartRate = "" //	N	Int(3)	平均心率
}
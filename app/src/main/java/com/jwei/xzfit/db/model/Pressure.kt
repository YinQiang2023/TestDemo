package com.jwei.xzfit.db.model

/**
 * Created by Android on 2022/10/13.
 * 压力数据
 */
class Pressure : BaseData() {
    var userId: String = ""
    var continuousPressureFrequency: String = ""
    var pressureData: String = ""
    var maxPressure = "" //	N	Int(3)	最高压力
    var minPressure = "" //	N	Int(3)	最低压力
    var avgPressure = "" //	N	Int(3)	平均压力
}
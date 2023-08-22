package com.smartwear.publicwatch.https.response

/**
 * Created by Android on 2022/10/13.
 */
class SinglePressureResponse {
    var maxPressure = "" //		int	日期区间最高压力值
    var minPressure = "" //		int	日期区间最低压力值
    var avgPressure = "" //		int	日期区间平均压力值
    lateinit var dataList: List<SingleHeartRateResponse.Data>

    class Data {
        var id = ""//	Long	数据Id
        var measureTime = ""//	String	测量时间（格式：yyyy-MM-dd HH:mi:ss）
        var measureData = ""//	String	心率测量值

        override fun toString(): String {
            return "Data(id='$id', measureTime='$measureTime', measureData='$measureData')"
        }

    }

    override fun toString(): String {
        return "SinglePressureResponse(maxPressure='$maxPressure', minPressure='$minPressure', avgPressure='$avgPressure', dataList=$dataList)"
    }


}
package com.smartwear.xzfit.https.response

/**
 * Created by Android on 2023/2/6.
 */
class SinglePressureListResponse {
    var maxPressure = ""//	int	日期区间最高压力
    var minPressure = ""//	int	日期区间最低压力
    var avgPressure = ""//	int	日期区间平均压力
    lateinit var dataList: List<Data>

    class Data {
        var date = ""//	String	日期 （格式：yyyy-MM-dd）
        var maxPressure = ""//	int	日期区间最高压力
        var minPressure = ""//	int	日期区间最低压力
        var avgPressure = ""//	int	日期区间平均压力

        override fun toString(): String {
            return "Data(date='$date', maxPressure='$maxPressure', minPressure='$minPressure', avgPressure='$avgPressure')"
        }

    }

    override fun toString(): String {
        return "SinglePressureListResponse(maxPressure='$maxPressure', minPressure='$minPressure', avgPressure='$avgPressure', dataList=$dataList)"
    }

}
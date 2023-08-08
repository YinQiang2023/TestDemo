package com.smartwear.xzfit.https.response

/**
 * Created by Android on 2022/10/13.
 */
class PressureListResponse {
    var maxPressure = ""//	int	查询日期区间最高压力
    var minPressure = ""//	int	查询日期区间最低压力
    var avgPressure = ""//	int	查询日期区间平均压力

    lateinit var dataList: List<Data>

    class Data {
        var id = ""//	Long	数据Id
        var date = ""//	String	压力监测日期（格式：yyyy-MM-dd HH:mi:ss）
        var maxPressure = ""//	int	当日最高压力
        var minPressure = ""//	int	当日最低压力

        override fun toString(): String {
            return "Data(id='$id', date='$date', maxPressure='$maxPressure', minPressure='$minPressure')"
        }

    }

    override fun toString(): String {
        return "PressureListResponse(maxPressure='$maxPressure', minPressure='$minPressure', avgPressure='$avgPressure', dataList=$dataList)"
    }

}
package com.smartwear.xzfit.https.response

class HeartRateListResponse {
    var maxHeartRate = ""//	int	查询日期区间最高心率
    var minHeartRate = ""//	int	查询日期区间最低心率
    var avgHeartRate = ""//	int	查询日期区间平均心率

    lateinit var dataList: List<Data>

    class Data {
        var id = ""//	Long	数据Id
        var date = ""//	String	心率监测日期（格式：yyyy-MM-dd HH:mi:ss）
        var maxHeartRate = ""//	int	当日最高心率
        var minHeartRate = ""//	int	当日最低心率

        override fun toString(): String {
            return "Data(id='$id', date='$date', maxHeartRate='$maxHeartRate', minHeartRate='$minHeartRate')"
        }

    }

    override fun toString(): String {
        return "HeartRateListResponse(maxHeartRate='$maxHeartRate', minHeartRate='$minHeartRate', avgHeartRate='$avgHeartRate', dataList=$dataList)"
    }

}
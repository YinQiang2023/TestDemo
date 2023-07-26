package com.jwei.xzfit.https.response

class EffectiveStandListResponse {
    var totalTimes = ""//	int	总次数
    var totalDuration = ""//	int	总时长
    lateinit var dataList: List<Data>

    class Data {
        var date = ""//	String	日期
        var effectiveStandingFrequency = ""//	int	频次，以分钟为单位（5/10/30/60）分钟(0-100)
        var effectiveStandingData = ""//	String	有效站立数据
        var effectiveStandingTimes = "0"//	int	有效站立次数
        var effectiveStandingDuration = ""//	int	有效站立时长

        override fun toString(): String {
            return "Data(date='$date', effectiveStandingFrequency='$effectiveStandingFrequency', effectiveStandingData='$effectiveStandingData', effectiveStandingTimes='$effectiveStandingTimes', effectiveStandingDuration='$effectiveStandingDuration')"
        }

    }

    override fun toString(): String {
        return "EffectiveStandListResponse(totalTimes='$totalTimes', totalDuration='$totalDuration', dataList=$dataList)"
    }

}
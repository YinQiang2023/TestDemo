package com.smartwear.publicwatch.https.response

class EffectiveStandResponse {
    var date = ""//	String	日期
    var effectiveStandingFrequency = ""//	int	频次，以分钟为单位（5/10/30/60）分钟(0-100)
    var effectiveStandingData = ""//	String	有效站立数据
    var effectiveStandingTimes = ""//	int	有效站立次数
    var effectiveStandingDuration = ""//	int	有效站立时长

    override fun toString(): String {
        return "EffectiveStandResponse(date='$date', effectiveStandingFrequency='$effectiveStandingFrequency', effectiveStandingData='$effectiveStandingData', effectiveStandingTimes='$effectiveStandingTimes', effectiveStandingDuration='$effectiveStandingDuration')"
    }

}
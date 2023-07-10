package com.jwei.publicone.https.response

class DailyDayResponse {
    var todayData = ""
    var stepFrequency = ""    //	int	步数频次，以分钟为单位（1/5/10/30/60）分钟(0-100)
    var distanceFrequency = ""    //	int	距离频次，以分钟为单位（1/5/10/30/60）分钟(0-100)
    var calorieFrequency = ""    //	int	卡路里频次，以分钟为单位（1/5/10/30/60）分钟(0-100)
    var stepData = ""    //	String	步数数据
    var totalStep = ""    //	int	当日总步数
    var distanceData = ""    //	String	距离数据
    var totalDistance = ""    //	double	当日总距离，2未小数点
    var calorieData = ""    //	String	卡路里数据
    var totalCalorie = ""    //	double	当日总卡路里，2位小数点
    var yesterdayStep = ""    //	int	昨日运动总步数
    var yesterdayDistance = ""    //	double	昨日运动总距离
    var yesterdayCalorie = ""    //	double	昨日运动总卡路里

    override fun toString(): String {
        return "DailyDayResponse(todayData='$todayData', stepFrequency='$stepFrequency', distanceFrequency='$distanceFrequency', calorieFrequency='$calorieFrequency', stepData='$stepData', totalStep='$totalStep', distanceData='$distanceData', totalDistance='$totalDistance', calorieData='$calorieData', totalCalorie='$totalCalorie', yesterdayStep='$yesterdayStep', yesterdayDistance='$yesterdayDistance', yesterdayCalorie='$yesterdayCalorie')"
    }

}
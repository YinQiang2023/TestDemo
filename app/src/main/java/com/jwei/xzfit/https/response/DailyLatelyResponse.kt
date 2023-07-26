package com.jwei.xzfit.https.response

class DailyLatelyResponse {
    var date = ""    //	String	日期（格式：yyyy-MM-dd）
    var stepFrequency = ""    //	int(4)	步数频次，以分钟为单位（1/5/10/30/60）分钟(0-100)
    var distanceFrequency = ""    //	int(4)	距离频次，以分钟为单位（1/5/10/30/60）分钟(0-100)
    var calorieFrequency = ""    //	int(4)	卡路里频次，以分钟为单位（1/5/10/30/60）分钟(0-100)
    var stepData = ""    //	String	步数数据
    var totalStep = ""    //	int(6)	当日总步数
    var distanceData = ""    //	String	距离数据
    var totalDistance = ""    //	double(6,2)	当日总距离，2未小数点
    var calorieData = ""    //	String	卡路里数据
    var totalCalorie = ""    //	double(6,2)	当日总卡路里，2位小数点

    override fun toString(): String {
        return "DailyLatelyResponse(date='$date', stepFrequency='$stepFrequency', distanceFrequency='$distanceFrequency', calorieFrequency='$calorieFrequency', stepData='$stepData', totalStep='$totalStep', distanceData='$distanceData', totalDistance='$totalDistance', calorieData='$calorieData', totalCalorie='$totalCalorie')"
    }


}
package com.smartwear.xzfit.db.model

class Daily : BaseData() {
    var userId: String = ""

    //步数频率
    var stepsFrequency = 0

    //步数数据
    var stepsData: String = ""

    //距离频率
    var distanceFrequency = 0

    //距离数据
    var distanceData: String = ""

    //卡路里频率
    var calorieFrequency = 0

    //卡路里数据
    var calorieData: String = ""

    //日期
//    var date: String = ""

    //    var timeStamp : Long = 0
//    var createDateTime: String = ""
//    var upDateTime: String = ""
    var totalSteps = ""
    var totalCalorie = ""
    var totalDistance = ""

    //    var deviceType = ""
//    var deviceMac = ""
//    var deviceVersion = ""
    override fun toString(): String {
        return "Daily(createDateTime='$createDateTime'  upDateTime='$upDateTime' userId='$userId', stepsFrequency=$stepsFrequency, stepsData=${stepsData}, " +
                "distanceFrequency=$distanceFrequency, distanceFata=${distanceData}, calorieFrequency=$calorieFrequency, " +
                "calorieData=${calorieData}, date='$date', totalSteps='$totalSteps', totalCalorie='$totalCalorie', " +
                "timeStamp='$timeStamp', totalDistance='$totalDistance', deviceType='$deviceType', deviceMac='$deviceMac', " +
                "deviceVersion='$deviceVersion' )"
    }


}
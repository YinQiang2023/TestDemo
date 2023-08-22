package com.smartwear.publicwatch.https.response

class DailyListResponse {
    //总步数
    var step: String = ""

    //总距离
    var distance: String = ""

    //总卡路里
    var calorie: String = ""

    //上周/上月总步数
    var previousStep: String = ""

    //上周/上月总距离
    var previousDistance: String = ""

    //上周/上月总卡路里
    var previousCalorie: String = ""
    lateinit var dataList: List<Data>

    class Data {
        //日期
        var date: String = ""

        //当日总步数
        var totalStep: String = ""

        //当日总距离，2未小数点
        var totalDistance: String = ""

        //当日总卡路里，2位小数点
        var totalCalorie: String = ""

        override fun toString(): String {
            return "Data(date='$date', totalStep='$totalStep', totalDistance='$totalDistance', totalCalorie='$totalCalorie')"
        }

    }

    override fun toString(): String {
        return "DailyListResponse(step='$step', distance='$distance', calorie='$calorie', previousStep='$previousStep', previousDistance='$previousDistance', previousCalorie='$previousCalorie', dataList=$dataList)"
    }

}
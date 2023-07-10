package com.jwei.publicone.https.response

class TargetSettingResponse : Cloneable {
    var userId: Long = -1
    var sportTarget: Int = 0
    var sleepTarget: Int = 0
    var distanceTarget: Int = 0
    var bloodPressureLevel: Byte = 0
    var calibrationHeart: String = ""
    var calibrationDiastolic: String = ""
    var calibrationSystolic: Byte = 0
    var wearWay: String = ""
    var consumeTarget: Int = 0
    var unit: String = ""
    var temperature: String = ""


    public override fun clone(): Any {
        return super.clone()
    }

    override fun toString(): String {
        return "TargetSettingResponse(userId=$userId, sportTarget=$sportTarget, sleepTarget=$sleepTarget, distanceTarget=$distanceTarget, bloodPressureLevel=$bloodPressureLevel, calibrationHeart='$calibrationHeart', calibrationDiastolic='$calibrationDiastolic', calibrationSystolic=$calibrationSystolic, wearWay='$wearWay', consumeTarget=$consumeTarget, unit='$unit', temperature='$temperature')"
    }

}
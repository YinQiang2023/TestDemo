package com.jwei.publicone.ui.user.bean

import com.jwei.publicone.https.Response
import com.jwei.publicone.https.response.TargetSettingResponse
import com.jwei.publicone.ui.user.utils.UnitConverUtils
import com.jwei.publicone.utils.Constant
import com.jwei.publicone.utils.SpUtils
import com.jwei.publicone.utils.TextStringUtils

//userId	Y	Long	用户id
//sportTarget	N	Integer	运动目标
//sleepTarget	N	Integer	睡眠目标
//consumeTarget	N	Integer	消耗目标（没有补0）
//distanceTarget	N	Integer	距离目标（没有补0）
//calibrationHeart	N	String	校准心率
//calibrationDiastolic	N	String	校准舒张压
//calibrationSystolic	N	Byte	校准收缩压
//wearWay	N	String	佩戴方式(左和右)L,R
//unit	N	String	单位设置 （0公制，  1英制）
//temperature	N	String	温度设置（0 摄氏度 1华摄度）


data class TargetBean(
    var sportTarget: String = "",
    var sleepTarget: String = "",
    var consumeTarget: String = "",
    var distanceTarget: String = "",

    var unit: String = "",
    var temperature: String = "",
    var wearWay: String = "",
    var calibrationHeart: String = "",
    var calibrationDiastolic: String = "",
    var calibrationSystolic: String = ""
) {
    fun saveData(result: Response<TargetSettingResponse>) {

        val targetBean = TargetBean(
            sportTarget = if (!TextStringUtils.isNull(result.data.sportTarget.toString())) result.data.sportTarget.toString() else Constant.STEP_TARGET_DEFAULT_VALUE.toString(),
            sleepTarget = if (!TextStringUtils.isNull(result.data.sleepTarget.toString())) result.data.sleepTarget.toString() else Constant.SLEEP_TARGET_DEFAULT_VALUE.toString(),
            consumeTarget = if (!TextStringUtils.isNull(result.data.consumeTarget.toString())) result.data.consumeTarget.toString() else Constant.CALORIE_TARGET_DEFAULT_VALUE.toString(),
            distanceTarget = if (!TextStringUtils.isNull(result.data.distanceTarget.toString())) result.data.distanceTarget.toString() else Constant.DISTANCE_TARGET_DEFAULT_VALUE.toString(),
            unit = if (!TextStringUtils.isNull(result.data.unit)) result.data.unit else Constant.UNIT_DEFAULT_VALUE,
            temperature = if (!TextStringUtils.isNull(result.data.temperature)) result.data.temperature else Constant.TEMPERATURE_DEFAULT_VALUE
//                ,
//                wearWay = if (!TextStringUtils.isNull(result.data.weight)) result.data.weight else Constant.WEIGHT_TARGET_METRIC_DEFAULT_VALUE.toString(),
//                calibrationHeart = if (!TextStringUtils.isNull(result.data.weight)) result.data.weight else Constant.WEIGHT_TARGET_METRIC_DEFAULT_VALUE.toString(),
//                calibrationDiastolic = if (!TextStringUtils.isNull(result.data.weight)) result.data.weight else Constant.WEIGHT_TARGET_METRIC_DEFAULT_VALUE.toString(),
//                calibrationSystolic = if (!TextStringUtils.isNull(result.data.weight)) result.data.weight else Constant.WEIGHT_TARGET_METRIC_DEFAULT_VALUE.toString(),
        )
        //旧版本服务器距离目标千米处理
        try {
            val distanceTargetInt = targetBean.distanceTarget.toInt()
            if (distanceTargetInt >= 1000) {
                targetBean.distanceTarget = (distanceTargetInt / 1000).toString()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        saveData(targetBean)
    }

    fun saveData(targetBean: TargetBean) {
        SpUtils.setSportTarget(targetBean.sportTarget)
        SpUtils.setSleepTarget(targetBean.sleepTarget)
        SpUtils.setConsumeTarget(targetBean.consumeTarget)
        SpUtils.setDistanceTarget(targetBean.distanceTarget)
        SpUtils.setUnit(targetBean.unit)
        SpUtils.setTemperature(targetBean.temperature)
    }

    fun saveNullData() {
        SpUtils.setSportTarget(Constant.STEP_TARGET_DEFAULT_VALUE.toString())
        SpUtils.setSleepTarget(Constant.SLEEP_TARGET_DEFAULT_VALUE.toString())
        SpUtils.setConsumeTarget(Constant.CALORIE_TARGET_DEFAULT_VALUE.toString())
        SpUtils.setDistanceTarget(Constant.DISTANCE_TARGET_DEFAULT_VALUE.toString())
        SpUtils.setUnit(Constant.UNIT_DEFAULT_VALUE)
        SpUtils.setTemperature(Constant.TEMPERATURE_DEFAULT_VALUE)
    }

//    fun saveData(userBean: UserBean) {
//        SpUtils.setSportTarget(userBean.stepTarget)
//        SpUtils.setSleepTarget(userBean.sleepTarget)
//        SpUtils.setConsumeTarget(userBean.consumeTarget)
//        SpUtils.setDistanceTarget(userBean.distanceTarget)
//        SpUtils.setUnit(userBean.unit)
//    }

    fun getData(): TargetBean {

        return TargetBean(
            sportTarget = SpUtils.getSportTarget(),
            sleepTarget = SpUtils.getSleepTarget(),
            consumeTarget = SpUtils.getConsumeTarget(),
            distanceTarget = SpUtils.getDistanceTarget(),
            unit = SpUtils.getUnit(),
            temperature = SpUtils.getTemperature()
//                ,
//                wearWay = if (!TextStringUtils.isNull(result.data.weight)) result.data.weight else Constant.WEIGHT_TARGET_METRIC_DEFAULT_VALUE.toString(),
//                calibrationHeart = if (!TextStringUtils.isNull(result.data.weight)) result.data.weight else Constant.WEIGHT_TARGET_METRIC_DEFAULT_VALUE.toString(),
//                calibrationDiastolic = if (!TextStringUtils.isNull(result.data.weight)) result.data.weight else Constant.WEIGHT_TARGET_METRIC_DEFAULT_VALUE.toString(),
//                calibrationSystolic = if (!TextStringUtils.isNull(result.data.weight)) result.data.weight else Constant.WEIGHT_TARGET_METRIC_DEFAULT_VALUE.toString(),
        )


    }

    fun clearData() {
        SpUtils.setSportTarget("")
        SpUtils.setSleepTarget("")
        SpUtils.setConsumeTarget("")
        SpUtils.setDistanceTarget("")
        SpUtils.setUnit("")
        SpUtils.setTemperature("")
    }

    /**
     * 获取目标距离
     * @return String 单位米
     */
    fun getDistanceTargetMi(): String {
        //公制
        return if (unit == "0") {
            //千米-转换成米
            (distanceTarget.trim().toInt() * 1000).toString()
        }
        //英制
        else {
            //英里-转换成米
            UnitConverUtils.miToKm((distanceTarget.trim().toInt() * 1000).toString())
        }
    }
}


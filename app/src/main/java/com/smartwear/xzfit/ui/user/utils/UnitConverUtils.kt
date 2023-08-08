package com.smartwear.xzfit.ui.user.utils

import android.content.Context
import android.text.TextUtils
import com.zhapp.ble.utils.UnitConversionUtils
import com.smartwear.xzfit.R
import com.smartwear.xzfit.ui.user.bean.TargetBean
import com.smartwear.xzfit.utils.Constant
import java.math.RoundingMode
import java.text.DecimalFormat

object UnitConverUtils {

    fun getWeightDisparity(number: Int): Int {
        val aa = (number.toFloat() / 2.2f).toInt()
        val bb = (aa * 2.2).toInt()
        return number - bb
    }

    //磅转KG
    fun lbToKGString(lb: String): String {
        var kg = (lb.trim().toFloat() / 2.2f).toInt()
        kg = Math.max(kg,Constant.WEIGHT_TARGET_METRIC_MIN_VALUE)
        kg = Math.min(kg,Constant.WEIGHT_TARGET_METRIC_MAX_VALUE)
        return kg.toString()
    }

    //KG转磅
    fun kGToLbString(kg: String): String {
        var lbs = (kg.trim().toFloat() * 2.2f).toInt()
        lbs = Math.max(lbs,Constant.WEIGHT_TARGET_BRITISH_MIN_VALUE)
        lbs = Math.min(lbs,Constant.WEIGHT_TARGET_BRITISH_MAX_VALUE)
        return lbs.toString()
    }

    //厘米转英制
    fun cmToInInt(cm: String?): Int {
        return if (Integer.valueOf(cm) == 127 || Integer.valueOf(cm) == 254) {
            (java.lang.Double.valueOf(cm) / 2.54).toInt()
        } else {
            (java.lang.Double.valueOf(cm) / 2.54 + 1).toInt()
        }
    }

    //英里转公里
    fun miToKm(mi: String): String {
        return "${(mi.trim().toFloat() * 1.61f).toInt()}"
    }

    //公里转英里
    fun kmToMi(km: String): String {
        return "${(km.trim().toFloat() / 1.61f).toInt()}"
    }

    //公里转英里(保留两位小数)
    fun kmToMiKeepRemainder(km: String): String {
        if (TextUtils.isEmpty(km.trim())) return "0.00"
//        val number = km.trim().toFloat() / 1.61f
        return UnitConversionUtils.bigDecimalFormat(UnitConversionUtils.kilometersToMiles(km.trim().toFloat()))
    }

    //公里转英里(保留一位小数)
    fun kmToMiKeepRemainderToOneLen(km: String): String {
        if (TextUtils.isEmpty(km.trim())) return "0.0"
//        val number = km.trim().toFloat() / 1.61f
        return UnitConversionUtils.bigDecimalFormatToLen(UnitConversionUtils.kilometersToMiles(km.trim().toFloat()),1)
    }

    //公里转英里(保留三位小数)
    fun kmToMiKeepRemainderToThreeLen(km: String): String {
        if (TextUtils.isEmpty(km.trim())) return "0.000"
//        val number = km.trim().toFloat() / 1.61f
        return UnitConversionUtils.bigDecimalFormatToLen(UnitConversionUtils.kilometersToMiles(km.trim().toFloat()),3)
    }
    fun twoDecimalPlaces(number: Float): String {
        val format = DecimalFormat("0.##")
        format.roundingMode = RoundingMode.DOWN
        return format.format(number)
    }

    //厘米转英寸
    fun cmToInchString(cm: String): String {
        return "${(cm.trim().toFloat() * 0.4f).toInt()}"
    }

    //英寸转厘米
    fun inchToCmString(inch: String): String {
        return "${(inch.trim().toFloat() / 0.4f).toInt()}"
    }

    //根据app保存的单位公英制显示对应的距离
    fun showDistanceByUnitStyle(km: String): String {
        if (TextUtils.isEmpty(km)) return "0"
        val trim = km.trim()
        if (TextUtils.isEmpty(trim)) return "0"
//        var value = twoDecimalPlaces(trim.toFloat())
        var value = UnitConversionUtils.bigDecimalFormat(trim.toFloat())

        val mTargetBean = TargetBean().getData()
        if (mTargetBean != null && mTargetBean.unit != "0") {
            value = kmToMiKeepRemainder(trim)
        }
        return value
    }

    //根据app保存的单位公英制显示对应的距离单位文本
    fun showDistanceUnitStyle(context: Context): String {
        var value = context.getString(R.string.unit_distance_0)
        val mTargetBean = TargetBean().getData()
        if (mTargetBean != null && mTargetBean.unit != "0") {
            value = context.getString(R.string.unit_distance_1)
        }
        return value
    }

    fun showDistanceKmStyle(km: String): String {
        if (TextUtils.isEmpty(km)) return "0.00"
        val trim = km.trim()
        if (TextUtils.isEmpty(trim)) return "0.00"
        var value = /*精度丢失UnitConversionUtils.bigDecimalFormat(UnitConversionUtils.metersToKilometer(trim.toFloat().toLong()))*/
            UnitConversionUtils.metersToKilometer(trim.toFloat().toLong()).toString()
        val mTargetBean = TargetBean().getData()
        if (mTargetBean != null) {
            value = if (mTargetBean.unit != "0") {
                kmToMiKeepRemainder(value)
            } else {
                UnitConversionUtils.bigDecimalFormat(value.toFloat())
            }
        }
        return value
    }

    //保留疑问小数点
    @JvmStatic
    fun showDistanceStyleToOneLen(m: String): String {
        if (TextUtils.isEmpty(m)) return "0.0"
        val trim = m.trim()
        if (TextUtils.isEmpty(trim)) return "0.0"
        var value = UnitConversionUtils.metersToKilometer(trim.toFloat().toLong()).toString()
        val mTargetBean = TargetBean().getData()
        value = if (mTargetBean.unit != "0") {
            kmToMiKeepRemainderToOneLen(value)
        } else {
            UnitConversionUtils.bigDecimalFormatToLen(value.toFloat(),1)
        }
        return value
    }

    //保留疑问小数点
    @JvmStatic
    fun showDistanceStyleToThreeLen(m: String): String {
        if (TextUtils.isEmpty(m)) return "0.000"
        val trim = m.trim()
        if (TextUtils.isEmpty(trim)) return "0.000"
        var value = UnitConversionUtils.metersToKilometer(trim.toFloat().toLong()).toString()
        val mTargetBean = TargetBean().getData()
        value = if (mTargetBean.unit != "0") {
            kmToMiKeepRemainderToThreeLen(value)
        } else {
            UnitConversionUtils.bigDecimalFormatToLen(value.toFloat(),3);
        }
        return value
    }

}
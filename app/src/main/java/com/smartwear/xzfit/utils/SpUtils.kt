package com.smartwear.xzfit.utils

import android.content.Context
import android.text.TextUtils
import com.smartwear.xzfit.base.BaseApplication
import java.util.*

/**
 * Created by android
 * on 2021/8/31
 */
object SpUtils {
    const val sharedPreferencesFit = "info_fit"

    const val APP_FIRST_START = "APP_FIRST_START"
    const val APP_FIRST_START_DEFAULT = "0"

    const val USER_IS_LOGIN = "user_is_login"
    const val USER_IS_LOGIN_DEFAULT = "0"

    //agps服务器获取下载完成时间
    const val AGPS_DOWNLOAD_TIME = "agps_download_time"
    //agps同步至设备时间
    const val AGPS_SYNC_TIME = "agps_sync_time"
    //时间设置发送时间
    const val TIME_SET_SEND_TIME = "time_set_send_time"
    //时间设置同步成功
    const val TIME_SET_SUCCESS_TIME = "time_set_success_time"


    //服务器地区 TODO
    const val SERVICE_ADDRESS = "service_address"
    const val SERVICE_ADDRESS_DEFAULT = "0"//国内
    const val SERVICE_ADDRESS_TO_TYPE1 = "1"//国内
    const val SERVICE_ADDRESS_TO_TYPE2 = "2"//国外
    const val SERVICE_REGION_COUNTRY_CODE = "service_region_country_code"
    const val SERVICE_REGION_AREA_CODE = "service_region_area_code"

    //首页权限说明弹窗标志位
    const val FIRST_MAIN_PERMISSION_EXPLAIN = "MAIN_PER_EXPLAIN"

    //绑定设备后台保活说明弹窗标志位  绑定成功置false 弹窗说明置true
    const val BIND_DEVICE_CONNECTED_KEEPLIVE_EXPLANATION = "MAIN_BDCKE"

    const val USER_ID = "USER_ID"//用户ID
    const val USER_NAME = "USER_NAME"//用户名
    const val USER_PASSWORD = "USER_PASSWORD"//密码
    const val AUTHORIZATION = "AUTHORIZATION"//登录凭证
    const val ACCOUNT_TYPE = "ACCOUNT_TYPE"//账号类型：1、手机，2、邮箱 3、微信 4、qq 5、游客登录
    const val REGISTER_TIME = "REGISTER_TIME"//注册时间
    const val USER_LOCAL_DATA = "USER_LOCAL_DATA"
    const val USER_INFO_AVATAR_URI = "USER_INFO_AVATAR_URI"
    const val HEALTHY_SHOW_ITEM_LIST = "HEALTHY_SHOW_ITEM_LIST_VALUE"
    const val EDIT_CARD_ITEM_LIST = "EDIT_CARD_ITEM_LIST_VALUE"//卡片编辑数据缓存
    const val TOURIST_UUID = "TOURIST_UUID"//游客登录ID

    //设备昵称
    const val DEVICE_NAME = "DEVICE_NAME"

    //设备mac
    const val DEVICE_MAC = "DEVICE_MAC"

    //设备版本号
    const val DEVICE_VERSION = "DEVICE_VERSION"

    //设备消息通知列表
    const val DEVICE_MSG_NOTIFY_ITEM_LIST = "NEW_MSG_NOTIFY_SYS"

    //设备其它APP消息通知列表
    const val DEVICE_MSG_NOTIFY_ITEM_LIST_OTHER = "NEW_MSG_NOTIFY_OTHER"

    //设备快捷回复列表
    const val DEVICE_SHORT_REPLY_LIST = "DEVICE_SHORT_REPLY_LIST"

    //Google fit 开关
    const val SWITCH_GOOGLE_FIT = "SWITCH_GOOGLE_FIT"

    //Strava 开关
    const val SWITCH_STRAVA = "SWITCH_STRAVA"

    //设备时间是否12小时设置
    const val DEVICE_TIME_IS12 = "DEVICE_TIME_IS12"

    //设备省电设置开关
    const val DEVICE_POWER_SAVING = "DEVICE_POWER_SAVING"

    //app启动初次打开运动界面时请求权限
    const val FIRST_REQUEST_LOCATION_PERMISSION = "FIRST_LOCATION_PERMISSION"

    //设备选中语言id
    const val DEVICE_SELECT_LANGUAGE_ID = "DEVICE_SELECT_LANGUAGE_ID"

    const val WOMEN_HEALTH_SHOW_CYCLE_DATE_SWITCH = "WOMEN_HEALTH_SHOW_CYCLE_DATE_SWITCH"
    const val WOMEN_HEALTH_MENSTRUAL_CYCLE_LENGTH = "WOMEN_HEALTH_MENSTRUAL_CYCLE_LENGTH"
    const val WOMEN_HEALTH_SUM_SAFETY_PERIOD = "WOMEN_HEALTH_SUM_SAFETY_PERIOD"//距离安全期（不包含当天）
    const val WOMEN_HEALTH_PERIOD_LENGTH = "WOMEN_HEALTH_PERIOD_LENGTH"
    const val WOMEN_HEALTH_CYCLE_TIME = "WOMEN_HEALTH_CYCLE_TIME"
    const val WOMEN_HEALTH_OPEN_HEALTH_CHECK = "WOMEN_HEALTH_OPEN_HEALTH_CHECK"
    const val WOMEN_HEALTH_ADVANCE_DAY = "WOMEN_HEALTH_ADVANCE_DAY"

    const val WOMEN_HEALTH_MENSTRUAL_PERIOD_DAY = "WOMEN_HEALTH_MENSTRUAL_PERIOD_DAY" //月经期
    const val WOMEN_HEALTH_OVIPOSIT_PERIOD_DAY = "WOMEN_HEALTH_OVIPOSIT_PERIOD_DAY"  //排卵期
    const val WOMEN_HEALTH_SAFETY1_PERIOD_DAY = "WOMEN_HEALTH_SAFETY1_PERIOD_DAY"  //安全期1
    const val WOMEN_HEALTH_SAFETY2_PERIOD_DAY = "WOMEN_HEALTH_SAFETY2_PERIOD_DAY"  //安全期2
//    const val WOMEN_HEALTH_DATA_FROM_DEVICE = "WOMEN_HEALTH_DATA_FROM_DEVICE"  //同步设备生理周期数据


    const val DEVICE_SETTING = "DEVICE_SETTING"  //设备设置展示 UI

    const val WEATHER_SWITCH = "WEATHER_SWITCH" //天气开关
    const val WEATHER_LONGITUDE_LATITUDE = "WEATHER_LONGITUDE_LATITUDE" //天气经纬度
    const val WEATHER_CITY_NAME = "WEATHER_CITY_NAME" //天气城市名

    //    const val WEATHER_CITY_ID = "WEATHER_CITY_ID" //天气城市ID
    const val WEATHER_SYNC_TIME = "WEATHER_SYNC_TIME"//每次同步天气时间戳
    const val WEATHER_DAYS_INFO = "WEATHER_DAYS_INFO"//每日天气信息
    const val WEATHER_AQI_INFO = "WEATHER_AQI_INFO"//天气AQI信息
    const val WEATHER_PER_HOUR_INFO = "WEATHER_PER_HOUR_INFO"//天气PER_HOUR信息


    const val REQUEST_DEVICE_INFO_IS_NO_DATA = "REQUEST_DEVICE_INFO_IS_NO_DATA"
    const val REQUEST_DEVICE_INFO_IS_HAS_DATA = "REQUEST_DEVICE_INFO_IS_HAS_DATA"
    const val LAST_DEVICE_LOGIN_TIME = "LAST_DEVICE_LOGIN_TIME" //同一个账号上次登录的时间



    //存储服务器返回MTU信息
    const val THE_SERVER_MTU_INFO_MTU = "THE_SERVER_MTU_INFO_MTU" //MTU
    const val THE_SERVER_MTU_INFO_MAX_VALUE = "THE_SERVER_MTU_INFO_MAX_VALUE" //max
    const val THE_SERVER_MTU_INFO_MIN_VALUE = "THE_SERVER_MTU_INFO_MIN_VALUE" //min

    //存储服务器返回设备坐标系
    const val THE_SERVER_GPS_TYPE = "SERVER_GPS_TYPE" //01：火星坐标系  02：WGS-84坐标系

    //存储服务器返回设备指令间隔
    const val THE_SERVER_PACK_SEND_INTERVAL = "SERVER_PACK_SEND_INTERVAL"

    //当前设备固件平台
    const val CURRENT_FIRMWARE_PLATFORM = "CURRENT_FIRMWARE_PLATFORM"

    //strava token
    const val STRAVA_TOKEN_KEY = "STRAVA_TOKEN"

    //bind Headset mac
    const val HEADSET_MAC = "HEADSET_MAC_KEY"

    const val HEADSET_NAME = "HEADSET_NAME_KEY"

    //定位经纬度缓存
    const val LOCATION_CACHE = "location_cache"

    //用户行为只记录一次的id缓存  ["1","2"] = 1，2已记录
    const val BEHAVIOR_ONLY_KEY = "BEHAVIOR_ONLY_KEY"

    //电池优化值缓存  0 关  1 开
    const val POWER_OPTIMIZATIONS = "POWER_OPTIMIZATIONS"

    //Diy效果图路径
    const val DIY_RENDERINGS_PATH = "DIY_RENDERINGS"

    //消息通知引导 true 已经显示  fasle 未显示
    const val NOTIFY_USER_GUIDANCE_TIPS = "NOTIFY_USER_GUIDANCE_TIPS"

    @JvmStatic
    fun setValue(key: String, value: String) {
        val sharedPreferences = BaseApplication.mContext?.getSharedPreferences(sharedPreferencesFit, Context.MODE_PRIVATE)
        val editor = sharedPreferences?.edit()
        editor?.putString(key, value)
        editor?.apply()
    }

    @JvmStatic
    fun getValue(key: String, defValue: String): String {
        val sharedPreferences = BaseApplication.mContext?.getSharedPreferences(sharedPreferencesFit, Context.MODE_PRIVATE)
        return sharedPreferences?.getString(key, defValue).toString()
    }

    @JvmStatic
    fun getSPUtilsInstance(): com.blankj.utilcode.util.SPUtils {
        return com.blankj.utilcode.util.SPUtils.getInstance(sharedPreferencesFit, Context.MODE_PRIVATE)
    }

    fun remove(key: String) {
        val sharedPreferences = BaseApplication.mContext?.getSharedPreferences(sharedPreferencesFit, Context.MODE_PRIVATE)
        sharedPreferences?.edit()?.remove(key)?.apply()
    }

    @JvmStatic
    fun putValue(key: String, value: String) {
        val sharedPreferences = BaseApplication.mContext?.getSharedPreferences(sharedPreferencesFit, Context.MODE_PRIVATE)
        val editor = sharedPreferences?.edit()
        editor?.putString(key, value)
        editor?.apply()
    }


    private const val UserTag = "UserTag"

    //用户信息接口
//    const val UserId = UserTag + "UserId"//用户id
    const val Nickname = UserTag + "Nickname"//昵称
    const val Height = UserTag + "Height"//昵称
    const val Weight = UserTag + "Weight"//体重
    const val BritishHeight = UserTag + "britishHeight"//昵称
    const val BritishWeight = UserTag + "britishWeight"//体重
    const val Birthday = UserTag + "Birthday"//出生日期
    const val Sex = UserTag + "Sex"//性别（0：男 1：女）
    const val Head = UserTag + "Head"//头像url
//    const val StepTarget = "StepTarget"
//    const val DistanceTarget = "DistanceTarget"
//    const val ConsumeTarget = "ConsumeTarget"
//    const val SleepTarget = "SleepTarget"
//    const val SkinColor = "SkinColor"
//    const val Unit = "Unit"

    fun setNickname(str: String) {
        setValue(Nickname, str)
    }

    fun getNickname(): String {
        return getValue(Nickname, " ")
    }

    fun setHeight(str: String) {
        setValue(Height, str)
    }

    fun getHeight(): String {
        return getValue(Height, Constant.HEIGHT_TARGET_METRIC_DEFAULT_VALUE.toString())
    }

    fun setBritishHeight(str: String) {
        setValue(BritishHeight, str)
    }

    fun getBritishHeight(): String {
        return getValue(BritishHeight, Constant.HEIGHT_TARGET_BRITISH_DEFAULT_VALUE.toString())
    }

    fun setWeight(str: String) {
        setValue(Weight, str)
    }

    fun getWeight(): String {
        return getValue(Weight, Constant.WEIGHT_TARGET_METRIC_DEFAULT_VALUE.toString())
    }

    fun setBritishWeight(str: String) {
        setValue(BritishWeight, str)
    }

    fun getBritishWeight(): String {
        return getValue(BritishWeight, Constant.WEIGHT_TARGET_BRITISH_DEFAULT_VALUE.toString())
    }

    fun setBirthday(str: String) {
        setValue(Birthday, str)
    }

    fun getBirthday(): String {
        return getValue(Birthday, Constant.BIRTHDAY_DEFAULT_VALUE)
    }

    fun setSex(str: String) {
        setValue(Sex, str)
    }

    fun getSex(): String {
        return getValue(Sex, Constant.SEX_DEFAULT_VALUE)
    }

    fun setHead(str: String) {
        setValue(Head, str)
    }

    fun getHead(): String {
        return getValue(Head, "")
    }


    private const val TargetTag = "TargetTag"

    //目标接口
    const val SportTarget = TargetTag + "SportTarget"//运动目标
    const val SleepTarget = TargetTag + "SleepTarget"//睡眠目标
    const val ConsumeTarget = TargetTag + "ConsumeTarget"//消耗目标（没有补0）-卡路里目标
    const val DistanceTarget = TargetTag + "DistanceTarget"//距离目标（没有补0）

    //    const val CalibrationHeart = TargetTag + "CalibrationHeart"//校准心率
//    const val CalibrationDiastolic = TargetTag + "CalibrationDiastolic"//校准舒张压
//    const val CalibrationSystolic = TargetTag + "CalibrationSystolic"//校准收缩压
//    const val WearWay = TargetTag + "WearWay"//佩戴方式(左和右)L,R
    const val Unit = TargetTag + "Unit"//单位设置 （0公制，  1英制）
    const val Temperature = TargetTag + "Temperature"//温度设置（0 摄氏度 1华摄度）
    fun setSportTarget(str: String) {
        setValue(SportTarget, str)
    }

    fun getSportTarget(): String {
        return getValue(SportTarget, Constant.STEP_TARGET_DEFAULT_VALUE.toString())
    }

    fun setSleepTarget(str: String) {
        setValue(SleepTarget, str)
    }

    fun getSleepTarget(): String {
        return getValue(SleepTarget, Constant.SLEEP_TARGET_DEFAULT_VALUE.toString())
    }

    fun setConsumeTarget(str: String) {
        setValue(ConsumeTarget, str)
    }

    fun getConsumeTarget(): String {
        return getValue(ConsumeTarget, Constant.CALORIE_TARGET_DEFAULT_VALUE.toString())
    }

    fun setDistanceTarget(str: String) {
        setValue(DistanceTarget, str)
    }

    fun getDistanceTarget(): String {
        return getValue(DistanceTarget, Constant.DISTANCE_TARGET_DEFAULT_VALUE.toString())
    }

//    fun setCalibrationHeart(str: String) {
//        setValue(CalibrationHeart, str)
//    }
//
//    fun getCalibrationHeart(): String {
//        return getValue(CalibrationHeart, "")
//    }
//
//    fun setCalibrationDiastolic(str: String) {
//        setValue(CalibrationDiastolic, str)
//    }
//
//    fun getCalibrationDiastolic(): String {
//        return getValue(CalibrationDiastolic, "")
//    }
//
//    fun setCalibrationSystolic(str: String) {
//        setValue(CalibrationSystolic, str)
//    }
//
//    fun getCalibrationSystolic(): String {
//        return getValue(CalibrationSystolic, "")
//    }
//
//    fun setWearWay(str: String) {
//        setValue(WearWay, str)
//    }
//
//    fun getWearWay(): String {
//        return getValue(WearWay, "")
//    }

    fun setUnit(str: String) {
        setValue(Unit, str)
    }

    fun getUnit(): String {
        return getValue(Unit, Constant.UNIT_DEFAULT_VALUE)
    }

    fun setTemperature(str: String) {
        setValue(Temperature, str)
    }

    fun getTemperature(): String {
        return getValue(Temperature, Constant.TEMPERATURE_DEFAULT_VALUE)
    }

    fun getGooglefitSwitch(): Boolean {
        return getSPUtilsInstance().getBoolean(SWITCH_GOOGLE_FIT, false)
    }

    fun setGooglefitSwitch(isSwitch: Boolean) {
        getSPUtilsInstance().put(SWITCH_GOOGLE_FIT, isSwitch)
    }

    fun getStravaSwitch(): Boolean {
        return getSPUtilsInstance().getBoolean(SWITCH_STRAVA, false)
    }

    fun setStravaSwitch(isSwitch: Boolean) {
        getSPUtilsInstance().put(SWITCH_STRAVA, isSwitch)
    }

    fun saveHeadsetName(bleMac: String, headsetName: String){
        if (!TextUtils.isEmpty(bleMac)) {
            getSPUtilsInstance().put(HEADSET_NAME + bleMac.uppercase(Locale.ENGLISH), headsetName)
        }
    }

    fun getHeadsetName(mac: String): String {
        if (TextUtils.isEmpty(mac)) return ""
        return getSPUtilsInstance().getString(HEADSET_NAME + mac.uppercase(Locale.ENGLISH), "")
    }

    fun getHeadsetMac(mac: String): String {
        if (TextUtils.isEmpty(mac)) return ""
        return getSPUtilsInstance().getString(HEADSET_MAC + mac.uppercase(Locale.ENGLISH), "")
    }

    fun saveHeadsetMac(bleMac: String, headsetMac: String) {
        if (!TextUtils.isEmpty(bleMac)) {
            getSPUtilsInstance().put(HEADSET_MAC + bleMac.uppercase(Locale.ENGLISH), headsetMac)
        }
    }

}
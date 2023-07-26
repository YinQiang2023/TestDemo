package com.jwei.xzfit.https.params

/**
 * Created by Android on 2023/4/13.
 */
class DevTrackingParam {
    /**
     * 设备号
     */
    var deviceType: String = ""

    /**
     * 设备版本号
     */
    var deviceVersion: String = ""

    /**
     * SN码 mac地址
     */
    var deviceSn: String = ""

    /**
     * 用户注册区域
     */
    var registrationArea: String = ""

    /**
     * 设备日志
     */
    var dataList: List<String>? = null

    /**
     * 文件名
     */
    var logFileName: String = ""
}
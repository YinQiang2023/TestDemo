package com.jwei.publicone.https.params

/**
 * Created by Android on 2023/4/10.
 * App异常埋点日志上传，每次每次上传不超过10条
 */
class TrackingAppParam {
    /**
     * 模块id
     */
    var pageModule:String = ""

    /**
     * 编码：XX00为未赋值(中途被杀掉,事件未完成等) , XX01为成功    错误码从XX10开始编码,  XX11，XX12...
     */
    var errorCode:String = ""

    /**
     * 设备型号
     */
    var deviceType:String = ""

    /**
     * 设备版本
     */
    var deviceVersion:String = ""

    /**
     * 设备SN
     */
    var deviceSn:String = ""

    /**
     * 事件开始时间
     */
    var startTimestamp:String = ""

    /**
     * 日志
     */
    var errorLog:String = ""

    /**
     * 用户注册地区
     */
    var registrationArea:String = ""

    /**
     * 成功次数
     */
    var successNum:String = ""

    /**
     * 是否警告  警告 1 异常 0
     */
    var warn:String = ""
}
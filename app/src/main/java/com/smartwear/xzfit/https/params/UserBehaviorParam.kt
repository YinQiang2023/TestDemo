package com.smartwear.xzfit.https.params

/**
 * Created by Android on 2023/4/13.
 */
class UserBehaviorParam {
    /**
     * 模块id
     */
    var pageModule:String = ""

    /**
     * 模块功能id
     */
    var moduleFunction:String = ""

    /**
     * 事件触发开始时间戳
     */
    var startTimestamp:String = ""

    /**
     * 时长单位s
     */
    var durationSec:String? = null

    /**
     * pageModule=3时必传，功能的使用状态，记录开关状态 int 0 关 1开
     */
    var functionUseStatus:String? = null

    /**
     * pageModule=3时必传，统计功能是否进行设置  int 0 关 1 开
     */
    var functionStatus:String? = null

    /**
     * 用户注册地区
     */
    var registrationArea:String? = null
}
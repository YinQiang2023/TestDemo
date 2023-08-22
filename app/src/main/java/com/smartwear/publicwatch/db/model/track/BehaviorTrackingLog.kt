package com.smartwear.publicwatch.db.model.track

import com.smartwear.publicwatch.utils.LogUtils
import org.litepal.crud.LitePalSupport

/**
 * Created by Android on 2023/4/13.
 * 用户行为埋点日志
 */
class BehaviorTrackingLog : LitePalSupport() {
    /**
     * 用户id
     */
    var userId: String = ""

    /**
     * APP触发事件时间戳 秒级时间戳
     */
    var startTimestamp: String = ""

    /**
     * 模块id
     */
    var pageModule: String = ""

    /**
     * 模块功能id
     */
    var moduleFunction: String = ""

    /**
     * 时长单位 多少s
     */
    var durationSec: String = ""

    /**
     * pageModule=3时必传，功能的使用状态，记录开关状态
     */
    var functionSwitchStatus: String = ""

    /**
     * pageModule=3时必传，统计功能是否进行设置
     */
    var functionStatus: String = ""

    /**
     * 创建时间戳
     */
    var createDateTime: String = ""

    /**
     * 更新时间戳
     */
    var upDateTime: String = ""

    /**
     * 用户注册地区
     */
    var registrationArea: String = ""

    /**
     * 是否上传至服务器
     */
    var isUpLoad: Boolean = false


    fun saveUpdate(): Boolean {
        synchronized(BehaviorTrackingLog::class.java) {
            return try {
                if (createDateTime.isEmpty()) {
                    createDateTime = System.currentTimeMillis().toString()
                }
                upDateTime = System.currentTimeMillis().toString()
                save()
            } catch (e: Exception) {
                LogUtils.e("BehaviorTrackingLog", "saveUpdate Exception e = $e", true)
                false
            }
        }
    }

    override fun toString(): String {
        return "BehaviorTrackingLog(userId='$userId', startTimestamp='$startTimestamp', pageModule='$pageModule', moduleFunction='$moduleFunction', durationSec='$durationSec', functionUseStatus='$functionSwitchStatus', function_status='$functionStatus', createDateTime='$createDateTime', upDateTime='$upDateTime', registrationArea=$registrationArea, isUpLoad=$isUpLoad)"
    }


}
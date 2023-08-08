package com.smartwear.xzfit.db.model.track

import com.smartwear.xzfit.utils.LogUtils
import org.litepal.crud.LitePalSupport

/**
 * Created by Android on 2023/4/7.
 * 异常埋点
 */
class AppTrackingLog : LitePalSupport() {
    /**
     * 用户id
     */
    var userId: String = ""

    /**
     * APP触发事件时间戳 秒级
     */
    var startTimestamp: String = ""

    /**
     * 模块代码  10 = 注册；11 = 登录；12 = 绑定解绑；13 = 蓝牙重连；14 = 同步数据；15 = 运动记录；16 = 天气；17 = 消息通知；18 = 表盘；19 = OTA；20 = AGPS；21 = 辅助运动
     */
    var pageModule: String = ""

    /**
     *  编码：XX00为未赋值(中途被杀掉,事件未完成等) , XX01为成功    错误码从XX10开始编码,  XX11，XX12...
     */
    var errorCode: String = ""

    /**
     * 错误日志
     * @see com.smartwear.xzfit.db.model.track.TrackingLog
     */
    var errorLog: String = ""


    /**
     * 创建时间戳
     */
    var createDateTime: String = ""

    /**
     * 更新时间戳
     */
    var upDateTime: String = ""

    /**
     * 设备型号
     */
    var deviceType: String = ""

    /**
     * 设备mac
     */
    var deviceMac: String = ""

    /**
     * 设备版本
     */
    var deviceVersion: String = ""

    /**
     * app版本
     */
    var appVersion: String = ""

    /**
     * 是否埋点结束 0 false 1 true
     */
    var isEndTrack: Boolean = false

    /**
     * 是否上传至服务器
     */
    var isUpLoad: Boolean = false


    fun saveUpdate(): Boolean {
        synchronized(AppTrackingLog::class.java) {
            return try {
                if (createDateTime.isEmpty()) {
                    createDateTime = System.currentTimeMillis().toString()
                }
                upDateTime = System.currentTimeMillis().toString()
                save()
            } catch (e: Exception) {
                LogUtils.e("AppTrackingLog", "saveUpdate Exception e = $e", true)
                false
            }
        }
    }

    override fun toString(): String {
        return "AppTrackingLog(isEndTrack=$isEndTrack, isUpLoad=$isUpLoad, userId='$userId', startTimestamp='$startTimestamp', pageModule='$pageModule', errorCode='$errorCode', errorLog='$errorLog', createDateTime='$createDateTime', upDateTime='$upDateTime', deviceType='$deviceType', deviceMac='$deviceMac', deviceVersion='$deviceVersion', appVersion='$appVersion')"
    }


}
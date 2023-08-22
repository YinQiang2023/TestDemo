package com.smartwear.publicwatch.db.model.track

import com.smartwear.publicwatch.utils.LogUtils
import org.litepal.crud.LitePalSupport

/**
 * Created by Android on 2023/4/13.
 * 设备埋点日志
 */
class DevTrackingLog : LitePalSupport() {
    /**
     * 用户id
     */
    var userId: String = ""

    /**
     * 创建时间戳
     */
    var createDateTime: String = ""

    /**
     * 更新时间戳
     */
    var upDateTime: String = ""

    /**
     * 设备类型
     */
    var deviceType: String = ""

    /**
     * 设备版本
     */
    var deviceVersion: String = ""

    /**
     * 设备sn
     */
    var deviceSn: String = ""

    /**
     * 注册区域
     */
    var registrationArea: String = ""

    /**
     * 数据
     */
    var data: String = ""

    /**
     * 文件名
     */
    var logFileName: String = ""

    /**
     * 是否上传至服务器
     */
    var isUpLoad: Boolean = false


    fun saveUpdate(): Boolean {
        synchronized(DevTrackingLog::class.java) {
            return try {
                if (createDateTime.isEmpty()) {
                    createDateTime = System.currentTimeMillis().toString()
                }
                upDateTime = System.currentTimeMillis().toString()
                save()
            } catch (e: Exception) {
                LogUtils.e("DevTrackingLog", "saveUpdate Exception e = $e", true)
                false
            }
        }
    }

    override fun toString(): String {
        return "DevTrackingLog(userId='$userId', createDateTime='$createDateTime', upDateTime='$upDateTime', deviceType='$deviceType', deviceVersion='$deviceVersion', deviceSn='$deviceSn', registrationArea='$registrationArea', data='$data', logFileName='$logFileName', isUpLoad=$isUpLoad)"
    }


}
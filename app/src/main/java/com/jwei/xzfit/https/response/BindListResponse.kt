package com.jwei.xzfit.https.response

import java.io.Serializable

/**
 * Created by android
 * on 2021/9/9
 */
class BindListResponse(var dataList: MutableList<DeviceItem>) {
    class DeviceItem : Serializable {
        var id: Long = 0
        var userId: Long = 0
        var deviceType: Long = 0
        var deviceMac: String = ""
        var deviceName: String = ""
        var deviceVersion: String = ""
        var deviceSn: String = ""
        var broadcast: String = ""
        var bindTime: String = ""
        var deviceStatus: Int = 0

        override fun toString(): String {
            return "DeviceItem(id=$id, userId=$userId, deviceType=$deviceType, deviceMac='$deviceMac', deviceName='$deviceName', deviceVersion='$deviceVersion', deviceSn='$deviceSn', broadcast='$broadcast', bindTime='$bindTime', deviceStatus=$deviceStatus)"
        }
    }

    override fun toString(): String {
        return "BindListResponse(dataList=$dataList)"
    }

}

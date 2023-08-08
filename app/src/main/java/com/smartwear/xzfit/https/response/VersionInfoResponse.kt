package com.smartwear.xzfit.https.response

class VersionInfoResponse {
    //    lateinit var dataList : List<VersionInfoResponse.Data>
//    class Data{
    var deviceType = "" //	Integer	设备号
    var deviceVersion = "" //	String	设备号版本
    var bluetoothName = "" //	Long	蓝牙名称
    var pushLengthLimit = "" //	String	推送长度（Android使用）
    var reminderRelated = "" //	String	提醒相关  返回功能解释建下面表格
    var settingsRelated = "" //	String	设置相关  返回功能解释建下面表格
    var functionRelated = "" //	Byte	功能相关  返回功能解释建下面表格
    var dataRelated = "" //	String	数据相关  返回功能解释建下面表格

    //    }
    override fun toString(): String {
        return "VersionInfoResponse(deviceType='$deviceType', deviceVersion='$deviceVersion', bluetoothName='$bluetoothName', pushLengthLimit='$pushLengthLimit', reminderRelated='$reminderRelated', settingsRelated='$settingsRelated', functionRelated='$functionRelated', dataRelated='$dataRelated')"
    }

}
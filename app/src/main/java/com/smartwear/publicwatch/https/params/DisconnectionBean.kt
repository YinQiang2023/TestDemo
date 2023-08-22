package com.smartwear.publicwatch.https.params

/**
 * Created by Android on 2022/8/11.
 */
class DisconnectionBean {
    var userId: String = ""  //	Long	用户id
    var deviceType: String = ""  //	Long	设备号
    var deviceVersion: String = ""  //	String	设备版本
    var deviceMac: String = ""  //	String	设备MAC
    var sn: String = ""  //	String
    var keepAlivePermission: String = ""  //	Integer	  App保活开启的权限(Android)   1：开启 iOS不需要传
    var phoneSystemType: String = "2"  //	Integer	手机系统类型(0:其他,1:ios,2:andorid)
    var phoneModel: String = ""  //	String	手机型号
    var phoneSystemVersion: String = ""  //	String	手机系统版本
    var appPushTime: String = ""  //	String	App上传时间
    var startAppTimestamp: String = ""  //	String	  启动App时间戳
    var batteryInfo: String = ""  //	String	设备电量信息
    var disconnectionTimestamp: String = ""  //	String	断连时间戳
    var disconnectReasonCode: String = ""  //		String	断连原因编码
    var deviceLog: String = ""  //		String	记录字符串
}
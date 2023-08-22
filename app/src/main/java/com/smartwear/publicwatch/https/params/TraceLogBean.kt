package com.smartwear.publicwatch.https.params

import com.google.gson.annotations.SerializedName

/**
 * Created by Android on 2022/7/27.
 */
class TraceLogBean {
    var userId: String = ""    //用户id
    var page: String = ""    //页面模块（见下列表定义）
    var module: String = ""    //具体功能（见下列表定义）

    @SerializedName("val")
    var value: String = ""    //值
    var phoneSystemType: String = "2"    //手机系统类型(0:其他,1:ios,2:andorid)
    var phoneSystemLanguage: String = ""    //手机系统语言
    var imei: String = ""    //安卓设备唯一标识（苹果设备唯一标识 ifda）为空
    var phoneType: String = ""    //手机型号
    var phoneSystemVersion: String = ""    //手机系统版本
    var idfv: String? = null    //(IDFV-identifierForVendor）苹果设备广告主标识
    var longitude: String = ""    //经度
    var latitude: String = ""    //纬度
    var phoneSystemArea: String = ""    //手机系统地区
    var errorLog: String? = ""      //功能异常错误日志，字段长度暂定2000
}
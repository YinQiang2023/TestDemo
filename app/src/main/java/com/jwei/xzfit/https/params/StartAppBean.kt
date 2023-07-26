package com.jwei.xzfit.https.params

/**
 * Created by Android on 2022/7/19.
 */
class StartAppBean {
    var userId: String = ""    //用户id
    var phoneSystemType: String = "2"    //Integer手机系统类型(0:其他,1:ios,2:andorid)
    var phoneSystemLanguage: String = ""    //String(200)手机系统语言
    var imei: String ?= null    //String(100)安卓设备唯一标识（苹果设备唯一标识 ifda）为空，不要调用接口
    var phoneType: String = ""    //String(50)手机型号
    var phoneSystemVersion: String = ""    //String(30)手机系统版本
    var phoneMac: String = ""    //String(50)手机MAC
    var phoneName: String? = null    //String(100)手机设备名称
    //var idfv: String = ""    //String(50)(IDFV-identifierForVendor）苹果设备广告主标识
    var appUnix: String = ""    //String(15)app时间戳
    var country: String = ""    //String（100）国家
    var province: String = ""    //String（100）省份
    var city: String = ""    //String(100)城市
    var internetType: String = ""    //String(50)联网类型  :4G 5G  wifi..
    var simType: String = ""    //int(50)sim运营商:中国移动、联通、cmcc
    var longitude: String = ""    //String(20)经度
    var latitude: String = ""    //String(20)纬度
    var ip: String = ""    //String(10)ip
    var phoneSystemArea: String = ""    //String(100)手机系统地区
}
package com.smartwear.publicwatch.https.params

/**
 * Created by Android on 2022/6/29.
 */
class DialLogBean {
    // Y
    var userId: String = ""                //用户id
    var dialId: String = ""                //表盘ID
    var dialFileType: Int = 1             //1:横向扫描 2:竖向扫描
    var deviceType: String = ""            //设备号
    var dataType: Int = 1                 //1、表盘下载 2、表盘传输 3、表盘传输成功 4、传输失败
    var phoneSystemType: Int = 2          //手机系统类型(0:其他1:ios2:andorid)
    var phoneSystemLanguage: String = ""   //手机系统语言
    var imei: String = ""                  //安卓设备唯一标识（苹果设备唯一标识 ifda）为空，不要调用接口
    var appVersion: String = ""            //app版本号
    var appName: String = "X Fit"        //App名称
    // N
    var phoneType: String? = null             //手机型号
    var phoneSystemVersion: String? = null    //手机系统版本
    var phoneMac: String? = null              //手机MAC
    var phoneName: String? = null             //手机设备名称
    var idfv: String? = null                  //(IDFV-identifierForVendor）苹果设备广告主标识
    var appUnix: String? = null               //app时间戳
    var country: String? = null               //国家
    var province: String? = null              //省份
    var city: String? = null                  //城市
    var internetType: String? = null          //联网类型  :4G 5G  wifi..
    var simType: String? = null               //sim运营商:中国移动、联通、cmcc
    var longitude: String? = null             //经度
    var latitude: String? = null              //纬度
    var ip: String? = null                    //ip
    var phoneSystemArea: String? = null       //手机系统地区
}

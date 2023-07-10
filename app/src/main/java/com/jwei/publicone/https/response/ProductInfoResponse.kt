package com.jwei.publicone.https.response

/**
 * Created by Android on 2021/11/16.
 */
class ProductInfoResponse {
    var deviceType = "" //	Integer	设备号
    var bluetoothName = "" //	String	蓝牙名称
    var firmwarePlatform = "" //	Long	固件平台：0-nordic，1-Realtek，2-dialog
    var crc = "" //	1:CRC加密 0：不加密
    var encryptionArray = "" //	String	CRC加密串
    var homeLogo = "" //	String	首页LOGO
    var productLogo = "" //Byte	蓝牙搜索列表LOGO
    var screenWidth = "" //	String	屏幕宽度
    var screenHeight = "" //	Integer	屏幕高度
    var shape = "" //	String	0:方形 1：球拍 2：圆形 3：圆角矩形
    var dialRule = "" //	String	表盘生成规则（01：规则1， 02：规则2，03：规则3，04:规则4）
    var gpsType = "" // String 坐标系   01：火星坐标系  02：WGS-84坐标系
    var packSendTimeIntervalIos = 0 // iOS打包发送间隔，0:未配置，大于0生效，单位（ms）
    var packSendTimeIntervalAndroid = 0 //Android打包发送间隔，0:未配置，大于0生效，单位（ms）

    var screenList: List<ScreenDisplayBean>? = null//--	息屏显示列表
    var guidePageData = "" // 引导页配置

    class ScreenDisplayBean {
        var key = "" //String	1:指针表盘 2：数字表盘 ..
        var url = "" //String	表盘URL

        override fun toString(): String {
            return "ScreenDisplayBean(key='$key', url='$url')"
        }

    }


    var mtuList: List<MtuListBean>? = null//--	MTU系统参数设置列表

    class MtuListBean {
        var phoneSystem = "" //	int	1：iOS  2:Android
        var phoneModel = "" //		手机型号
        var mtu = "" //		MTU值
        var minValue = "" //		连接参数-最小值
        var maxValue = "" //		连接参数-最大值
        override fun toString(): String {
            return "MtuListBean(phoneSystem='$phoneSystem', phoneModel='$phoneModel', mtu='$mtu', minValue='$minValue', maxValue='$maxValue')"
        }
    }

    override fun toString(): String {
        return "ProductInfoResponse(deviceType='$deviceType', bluetoothName='$bluetoothName', firmwarePlatform='$firmwarePlatform', crc='$crc', homeLogo='$homeLogo', productLogo='$productLogo', screenWidth='$screenWidth', screenHeight='$screenHeight', shape='$shape', dialRule='$dialRule', gpsType='$gpsType', packSendTimeIntervalIos=$packSendTimeIntervalIos, packSendTimeIntervalAndroid=$packSendTimeIntervalAndroid, guidePageData=$guidePageData, screenList=$screenList, mtuList=$mtuList)"
    }


}
package com.jwei.xzfit.https.response

class ProductListResponse {
    lateinit var dataList: List<ProductListResponse.Data>

    class Data {
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
        var supportBt = "" // Integer	1:支持 其他不支持
        var btBleName = "" // String	BT蓝牙名称
        var guidePageData = "" // 引导页配置

        override fun toString(): String {
            return "Data(deviceType='$deviceType', bluetoothName='$bluetoothName', firmwarePlatform='$firmwarePlatform'," +
//                    " crc='$crc', " +
//                    "encryptionArray='$encryptionArray'," +
                    " homeLogo='$homeLogo', productLogo='$productLogo', screenWidth='$screenWidth', screenHeight='$screenHeight'," +
                    " shape='$shape', dialRule='$dialRule',guidePageData='$guidePageData', supportBt='$supportBt, btBleName='$btBleName)"
        }

    }

    override fun toString(): String {
        return "ProductListResponse(dataList=$dataList)"
    }

}
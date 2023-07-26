package com.jwei.xzfit.https.params

class ErrorLogBean {
    var userId = ""    // N	Long	用户id
    var logModule = ""    //Y	String	模块（1、连接 2、绑定  3、OTA  4、表盘 5、AGPS）
    var moduleId = ""    //Y	String	1、蓝牙连接上发现服务识别 2、蓝牙连接上发现服务使能失败 3、绑定信息不匹配 4、网络异常 5、蓝牙断连
    var deviceType = ""    //N	Long	设备号
    var deviceVersion = ""    //N	String	设备版本号
    var appVersion = ""    //Y	String	app版本号
    var appId = ""    //Y	String	appId（01:F Fit 02:3+ PRO 05：InfoWear）
    var phoneModel = ""    //N	String	手机型号
    var phoneSystem = ""    //N	String	手机系统   1：iOS 2：安卓
    var remark = ""    //N	String(46K)	异常日志
    var level = ""    //N	Integer	日志等级
}
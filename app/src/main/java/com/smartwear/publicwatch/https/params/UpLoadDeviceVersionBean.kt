package com.smartwear.publicwatch.https.params

data class UpLoadDeviceVersionBean(
    var userId: String, //	Y	Long	用户ID
    var id: String,//	Y	Long	设备用户绑定ID
    var deviceVersion: String //	Y	String	设备版本号
)
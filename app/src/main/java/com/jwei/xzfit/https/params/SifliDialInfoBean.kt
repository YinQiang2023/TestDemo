package com.jwei.xzfit.https.params

/**
 * Created by Android on 2023/7/24.
 */
class SifliDialInfoBean {
    var dialId = "" //	Y	Long	表盘ID  列表返回dialId
    var userId = "" //	Y	Long	用户ID，特定用户id可以查询未上架的表盘
    var languageCode = "" //	Y	String	语言编码 思澈平台必须上传2023.07.17
    var productNo = "" //	Y	String	设备号   思澈平台必须上传2023.07.17
}
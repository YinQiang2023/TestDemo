package com.jwei.publicone.https.params

class MoreDialPageBean {
    var productNo = "" //	Y	String	设备号
    var productVersion = "" //	Y	Integer	设备版本号
    var userId = "" //	Y	Long	用户id
    var languageCode = "" //	Y	String	语言编码
    var dialTypeId = "" //	Y	Integer	表盘分类id
    var pageIndex = "" //	Y	Integer	查询页数，从1开始，不传默认查询第一页
    var pageSize = "" //	Y	Integer	页大小，建议设置为15，不传默认为20
}
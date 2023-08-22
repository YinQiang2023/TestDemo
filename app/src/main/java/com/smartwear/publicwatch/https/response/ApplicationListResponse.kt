package com.smartwear.publicwatch.https.response

/**
 * Created by Android on 2021/11/17.
 */
class ApplicationListResponse {
    var dataList: List<ApplicationListResponse.Data>? = null

    class Data {
        var protocolId: Int = 0
        var icoUrl: String = ""
        var languageName: String = ""

        override fun toString(): String {
            return "Data(protocolId=$protocolId, icoUrl='$icoUrl', languageName='$languageName')"
        }
    }

    override fun toString(): String {
        return "ApplicationListResponese(dataList=$dataList)"
    }
}
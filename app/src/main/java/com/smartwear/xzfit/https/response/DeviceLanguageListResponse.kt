package com.smartwear.xzfit.https.response

/**
 * Created by Android on 2021/11/4.
 */
class DeviceLanguageListResponse {
    var dataList: List<DeviceLanguageListResponse.Data>? = null

    class Data {
        var languageCode: Int = 0
        var languageName: String = ""
        var chooseLanguageName: String = ""

        override fun toString(): String {
            return "Data(languageCode=$languageCode, languageName='$languageName', chooseLanguageName='$chooseLanguageName')"
        }
    }

    override fun toString(): String {
        return "DeviceLanguageListResponse(dataList=$dataList)"
    }

}
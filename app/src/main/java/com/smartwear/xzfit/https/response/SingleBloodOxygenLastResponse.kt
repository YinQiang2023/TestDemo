package com.smartwear.xzfit.https.response

/**
 * Created by Android on 2023/2/13.
 */
class SingleBloodOxygenLastResponse {
    var id = ""//	Long	数据Id
    var measureTime = ""//	String	测量时间（格式：yyyy-MM-dd HH:mi:ss）
    var measureData = ""//	String	血氧测量值

    override fun toString(): String {
        return "SingleBloodOxygenLastResponse(id='$id', measureTime='$measureTime', measureData='$measureData')"
    }

}
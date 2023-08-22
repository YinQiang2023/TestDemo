package com.smartwear.publicwatch.https.response

/**
 * Created by Android on 2022/10/13.
 */
class SinglePressureLastResponse {
    var id = ""//	Long	数据Id
    var measureTime = ""//	String	测量时间（格式：yyyy-MM-dd HH:mi:ss）
    var measureData = ""//	String	心率测量值

    override fun toString(): String {
        return "SinglePressureLastResponse(id='$id', measureTime='$measureTime', measureData='$measureData')"
    }

}
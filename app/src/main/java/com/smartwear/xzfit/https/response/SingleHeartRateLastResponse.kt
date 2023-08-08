package com.smartwear.xzfit.https.response

class SingleHeartRateLastResponse {
    var id = ""//	Long	数据Id
    var measureTime = ""//	String	测量时间（格式：yyyy-MM-dd HH:mi:ss）
    var measureData = ""//	String	心率测量值

    override fun toString(): String {
        return "SingleHeartRateLastResponse(id='$id', measureTime='$measureTime', measureData='$measureData')"
    }

}
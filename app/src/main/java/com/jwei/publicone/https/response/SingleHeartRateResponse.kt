package com.jwei.publicone.https.response

class SingleHeartRateResponse {
    lateinit var dataList: List<SingleHeartRateResponse.Data>

    class Data {
        var id = ""//	Long	数据Id
        var measureTime = ""//	String	测量时间（格式：yyyy-MM-dd HH:mi:ss）
        var measureData = ""//	String	心率测量值

        override fun toString(): String {
            return "Data(id='$id', measureTime='$measureTime', measureData='$measureData')"
        }

    }

    override fun toString(): String {
        return "SingleHeartRateResponse(dataList=$dataList)"
    }

}
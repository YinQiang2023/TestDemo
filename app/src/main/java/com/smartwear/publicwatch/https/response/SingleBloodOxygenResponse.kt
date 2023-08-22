package com.smartwear.publicwatch.https.response

class SingleBloodOxygenResponse {
    var maxBloodOxygen = "" //		int	日期区间最高血氧饱和度
    var minBloodOxygen = "" //		int	日期区间最低血氧饱和度
    var avgBloodOxygen = "" //		int	日期区间平均血氧饱和度
    lateinit var dataList: List<Data>

    class Data {
        var id = "" //	Long	数据Id
        var measureTime = "" //		String	测量时间（格式：yyyy-MM-dd HH:mi:ss）
        var measureData = "" //		String	心率测量值

        override fun toString(): String {
            return "Data(id='$id', measureTime='$measureTime', measureData='$measureData')"
        }

    }

    override fun toString(): String {
        return "SingleBloodOxygenResponse(maxBloodOxygen='$maxBloodOxygen', minBloodOxygen='$minBloodOxygen', avgBloodOxygen='$avgBloodOxygen', dataList=$dataList)"
    }

}
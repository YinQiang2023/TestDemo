package com.jwei.xzfit.https.response

class SingleBloodOxygenListResponse {
    var maxBloodOxygen = ""//	int	日期区间最高血氧饱和度
    var minBloodOxygen = ""//	int	日期区间最低血氧饱和度
    var avgBloodOxygen = ""//	int	日期区间平均血氧饱和度
    lateinit var dataList: List<Data>

    class Data {
        var date = ""//	String	日期 （格式：yyyy-MM-dd）
        var maxBloodOxygen = ""//	int	日期区间最高血氧饱和度
        var minBloodOxygen = ""//	int	日期区间最低血氧饱和度
        var avgBloodOxygen = ""//	int	日期区间平均血氧饱和度

        override fun toString(): String {
            return "Data(date='$date', maxBloodOxygen='$maxBloodOxygen', minBloodOxygen='$minBloodOxygen', avgBloodOxygen='$avgBloodOxygen')"
        }

    }

    override fun toString(): String {
        return "SingleBloodOxygenListResponse(maxBloodOxygen='$maxBloodOxygen', minBloodOxygen='$minBloodOxygen', avgBloodOxygen='$avgBloodOxygen', dataList=$dataList)"
    }


}
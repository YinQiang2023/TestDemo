package com.jwei.xzfit.https.response

class BloodOxygenListResponse {
    var maxBloodOxygen = ""//	int	日期区间最高血氧饱和度
    var minBloodOxygen = ""//	int	日期区间最低血氧饱和度
    var avgBloodOxygen = ""//	int	日期区间平均血氧饱和度
    lateinit var dataList: List<Data>

    class Data {
        var id = ""//	Long	数据Id
        var date = ""//	String	心率监测日期（格式：yyyy-MM-dd HH:mi:ss）
        var maxBloodOxygen = ""//	int	当日最高血氧饱和度
        var minBloodOxygen = ""//	int	当日最低血氧饱和度

        override fun toString(): String {
            return "Data(id='$id', date='$date', maxBloodOxygen='$maxBloodOxygen', minBloodOxygen='$minBloodOxygen')"
        }

    }

    override fun toString(): String {
        return "BloodOxygenListResponse(maxBloodOxygen='$maxBloodOxygen', minBloodOxygen='$minBloodOxygen', avgBloodOxygen='$avgBloodOxygen', dataList=$dataList)"
    }

}
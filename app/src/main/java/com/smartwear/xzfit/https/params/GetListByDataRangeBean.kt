package com.smartwear.xzfit.https.params

data class GetListByDataRangeBean(
    var userId: String,
    var beginDate: String,
    var endDate: String,
    var dataType: Int,
)

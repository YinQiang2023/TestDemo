package com.jwei.publicone.https.params

data class GetListByDataRangeBean(
    var userId: String,
    var beginDate: String,
    var endDate: String,
    var dataType: Int,
)

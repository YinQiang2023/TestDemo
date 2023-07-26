package com.jwei.xzfit.https.params

/**
 * Created by Android on 2021/11/2.
 */
data class QueryExerciseListParam(
    var userId: String = "",     //用户ID
    var sportDate: String = "",   //yyyy-MM-dd
    var pageIndex: String = "",   //查询页数，从1开始，不传默认查询第一页
    var pageSize: String = "",    //页大小，建议设置为15，不传默认为20
    var timeZoneMinutes: String = "" //（UTC）比0时区相差分钟（-720：720之间）
)
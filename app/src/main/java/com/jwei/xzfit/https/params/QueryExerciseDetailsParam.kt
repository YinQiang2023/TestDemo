package com.jwei.xzfit.https.params

/**
 * Created by Android on 2021/11/3.
 * 获取运动详情
 */
data class QueryExerciseDetailsParam(
    var userId: String = "",     //用户ID
    var sportId: Long = 0L       //运动记录id
)

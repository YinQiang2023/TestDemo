package com.smartwear.publicwatch.https.response

import com.smartwear.publicwatch.db.model.sport.*

/**
 * Created by Android on 2021/11/1.
 * 多运动上传/获取对象
 */
class SportExerciseResponse {
    //上传
    var dataList: MutableList<SportModleInfo>? = null

    //获取
    var list: MutableList<SportModleInfo>? = null

    override fun toString(): String {
        return "SportExerciseResponse(dataList=$dataList, list=$list)"
    }


}
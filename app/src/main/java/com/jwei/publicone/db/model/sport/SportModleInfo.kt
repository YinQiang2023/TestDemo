package com.jwei.publicone.db.model.sport

import com.jwei.publicone.db.model.BaseData
import java.io.Serializable

/**
 * Created by Android on 2021/10/12.
 * 多运动信息总表， litepal  多表需要激进查询
 */
class SportModleInfo : BaseData(), Serializable, Cloneable {
    //DB id
    var id: Long = 0L

    //运动记录ID  上传服务器后查询时赋值
    var sportId: Long = 0L

    //用户ID
    var userId: Long = 0L

    //开始时间(时间戳 秒级)
    var sportTime: Long = 0L

    //结束时间(时间戳)
    var sportEndTime: Long = 0L

    //运动类型
    var exerciseType = ""

    //运动时长(精确到秒)
    var sportDuration: Int = 0

    //燃烧卡路里
    var burnCalories: Int = 0

    //数据类型（0:APP运动 1：副屏辅助运动 2：设备运动）
    var dataSources: Int = 0

    //deviceType dataSources=0,数据不传

    var exerciseApp: ExerciseApp? = null
    var exerciseAuxiliary: ExerciseAuxiliary? = null
    var exerciseIndoor: ExerciseIndoor? = null
    var exerciseOutdoor: ExerciseOutdoor? = null
    var exerciseSwimming: ExerciseSwimming? = null

    override fun toString(): String {
        return "SportModleInfo(id=$id, sportId=$sportId, userId=$userId, sportTime=$sportTime, sportEndTime=$sportEndTime, exerciseType='$exerciseType', sportDuration=$sportDuration, burnCalories=$burnCalories, dataSources=$dataSources, exerciseApp=$exerciseApp, exerciseAuxiliary=$exerciseAuxiliary, exerciseIndoor=$exerciseIndoor, exerciseOutdoor=$exerciseOutdoor, exerciseSwimming=$exerciseSwimming)"
    }

    public override fun clone(): SportModleInfo {
        val info = super.clone() as SportModleInfo
        info.exerciseApp = exerciseApp?.clone()
        info.exerciseAuxiliary = exerciseAuxiliary?.clone()
        info.exerciseIndoor = exerciseIndoor?.clone()
        info.exerciseOutdoor = exerciseOutdoor?.clone()
        info.exerciseSwimming = exerciseSwimming?.clone()
        return info
    }
}
package com.jwei.publicone.db.model.sport

import org.litepal.crud.LitePalSupport

/**
 * Created by Android on 2021/11/2.
 * APP副屏运动
 */
class ExerciseAuxiliary : LitePalSupport(), Cloneable {
    //运动总里程（米）
    var sportsMileage: String = ""

    //最高配速
    var maxPace: String = ""

    //最低配速
    var minPace: String = ""

    //平均配速
    var avgPace: String = ""

    //最高速度
    var maxSpeed: String = ""

    //最低速度
    var minSpeed: String = ""

    //平均速度
    var avgSpeed: String = ""

    //地图坐标点数据(经纬度数据)
    var mapData: String = ""

    //速度数据
    var paceDatas: String = ""

    //配速数据
    var speedDatas: String = ""

    //总步数
    var totalSteps: Int = 0

    //最大步频
    var maxCadence: Int = 0

    //最高步频
    var topCadence: Int = 0

    //最低步频
    var minCadence: Int = 0

    //最高心率
    var maxHearRate: Int = 0

    //最低心率
    var minHearRate: Int = 0

    //平均心率
    var avgHearRate: Int = 0

    //心率区间1
    var heartRateZone1: String = ""

    //心率区间2
    var heartRateZone2: String = ""

    //心率区间3
    var heartRateZone3: String = ""

    //心率区间4
    var heartRateZone4: String = ""

    //步数数据
    var stepDatas: String = ""

    //心率数据
    var hearRateDatas: String = ""

    override fun toString(): String {
        return "ExerciseAuxiliary(sportsMileage='$sportsMileage', maxPace='$maxPace', minPace='$minPace', avgPace='$avgPace', maxSpeed='$maxSpeed', minSpeed='$minSpeed', avgSpeed='$avgSpeed', mapData='$mapData', paceDatas='$paceDatas', speedDatas='$speedDatas', totalSteps=$totalSteps, maxCadence=$maxCadence, topCadence=$topCadence, minCadence=$minCadence, maxHearRate=$maxHearRate, minHearRate=$minHearRate, avgHearRate=$avgHearRate, heartRateZone1='$heartRateZone1', heartRateZone2='$heartRateZone2', heartRateZone3='$heartRateZone3', heartRateZone4='$heartRateZone4', stepDatas='$stepDatas', hearRateDatas='$hearRateDatas')"
    }

    public override fun clone(): ExerciseAuxiliary {
        return super.clone() as ExerciseAuxiliary
    }
}
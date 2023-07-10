package com.jwei.publicone.db.model.sport

import org.litepal.crud.LitePalSupport

/**
 * Created by Android on 2021/11/2.
 * APP运动数据对象
 */
class ExerciseApp : LitePalSupport(), Cloneable {
    //运动总里程  double(8,2)
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

    //地图坐标点数据(XY坐标集合)
    var mapData: String = ""

    //配速数据
    var paceDatas: String = ""

    //速度数据
    var speedDatas: String = ""

    //全段速度
    var fullSpeedDatas: String = ""


    override fun toString(): String {
        return "ExerciseApp(sportsMileage='$sportsMileage', maxPace='$maxPace', minPace='$minPace', avgPace='$avgPace', maxSpeed='$maxSpeed', minSpeed='$minSpeed', avgSpeed='$avgSpeed', mapData='$mapData', paceDatas='$paceDatas', speedDatas='$speedDatas',fullSpeedDatas='$fullSpeedDatas')"
    }

    public override fun clone(): ExerciseApp {
        return super.clone() as ExerciseApp
    }
}
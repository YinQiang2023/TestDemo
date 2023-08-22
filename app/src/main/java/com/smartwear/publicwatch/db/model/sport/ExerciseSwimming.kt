package com.smartwear.publicwatch.db.model.sport

import org.litepal.crud.LitePalSupport

/**
 * Created by Android on 2021/11/2.
 * 设备游泳数据
 */
class ExerciseSwimming : LitePalSupport(), Cloneable {

    //原始数据的id
    var recordPointDataId: String = ""

    //协议版本号
    var recordPointVersion: String = ""

    //类型描述
    var recordPointTypeDescription: String = ""

    //打点运动类型
    var recordPointSportType: String = ""

    //打点-压缩与加密
    var recordPointEncryption: String = ""

    //打点数据有效性1
    var recordPointDataValid1: String = ""

    //打点数据有效性2
    var recordPointDataValid2: String = ""

    //报告-压缩与加密
    var reportEncryption: String = ""

    //报告数据有效性1
    var reportDataValid1: String = ""

    //报告数据有效性2
    var reportDataValid2: String = ""

    //报告数据有效性3
    var reportDataValid3: String = ""

    //报告数据有效性4
    var reportDataValid4: String = ""

    //总里程（米）
    var reportDistance: String = ""

    //最快配速
    var reportFastPace: String = ""

    //最慢配速
    var reportSlowestPace: String = ""

    //最快速度（2位小数点）
    var reportFastSpeed: String = ""

    //训练效果
    var reportTrainingEffect: String = ""

    //最大摄氧量
    var reportMaxOxygenIntake: String = ""

    //身体能量消耗
    var reportEnergyConsumption: String = ""

    //预计恢复时间
    var reportRecoveryTime: String = ""

    //GPS-数据有效性1
    var gpsDataValid1: String = ""

    //GPS-压缩与加密
    var gpsEncryption: String = ""

    //打点16进制数据（大数据2M）
    var recordPointSportData: String = ""

    //设备-GPS数据
    var gpsMapDatas: String = ""

    //GPS-时间戳数据字符串(时间戳1,时间戳2,时间戳3)
    var gpsUnixDatas: String = ""

    //设备Mac
    var deviceMac: String = ""

    //总划水次数(次)
    var numberOfSwims: String = ""

    //主泳姿="" 0）混合泳；1）蛙泳；2）自由泳；3）仰泳；4）蝶泳
    var description: String = ""

    //最大划频(次/分钟)
    var maximumStrokeFrequency: String = ""

    //转身次数 (次)
    var numberOfTurns: String = ""

    //平均swolf(游泳效率的意思)
    var averageSwolf: String = ""

    //最佳swolf(游泳效率的意思)
    var bestSwolf: String = ""

    //泳池宽度(米)
    var poolWidth: String = ""

    override fun toString(): String {
        return "ExerciseSwimming(recordPointDataId='$recordPointDataId', recordPointVersion='$recordPointVersion', recordPointTypeDescription='$recordPointTypeDescription', recordPointSportType='$recordPointSportType', recordPointEncryption='$recordPointEncryption', recordPointDataValid1='$recordPointDataValid1', recordPointDataValid2='$recordPointDataValid2', reportEncryption='$reportEncryption', reportDataValid1='$reportDataValid1', reportDataValid2='$reportDataValid2', reportDataValid3='$reportDataValid3', reportDataValid4='$reportDataValid4', reportDistance='$reportDistance', reportFastPace='$reportFastPace', reportSlowestPace='$reportSlowestPace', reportFastSpeed='$reportFastSpeed', reportTrainingEffect='$reportTrainingEffect', reportMaxOxygenIntake='$reportMaxOxygenIntake', reportEnergyConsumption='$reportEnergyConsumption', reportRecoveryTime='$reportRecoveryTime', gpsDataValid1='$gpsDataValid1', gpsEncryption='$gpsEncryption', recordPointSportData='$recordPointSportData', gpsMapDatas='$gpsMapDatas', gpsUnixDatas='$gpsUnixDatas', deviceMac='$deviceMac', numberOfSwims='$numberOfSwims', description='$description', maximumStrokeFrequency='$maximumStrokeFrequency', numberOfTurns='$numberOfTurns', averageSwolf='$averageSwolf', bestSwolf='$bestSwolf', poolWidth='$poolWidth')"
    }

    public override fun clone(): ExerciseSwimming {
        return super.clone() as ExerciseSwimming
    }
}
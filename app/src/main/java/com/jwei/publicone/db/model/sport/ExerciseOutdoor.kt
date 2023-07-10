package com.jwei.publicone.db.model.sport

import org.litepal.crud.LitePalSupport

/**
 * Created by Android on 2021/11/2.
 * 设备户外运动带GPS
 */
class ExerciseOutdoor : LitePalSupport(), Cloneable {

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

    //总步数
    var reportTotalStep: String = ""

    //最大步频
    var reportMaxStepSpeed: String = ""

    //平均心率
    var reportAvgHeart: String = ""

    //最大心率
    var reportMaxHeart: String = ""

    //最小心率
    var reportMinHeart: String = ""

    //累计上升
    var reportCumulativeRise: String = ""

    //累计下降
    var reportCumulativeDecline: String = ""

    //平均高度
    var reportAvgHeight: String = ""

    //最大高度
    var reportMaxHeight: String = ""

    //最小高度
    var reportMinHeight: String = ""

    //训练效果
    var reportTrainingEffect: String = ""

    //最大摄氧量
    var reportMaxOxygenIntake: String = ""

    //身体能量消耗
    var reportEnergyConsumption: String = ""

    //预计恢复时间
    var reportRecoveryTime: String = ""

    //心率-极限时长
    var reportHeartLimitTime: String = ""

    //心率-无氧耐力时长
    var reportHeartAnaerobic: String = ""

    //心率-有氧耐力时长
    var reportHeartAerobic: String = ""

    //心率-燃脂时长
    var reportHeartFatBurning: String = ""

    //心率-热身时长
    var reportHeartWarmUp: String = ""

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

    // 坐标系
    var gpsType: String = "02"

    //设备Mac
    //deviceMac
    var deviceMac: String = ""

    override fun toString(): String {
        return "ExerciseOutdoor(recordPointDataId='$recordPointDataId', recordPointVersion='$recordPointVersion', recordPointTypeDescription='$recordPointTypeDescription', recordPointSportType='$recordPointSportType', recordPointEncryption='$recordPointEncryption', recordPointDataValid1='$recordPointDataValid1', recordPointDataValid2='$recordPointDataValid2', reportEncryption='$reportEncryption', reportDataValid1='$reportDataValid1', reportDataValid2='$reportDataValid2', reportDataValid3='$reportDataValid3', reportDataValid4='$reportDataValid4', reportDistance='$reportDistance', reportFastPace='$reportFastPace', reportSlowestPace='$reportSlowestPace', reportFastSpeed='$reportFastSpeed', reportTotalStep='$reportTotalStep', reportMaxStepSpeed='$reportMaxStepSpeed', reportAvgHeart='$reportAvgHeart', reportMaxHeart='$reportMaxHeart', reportMinHeart='$reportMinHeart', reportCumulativeRise='$reportCumulativeRise', reportCumulativeDecline='$reportCumulativeDecline', reportAvgHeight='$reportAvgHeight', reportMaxHeight='$reportMaxHeight', reportMinHeight='$reportMinHeight', reportTrainingEffect='$reportTrainingEffect', reportMaxOxygenIntake='$reportMaxOxygenIntake', reportEnergyConsumption='$reportEnergyConsumption', reportRecoveryTime='$reportRecoveryTime', reportHeartLimitTime='$reportHeartLimitTime', reportHeartAnaerobic='$reportHeartAnaerobic', reportHeartAerobic='$reportHeartAerobic', reportHeartFatBurning='$reportHeartFatBurning', reportHeartWarmUp='$reportHeartWarmUp', gpsDataValid1='$gpsDataValid1', gpsEncryption='$gpsEncryption', recordPointSportData='$recordPointSportData', gpsMapDatas='$gpsMapDatas', gpsUnixDatas='$gpsUnixDatas', deviceMac='$deviceMac')"
    }

    public override fun clone(): ExerciseOutdoor {
        return super.clone() as ExerciseOutdoor
    }
}
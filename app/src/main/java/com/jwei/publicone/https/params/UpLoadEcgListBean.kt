package com.jwei.publicone.https.params

import com.jwei.publicone.R
import com.jwei.publicone.base.BaseApplication
import com.jwei.publicone.db.model.Ecg
import java.util.ArrayList

class UpLoadEcgListBean {
    var dataList: MutableList<UpLoadEcgListBean.Data> = ArrayList()


    class Data {
        var userId = ""             //	Y	Long	用户id
        var heart = ""              //	Y	Integer	心率
        var systolic = ""           //	Y	Integer	收缩压
        var diastolic = ""          //	Y	Integer	舒张压
        var hrvResult = ""          //	Y	Integer	测量结果（HRV）
        var healthIndex = ""        //	Y	Integer	健康指数
        var fatigueIndex = ""       //	Y	Integer	疲劳指数
        var bodyLoad = ""           //	Y	Integer	身心负荷
        var bodyQuality = ""        //	Y	Integer	身体素质
        var cardiacFunction = ""    //	Y	Integer	心脏功能
        var healthMeasuringTime = ""//	Y	String	测量时间（ECG/PPG）：yyyy-MM-dd hh:mi:ss
        var appName = ""            //	Y	String	App名称
        var appVersion = ""         //	Y	String	App版本
        var deviceSensorType = ""   //	N	Integer	设备传感类型   最长为2，数值
        var bpStatus = ""           //	N	Integer	是否支持血压  0：支持 1：不支持 默认为0
        var deviceMac = ""          //	N	String	手环设备mac地址
        var deviceType = ""         //	Y	Long	设备号
        var deviceVersion = ""      //	Y	String	设备版本
        var ecgData = ""            //	N	String	ECG原始数据
        var initiator = ""          //	Y	Integer	发起者  0：离线 1：APP发起
        var deviceUnixTime = ""     //	N	String（15）	手环数据上传到App端App时间  传unix时间戳

    }

    object Utils {
        fun getData(userId: String, ecg: Ecg): Data {
            val bean = Data()
            bean.userId = userId
            bean.heart = ecg.heart
            bean.systolic = ecg.systolic
            bean.diastolic = ecg.diastolic
            bean.hrvResult = ecg.hrvResult
            bean.healthIndex = ecg.healthIndex
            bean.fatigueIndex = ecg.fatigueIndex
            bean.bodyLoad = ecg.bodyLoad
            bean.bodyQuality = ecg.bodyQuality
            bean.cardiacFunction = ecg.cardiacFunction
            bean.healthMeasuringTime = ecg.healthMeasuringTime
            bean.appName = BaseApplication.mContext.getString(R.string.main_app_name)
            bean.appVersion = ecg.appVersion
            bean.deviceSensorType = ecg.deviceSensorType
            bean.bpStatus = ecg.bpStatus
            bean.deviceMac = ecg.deviceMac
            bean.deviceType = ecg.deviceType
            bean.deviceVersion = ecg.deviceVersion
            bean.ecgData = ecg.ecgData
            bean.initiator = ecg.initiator
            bean.deviceUnixTime = ecg.createDateTime.trim()
            return bean
        }
    }


}

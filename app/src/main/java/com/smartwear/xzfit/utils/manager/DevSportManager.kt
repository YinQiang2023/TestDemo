package com.smartwear.xzfit.utils.manager

import android.bluetooth.BluetoothAdapter
import android.location.Location
import com.alibaba.fastjson.JSON
import com.amap.api.location.AMapLocation
import com.amap.api.maps.model.LatLng
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.PermissionUtils
import com.zhapp.ble.BleCommonAttributes
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.*
import com.zhapp.ble.bean.DevSportInfoBean.DeviceSportSwimEntity
import com.zhapp.ble.bean.DevSportInfoBean.RecordPointSportData
import com.zhapp.ble.callback.CallBackUtils
import com.zhapp.ble.callback.SportCallBack
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.SendCmdState
import com.zhapp.ble.parsing.SportParsing
import com.smartwear.xzfit.base.BaseApplication
import com.smartwear.xzfit.db.model.sport.ExerciseIndoor
import com.smartwear.xzfit.db.model.sport.ExerciseOutdoor
import com.smartwear.xzfit.db.model.sport.ExerciseSwimming
import com.smartwear.xzfit.db.model.sport.SportModleInfo
import com.smartwear.xzfit.db.model.track.TrackingLog
import com.smartwear.xzfit.service.LocationService
import com.smartwear.xzfit.ui.data.Global
import com.smartwear.xzfit.ui.device.bean.DeviceSettingBean
import com.smartwear.xzfit.ui.eventbus.EventAction
import com.smartwear.xzfit.ui.eventbus.EventMessage
import com.smartwear.xzfit.utils.AppUtils
import com.smartwear.xzfit.utils.GpsCoordinateUtils
import com.smartwear.xzfit.utils.SendCmdUtils
import com.smartwear.xzfit.utils.SpUtils
import com.smartwear.xzfit.utils.TimeUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import kotlin.collections.ArrayList

/**
 * Created by Android on 2021/11/10.
 * 设备运动管理类
 */
object DevSportManager {
    //产品功能列表
    private var deviceSettingBean: DeviceSettingBean? = null

    /**
     * 设备运动相关回调
     * */
    fun initDevSportCallBack() {
        CallBackUtils.sportCallBack = object : SportCallBack {
            override fun onDevSportInfo(data: DevSportInfoBean?) {
                data?.let {
                    saveSportInfo(it)
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_SPORT, TrackingLog.getAppTypeTrack("解析所有运动数据并保存").apply {
                        log = "运动数据总时长 -->${it.toSimpleString()}"
                    })
                }
            }

            override fun onSportStatus(statusBean: SportStatusBean?) {
                statusBean?.let {
                    LogUtils.d("获取设备运动状态 statusBean -->${GsonUtils.toJson(it)}")
                    //{"duration":0,"isAppLaunched":false,"isPaused":false,"isStandalone":false,"selectVersion":0,"sportType":0,"timestamp":0}
                    deviceSportStatus(it)
                }
            }

            override fun onSportRequest(requestBean: SportRequestBean?) {
                requestBean?.let {
                    //非独立运动才会回调
                    LogUtils.d("设备运动变化：--->${GsonUtils.toJson(it)}")
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SPORT, TrackingLog.getStartTypeTrack("辅助运动"), isStart = true)
                    deviceRequest(it)
                    //{"sportType":1,"state":0,"supportVersions":0,"timestamp":1637229490}
                }
            }
        }
        AppUtils.registerEventBus(this)
        deviceSettingBean = JSON.parseObject(
            SpUtils.getValue(
                SpUtils.DEVICE_SETTING,
                ""
            ), DeviceSettingBean::class.java
        )
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMsg(event: EventMessage) {
        when (event.action) {
            EventAction.ACTION_REF_DEVICE_SETTING -> {
                deviceSettingBean = JSON.parseObject(
                    SpUtils.getValue(
                        SpUtils.DEVICE_SETTING,
                        ""
                    ), DeviceSettingBean::class.java
                )
            }
        }
    }

    //region 处理运动数据
    /**
     * 收到设备运动数据
     * 储存至本地
     * */
    private fun saveSportInfo(sportInfo: DevSportInfoBean) {
        LogUtils.e("设备运动数据同步----->")
        com.smartwear.xzfit.utils.LogUtils.d("sportInfo", sportInfo.toString())
        //val list = parsingFitness(sportInfo.recordPointSportType.toString(),sportInfo.recordPointVersion.toString(),sportInfo.recordPointSportDataServer)
        //LogUtils.e("解析打点数据----->"+GsonUtils.toJson(list))
        StravaManager.uploads(sportInfo)
        val dbSportModel = SportModleInfo()
        dbSportModel.dataSources = 2
        dbSportModel.userId = SpUtils.getValue(SpUtils.USER_ID, "").toLong()
        dbSportModel.sportTime = sportInfo.reportSportStartTime / 1000
        dbSportModel.sportEndTime = sportInfo.reportSportEndTime / 1000
        dbSportModel.exerciseType = sportInfo.recordPointSportType.toString()
        dbSportModel.sportDuration = sportInfo.reportDuration.toInt()
        dbSportModel.burnCalories = sportInfo.reportCal.toInt()

        dbSportModel.timeStamp = System.currentTimeMillis().toString()
        dbSportModel.createDateTime = TimeUtils.getTime()
        dbSportModel.deviceMac = Global.deviceMac
        dbSportModel.deviceType = Global.deviceType
        dbSportModel.deviceVersion = Global.deviceVersion
        dbSportModel.appVersion = AppUtils.getAppVersionName()
        dbSportModel.date =
            com.blankj.utilcode.util.TimeUtils.millis2String(dbSportModel.sportTime * 1000, TimeUtils.getSafeDateFormat(TimeUtils.DATEFORMAT_COMM))

        val is84GPS = SpUtils.getSPUtilsInstance().getString(SpUtils.THE_SERVER_GPS_TYPE, "02") == "02"
        sportInfo.map_data?.let { mapData ->
            if (mapData.isNotEmpty()) {
                val mLatLngList: MutableList<LatLng> = mutableListOf()
                val mOkLatLngList: MutableList<LatLng> = mutableListOf()
                val pointDataArray: Array<String> = mapData.split(";").toTypedArray()
                //region 地图优化
                for (i in pointDataArray.indices) {
                    val latlng = LatLng(
                        pointDataArray[i].split(",").toTypedArray()[1].toDouble(),
                        pointDataArray[i].split(",").toTypedArray()[0].toDouble()
                    )
                    mLatLngList.add(latlng)
                }

                //修正
                for (location in mLatLngList) {
                    if (!GpsCoordinateUtils.isOutOfChina(location.latitude, location.longitude)) {
                        if (is84GPS) {
                            //84 - 火星
                            val newLatlng = GpsCoordinateUtils.calWGS84toGCJ02(location.latitude, location.longitude)
                            mOkLatLngList.add(LatLng(newLatlng[0], newLatlng[1]))
                        } else {
                            mOkLatLngList.add(LatLng(location.latitude, location.longitude))
                        }
                    } else {
                        if (is84GPS) {
                            mOkLatLngList.add(LatLng(location.latitude, location.longitude))
                        } else {
                            val newLatlng = GpsCoordinateUtils.calGCJ02toWGS84(location.latitude, location.longitude)
                            mOkLatLngList.add(LatLng(newLatlng[0], newLatlng[1]))
                        }
                    }
                }

                var newMapData = StringBuilder()
                for (i in 0 until mOkLatLngList.size) {
                    val okLatlng = mOkLatLngList.get(i)
                    newMapData.append(okLatlng.longitude).append(",").append(okLatlng.latitude)
                    if (i != mOkLatLngList.size - 1) {
                        newMapData.append(";")
                    }
                }
                sportInfo.map_data = newMapData.toString()
                LogUtils.d("设备定位数据修正:\n${sportInfo.map_data}")
            }
        }

        var isSubDbSuccess = false
        if (SportParsing.isData1(dbSportModel.exerciseType.toInt()) || SportParsing.isData3(
                dbSportModel.exerciseType.toInt()
            )
        ) {
            //exerciseOutdoor
            val exerciseOutdoor = ExerciseOutdoor()
            exerciseOutdoor.recordPointDataId = sportInfo.recordPointDataId
            exerciseOutdoor.recordPointVersion = sportInfo.recordPointVersion.toString()
            exerciseOutdoor.recordPointTypeDescription =
                sportInfo.recordPointTypeDescription.toString()
            exerciseOutdoor.recordPointSportType = sportInfo.recordPointSportType.toString()
            exerciseOutdoor.recordPointEncryption = sportInfo.recordPointEncryption.toString()
            exerciseOutdoor.recordPointDataValid1 = sportInfo.recordPointDataValid1.toString()
            exerciseOutdoor.recordPointDataValid2 = sportInfo.recordPointDataValid2.toString()
            exerciseOutdoor.reportEncryption = sportInfo.reportEncryption.toString()
            exerciseOutdoor.reportDataValid1 = sportInfo.reportDataValid1.toString()
            exerciseOutdoor.reportDataValid2 = sportInfo.reportDataValid2.toString()
            exerciseOutdoor.reportDataValid3 = sportInfo.reportDataValid3.toString()
            exerciseOutdoor.reportDataValid4 = sportInfo.reportDataValid4.toString()
            exerciseOutdoor.reportDistance = sportInfo.reportDistance.toString()
            exerciseOutdoor.reportFastPace = sportInfo.reportFastPace.toString()
            exerciseOutdoor.reportSlowestPace = sportInfo.reportSlowestPace.toString()
            exerciseOutdoor.reportFastSpeed = sportInfo.reportFastSpeed.toString()
            exerciseOutdoor.reportTotalStep = sportInfo.reportTotalStep.toString()
            exerciseOutdoor.reportMaxStepSpeed = sportInfo.reportMaxStepSpeed.toString()
            exerciseOutdoor.reportAvgHeart = sportInfo.reportAvgHeart.toString()
            exerciseOutdoor.reportMaxHeart = sportInfo.reportMaxHeart.toString()
            exerciseOutdoor.reportMinHeart = sportInfo.reportMinHeart.toString()
            exerciseOutdoor.reportCumulativeRise = sportInfo.reportCumulativeRise.toString()
            exerciseOutdoor.reportCumulativeDecline = sportInfo.reportCumulativeDecline.toString()
            exerciseOutdoor.reportAvgHeight = sportInfo.reportAvgHeight.toString()
            exerciseOutdoor.reportMaxHeight = sportInfo.reportMaxHeight.toString()
            exerciseOutdoor.reportMinHeight = sportInfo.reportMinHeight.toString()
            exerciseOutdoor.reportTrainingEffect = sportInfo.reportTrainingEffect.toString()
            exerciseOutdoor.reportMaxOxygenIntake = sportInfo.reportMaxOxygenIntake.toString()
            exerciseOutdoor.reportEnergyConsumption = sportInfo.reportEnergyConsumption.toString()
            exerciseOutdoor.reportRecoveryTime = sportInfo.reportRecoveryTime.toString()
            exerciseOutdoor.reportHeartLimitTime = sportInfo.reportHeartLimitTime.toString()
            exerciseOutdoor.reportHeartAnaerobic = sportInfo.reportHeartAnaerobic.toString()
            exerciseOutdoor.reportHeartAerobic = sportInfo.reportHeartAerobic.toString()
            exerciseOutdoor.reportHeartFatBurning = sportInfo.reportHeartFatBurning.toString()
            exerciseOutdoor.reportHeartWarmUp = sportInfo.reportHeartWarmUp.toString()
            exerciseOutdoor.gpsDataValid1 = sportInfo.reportGpsValid1.toString()
            exerciseOutdoor.gpsEncryption = sportInfo.reportGpsEncryption.toString()
            if (!sportInfo.recordPointSportDataServer.isNullOrEmpty()) {
                exerciseOutdoor.recordPointSportData = sportInfo.recordPointSportDataServer
            }
            sportInfo.map_data?.let { exerciseOutdoor.gpsMapDatas = it }
            sportInfo.recordGpsTime?.let { exerciseOutdoor.gpsUnixDatas = it }
            exerciseOutdoor.gpsType = SpUtils.getSPUtilsInstance().getString(SpUtils.THE_SERVER_GPS_TYPE, "02")
            exerciseOutdoor.deviceMac = Global.deviceMac
            //存数据库
            dbSportModel.exerciseOutdoor = exerciseOutdoor
            isSubDbSuccess = dbSportModel.exerciseOutdoor!!.save() //子表
        } else if (SportParsing.isData2(dbSportModel.exerciseType.toInt()) || SportParsing.isData4(
                dbSportModel.exerciseType.toInt()
            )
        ) {
            //exerciseIndoor
            val exerciseIndoor = ExerciseIndoor()
            exerciseIndoor.recordPointDataId = sportInfo.recordPointDataId
            exerciseIndoor.recordPointVersion = sportInfo.recordPointVersion.toString()
            exerciseIndoor.recordPointTypeDescription =
                sportInfo.recordPointTypeDescription.toString()
            exerciseIndoor.recordPointSportType = sportInfo.recordPointSportType.toString()
            exerciseIndoor.recordPointEncryption = sportInfo.recordPointEncryption.toString()
            exerciseIndoor.recordPointDataValid1 = sportInfo.recordPointDataValid1.toString()
            exerciseIndoor.recordPointDataValid2 = sportInfo.recordPointDataValid2.toString()
            exerciseIndoor.reportEncryption = sportInfo.reportEncryption.toString()
            exerciseIndoor.reportDataValid1 = sportInfo.reportDataValid1.toString()
            exerciseIndoor.reportDataValid2 = sportInfo.reportDataValid2.toString()
            exerciseIndoor.reportDataValid3 = sportInfo.reportDataValid3.toString()
            exerciseIndoor.reportDataValid4 = sportInfo.reportDataValid4.toString()
            exerciseIndoor.reportDistance = sportInfo.reportDistance.toString()
            exerciseIndoor.reportFastPace = sportInfo.reportFastPace.toString()
            exerciseIndoor.reportSlowestPace = sportInfo.reportSlowestPace.toString()
            exerciseIndoor.reportFastSpeed = sportInfo.reportFastSpeed.toString()
            exerciseIndoor.reportTotalStep = sportInfo.reportTotalStep.toString()
            exerciseIndoor.reportMaxStepSpeed = sportInfo.reportMaxStepSpeed.toString()
            exerciseIndoor.reportAvgHeart = sportInfo.reportAvgHeart.toString()
            exerciseIndoor.reportMaxHeart = sportInfo.reportMaxHeart.toString()
            exerciseIndoor.reportMinHeart = sportInfo.reportMinHeart.toString()
            exerciseIndoor.reportCumulativeRise = sportInfo.reportCumulativeRise.toString()
            exerciseIndoor.reportCumulativeDecline = sportInfo.reportCumulativeDecline.toString()
            exerciseIndoor.reportAvgHeight = sportInfo.reportAvgHeight.toString()
            exerciseIndoor.reportMaxHeight = sportInfo.reportMaxHeight.toString()
            exerciseIndoor.reportMinHeight = sportInfo.reportMinHeight.toString()
            exerciseIndoor.reportTrainingEffect = sportInfo.reportTrainingEffect.toString()
            exerciseIndoor.reportMaxOxygenIntake = sportInfo.reportMaxOxygenIntake.toString()
            exerciseIndoor.reportEnergyConsumption = sportInfo.reportEnergyConsumption.toString()
            exerciseIndoor.reportRecoveryTime = sportInfo.reportRecoveryTime.toString()
            exerciseIndoor.reportHeartLimitTime = sportInfo.reportHeartLimitTime.toString()
            exerciseIndoor.reportHeartAnaerobic = sportInfo.reportHeartAnaerobic.toString()
            exerciseIndoor.reportHeartAerobic = sportInfo.reportHeartAerobic.toString()
            exerciseIndoor.reportHeartFatBurning = sportInfo.reportHeartFatBurning.toString()
            exerciseIndoor.reportHeartWarmUp = sportInfo.reportHeartWarmUp.toString()
            if (!sportInfo.recordPointSportDataServer.isNullOrEmpty()) {
                exerciseIndoor.recordPointSportData = sportInfo.recordPointSportDataServer
            }
            exerciseIndoor.deviceMac = Global.deviceMac
            //存数据库
            dbSportModel.exerciseIndoor = exerciseIndoor
            isSubDbSuccess = dbSportModel.exerciseIndoor!!.save() //子表
        } else {
            //exerciseSwimming
            val exerciseSwimming = ExerciseSwimming()
            exerciseSwimming.recordPointDataId = sportInfo.recordPointDataId
            exerciseSwimming.recordPointVersion = sportInfo.recordPointVersion.toString()
            exerciseSwimming.recordPointTypeDescription =
                sportInfo.recordPointTypeDescription.toString()
            exerciseSwimming.recordPointSportType = sportInfo.recordPointSportType.toString()
            exerciseSwimming.recordPointEncryption = sportInfo.recordPointEncryption.toString()
            exerciseSwimming.recordPointDataValid1 = sportInfo.recordPointDataValid1.toString()
            exerciseSwimming.recordPointDataValid2 = sportInfo.recordPointDataValid2.toString()
            exerciseSwimming.reportEncryption = sportInfo.reportEncryption.toString()
            exerciseSwimming.reportDataValid1 = sportInfo.reportDataValid1.toString()
            exerciseSwimming.reportDataValid2 = sportInfo.reportDataValid2.toString()
            exerciseSwimming.reportDataValid3 = sportInfo.reportDataValid3.toString()
            exerciseSwimming.reportDataValid4 = sportInfo.reportDataValid4.toString()
            exerciseSwimming.reportDistance = sportInfo.reportDistance.toString()
            exerciseSwimming.reportFastPace = sportInfo.reportFastPace.toString()
            exerciseSwimming.reportSlowestPace = sportInfo.reportSlowestPace.toString()
            exerciseSwimming.reportFastSpeed = sportInfo.reportFastSpeed.toString()
            exerciseSwimming.reportTrainingEffect = sportInfo.reportTrainingEffect.toString()
            exerciseSwimming.reportMaxOxygenIntake = sportInfo.reportMaxOxygenIntake.toString()
            exerciseSwimming.reportEnergyConsumption = sportInfo.reportEnergyConsumption.toString()
            exerciseSwimming.reportRecoveryTime = sportInfo.reportRecoveryTime.toString()
            exerciseSwimming.gpsDataValid1 = sportInfo.reportGpsValid1.toString()
            exerciseSwimming.gpsEncryption = sportInfo.reportGpsEncryption.toString()
            if (!sportInfo.recordPointSportDataServer.isNullOrEmpty()) {
                exerciseSwimming.recordPointSportData = sportInfo.recordPointSportDataServer
            }
            sportInfo.map_data?.let { exerciseSwimming.gpsMapDatas = it }
            sportInfo.recordGpsTime?.let { exerciseSwimming.gpsUnixDatas = it }
            exerciseSwimming.numberOfSwims = sportInfo.reportTotalSwimNum.toString()
            exerciseSwimming.description = sportInfo.reportSwimStyle.toString()
            exerciseSwimming.maximumStrokeFrequency = sportInfo.reportMaxSwimFrequency.toString()
            exerciseSwimming.numberOfTurns = sportInfo.reportFaceAboutNum.toString()
            exerciseSwimming.averageSwolf = sportInfo.reportAvgSwolf.toString()
            exerciseSwimming.bestSwolf = sportInfo.reportOptimalSwolf.toString()
            exerciseSwimming.poolWidth = sportInfo.reportPoolWidth.toString()
            exerciseSwimming.deviceMac = Global.deviceMac
            //存数据库
            dbSportModel.exerciseSwimming = exerciseSwimming
            isSubDbSuccess = dbSportModel.exerciseSwimming!!.save() //子表
        }
        //存数据库
        val isSuccess = dbSportModel.save() //总表
        LogUtils.w("储存运动数据成功? = $isSubDbSuccess $isSuccess")
        //设备同步数据存本地时刷新首页运动记录
        SendCmdUtils.getSportData()
    }

    /**
     * 解析打点数据
     * */
    fun parsingPointData(
        RecordPointSportType: String?,
        recordPointVersion: String?,
        recordPointSportData: String?
    ): ArrayList<DevSportInfoBean.RecordPointSportData> {
        val recordPointSportDataList = arrayListOf<DevSportInfoBean.RecordPointSportData>()
        try {
            if (!RecordPointSportType.isNullOrEmpty() && !recordPointVersion.isNullOrEmpty() && !recordPointSportData.isNullOrEmpty()) {
                if (recordPointSportData.contains("-")) {
                    val sportData = recordPointSportData.split("-")
                    if (SportParsing.isData1(RecordPointSportType.toInt())) {
                        when (recordPointVersion) {
                            "1" -> {
                                var i = 0
                                while (i < sportData.size) {
                                    val altitude: Long =
                                        (sportData.get(i + 3) + sportData.get(i + 2) + sportData.get(i + 1) + sportData.get(
                                            i
                                        )).toLong(16)
                                    i += 4
                                    val dataNumber: Long =
                                        (sportData.get(i + 3) + sportData.get(i + 2) + sportData.get(i + 1) + sportData.get(
                                            i
                                        )).toLong(16)
                                    i += 4
                                    val time: Long =
                                        (sportData.get(i + 3) + sportData.get(i + 2) + sportData.get(i + 1) + sportData.get(
                                            i
                                        )).toLong(16)
                                    i += 4
                                    for (j in 0 until dataNumber) {
                                        val byte1: Int = sportData.get(i).toInt(16)
                                        val byte2: Int = sportData.get(i + 1).toInt(16)
                                        val byte3: Int = sportData.get(i + 2).toInt(16)
                                        val byte4: Int = sportData.get(i + 3).toInt(16)
                                        val cal = byte1 shr 4
                                        val step = byte1 and 0x0f
                                        val isFullKilometer = byte3 shr 7 == 1
                                        val heightType = byte3 shr 6 and 0x01 //0=下降，1=上升
                                        val height =
                                            (byte3 and 0x1f) / 10.0 // 实际数据精确到0.1，但是打点存储×10后保存为整数
                                        val distance = byte4 / 10.0
                                        i += 4
                                        // one data paring over, insert to db
                                        val recordPointSportDataItem = RecordPointSportData()
                                        if (j == 0L) {
                                            recordPointSportDataItem.altitude = altitude
                                        }
                                        recordPointSportDataItem.time = time + j
                                        recordPointSportDataItem.cal = cal
                                        recordPointSportDataItem.step = step
                                        recordPointSportDataItem.heart = byte2
                                        recordPointSportDataItem.isFullKilometer = isFullKilometer
                                        recordPointSportDataItem.heightType = heightType
                                        recordPointSportDataItem.height = height
                                        recordPointSportDataItem.distance = distance
                                        recordPointSportDataList.add(recordPointSportDataItem)
                                    }
                                }
                            }
                            "2" -> {
                                var i = 0
                                while (i < sportData.size) {
                                    val altitude =
                                        (sportData[i + 3] + sportData[i + 2] + sportData[i + 1] + sportData[i]).toLong(
                                            16
                                        )
                                    i += 4
                                    val dataNumber =
                                        (sportData[i + 3] + sportData[i + 2] + sportData[i + 1] + sportData[i]).toLong(
                                            16
                                        )
                                    i += 4
                                    val time =
                                        (sportData[i + 3] + sportData[i + 2] + sportData[i + 1] + sportData[i]).toLong(
                                            16
                                        )
                                    i += 4
                                    for (j in 0 until dataNumber) {
                                        val byte1 = sportData[i].toInt(16)
                                        val byte2 = sportData[i + 1].toInt(16)
                                        val byte3 = sportData[i + 2].toInt(16)
                                        val byte4 = sportData[i + 4].toInt(16)
                                        val byte5 = sportData[i + 3].toInt(16)
                                        val byte6 = sportData[i + 5].toInt(16)
                                        val byte7 = sportData[i + 6].toInt(16)
                                        val isFullKilometer = byte4 and 0x80 == 0x80
                                        val isMile = byte4 and 0x40 == 0x40
                                        val heightType = byte4 and 0x20 shr 5 and 0x01 //0=下降，1=上升
                                        val height =
                                            (byte4 and 0x1f shl 8 or (byte5 and 0xff)) / 10.0 // 实际数据精确到0.1，但是打点存储×10后保存为整数
                                        val distance =
                                            (byte7 and 0xff shl 8 or (byte6 and 0xff)) / 10.0
                                        i += 7
                                        // one data paring over, insert to db
                                        val recordPointSportDataItem = RecordPointSportData()
                                        if (j == 0L) {
                                            recordPointSportDataItem.altitude = altitude
                                        }
                                        recordPointSportDataItem.time = time + j * 10
                                        recordPointSportDataItem.cal = byte1
                                        recordPointSportDataItem.step = byte2
                                        recordPointSportDataItem.heart = byte3
                                        recordPointSportDataItem.isFullKilometer = isFullKilometer
                                        recordPointSportDataItem.heightType = heightType
                                        recordPointSportDataItem.height = height
                                        recordPointSportDataItem.distance = distance
                                        recordPointSportDataItem.isMile = isMile
                                        recordPointSportDataList.add(recordPointSportDataItem)
                                    }
                                }
                            }
                        }
                    } else if (SportParsing.isData2(RecordPointSportType.toInt())) {
                        when (recordPointVersion) {
                            "1" -> {
                                var i = 0
                                while (i < sportData.size) {
                                    val dataNumber =
                                        (sportData[i + 3] + sportData[i + 2] + sportData[i + 1] + sportData[i]).toLong(
                                            16
                                        )
                                    i += 4
                                    val time =
                                        (sportData[i + 3] + sportData[i + 2] + sportData[i + 1] + sportData[i]).toLong(
                                            16
                                        )
                                    i += 4
                                    for (j in 0 until dataNumber) {
                                        val byte1 = sportData[i].toInt(16)
                                        val byte2 = sportData[i + 1].toInt(16)
                                        val byte3 = sportData[i + 2].toInt(16)
                                        val cal = byte1 shr 4
                                        val step = byte1 and 0x0f
                                        val distance = byte3 / 10.0
                                        i += 3
                                        val recordPointSportDataItem = RecordPointSportData()
                                        recordPointSportDataItem.time = time + j
                                        recordPointSportDataItem.cal = cal
                                        recordPointSportDataItem.step = step
                                        recordPointSportDataItem.heart = byte2
                                        recordPointSportDataItem.distance = distance
                                        recordPointSportDataList.add(recordPointSportDataItem)
                                    }
                                }
                            }
                            "2" -> {
                                var i = 0
                                while (i < sportData.size) {
                                    val dataNumber =
                                        (sportData[i + 3] + sportData[i + 2] + sportData[i + 1] + sportData[i]).toLong(
                                            16
                                        )
                                    i += 4
                                    val time =
                                        (sportData[i + 3] + sportData[i + 2] + sportData[i + 1] + sportData[i]).toLong(
                                            16
                                        )
                                    i += 4
                                    for (j in 0 until dataNumber) {
                                        val byte1 = sportData[i].toInt(16)
                                        val byte2 = sportData[i + 1].toInt(16)
                                        val byte3 = sportData[i + 2].toInt(16)
                                        val byte4 = sportData[i + 3].toInt(16)
                                        val byte5 = sportData[i + 4].toInt(16)
                                        val distance = (byte5 and 0xff shl 8 or (byte4 and 0xff)) / 10.0
                                        i += 5
                                        val recordPointSportDataItem = RecordPointSportData()
                                        recordPointSportDataItem.time = time + j * 10
                                        recordPointSportDataItem.cal = byte1
                                        recordPointSportDataItem.step = byte2
                                        recordPointSportDataItem.heart = byte3
                                        recordPointSportDataItem.distance = distance
                                        recordPointSportDataList.add(recordPointSportDataItem)
                                    }
                                }
                            }
                        }
                    } else if (SportParsing.isData3(RecordPointSportType.toInt())) {
                        when (recordPointVersion) {
                            "1" -> {
                                var i = 0
                                while (i < sportData.size) {
                                    val altitude =
                                        (sportData[i + 3] + sportData[i + 2] + sportData[i + 1] + sportData[i]).toLong(
                                            16
                                        )
                                    i += 4
                                    val dataNumber =
                                        (sportData[i + 3] + sportData[i + 2] + sportData[i + 1] + sportData[i]).toLong(
                                            16
                                        )
                                    i += 4
                                    val time =
                                        (sportData[i + 3] + sportData[i + 2] + sportData[i + 1] + sportData[i]).toLong(
                                            16
                                        )
                                    i += 4
                                    for (j in 0 until dataNumber) {
                                        val byte1 = sportData[i].toInt(16)
                                        val byte2 = sportData[i + 1].toInt(16)
                                        val byte3 = sportData[i + 2].toInt(16)
                                        val isFullKilometer = byte3 shr 7 == 1
                                        val heightType = byte3 shr 6 and 0x01 //0=下降，1=上升
                                        val height =
                                            (byte3 and 0x1f) / 10.0 // 实际数据精确到0.1，但是打点存储×10后保存为整数
                                        i += 3
                                        // one data paring over, insert to db
                                        val recordPointSportDataItem = RecordPointSportData()
                                        if (j == 0L) {
                                            recordPointSportDataItem.altitude = altitude
                                        }
                                        recordPointSportDataItem.time = time + j
                                        recordPointSportDataItem.cal = byte1
                                        recordPointSportDataItem.heart = byte2
                                        recordPointSportDataItem.isFullKilometer = isFullKilometer
                                        recordPointSportDataItem.heightType = heightType
                                        recordPointSportDataItem.height = height
                                        recordPointSportDataList.add(recordPointSportDataItem)
                                    }
                                }
                            }
                            "2" -> {
                                var i = 0
                                while (i < sportData.size) {
                                    val altitude =
                                        (sportData[i + 3] + sportData[i + 2] + sportData[i + 1] + sportData[i]).toLong(
                                            16
                                        )
                                    i += 4
                                    val dataNumber =
                                        (sportData[i + 3] + sportData[i + 2] + sportData[i + 1] + sportData[i]).toLong(
                                            16
                                        )
                                    i += 4
                                    val time =
                                        (sportData[i + 3] + sportData[i + 2] + sportData[i + 1] + sportData[i]).toLong(
                                            16
                                        )
                                    i += 4
                                    for (j in 0 until dataNumber) {
                                        val byte1 = sportData[i].toInt(16)
                                        val byte2 = sportData[i + 1].toInt(16)
                                        val byte3 = sportData[i + 3].toInt(16)
                                        val byte4 = sportData[i + 2].toInt(16)
                                        val isFullKilometer = byte3 and 0x80 == 0x80
                                        val isMile = byte3 and 0x40 == 0x40
                                        val heightType = byte3 and 0x20 shr 5 and 0x01 //0=下降，1=上升
                                        val height =
                                            (byte3 and 0x1f shl 8 or (byte4 and 0xff)) / 10.0 // 实际数据精确到0.1，但是打点存储×10后保存为整数
                                        i += 4
                                        // one data paring over, insert to db
                                        val recordPointSportDataItem = RecordPointSportData()
                                        if (j == 0L) {
                                            recordPointSportDataItem.altitude = altitude
                                        }
                                        recordPointSportDataItem.time = time + j * 10
                                        recordPointSportDataItem.cal = byte1
                                        recordPointSportDataItem.heart = byte2
                                        recordPointSportDataItem.isFullKilometer = isFullKilometer
                                        recordPointSportDataItem.heightType = heightType
                                        recordPointSportDataItem.height = height
                                        recordPointSportDataItem.isMile = isMile
                                        recordPointSportDataList.add(recordPointSportDataItem)
                                    }
                                }
                            }
                        }
                    } else if (SportParsing.isData4(RecordPointSportType.toInt())) {
                        var i = 0
                        while (i < sportData.size) {
                            val dataNumber =
                                (sportData[i + 3] + sportData[i + 2] + sportData[i + 1] + sportData[i]).toLong(
                                    16
                                )
                            i += 4
                            val time =
                                (sportData[i + 3] + sportData[i + 2] + sportData[i + 1] + sportData[i]).toLong(
                                    16
                                )
                            i += 4
                            for (j in 0 until dataNumber) {
                                val byte1 = sportData[i].toInt(16)
                                val byte2 = sportData[i + 1].toInt(16)
                                i += 2
                                val heart = byte1
                                val cal = byte2

                                var deviceSportEntity = DevSportInfoBean.RecordPointSportData()
                                deviceSportEntity.cal = cal
                                deviceSportEntity.heart = heart
                                recordPointSportDataList.add(deviceSportEntity)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return recordPointSportDataList
    }

    /**
     * 解析游泳打点数据
     * */
    fun parsingFitnessNew(
        RecordPointSportType: String?,
        recordPointVersion: String?,
        recordPointSportData: String?
    ): ArrayList<DevSportInfoBean.DeviceSportSwimEntity> {
        val deviceSportList = ArrayList<DevSportInfoBean.DeviceSportSwimEntity>()
        try {
            if (!RecordPointSportType.isNullOrEmpty() && !recordPointVersion.isNullOrEmpty() && !recordPointSportData.isNullOrEmpty()) {
                if (recordPointSportData.contains("-")) {
                    val sportData = recordPointSportData.split("-")
                    if (SportParsing.isData5(RecordPointSportType.toInt())) {
                        var i = 0
                        while (i + 3 < sportData.size) {
                            val dataNumber = (sportData[i + 3] + sportData[i + 2] + sportData[i + 1] + sportData[i]).toLong(16)
                            i += 4
                            val startTime = (sportData[i + 3] + sportData[i + 2] + sportData[i + 1] + sportData[i]).toLong(16)
                            i += 4
                            for (j in 0 until dataNumber) {
                                val swim = DeviceSportSwimEntity()
                                val dataType1 = sportData[i].toInt(16)
                                i += 1
                                swim.dataType = dataType1
                                val endTime = (sportData[i + 3] + sportData[i + 2] + sportData[i + 1] + sportData[i]).toLong(16)
                                i += 4
                                swim.timeStamp = endTime * 1000
                                val swimStyle = sportData[i].toInt(16)
                                i += 1
                                swim.style = swimStyle
                                val pace = (sportData[i + 1] + sportData[i]).toInt(16)
                                i += 2
                                swim.pace = pace
                                val swolf = (sportData[i + 1] + sportData[i]).toInt(16)
                                i += 2
                                swim.swolf = swolf
                                val distance = (sportData[i + 1] + sportData[i]).toInt(16)
                                i += 2
                                swim.distance = distance
                                val cal = (sportData[i + 1] + sportData[i]).toInt(16)
                                i += 2
                                swim.cal = cal
                                val frequency = (sportData[i + 1] + sportData[i]).toInt(16)
                                i += 2
                                swim.frequency = frequency
                                val turns = (sportData[i + 1] + sportData[i]).toInt(16)
                                i += 2
                                swim.turns = turns
                                val swipe = sportData[i].toInt(16)
                                i += 1
                                swim.swipe = swipe
                                val strokes0 = sportData[i].toInt(16)
                                i += 1
                                swim.strokes0 = strokes0
                                val strokes1 = sportData[i].toInt(16)
                                i += 1
                                swim.strokes1 = strokes1
                                val strokes2 = sportData[i].toInt(16)
                                i += 1
                                swim.strokes2 = strokes2
                                val strokes3 = sportData[i].toInt(16)
                                i += 1
                                swim.strokes3 = strokes3
                                val strokes4 = sportData[i].toInt(16)
                                i += 1
                                swim.strokes4 = strokes4
                                deviceSportList.add(swim)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return deviceSportList
    }

    /**
     * 计算配速
     * 配速 ?分?秒/公里
     * */
    fun calculateMinkm(cal: Int): String {
        if (cal < 1) {
            return "00'00\""
        }
        val resultF = (cal * (if (AppUtils.getDeviceUnit() == 0) 1f else 1.61f)).toInt() / 60
        val resultM = (cal * (if (AppUtils.getDeviceUnit() == 0) 1f else 1.61f)).toInt() % 60
        //峰值限制
        if (resultF > 59) {
            return "00'00\""
        }
        val result = StringBuilder()
        if (resultF < 10) {
            result.append("0").append(resultF)
        } else {
            result.append(resultF)
        }
        result.append("'")
        if (resultM < 10) {
            result.append("0").append(resultM)
        } else {
            result.append(resultM)
        }
        result.append("\"")
        return result.toString()
    }

    /**
     * 计算配速
     * 配速 ?分?秒/公里
     * */
    fun calculateMinkm(millis: Long, distance: Float): String {
        if (distance < 1f) {
            return "00'00\""
        }
        val km = distance / 1000f / (if (AppUtils.getDeviceUnit() == 0) 1f else 1.61f)
        val resultF = (millis / 1000f / km).toInt() / 60
        val resultM = (millis / 1000f / km).toInt() % 60
        //峰值限制
        if (resultF > 59) {
            return "00'00\""
        }
        val result = StringBuilder()
        if (resultF < 10) {
            result.append("0").append(resultF)
        } else {
            result.append(resultF)
        }
        result.append("'")
        if (resultM < 10) {
            result.append("0").append(resultM)
        } else {
            result.append(resultM)
        }
        result.append("\"")
        return result.toString()
    }

    /**
     * 计算游泳配速
     *
     * @param second
     * @param distance
     * @return ?分?秒/100米
     */
    fun calculateSwimMinkm(second: Int, distance: Float): String? {
        if (distance < 1f) {
            return "00'00\""
        }
        val km = distance * if (AppUtils.getDeviceUnit() == 0) 1f else 3.2808399f / 100.0f
        val resultF = (second / km).toInt() / 60
        val resultM = (second / km).toInt() % 60
        //峰值限制
        if (resultF > 59) {
            return "00'00\""
        }
        val result = java.lang.StringBuilder()
        if (resultF < 10) {
            result.append("0").append(resultF)
        } else {
            result.append(resultF)
        }
        result.append("'")
        if (resultM < 10) {
            result.append("0").append(resultM)
        } else {
            result.append(resultM)
        }
        result.append("\"")
        return result.toString()
    }

    /**
     * 配速是否为0
     * */
    fun isShow00Pace(minute: Int, second: Int): Boolean {
        val totalSecond = minute * 60 + second
        return totalSecond > 50 * 60 + 58 || totalSecond <= 0
    }

    /**
     * 配速是否为0
     * */
    fun isShow00Pace(totalSecond: Int): Boolean {
        return totalSecond > 50 * 60 + 58 || totalSecond <= 0
    }

    /**
     * 获取设备运动状态
     * */
    fun getDevSportStatus() {
        ControlBleTools.getInstance().getSportStatus(object : ParsingStateManager.SendCmdStateListener(null) {
            override fun onState(state: SendCmdState) {
                LogUtils.d("获取设备运动状态 State -->$state")
            }
        })
    }
    //endregion

    //region 辅助运动

    //设备是否再运动中
    var isDeviceSporting = false

    //设备运动是否暂停
    var isPause = false

    //定位经纬度
    private var mLatitude: Double = 0.0
    private var mLongitude: Double = 0.0

    //上次发送辅助定位数据的时间戳,经纬度
    private var mLastTime = 0L
    private var mLastLat = 0.0
    private var mLastLon = 0.0

    var isSaveNoSportLog = false

    init {
        initEventBus()
    }

    private fun initEventBus() {
        AppUtils.registerEventBus(this)
    }

    /**
     * 是否支持辅助运动
     */
    private fun isSportDevSport(): Boolean {
        if (deviceSettingBean != null && !deviceSettingBean!!.functionRelated.auxiliary_exercise) {
            if (!isSaveNoSportLog) {
                com.smartwear.xzfit.utils.LogUtils.e("DevSport", "设备不支持辅助运动", true)
                //本次进程生命避免重复存入日志
                isSaveNoSportLog = true
            }
            return false
        }
        return true
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public fun onMapLocation(event: EventMessage) {
        if (event.action == EventAction.ACTION_REF_DEVICE_SETTING) {
            //设备产品更新
            deviceSettingBean = JSON.parseObject(
                SpUtils.getValue(
                    SpUtils.DEVICE_SETTING,
                    ""
                ), DeviceSettingBean::class.java
            )
        }
        if (event.action == EventAction.ACTION_DEVICE_CONNECTED) {
            //设备是否支持辅助运动
            if (!isSportDevSport()) {
                return
            }
            LogUtils.d("手表设备连接上 -->" + ControlBleTools.getInstance().isConnect)
            if (ControlBleTools.getInstance().isConnect) {
                //查询设备是否在运动
                getDevSportStatus()
            }
        } else if (event.action == EventAction.ACTION_DEVICE_BLE_STATUS_CHANGE) {
            //设备是否支持辅助运动
            if (!isSportDevSport()) {
                return
            }
            if (event.arg == BleCommonAttributes.STATE_DISCONNECTED) {
                //LogUtils.d("设备断开 --> " + ControlBleTools.getInstance().isConnect)
                //在定位,不是app运动,关闭定位
                if (LocationService.binder?.service != null &&
                    LocationService.binder?.service?.isLocationDoing!! &&
                    !LocationService.binder?.service?.isAppSport!!
                ) {
                    isDeviceSporting = false
                    LocationService.binder?.service?.stopLocation()
                }
            }
        } else if (event.action == EventAction.ACTION_BLE_STATUS_CHANGE) {
            //设备是否支持辅助运动
            if (!isSportDevSport()) {
                return
            }
            //蓝牙关闭
            if (event.arg == BluetoothAdapter.STATE_OFF) {
                //设备运动中，不是app运动，关闭定位
                if (isDeviceSporting) {
                    isDeviceSporting = false

                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SPORT, TrackingLog.getAppTypeTrack("运动过程中蓝牙断连"), "2117", true)

                    if (!LocationService.binder?.service?.isAppSport!!) {
                        LocationService.binder?.service?.stopLocation()
                    }
                }
            }
        } else if (event.action == EventAction.ACTION_LOCATION) {
            //设备是否支持辅助运动
            if (!isSportDevSport()) {
                return
            }
            if (!AppUtils.isEnableGoogleMap()) {
                val location = event.obj as AMapLocation?
                //location.latitude
                location?.latitude?.let {
                    mLatitude = it
                }
                location?.longitude?.let {
                    mLongitude = it
                }
            } else {
                val location = event.obj as Location?
                location?.latitude?.let {
                    mLatitude = it
                }
                location?.longitude?.let {
                    mLongitude = it
                }
            }
            sendPhoneLocationData()
        }
    }

    /**
     * 设备运动状态
     * */
    private fun deviceSportStatus(statusBean: SportStatusBean) {
        //{"duration":0,"isAppLaunched":false,"isPaused":false,"isStandalone":false,"selectVersion":0,"sportType":0,"timestamp":0}
        if (!isSportDevSport()) {
            return
        }
        if (statusBean.isStandalone) {
            return
        }
        //设备在运动 && 不是暂停
        if (statusBean.duration != 0L && statusBean.sportType != 0 && statusBean.timestamp != 0L
            && !statusBean.isPaused
        ) {
            isDeviceSporting = true
            isPause = false
            //未定位，开启定位
            if (!LocationService.binder?.service?.isLocationDoing!!) {
                LocationService.binder?.service?.startLocation()
            }
        } else {
            //设备运动中，不是app运动，关闭定位
            if (isDeviceSporting) {
                isDeviceSporting = false
                if (!LocationService.binder?.service?.isAppSport!!) {
                    LocationService.binder?.service?.stopLocation()
                }
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SPORT, TrackingLog.getEndTypeTrack("辅助运动"), isEnd = true)
            }
        }
    }


    /**
     * 处理设备发送运动请求
     * */
    private fun deviceRequest(devSportRequest: SportRequestBean) {
        //{"sportType":1,"state":0,"supportVersions":0,"timestamp":1637229490}
        //GPS开启定位，进行预定位
        if (devSportRequest.state == 0) {
            // 回复设备SportResponseBean
            sendSportResponseBean()
        }
        if (!isSportDevSport()) {
            return
        }
        when (devSportRequest.state) {
            1 -> { //运动开始
                //未定位，开启定位
                if (!PermissionUtils.isGranted(*com.smartwear.xzfit.utils.PermissionUtils.PERMISSION_GROUP_LOCATION) ||
                    !AppUtils.isGPSOpen(BaseApplication.mContext)
                ) {
                    return
                }
                isDeviceSporting = true
                isPause = false
                if (!LocationService.binder?.service?.isLocationDoing!!) {
                    LocationService.binder?.service?.startLocation()
                }
            }
            2 -> { //运动暂停
                isPause = true
            }
            3 -> { //运动重新开始
                //未定位，开启定位
                if (!PermissionUtils.isGranted(*com.smartwear.xzfit.utils.PermissionUtils.PERMISSION_GROUP_LOCATION) ||
                    !AppUtils.isGPSOpen(BaseApplication.mContext)
                ) {
                    return
                }
                isPause = false
                isDeviceSporting = true
                if (!LocationService.binder?.service?.isLocationDoing!!) {
                    LocationService.binder?.service?.startLocation()
                }
            }
            4 -> { //运动结束
                isDeviceSporting = false
                isPause = false
                //不在app运动，关闭定位
                if (!LocationService.binder?.service?.isAppSport!!) {
                    LocationService.binder?.service?.stopLocation()
                }
                mLastTime = 0L
                mLastLat = 0.0
                mLastLon = 0.0
            }
        }
    }

    /**
     * 回复设备初始运动请求
     * */
    private fun sendSportResponseBean() {
        val response = SportResponseBean()
        response.code = getResponseCode()
        response.gpsAccuracy = getGpsAccuracy()

        val trackingLog = TrackingLog.getDevTyepTrack("设备上报开始运动，app回复运动状态", "APP回复设备请求运动", "SPORT_REQUEST")
        trackingLog.log = GsonUtils.toJson(response)
        ControlBleTools.getInstance().replyDevSportRequest(response, object : ParsingStateManager.SendCmdStateListener(null) {
            override fun onState(state: SendCmdState) {
                LogUtils.d("replyDevSportRequest: $response --> $state")
                trackingLog.endTime = TrackingLog.getNowString()
                trackingLog.devResult = "state : $state"
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SPORT, trackingLog)
            }
        })
        //开始辅助运动
        if (response.code == 0) {
            LocationService.binder?.service?.startLocation()
            isDeviceSporting = true
        }
    }

    private fun getGpsAccuracy(): Int {
        //GPS状态 低 0; 中 1; 高 2; 未知 10;
        var m = LocationService.binder?.service?.getMaxSatellites()
        var v = LocationService.binder?.service?.getValidCount()
        if (m == null || v == null) {
            return 0
        }
        if (m == -1 || m == 0 || v == -1 || v == 0) {
            return 0
        }
        return when (v * 1.0f / m) {
            in 0f..0.33f -> {
                0
            }
            in 0.33f..0.66f -> {
                1
            }
            in 0.66f..1.0f -> {
                2
            }
            else -> 10
        }
    }

    private fun getResponseCode(): Int {
        //状态回应 0 OK; 1 设备正忙; 2 恢复/暂停类型不匹配; 3 没有位置权限;
        // 4 运动不支持; 5 精确gps关闭或后台无gps许可; 6 充电中; 7 低电量 ; 10 未知
        if (!isSportDevSport()) {
            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SPORT, TrackingLog.getAppTypeTrack("设备不支持辅助运动"), "2110", true)
            return 10
        }
        if (AppUtils.isEnableGoogleMap()) {
            if (!AppUtils.checkGooglePlayServices(BaseApplication.mContext)) {
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SPORT, TrackingLog.getAppTypeTrack("Google地图无服务"), "2111", true)
                return 1
            }
        }
        /*if(LocationService.binder.service.isLocationDoing){
            return 1
        }*/
        if (isDeviceSporting) {
            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SPORT, TrackingLog.getAppTypeTrack("恢复/暂停类型不匹配"), "2112", true)
            return 2
        }
        if (!PermissionUtils.isGranted(*com.smartwear.xzfit.utils.PermissionUtils.PERMISSION_GROUP_LOCATION)) {
            EventBus.getDefault().post(EventMessage(EventAction.ACTION_DEV_SPORT_NO_PERMISSION))
            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SPORT, TrackingLog.getAppTypeTrack("没有位置权限"), "2113", true)
            return 3
        }
        //TODO 4
        if (!AppUtils.isGPSOpen(BaseApplication.mContext)) {
            EventBus.getDefault().post(EventMessage(EventAction.ACTION_DEV_SPORT_NO_GPS))
            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SPORT, TrackingLog.getAppTypeTrack("GPS未开启"), "2114", true)
            return 5
        }
        //TODO 6 7
        return 0
    }

    /**
     * 开始发送手机定位数据
     */
    private fun sendPhoneLocationData() {
        if (!isDeviceSporting || isPause) return
        if (mLatitude == 0.0 || mLongitude == 0.0) return
        var phoneSportDataBean: PhoneSportDataBean? = null
        if (mLastTime == 0L || mLastLat == 0.0 || mLastLon == 0.0) {
            //初次定位发送
            phoneSportDataBean = setDataPhoneSportDataBean()
        } else {
            //5s | 定位有变化
            if (Math.abs(System.currentTimeMillis() - mLastTime) >= 5000 || (mLatitude != mLastLat || mLongitude != mLastLon)) {
                phoneSportDataBean = setDataPhoneSportDataBean()
            }
        }
        phoneSportDataBean?.let {
            mLastTime = System.currentTimeMillis()
            mLastLat = mLatitude
            mLastLon = mLongitude

            val trackingLog = TrackingLog.getDevTyepTrack("App发送定位数据", "发送APP定位数据给设备", "PHONE_SPORT_DATA")
            trackingLog.log = GsonUtils.toJson(it)

            ControlBleTools.getInstance().sendPhoneSportData(it, object : ParsingStateManager.SendCmdStateListener(null) {
                override fun onState(state: SendCmdState) {
                    LogUtils.d("sendPhoneSportData: $phoneSportDataBean --> $state")
                    trackingLog.endTime = TrackingLog.getNowString()
                    trackingLog.devResult = "state : $state"
                    if (state != SendCmdState.SUCCEED && ControlBleTools.getInstance().isConnect) {
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SPORT, trackingLog.apply {
                            log += "\n位置发送超时/失败"
                        }, "2116", true)
                    } else {
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SPORT, trackingLog)
                    }
                }
            })
        }
    }

    private fun setDataPhoneSportDataBean(): PhoneSportDataBean {
        val phoneSportDataBean = PhoneSportDataBean()
        phoneSportDataBean.gpsAccuracy = getGpsAccuracy()
        phoneSportDataBean.timestamp = (System.currentTimeMillis() / 1000).toInt()

        phoneSportDataBean.gpsCoordinateSystemType = 2
        if (SpUtils.getSPUtilsInstance().getString(SpUtils.THE_SERVER_GPS_TYPE, "02") == "01") {
            phoneSportDataBean.gpsCoordinateSystemType = 1
        } else if (SpUtils.getSPUtilsInstance().getString(SpUtils.THE_SERVER_GPS_TYPE, "02") == "02") {
            phoneSportDataBean.gpsCoordinateSystemType = 2
        }

        if (!GpsCoordinateUtils.isOutOfChina(mLatitude, mLongitude)) {
            //国内 高德 GooGle - 火星
            if (phoneSportDataBean.gpsCoordinateSystemType == 2) {
                //火星转84
                val newLatlng = GpsCoordinateUtils.calGCJ02toWGS84(mLatitude, mLongitude)
                phoneSportDataBean.latitude = newLatlng[0]
                phoneSportDataBean.longitude = newLatlng[1]
            } else {
                phoneSportDataBean.latitude = mLatitude
                phoneSportDataBean.longitude = mLongitude
            }
        } else {
            //国外 高德 GooGle - 84
            if (phoneSportDataBean.gpsCoordinateSystemType == 1) {
                //84 转 火星
                val newLatlng = GpsCoordinateUtils.calWGS84toGCJ02(mLatitude, mLongitude)
                phoneSportDataBean.latitude = newLatlng[0]
                phoneSportDataBean.longitude = newLatlng[1]
            } else {
                phoneSportDataBean.latitude = mLatitude
                phoneSportDataBean.longitude = mLongitude
            }
        }


        return phoneSportDataBean
    }


    private fun unRegEventBus() {
        AppUtils.unregisterEventBus(this)
    }
    //endregion

}
package com.smartwear.publicwatch.viewmodel

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.format.DateFormat
import androidx.lifecycle.MutableLiveData
import com.alibaba.fastjson.JSON
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.ThreadUtils
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.zhapp.ble.bean.*
import com.zhapp.ble.utils.BleUtils
import com.smartwear.publicwatch.R
import com.smartwear.publicwatch.base.BaseApplication
import com.smartwear.publicwatch.base.BaseViewModel
import com.smartwear.publicwatch.db.model.*
import com.smartwear.publicwatch.db.model.track.TrackingLog
import com.smartwear.publicwatch.https.HttpCommonAttributes
import com.smartwear.publicwatch.https.MyRetrofitClient
import com.smartwear.publicwatch.https.Response
import com.smartwear.publicwatch.https.params.*
import com.smartwear.publicwatch.https.response.*
import com.smartwear.publicwatch.ui.data.Global
import com.smartwear.publicwatch.ui.livedata.RefreshHealthyMainData
import com.smartwear.publicwatch.utils.*
import com.smartwear.publicwatch.utils.manager.AppTrackingManager
import com.zh.ble.wear.protobuf.FitnessProtos
import kotlinx.coroutines.*
import org.litepal.LitePal
import org.litepal.LitePal.deleteAll
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.math.abs


class DailyModel : BaseViewModel() {
    private val TAG: String = DailyModel::class.java.simpleName

    val currentStepTag = "currentStepData"
    val currentCaloriesTag = "currentCaloriesData"
    val currentDistanceTag = "currentDistanceData"
    val dataTypeTag = "type"

    private fun getTimeString(typeId: FitnessProtos.SEFitnessTypeId): String {
        return BleUtils.timeToString(
            typeId.time.year, typeId.time.month, typeId.time.day,
            typeId.time.hour, typeId.time.minute, typeId.time.second
        )
    }

    private fun calcDailyData(list: List<Int>? = null): Int {
        if (list == null) return -1
        var sum = 0
        for (i in list.indices) {
            sum += list[i]
        }
        return sum
    }

    private fun calcMaxMinAvgValue(data: String): Array<String> {
        val arrays = data.trim().split(",")
        var max = 0
        var min = 0
        val avgList = mutableListOf<Int>()
        var avg = 0

        for (i in arrays.indices) {
            val number = arrays[i].trim().toInt()
            if (number > 0) {
                if (number > max) max = number
                if (number < min) min = number
                avgList.add(number)
            }
        }

        var tempSum = 0
        if (avgList.size > 0) {
            for (i in avgList.indices) {
                tempSum += avgList[i]
            }
            avg = tempSum / avgList.size
        }

        return arrayOf<String>("$max", "$min", "$avg")
    }

    private fun calcMaxMinAvgValueHeartRate(data: String): Array<String> {
        val arrays = data.trim().split(",")
        var max = 0
        var min = 0
        val avgList = mutableListOf<Int>()
        var avg = 0

        for (i in arrays.indices) {
            val number = arrays[i].trim().toInt()
            if (number > 0) {
                if (number > max) max = number
                if (number < min) min = number
                avgList.add(number)
            }
        }

        var tempSum = 0
        var avgCount = 0
        if (avgList.size > 0) {
            for (i in avgList.indices) {
                if (avgList[i] > 0) {
                    tempSum += avgList[i]
                    avgCount++
                }
            }
            avg = tempSum / avgCount
        }

        return arrayOf<String>("$max", "$min", "$avg")
    }

    private fun calcMaxMinAvgValuePressure(data: String): Array<String> {
        val arrays = data.trim().split(",")
        var max = 0
        var min = 0
        val avgList = mutableListOf<Int>()
        var avg = 0

        for (i in arrays.indices) {
            val number = arrays[i].trim().toInt()
            if (number > 0) {
                if (number > max) max = number
                if (number < min) min = number
                avgList.add(number)
            }
        }

        var tempSum = 0
        var avgCount = 0
        if (avgList.size > 0) {
            for (i in avgList.indices) {
                if (avgList[i] > 0) {
                    tempSum += avgList[i]
                    avgCount++
                }
            }
            avg = tempSum / avgCount
        }

        return arrayOf<String>("$max", "$min", "$avg")
    }

    //region 日常数据
    val syncDailyData = MutableLiveData<Daily>()
    fun saveDailyData(data: DailyBean) {
        launchUI {
            withContext(Dispatchers.IO) {
                LogUtils.i(TAG, "saveDailyData --> ${data.toString()}")
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                val date = DateUtils.getStringDate(DateUtils.getLongTime(data.date, DateUtils.TIME_YYYY_MM_DD), DateUtils.TIME_YYYY_MM_DD)
                if (!DateUtils.isEffectiveDate(date)) return@withContext
                LogUtils.i(TAG, "saveDailyData --> In the valid period $date")

                val findData = LitePal.where(
                    "userId = ? and date = ?",
                    userId, date
                ).find(Daily::class.java)
                var isChange = false
                if (findData.size > 0) {
                    val step = data.stepsData.toTypedArray().contentToString().replace("[", "")
                        .replace("]", "").replace(" ", "")
                    val distance = data.distanceData.toTypedArray().contentToString().replace("[", "")
                        .replace("]", "").replace(" ", "")
                    val calorie = data.calorieData.toTypedArray().contentToString().replace("[", "")
                        .replace("]", "").replace(" ", "")
                    if (findData[0].stepsData != step || findData[0].distanceData != distance || findData[0].calorieData != calorie) {
                        isChange = true
                        deleteAll(Daily::class.java, "userId = ? and date = ?", userId, date)
                    }
                } else {
                    isChange = true
                }
                var dialy = Daily()
                var steps = 0
                var calorie = 0
                var distance = 0
                if (isChange) {
                    steps = calcDailyData(data.stepsData)
                    calorie = calcDailyData(data.calorieData)
                    distance = calcDailyData(data.distanceData)
                    val list = mutableListOf<Daily>()
                    dialy.userId = userId
                    dialy.date = date
                    dialy.stepsFrequency = data.stepsFrequency
                    dialy.stepsData = data.stepsData.toTypedArray().contentToString().replace("[", "")
                        .replace("]", "").replace(" ", "")
                    dialy.distanceFrequency = data.distanceFrequency
                    dialy.distanceData = data.distanceData.toTypedArray().contentToString().replace("[", "")
                        .replace("]", "").replace(" ", "")
                    dialy.calorieFrequency = data.calorieFrequency
                    dialy.calorieData = data.calorieData.toTypedArray().contentToString().replace("[", "")
                        .replace("]", "").replace(" ", "")
                    dialy.totalSteps = "$steps"
                    dialy.totalCalorie = "$calorie"
                    dialy.totalDistance = "$distance"
                    dialy.timeStamp = System.currentTimeMillis().toString()
                    dialy.deviceType = Global.deviceType
                    dialy.deviceMac = Global.deviceMac
                    dialy.deviceVersion = Global.deviceVersion
                    dialy.appVersion = AppUtils.getAppVersionName()
                    list.addAll(queryUnUploadDailyData(false))

                    val success = dialy.saveUpdate(
                        Daily::class.java,
                        "userId = ? and date = ?",
                        dialy.userId,
                        dialy.date
                    )
                    if (success) {
                        list.add(dialy)
                    }
                } else {
                    dialy = findData[0]
                    val stepsTmp = findData[0].stepsData.trim().split(",")
                    val stepsList = mutableListOf<Int>()
                    for (i in stepsTmp.indices) {
                        stepsList.add(stepsTmp[i].trim().toInt())
                    }
                    val calorieTmp = findData[0].calorieData.trim().split(",")
                    val calorieList = mutableListOf<Int>()
                    for (i in calorieTmp.indices) {
                        calorieList.add(calorieTmp[i].trim().toInt())
                    }
                    val distanceTmp = findData[0].distanceData.trim().split(",")
                    val distanceList = mutableListOf<Int>()
                    for (i in distanceTmp.indices) {
                        distanceList.add(distanceTmp[i].trim().toInt())
                    }
                    steps = calcDailyData(stepsList)
                    calorie = calcDailyData(calorieList)
                    distance = calcDailyData(distanceList)
                    dialy.totalSteps = "$steps"
                    dialy.totalCalorie = "$calorie"
                    dialy.totalDistance = "$distance"
                }
                /*TODO for (i in 0 until Global.healthyItemList.size) {
                    when (Global.healthyItemList[i].topTitleText) {
                        BaseApplication.mContext.getString(R.string.healthy_sports_list_step) -> {
                            Global.healthyItemList[i].context = "$steps"
                        }
                        BaseApplication.mContext.getString(R.string.healthy_sports_list_distance) -> {
                            Global.healthyItemList[i].context = "$distance"
                        }
                        BaseApplication.mContext.getString(R.string.healthy_sports_list_calories) -> {
                            Global.healthyItemList[i].context = "$calorie"
                        }
                        BaseApplication.mContext.getString(R.string.healthy_sports_list_sleep) -> {

                        }
                        BaseApplication.mContext.getString(R.string.healthy_sports_list_heart) -> {

                        }
                        BaseApplication.mContext.getString(R.string.healthy_sports_list_blood_oxygen) -> {

                        }
                        BaseApplication.mContext.getString(R.string.healthy_sports_list_sport_record) -> {

                        }
                    }
                }*/
                syncDailyData.postValue(dialy)
            }
        }
    }


    //获取每日数据
    val getDataByDay = MutableLiveData<Response<DailyDayResponse>>()
    fun getDataByDay(date: String) {
        launchUI {
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (userId.isNotEmpty()) {
                    val result =
                        MyRetrofitClient.service.getDailyDataByDay(
                            JsonUtils.getRequestJson(
                                "getDataByDay",
                                GetDailyDataByDayBean(userId, date),
                                GetDailyDataByDayBean::class.java
                            )
                        )
                    LogUtils.i(TAG, "getDataByDay result = $result")
                    getDataByDay.postValue(result)
                    userLoginOut(result.code)
                } else {
                    val result = Response("", "", HttpCommonAttributes.LOGIN_OUT, "", EffectiveStandLatelyResponse())
                    userLoginOut(result.code)
                }
            } catch (e: Exception) {
//                val stackTrace = e.stackTrace[0]
                LogUtils.e(TAG, "getDataByDay e =$e" /*+ "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className*/)
                val result = Response<DailyDayResponse>("", "", HttpCommonAttributes.SERVER_ERROR, "", DailyDayResponse())
                getDataByDay.postValue(result)
            }
        }
    }

    //上传日常数据
    fun upLoadDaily(list: MutableList<Daily>) {
        launchUI {
            val trackingLog = TrackingLog.getSerTypeTrack("日常数据上传服务器", "日常运动 批量上传", "infowear/dailyExercise/bulk")
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (userId.isNotEmpty()) {
                    val dataList = UpLoadDailyBean()
                    for (i in 0 until list.size) {
                        val bean = UpLoadDailyBean.Data()
                        bean.userId = userId
                        bean.date = list[i].date
                        bean.deviceType = list[i].deviceType
                        bean.deviceMac = list[i].deviceMac
                        bean.deviceVersion = list[i].deviceVersion
                        bean.deviceSyncTimestamp = list[i].timeStamp.toString()
                        bean.stepFrequency = list[i].stepsFrequency.toString()
                        bean.distanceFrequency = list[i].distanceFrequency.toString()
                        bean.calorieFrequency = list[i].calorieFrequency.toString()
                        bean.stepData = list[i].stepsData
                        bean.totalStep = list[i].totalSteps
                        bean.distanceData = list[i].distanceData
                        bean.totalDistance = list[i].totalDistance
                        bean.calorieData = list[i].calorieData
                        bean.totalCalorie = list[i].totalCalorie
                        dataList.dataList.add(bean)
                    }
                    trackingLog.serReqJson = AppUtils.toSimpleJsonString(dataList)
                    val result = MyRetrofitClient.service.upLoadDailyData(
                        JsonUtils.getRequestJson(
                            "upLoadDaily",
                            dataList, UpLoadDailyBean::class.java
                        )
                    )
                    LogUtils.i(TAG, "upLoadDaily result = $result")
                    trackingLog.endTime = TrackingLog.getNowString()
                    trackingLog.serResJson = AppUtils.toSimpleJsonString(result)

                    userLoginOut(result.code)
                    if (result.code == HttpCommonAttributes.REQUEST_SUCCESS) {
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, trackingLog)
                        for (i in 0 until list.size) {
                            val dialy = Daily()
                            dialy.isUpLoad = true
                            dialy.updateAll("userId = ? and date = ?", userId, list[i].date)
                        }
                    } else {
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, trackingLog.apply {
                            log += "\n日常数据上传服务器失败/超时"
                        }, "1426", true)
                    }

                }
            } catch (e: Exception) {
                if (e.stackTrace.isNotEmpty()) {
                    val stackTrace = e.stackTrace[0]
                    LogUtils.e(TAG, "upLoadDaily e =$e" + "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className)
                }
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, trackingLog.apply {
                    log += "\nupLoadDaily e =$e\n日常数据上传服务器失败/超时"
                }, "1426", true)
            }
        }
    }

    //获取日常-天数据详情
    val getDailyListByDateRange = MutableLiveData<Response<DailyListResponse>>()
    fun getDailyListByDateRange(beginDate: String, endDate: String, dataType: Int) {
        launchUI {
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (userId.isNotEmpty()) {
                    val result =
                        MyRetrofitClient.service.getDailyListByDateRange(
                            JsonUtils.getRequestJson(GetListByDataRangeBean(userId, beginDate, endDate, dataType), GetListByDataRangeBean::class.java)
                        )
                    LogUtils.i(TAG, "getDailyListByDateRange result = $result")
                    getDailyListByDateRange.postValue(result)
                    userLoginOut(result.code)
                } else {
                    val result = Response("", "", HttpCommonAttributes.LOGIN_OUT, "", EffectiveStandLatelyResponse())
                    userLoginOut(result.code)
                }
            } catch (e: Exception) {
//                val stackTrace = e.stackTrace[0]
                LogUtils.e(TAG, "getDailyListByDateRange e =$e" /*+ "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className*/)
                val result = Response<DailyListResponse>("", "", HttpCommonAttributes.SERVER_ERROR, "", DailyListResponse())
                getDailyListByDateRange.postValue(result)
            }
        }
    }

    //查询未上传的日常数据并上传
    suspend fun queryUnUploadDailyData(isSend: Boolean): MutableList<Daily> {
        return suspendCancellableCoroutine<MutableList<Daily>> {
            synchronized(DailyModel::class.java) {
                var list: MutableList<Daily>? = null
                list = LitePal.where(
                    "isUpLoad = 0 and userId = ?",
                    SpUtils.getValue(SpUtils.USER_ID, "")
                ).limit(10).find(Daily::class.java)
                if (list == null) list = mutableListOf()
                if (isSend) {
                    if (list!!.size > 0) {
                        upLoadDaily(list!!)
                    }
                }
                it.resume(list)
            }
        }
    }
    //endregion

    //region 睡眠数据
    //-----------睡眠
    val saveSleepData = MutableLiveData<Sleep>()
    fun saveSleepData(data: SleepBean) {
        launchUI {
            val userId = SpUtils.getValue(SpUtils.USER_ID, "")
            //收到设备数据时间加一天
            LogUtils.i(TAG, "saveSleepData --> ${data.toString()}")
            val date = DateUtils.getStringDate(
                DateUtils.getNextDay(
                    DateUtils.getStringDate(
                        DateUtils.getLongTime(data.date, DateUtils.TIME_YYYY_MM_DD),
                        DateUtils.TIME_YYYY_MM_DD
                    ), 1
                ), DateUtils.TIME_YYYY_MM_DD
            )

            if (!DateUtils.isEffectiveDate(date)) return@launchUI
            LogUtils.i(TAG, "saveSleepData --> In the valid period $date")

            val findData = LitePal.where(
                "userId = ? and date = ?",
                userId, date
            ).find(Sleep::class.java)
            var isChange = false
            if (findData.size > 0) {
                val value = JSON.toJSONString(data.list)
                if (findData[0].sleepDistributionDataList != value) {
                    isChange = true
                    deleteAll(Sleep::class.java, "userId = ? and date = ?", userId, date)
                }
            } else {
                isChange = true
            }

            var bean = Sleep()
            if (isChange) {
                val list = mutableListOf<Sleep>()
                bean.userId = userId
                bean.date = date
                bean.startSleepTimestamp = data.startSleepTimestamp.toString()
                bean.endSleepTimestamp = data.endSleepTimestamp.toString()
                bean.sleepDuration = data.sleepDuration.toString()
                bean.sleepScore = data.sleepScore.toString()
                bean.awakeTime = data.awakeTime.toString()
                bean.awakeTimePercentage = data.awakeTimePercentage.toString()
                bean.lightSleepTime = data.lightSleepTime.toString()
                bean.lightSleepTimePercentage = data.lightSleepTimePercentage.toString()
                bean.deepSleepTime = data.deepSleepTime.toString()
                bean.deepSleepTimePercentage = data.deepSleepTimePercentage.toString()
                bean.rapidEyeMovementTime = data.rapidEyeMovementTime.toString()
                bean.rapidEyeMovementTimePercentage = data.rapidEyeMovementTimePercentage.toString()
                bean.sleepDistributionDataList = JSON.toJSONString(data.list)
                bean.timeStamp = System.currentTimeMillis().toString()
                bean.deviceType = Global.deviceType
                bean.deviceMac = Global.deviceMac
                bean.deviceVersion = Global.deviceVersion
                bean.appVersion = AppUtils.getAppVersionName()
                list.addAll(queryUnUploadSleepData(false))

                val success = bean.saveUpdate(
                    Sleep::class.java,
                    "userId = ? and date = ?",
                    bean.userId,
                    bean.date
                )
                if (success) {
                    list.add(bean)
                }
            } else {
                bean = findData[0]
            }
            saveSleepData.postValue(bean)
        }
    }

    val getSleepDayData = MutableLiveData<Response<SleepDayResponse>>()
    fun getSleepDayData(date: String) {
        launchUI {
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (userId.isNotEmpty()) {
                    val result =
                        MyRetrofitClient.service.getSleepDataByDay(
                            JsonUtils.getRequestJson(
                                GetDailyDataByDayBean(userId, date),
                                GetDailyDataByDayBean::class.java
                            )
                        )
                    LogUtils.i(TAG, "getSleepDayData result = $result")
                    getSleepDayData.postValue(result)
                    userLoginOut(result.code)
                } else {
                    val result = Response("", "", HttpCommonAttributes.LOGIN_OUT, "", SleepDayResponse())
                    userLoginOut(result.code)
                }
            } catch (e: Exception) {
//                val stackTrace = e.stackTrace[0]
                LogUtils.e(TAG, "getSleepDayData e =$e"/* + "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className*/)
                val result = Response<SleepDayResponse>("", "", HttpCommonAttributes.SERVER_ERROR, "", SleepDayResponse())
                getSleepDayData.postValue(result)
            }
        }
    }

    val getSleepListByDateRange = MutableLiveData<Response<SleepListResponse>>()
    fun getSleepListByDateRange(beginDate: String, endDate: String) {
        launchUI {
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (userId.isNotEmpty()) {
                    val result =
                        MyRetrofitClient.service.getSleepListByDateRange(
                            JsonUtils.getRequestJson(
                                DataRangeNoTypeBean(userId, beginDate, endDate),
                                DataRangeNoTypeBean::class.java
                            )
                        )
                    LogUtils.i(TAG, "getSleepListByDateRange result = $result")
                    getSleepListByDateRange.postValue(result)
                    userLoginOut(result.code)
                } else {
                    val result = Response("", "", HttpCommonAttributes.LOGIN_OUT, "", SleepDayResponse())
                    userLoginOut(result.code)
                }
            } catch (e: Exception) {
//                val stackTrace = e.stackTrace[0]
                LogUtils.e(TAG, "getSleepListByDateRange e =$e" /*+ "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className*/)
                val result = Response<SleepListResponse>("", "", HttpCommonAttributes.SERVER_ERROR, "", SleepListResponse())
                getSleepListByDateRange.postValue(result)
            }
        }
    }

    suspend fun queryUnUploadSleepData(isSend: Boolean): MutableList<Sleep> {
        return suspendCancellableCoroutine<MutableList<Sleep>> {
            synchronized(DailyModel::class.java) {
                var list: MutableList<Sleep>? = null
                list = LitePal.where("isUpLoad = 0 and userId = ?", SpUtils.getValue(SpUtils.USER_ID, "")).limit(10).find(Sleep::class.java)
                if (list == null) list = mutableListOf()
                if (isSend) {
                    if (list.size > 0) {
                        upLoadSleep(list)
                    }
                }
                it.resume(list)
            }
        }
    }

    val upLoadSleepData = MutableLiveData("")

    //上传睡眠数据
    fun upLoadSleep(list: MutableList<Sleep>) {
        launchUI {
            val trackingLog = TrackingLog.getSerTypeTrack("日常数据上传服务器", "睡眠 批量上传", "infowear/sleep/bulk")
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (userId.isNotEmpty()) {
                    val dataList = UpLoadSleepBean()
                    for (i in 0 until list.size) {
                        val bean = UpLoadSleepBean.Data()
                        bean.userId = userId
                        bean.date = list[i].date
                        bean.deviceType = list[i].deviceType
                        bean.deviceMac = list[i].deviceMac
                        bean.deviceVersion = list[i].deviceVersion
                        bean.deviceSyncTimestamp = list[i].timeStamp.toString()
                        bean.startSleepTimestamp = list[i].startSleepTimestamp
                        bean.endSleepTimestamp = list[i].endSleepTimestamp
                        bean.sleepDuration = list[i].sleepDuration
                        bean.sleepScore = list[i].sleepScore
                        bean.awakeTime = list[i].awakeTime
                        bean.awakeTimePercentage = list[i].awakeTimePercentage
                        bean.lightSleepTime = list[i].lightSleepTime
                        bean.lightSleepTimePercentage = list[i].lightSleepTimePercentage
                        bean.deepSleepTime = list[i].deepSleepTime
                        bean.deepSleepTimePercentage = list[i].deepSleepTimePercentage
                        bean.rapidEyeMovementTime = list[i].rapidEyeMovementTime
                        bean.rapidEyeMovementTimePercentage = list[i].rapidEyeMovementTimePercentage
                        val data2List = JSON.parseArray(list[i].sleepDistributionDataList, SleepBean.SleepDistributionData::class.java)
                        for (j in data2List.indices) {
                            val data = UpLoadSleepBean.BreakData()
                            data.sleepDistributionType = data2List[j].sleepDistributionType.toString()
                            data.startTimestamp = data2List[j].startTimestamp.toString()
                            data.sleepDuration = data2List[j].sleepDuration.toString()
                            bean.sectionList.add(data)
                        }
                        dataList.dataList.add(bean)
                    }
                    trackingLog.serReqJson = AppUtils.toSimpleJsonString(dataList)
                    val result = MyRetrofitClient.service.upLoadSleepData(
                        JsonUtils.getRequestJson(
                            "upLoadSleep",
                            dataList,
                            UpLoadSleepBean::class.java
                        )
                    )
                    LogUtils.i(TAG, "upLoadSleep result = $result")
                    trackingLog.endTime = TrackingLog.getNowString()
                    trackingLog.serResJson = AppUtils.toSimpleJsonString(result)
                    userLoginOut(result.code)
                    if (result.code == HttpCommonAttributes.REQUEST_SUCCESS) {
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, trackingLog)
                        for (i in 0 until list.size) {
                            var sleep = Sleep()
                            sleep.isUpLoad = true
                            sleep.updateAll("userId = ? and date = ?", userId, list[i].date)
                        }
                    } else {
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, trackingLog.apply {
                            log += "\n日常数据上传服务器失败/超时"
                        }, "1426", true)
                    }
                    upLoadSleepData.postValue(result.code)
                }
            } catch (e: Exception) {
//                val stackTrace = e.stackTrace[0]
                LogUtils.e(TAG, "upLoadSleep e =$e" /*+ "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className*/)
                upLoadSleepData.postValue(HttpCommonAttributes.SERVER_ERROR)
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, trackingLog.apply {
                    log += "\bupLoadSleep e =$e\n日常数据上传服务器失败/超时"
                }, "1426", true)
            }
        }
    }
    //endregion

    //region 心率

    val saveHeartRateData = MutableLiveData<HeartRate>()
    fun saveHeartRateData(data: ContinuousHeartRateBean) {
        launchUI {
            LogUtils.i(TAG, "saveHeartRateData --> ${data.toString()}")
            val userId = SpUtils.getValue(SpUtils.USER_ID, "")
            val date = DateUtils.getStringDate(DateUtils.getLongTime(data.date, DateUtils.TIME_YYYY_MM_DD), DateUtils.TIME_YYYY_MM_DD)
            if (!DateUtils.isEffectiveDate(date)) return@launchUI
            LogUtils.i(TAG, "saveHeartRateData --> In the valid period $date")

            val findData = LitePal.where(
                "userId = ? and date = ?",
                userId, date
            ).find(HeartRate::class.java)
            var isChange = false
            if (findData.size > 0) {
                val value = data.heartRateData.toTypedArray().contentToString()
                    .replace("[", "").replace("]", "").replace(" ", "")
                if (findData[0].heartRateData != value) {
                    isChange = true
                    deleteAll(HeartRate::class.java, "userId = ? and date = ?", userId, date)
                }
            } else {
                isChange = true
            }

            var bean = HeartRate()
            if (isChange) {
                val list = mutableListOf<HeartRate>()
                bean.userId = userId
                bean.date = date
                bean.continuousHeartRateFrequency = data.continuousHeartRateFrequency.toString()
                bean.heartRateData = data.heartRateData.toTypedArray().contentToString()
                    .replace("[", "").replace("]", "").replace(" ", "")
                val maxMinAvg = calcMaxMinAvgValueHeartRate(bean.heartRateData)
                bean.maxHeartRate = data.max.toString()
                bean.minHeartRate = data.min.toString()
                bean.avgHeartRate = maxMinAvg[2]
                bean.timeStamp = System.currentTimeMillis().toString()
                bean.deviceType = Global.deviceType
                bean.deviceMac = Global.deviceMac
                bean.deviceVersion = Global.deviceVersion
                bean.appVersion = AppUtils.getAppVersionName()
                list.addAll(queryUnUploadHeartRate(false))
                val success = bean.saveUpdate(
                    HeartRate::class.java,
                    "userId = ? and date = ?",
                    bean.userId,
                    bean.date
                )
                if (success) {
                    list.add(bean)
                }
            } else {
                bean = findData[0]
            }
            saveHeartRateData.postValue(bean)
        }
    }

    //心率 获取每日数据
    val getHeartRateDataByDay = MutableLiveData<Response<HeartRateResponse>>()
    fun getHeartRateDataByDay(date: String) {
        launchUI {
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (userId.isNotEmpty()) {
                    val result =
                        MyRetrofitClient.service.getHeartRateDataByDay(
                            JsonUtils.getRequestJson(
                                GetDailyDataByDayBean(userId, date),
                                GetDailyDataByDayBean::class.java
                            )
                        )
                    LogUtils.i(TAG, "getHeartRateDataByDay result = $result")
                    getHeartRateDataByDay.postValue(result)
                    userLoginOut(result.code)
                } else {
                    val result = Response("", "", HttpCommonAttributes.LOGIN_OUT, "", SleepDayResponse())
                    userLoginOut(result.code)
                }
            } catch (e: Exception) {
//                val stackTrace = e.stackTrace[0]
                LogUtils.e(TAG, "getHeartRateDataByDay e =$e"/* + "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className*/)
                val result = Response<HeartRateResponse>("", "", HttpCommonAttributes.SERVER_ERROR, "", HeartRateResponse())
                getHeartRateDataByDay.postValue(result)
            }
        }
    }

    val getHeartRateListByDateRange = MutableLiveData<Response<HeartRateListResponse>>()
    fun getHeartRateListByDateRange(beginDate: String, endDate: String) {
        launchUI {
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (userId.isNotEmpty()) {
                    val result =
                        MyRetrofitClient.service.getHeartRateListByDateRange(
                            JsonUtils.getRequestJson(
                                DataRangeNoTypeBean(userId, beginDate, endDate),
                                DataRangeNoTypeBean::class.java
                            )
                        )
                    LogUtils.i(TAG, "getHeartRateListByDateRange result = $result")
                    getHeartRateListByDateRange.postValue(result)
                    userLoginOut(result.code)
                } else {
                    val result = Response("", "", HttpCommonAttributes.LOGIN_OUT, "", SleepDayResponse())
                    userLoginOut(result.code)
                }
            } catch (e: Exception) {
//                val stackTrace = e.stackTrace[0]
                LogUtils.e(TAG, "getHeartRateListByDateRange e =$e" /*+ "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className*/)
                val result = Response<HeartRateListResponse>(
                    "", "", HttpCommonAttributes.SERVER_ERROR, "", HeartRateListResponse()
                )
                getHeartRateListByDateRange.postValue(result)
            }
        }
    }

    val upLoadHeartRateData = MutableLiveData("")

    //上传心率数据
    fun upLoadHeartRate(list: MutableList<HeartRate>) {
        launchUI {
            val trackingLog = TrackingLog.getSerTypeTrack("日常数据上传服务器", "心率 批量上传", "infowear/heartRate/bulk")
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (userId.isNotEmpty()) {
                    val dataList = UpLoadHeartRateBean()
                    for (i in 0 until list.size) {
                        val bean = UpLoadHeartRateBean.Data()
                        bean.userId = userId
                        bean.date = list[i].date
                        bean.deviceType = list[i].deviceType
                        bean.deviceMac = list[i].deviceMac
                        bean.deviceVersion = list[i].deviceVersion
                        bean.deviceSyncTimestamp = list[i].timeStamp.toString()
                        bean.heartRateFrequency = list[i].continuousHeartRateFrequency
                        bean.heartRateData = list[i].heartRateData
                        bean.maxHeartRate = list[i].maxHeartRate
                        bean.minHeartRate = list[i].minHeartRate
                        bean.avgHeartRate = list[i].avgHeartRate
                        dataList.dataList.add(bean)
                    }
                    trackingLog.serReqJson = AppUtils.toSimpleJsonString(dataList)
                    val result = MyRetrofitClient.service.upLoadHeartRateData(
                        JsonUtils.getRequestJson(
                            "upLoadHeartRate",
                            dataList,
                            UpLoadHeartRateBean::class.java
                        )
                    )
                    LogUtils.i(TAG, "upLoadHeartRate result = $result")
                    trackingLog.endTime = TrackingLog.getNowString()
                    trackingLog.serResJson = AppUtils.toSimpleJsonString(result)
                    if (result.code == HttpCommonAttributes.REQUEST_SUCCESS) {
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, trackingLog)
                        for (i in 0 until list.size) {
                            var heartrate = HeartRate()
                            heartrate.isUpLoad = true
                            heartrate.updateAll("userId = ? and date = ?", userId, list[i].date)
                        }
                    } else {
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, trackingLog.apply {
                            log += "\n日常数据上传服务器失败/超时"
                        }, "1426", true)
                    }
                    upLoadHeartRateData.postValue(result.code)
                    userLoginOut(result.code)
                }
            } catch (e: Exception) {
//                val stackTrace = e.stackTrace[0]
                LogUtils.e(TAG, "upLoadHeartRate e =$e" /*+ "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className*/)
                upLoadHeartRateData.postValue(HttpCommonAttributes.SERVER_ERROR)
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, trackingLog.apply {
                    log += "\nupLoadHeartRate e =$e\n日常数据上传服务器失败/超时"
                }, "1426", true)
            }
        }
    }

    suspend fun queryUnUploadHeartRate(isSend: Boolean): MutableList<HeartRate> {
        return suspendCancellableCoroutine<MutableList<HeartRate>> {
            synchronized(DailyModel::class.java) {
                var list: MutableList<HeartRate>? = null
                list = LitePal.where(
                    "isUpLoad = 0 and userId = ?",
                    SpUtils.getValue(SpUtils.USER_ID, "")
                ).limit(10).find(HeartRate::class.java)
                if (list == null) list = mutableListOf()
                if (isSend) {
                    if (list.size > 0) {
                        upLoadHeartRate(list)
                    }
                }
                it.resume(list)
            }
        }
    }
    //endregion

    //region 心率单次测量
    val saveSingleHeartRateData = MutableLiveData<MutableList<SingleHeartRate>>()
    fun saveSingleHeartRateData(data: OfflineHeartRateBean) {
        launchUI {
            LogUtils.i(TAG, "saveSingleHeartRateData --> ${data.toString()}")
            val userId = SpUtils.getValue(SpUtils.USER_ID, "")
            val list = mutableListOf<SingleHeartRate>()
            //去重
            data.list = data.list.distinct()
            for (i in data.list.indices) {
                val bean = SingleHeartRate()
                bean.userId = userId
                val findData = LitePal.where(
                    "userId = ? and measureTimestamp = ?",
                    userId, data.list[i].measureTimestamp.toString()
                ).find(SingleHeartRate::class.java)
                if (findData.size > 0) {
                    for (oldItem in findData) {
                        //只要用户id 测试时间一致，就把旧数据移除
                        oldItem.delete()
                    }
                }
                bean.singleHeartRateData = data.list[i].measureData.toString()
                bean.measureTimestamp = data.list[i].measureTimestamp.toString()
                bean.timeStamp = System.currentTimeMillis().toString()
                bean.deviceType = Global.deviceType
                bean.deviceMac = Global.deviceMac
                bean.deviceVersion = Global.deviceVersion
                bean.appVersion = AppUtils.getAppVersionName()
                list.add(bean)
            }

            val result = LitePal.saveAll(list)
            saveSingleHeartRateData.postValue(list)
        }
    }

    val getSingleHeartRateDataByDay = MutableLiveData<Response<SingleHeartRateResponse>>()
    fun getSingleHeartRateDataByDay(date: String) {
        launchUI {
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (userId.isNotEmpty()) {
                    val result =
                        MyRetrofitClient.service.getSingleHeartRateDataByDay(
                            JsonUtils.getRequestJson(
                                GetDailyDataByDayBean(userId, date),
                                GetDailyDataByDayBean::class.java
                            )
                        )
                    LogUtils.i(TAG, "getSingleHeartRateDataByDay result = $result")
                    getSingleHeartRateDataByDay.postValue(result)
                    userLoginOut(result.code)
                } else {
                    val result = Response("", "", HttpCommonAttributes.LOGIN_OUT, "", SleepDayResponse())
                    userLoginOut(result.code)
                }
            } catch (e: Exception) {
//                val stackTrace = e.stackTrace[0]
                LogUtils.e(TAG, "getSingleHeartRateDataByDay e =$e"/* + "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className*/)
                val result = Response<SingleHeartRateResponse>("", "", HttpCommonAttributes.SERVER_ERROR, "", SingleHeartRateResponse())
                getSingleHeartRateDataByDay.postValue(result)
            }
        }
    }

    val getSingleLastHeartRateData = MutableLiveData<Response<SingleHeartRateLastResponse>>()
    fun getSingleLastHeartRateData(date: String) {
        launchUI {
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (userId.isNotEmpty()) {
                    val result =
                        MyRetrofitClient.service.getSingleLastHeartRateData(
                            JsonUtils.getRequestJson(
                                GetDailyDataByDayBean(userId, date),
                                GetDailyDataByDayBean::class.java
                            )
                        )
                    LogUtils.i(TAG, "getSingleLastHeartRateData result = $result")
                    getSingleLastHeartRateData.postValue(result)
                    userLoginOut(result.code)
                } else {
                    val result = Response("", "", HttpCommonAttributes.LOGIN_OUT, "", SleepDayResponse())
                    userLoginOut(result.code)
                }
            } catch (e: Exception) {
//                val stackTrace = e.stackTrace[0]
                LogUtils.e(TAG, "getSingleLastHeartRateData e =$e"/* + "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className*/)
                val result = Response<SingleHeartRateLastResponse>("", "", HttpCommonAttributes.SERVER_ERROR, "", SingleHeartRateLastResponse())
                getSingleLastHeartRateData.postValue(result)
            }
        }
    }

    //上传心率单次测量数据
    val upLoadSingleHeartRate = MutableLiveData("")
    fun upLoadSingleHeartRate(list: MutableList<SingleHeartRate>) {
        launchUI {
            val trackingLog = TrackingLog.getSerTypeTrack("日常数据上传服务器", "心率单次测量 批量上传", "infowear/heartRateMeasure/bulk")
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (userId.isNotEmpty()) {
                    val dataList = UpLoadSingleHeartRateBean()
                    val lenght = if (list.size >= outLineDataCount) {
                        outLineDataCount
                    } else {
                        list.size
                    }
                    for (i in 0 until lenght) {
                        val bean = UpLoadSingleHeartRateBean.Data()
                        bean.userId = userId
                        bean.deviceType = list[i].deviceType
                        bean.deviceMac = list[i].deviceMac
                        bean.deviceVersion = list[i].deviceVersion
                        bean.deviceSyncTimestamp = list[i].timeStamp.toString()
                        bean.measureData = list[i].singleHeartRateData
                        bean.measureTime = list[i].measureTimestamp
                        //如果存在测试数据测量时间一致的数据则不上报,并移除
                        var isCanAdd = true
                        val old = dataList.dataList.find { it.measureData == bean.measureData && it.measureTime == bean.measureTime }
                        if(old != null) {
                            isCanAdd = false
                            list[i].delete()
                        }
                        if(isCanAdd) {
                            dataList.dataList.add(bean)
                        }
                    }
                    trackingLog.serReqJson = AppUtils.toSimpleJsonString(dataList)
                    val result = MyRetrofitClient.service.upLoadSingleHeartRateData(
                        JsonUtils.getRequestJson(
                            "upLoadSingleHeartRate",
                            dataList,
                            UpLoadSingleHeartRateBean::class.java
                        )
                    )
                    LogUtils.i(TAG, "upLoadSingleHeartRate result = $result")
                    trackingLog.endTime = TrackingLog.getNowString()
                    trackingLog.serResJson = AppUtils.toSimpleJsonString(result)
                    if (result.code == HttpCommonAttributes.REQUEST_SUCCESS) {
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, trackingLog)
                        for (i in 0 until list.size) {
                            var heartrate = SingleHeartRate()
                            heartrate.isUpLoad = true
                            heartrate.updateAll("userId = ? and measureTimestamp = ?", userId, list[i].measureTimestamp)
                        }
                    } else {
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, trackingLog.apply {
                            log += "\n日常数据上传服务器失败/超时"
                        }, "1426", true)
                    }
                    upLoadSingleHeartRate.postValue(result.code)
                    userLoginOut(result.code)
                }
            } catch (e: Exception) {
//                val stackTrace = e.stackTrace[0]
                LogUtils.e(TAG, "upLoadSingleHeartRate e =$e" /*+ "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className*/)
                upLoadSingleHeartRate.postValue(HttpCommonAttributes.SERVER_ERROR)
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, trackingLog.apply {
                    log += "\nupLoadSingleHeartRate e =$e\n日常数据上传服务器失败/超时"
                }, "1426", true)
            }
        }
    }

    fun queryUnUploadSingleHeartRate() {
        launchUI {
            synchronized(DailyModel::class.java) {
                val list = LitePal.where(
                    "isUpLoad = 0 and userId = ?",
                    SpUtils.getValue(SpUtils.USER_ID, "")
                ).limit(10).find(SingleHeartRate::class.java)
                if (list.size > 0) {
                    upLoadSingleHeartRate(list)
                }
            }
        }
    }
    //endregion

    //region 血氧饱和度
    val saveBloodOxygenData = MutableLiveData<BloodOxygen>()
    fun saveBloodOxygenData(data: ContinuousBloodOxygenBean) {
        launchUI {
            LogUtils.i(TAG, "saveBloodOxygenData --> ${data.toString()}")
            val date = DateUtils.getStringDate(DateUtils.getLongTime(data.date, DateUtils.TIME_YYYY_MM_DD), DateUtils.TIME_YYYY_MM_DD)
            if (!DateUtils.isEffectiveDate(date)) return@launchUI
            LogUtils.i(TAG, "saveBloodOxygenData --> In the valid period $date")

            val findData = LitePal.where(
                "userId = ? and date = ?",
                SpUtils.getValue(SpUtils.USER_ID, ""), date
            ).find(BloodOxygen::class.java)
            var isChange = false
            if (findData.size > 0) {
                val value = data.bloodOxygenData.toTypedArray().contentToString()
                    .replace("[", "").replace("]", "").replace(" ", "")
                if (findData[0].bloodOxygenData != value) {
                    isChange = true
                    deleteAll(BloodOxygen::class.java, "userId = ? and date = ?", SpUtils.getValue(SpUtils.USER_ID, ""), date)
                }
            } else {
                isChange = true
            }

            var bean = BloodOxygen()
            if (isChange) {
                val list = mutableListOf<BloodOxygen>()
                bean.userId = SpUtils.getValue(SpUtils.USER_ID, "")
                bean.date = date
                bean.bloodOxygenFrequency = data.bloodOxygenFrequency.toString()
                bean.bloodOxygenData = data.bloodOxygenData.toTypedArray().contentToString()
                    .replace("[", "").replace("]", "").replace(" ", "")
                val maxMinAvg = calcMaxMinAvgValue(bean.bloodOxygenData)
                bean.maxBloodOxygen = maxMinAvg[0]
                bean.minBloodOxygen = maxMinAvg[1]
                bean.avgBloodOxygen = maxMinAvg[2]

//                bean.timeStamp = (DateUtils.getLongTime(data.date , DateUtils.TIME_YYYY_MM_DD_HHMMSS) / 1000).toString()
                bean.timeStamp = System.currentTimeMillis().toString()
                bean.deviceType = Global.deviceType
                bean.deviceMac = Global.deviceMac
                bean.deviceVersion = Global.deviceVersion
                bean.appVersion = AppUtils.getAppVersionName()
                list.addAll(queryUnUploadBloodOxygen(false))

                val success = bean.saveUpdate(
                    BloodOxygen::class.java,
                    "userId = ? and date = ?",
                    bean.userId,
                    bean.date
                )
                if (success) {
                    list.add(bean)
                    //upLoadBloodOxygen(list)
                }

            } else {
                bean = findData[0]
            }
            saveBloodOxygenData.postValue(bean)
        }
    }

    val getBloodOxygenDataByDay = MutableLiveData<Response<BloodOxygenResponse>>()
    fun getBloodOxygenDataByDay(date: String) {
        launchUI {
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (userId.isNotEmpty()) {
                    val result =
                        MyRetrofitClient.service.getBloodOxygenDataByDay(
                            JsonUtils.getRequestJson(
                                GetDailyDataByDayBean(userId, date),
                                GetDailyDataByDayBean::class.java
                            )
                        )
                    LogUtils.i(TAG, "getBloodOxygenDataByDay result = $result")
                    getBloodOxygenDataByDay.postValue(result)
                    userLoginOut(result.code)
                } else {
                    val result = Response("", "", HttpCommonAttributes.LOGIN_OUT, "", SleepDayResponse())
                    userLoginOut(result.code)
                }
            } catch (e: Exception) {
//                val stackTrace = e.stackTrace[0]
                LogUtils.e(TAG, "getBloodOxygenDataByDay e =$e"/* + "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className*/)
                val result = Response<BloodOxygenResponse>("", "", HttpCommonAttributes.SERVER_ERROR, "", BloodOxygenResponse())
                getBloodOxygenDataByDay.postValue(result)
            }
        }
    }

    val getBloodOxygenListByDateRange = MutableLiveData<Response<BloodOxygenListResponse>>()
    fun getBloodOxygenListByDateRange(beginDate: String, endDate: String) {
        launchUI {
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (userId.isNotEmpty()) {
                    val result =
                        MyRetrofitClient.service.getBloodOxygenListByDateRange(
                            JsonUtils.getRequestJson(
                                DataRangeNoTypeBean(userId, beginDate, endDate),
                                DataRangeNoTypeBean::class.java
                            )
                        )
                    LogUtils.i(TAG, "getBloodOxygenListByDateRange result = $result")
                    getBloodOxygenListByDateRange.postValue(result)
                    userLoginOut(result.code)
                } else {
                    val result = Response("", "", HttpCommonAttributes.LOGIN_OUT, "", SleepDayResponse())
                    userLoginOut(result.code)
                }
            } catch (e: Exception) {
//                val stackTrace = e.stackTrace[0]
                LogUtils.e(TAG, "getBloodOxygenListByDateRange e =$e" /*+ "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className*/)
                val result = Response<BloodOxygenListResponse>(
                    "", "", HttpCommonAttributes.SERVER_ERROR, "", BloodOxygenListResponse()
                )
                getBloodOxygenListByDateRange.postValue(result)
            }
        }
    }

    val upLoadBloodOxygen = MutableLiveData("")
    fun upLoadBloodOxygen(list: MutableList<BloodOxygen>) {
        launchUI {
            val trackingLog = TrackingLog.getSerTypeTrack("日常数据上传服务器", "血氧饱和度 批量上传", "infowear/bloodOxygen/bulk")
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (userId.isNotEmpty()) {
                    val dataList = UpLoadBloodOxygenBean()
                    for (i in 0 until list.size) {
                        val bean = UpLoadBloodOxygenBean.Data()
                        bean.userId = userId
                        bean.date = list[i].date
                        bean.deviceType = list[i].deviceType
                        bean.deviceMac = list[i].deviceMac
                        bean.deviceVersion = list[i].deviceVersion
                        bean.deviceSyncTimestamp = list[i].timeStamp.toString()
                        bean.bloodOxygenFrequency = list[i].bloodOxygenFrequency
                        bean.bloodOxygenData = list[i].bloodOxygenData
                        bean.maxBloodOxygen = list[i].maxBloodOxygen
                        bean.minBloodOxygen = list[i].minBloodOxygen
                        bean.avgBloodOxygen = list[i].avgBloodOxygen
                        dataList.dataList.add(bean)
                    }
                    trackingLog.serReqJson = AppUtils.toSimpleJsonString(dataList)
                    val result = MyRetrofitClient.service.upLoadBloodOxygen(
                        JsonUtils.getRequestJson(
                            "upLoadBloodOxygen",
                            dataList,
                            UpLoadBloodOxygenBean::class.java
                        )
                    )
                    LogUtils.i(TAG, "upLoadBloodOxygen result = $result")
                    trackingLog.endTime = TrackingLog.getNowString()
                    trackingLog.serResJson = AppUtils.toSimpleJsonString(result)
                    if (result.code == HttpCommonAttributes.REQUEST_SUCCESS) {
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, trackingLog)
                        for (i in 0 until list.size) {
                            var bloodoxygen = BloodOxygen()
                            bloodoxygen.isUpLoad = true
                            bloodoxygen.updateAll("userId = ? and date = ?", userId, list[i].date)
                        }
                    } else {
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, trackingLog.apply {
                            log += "\n日常数据上传服务器失败/超时"
                        }, "1426", true)
                    }
                    upLoadBloodOxygen.postValue(result.code)
                    userLoginOut(result.code)
                }
            } catch (e: Exception) {
//                val stackTrace = e.stackTrace[0]
                LogUtils.e(TAG, "upLoadBloodOxygen e =$e" /*+ "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className*/)
                upLoadBloodOxygen.postValue(HttpCommonAttributes.SERVER_ERROR)
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, trackingLog.apply {
                    log += "\nupLoadBloodOxygen e =$e\n日常数据上传服务器失败/超时"
                }, "1426", true)
            }
        }
    }

    suspend fun queryUnUploadBloodOxygen(isSend: Boolean): MutableList<BloodOxygen> {
        return suspendCancellableCoroutine<MutableList<BloodOxygen>> {
            synchronized(DailyModel::class.java) {
                var list: MutableList<BloodOxygen>? = null
                list = LitePal.where(
                    "isUpLoad = 0 and userId = ?",
                    SpUtils.getValue(SpUtils.USER_ID, "")
                ).limit(10).find(BloodOxygen::class.java)
                if (list == null) list = mutableListOf()
                if (isSend) {
                    if (list.size > 0) {
                        upLoadBloodOxygen(list)
                    }
                }
                it.resume(list)
            }
        }
    }
    //endregion

    //region 离线血氧
    val saveSingleBloodOxygenData = MutableLiveData<MutableList<SingleBloodOxygen>>()
    fun saveSingleBloodOxygenData(data: OfflineBloodOxygenBean) {
        launchUI {
            LogUtils.i(TAG, "saveSingleBloodOxygenData --> ${data.toString()}")
            val userId = SpUtils.getValue(SpUtils.USER_ID, "")

            val listTemp = mutableListOf<SingleBloodOxygen>()
            val list = mutableListOf<SingleBloodOxygen>()
            //去重
            data.list = data.list.distinct()
            for (i in data.list.indices) {
                val bean = SingleBloodOxygen()
                bean.userId = userId
                val findData = LitePal.where(
                    "userId = ? and measureTimestamp = ?",
                    userId, (data.list[i].measureTimestamp.toLong() * 1000).toString()
                ).find(SingleBloodOxygen::class.java)
                if (findData.size > 0) {
                    for (oldItem in findData) {
                        //只要用户id 测试时间一致，就把旧数据移除
                        oldItem.delete()
                    }
                }
                bean.singleBloodOxygenData = data.list[i].measureData.toString()
                bean.measureTimestamp = (data.list[i].measureTimestamp.toLong() * 1000).toString()
                bean.timeStamp = System.currentTimeMillis().toString()
                bean.deviceType = Global.deviceType
                bean.deviceMac = Global.deviceMac
                bean.deviceVersion = Global.deviceVersion
                bean.appVersion = AppUtils.getAppVersionName()
                listTemp.add(bean)
            }
            list.addAll(queryUnUploadSingleBloodOxygen(false))
            val result = LitePal.saveAll(listTemp)
            if (result) {
                list.addAll(listTemp)
            }
            saveSingleBloodOxygenData.postValue(list)
        }
    }

    val getSingleBloodOxygenDataByDay = MutableLiveData<Response<SingleBloodOxygenResponse>>()
    fun getSingleBloodOxygenDataByDay(date: String) {
        launchUI {
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (userId.isNotEmpty()) {
                    val result =
                        MyRetrofitClient.service.getSingleBloodOxygenDataByDay(
                            JsonUtils.getRequestJson(
                                GetDailyDataByDayBean(userId, date),
                                GetDailyDataByDayBean::class.java
                            )
                        )
                    LogUtils.i(TAG, "getSingleBloodOxygenDataByDay result = $result")
                    getSingleBloodOxygenDataByDay.postValue(result)
                    userLoginOut(result.code)
                } else {
                    val result = Response("", "", HttpCommonAttributes.LOGIN_OUT, "", SleepDayResponse())
                    userLoginOut(result.code)
                }
            } catch (e: Exception) {
//                val stackTrace = e.stackTrace[0]
                LogUtils.e(TAG, "getSingleBloodOxygenDataByDay e =$e"/* + "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className*/)
                val result = Response<SingleBloodOxygenResponse>("", "", HttpCommonAttributes.SERVER_ERROR, "", SingleBloodOxygenResponse())
                getSingleBloodOxygenDataByDay.postValue(result)
            }
        }
    }

    val getSingleBloodOxygenListByDateRange = MutableLiveData<Response<SingleBloodOxygenListResponse>>()
    fun getSingleBloodOxygenListByDateRange(beginDate: String, endDate: String) {
        launchUI {
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (userId.isNotEmpty()) {
                    val result =
                        MyRetrofitClient.service.getSingleBloodOxygenListByDateRange(
                            JsonUtils.getRequestJson(
                                DataRangeNoTypeBean(userId, beginDate, endDate),
                                DataRangeNoTypeBean::class.java
                            )
                        )
                    LogUtils.i(TAG, "getSingleBloodOxygenListByDateRange result = $result")
                    getSingleBloodOxygenListByDateRange.postValue(result)
                    userLoginOut(result.code)
                } else {
                    val result = Response("", "", HttpCommonAttributes.LOGIN_OUT, "", SleepDayResponse())
                    userLoginOut(result.code)
                }
            } catch (e: Exception) {
//                val stackTrace = e.stackTrace[0]
                LogUtils.e(TAG, "getSingleBloodOxygenListByDateRange e =$e" /*+ "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className*/)
                val result = Response<SingleBloodOxygenListResponse>(
                    "", "", HttpCommonAttributes.SERVER_ERROR, "", SingleBloodOxygenListResponse()
                )
                getSingleBloodOxygenListByDateRange.postValue(result)
            }
        }
    }

    private val outLineDataCount = 30

    //上传单次测量数据
    val upLoadSingleBloodOxygen = MutableLiveData("")
    fun upLoadSingleBloodOxygen(list: MutableList<SingleBloodOxygen>) {
        launchUI {
            val trackingLog = TrackingLog.getSerTypeTrack("日常数据上传服务器", "血氧单次测量上传", "infowear/bloodOxygenMeasure/bulk")
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (userId.isNotEmpty()) {
                    val dataList = UpLoadSingleBloodOxygenBean()
                    val lenght = if (list.size >= outLineDataCount) {
                        outLineDataCount
                    } else {
                        list.size
                    }
                    LogUtils.i(TAG, "upLoadSingleBloodOxygen list = ${list.toTypedArray().contentToString()}")
                    for (i in 0 until lenght) {
                        val bean = UpLoadSingleBloodOxygenBean.Data()
                        bean.userId = userId
                        bean.deviceType = list[i].deviceType
                        bean.deviceMac = list[i].deviceMac
                        bean.deviceVersion = list[i].deviceVersion
                        bean.deviceSyncTimestamp = list[i].timeStamp.toString()
                        bean.measureData = list[i].singleBloodOxygenData
                        bean.measureTime = DateUtils.getStringDate(list[i].measureTimestamp.trim().toLong(), DateUtils.TIME_YYYY_MM_DD_HHMMSS)
                        //如果存在测试数据测量时间一致的数据则不上报,并移除
                        var isCanAdd = true
                        val old = dataList.dataList.find { it.measureData == bean.measureData && it.measureTime == bean.measureTime }
                        if(old != null) {
                            isCanAdd = false
                            list[i].delete()
                        }
                        if(isCanAdd) {
                            dataList.dataList.add(bean)
                        }
                    }
                    LogUtils.i(TAG, "upLoadSingleBloodOxygen dataList = ${AppUtils.toSimpleJsonString(dataList)}")
                    trackingLog.serReqJson = AppUtils.toSimpleJsonString(dataList)
                    val result = MyRetrofitClient.service.upLoadSingleBloodOxygen(
                        JsonUtils.getRequestJson(
                            "upLoadSingleBloodOxygen",
                            dataList,
                            UpLoadSingleBloodOxygenBean::class.java
                        )
                    )
                    LogUtils.i(TAG, "upLoadSingleBloodOxygen result = $result")
                    trackingLog.endTime = TrackingLog.getNowString()
                    trackingLog.serResJson = AppUtils.toSimpleJsonString(result)
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, trackingLog)
                    if (result.code == HttpCommonAttributes.REQUEST_SUCCESS) {
                        for (i in 0 until list.size) {
                            var heartrate = SingleBloodOxygen()
                            heartrate.isUpLoad = true
                            heartrate.updateAll("userId = ? and measureTimestamp = ?", userId, list[i].measureTimestamp)
                        }
                    } else {
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, trackingLog.apply {
                            log += "\n日常数据上传服务器失败/超时"
                        }, "1426", true)
                    }
                    upLoadSingleBloodOxygen.postValue(result.code)
                    userLoginOut(result.code)
                }
            } catch (e: Exception) {
//                val stackTrace = e.stackTrace[0]
                LogUtils.e(TAG, "upLoadSingleBloodOxygen e =$e" /*+ "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className*/)
                upLoadSingleBloodOxygen.postValue(HttpCommonAttributes.SERVER_ERROR)
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, trackingLog.apply {
                    log += "\nupLoadSingleBloodOxygen e =$e\n日常数据上传服务器失败/超时"
                }, "1426", true)
            }
        }
    }

    suspend fun queryUnUploadSingleBloodOxygen(isSend: Boolean): MutableList<SingleBloodOxygen> {
        return suspendCancellableCoroutine<MutableList<SingleBloodOxygen>> {
            synchronized(DailyModel::class.java) {
                var list: MutableList<SingleBloodOxygen>? = null
                list = LitePal.where(
                    "isUpLoad = 0 and userId = ?",
                    SpUtils.getValue(SpUtils.USER_ID, "")
                ).limit(30).find(SingleBloodOxygen::class.java)
                if (list == null) list = mutableListOf()
                if (isSend) {
                    if (list.size > 0) {
                        upLoadSingleBloodOxygen(list)
                    }
                }
                it.resume(list)
            }
        }
    }
    //endregion

    //region 压力
    val savePressureData = MutableLiveData<Pressure>()
    fun savePressureData(data: ContinuousPressureBean?) {
        if (data == null) return
        launchUI {
            LogUtils.i(TAG, "savePressureData --> ${data.toString()}")
            val userId = SpUtils.getValue(SpUtils.USER_ID, "")
            val date = DateUtils.getStringDate(DateUtils.getLongTime(data.date, DateUtils.TIME_YYYY_MM_DD), DateUtils.TIME_YYYY_MM_DD)
            if (!DateUtils.isEffectiveDate(date)) return@launchUI
            LogUtils.i(TAG, "savePressureData --> In the valid period $date")
            val findData = LitePal.where(
                "userId = ? and date = ?",
                userId, date
            ).find(Pressure::class.java)
            var isChange = false
            if (findData.size > 0) {
                val value = data.pressureData.toTypedArray().contentToString()
                    .replace("[", "").replace("]", "").replace(" ", "")
                if (findData[0].pressureData != value) {
                    isChange = true
                    deleteAll(Pressure::class.java, "userId = ? and date = ?", userId, date)
                }
            } else {
                isChange = true
            }

            var bean = Pressure()
            if (isChange) {
                val list = mutableListOf<Pressure>()
                bean.userId = userId
                bean.date = date
                bean.continuousPressureFrequency = data.pressureFrequency.toString()
                bean.pressureData = data.pressureData.toTypedArray().contentToString()
                    .replace("[", "").replace("]", "").replace(" ", "")
                val maxMinAvg = calcMaxMinAvgValuePressure(bean.pressureData)
                bean.maxPressure = data.pressureDataMaxValue.toString()
                bean.minPressure = data.pressureDataMinValue.toString()
                bean.avgPressure = maxMinAvg[2]
                bean.timeStamp = System.currentTimeMillis().toString()
                bean.deviceType = Global.deviceType
                bean.deviceMac = Global.deviceMac
                bean.deviceVersion = Global.deviceVersion
                bean.appVersion = AppUtils.getAppVersionName()
                list.addAll(queryUnUploadPressure(false))

                val success = bean.saveUpdate(
                    Pressure::class.java,
                    "userId = ? and date = ?",
                    bean.userId,
                    bean.date
                )
                if (success) {
                    list.add(bean)
                }
            } else {
                bean = findData[0]
            }
            savePressureData.postValue(bean)
        }
    }

    // 压力 获取每日数据
    val getPressureDataByDay = MutableLiveData<Response<PressureResponse>>()
    fun getPressureDataByDay(date: String) {
        launchUI {
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (userId.isNotEmpty()) {
                    val result =
                        MyRetrofitClient.service.getPressureDataByDay(
                            JsonUtils.getRequestJson(
                                GetDailyDataByDayBean(userId, date),
                                GetDailyDataByDayBean::class.java
                            )
                        )
                    LogUtils.i(TAG, "getPressureDataByDay result = $result")
                    getPressureDataByDay.postValue(result)
                    userLoginOut(result.code)
                } else {
                    val result = Response("", "", HttpCommonAttributes.LOGIN_OUT, "", SleepDayResponse())
                    userLoginOut(result.code)
                }
            } catch (e: Exception) {
//                val stackTrace = e.stackTrace[0]
                LogUtils.e(TAG, "getPressureDataByDay e =$e"/* + "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className*/)
                val result = Response<PressureResponse>("", "", HttpCommonAttributes.SERVER_ERROR, "", PressureResponse())
                getPressureDataByDay.postValue(result)
            }
        }
    }

    val getPressureListByDateRange = MutableLiveData<Response<PressureListResponse>>()
    fun getPressureListByDateRange(beginDate: String, endDate: String) {
        launchUI {
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (userId.isNotEmpty()) {
                    val result =
                        MyRetrofitClient.service.getPressureListByDateRange(
                            JsonUtils.getRequestJson(
                                DataRangeNoTypeBean(userId, beginDate, endDate),
                                DataRangeNoTypeBean::class.java
                            )
                        )
                    LogUtils.i(TAG, "getPressureListByDateRange result = $result")
                    getPressureListByDateRange.postValue(result)
                    userLoginOut(result.code)
                } else {
                    val result = Response("", "", HttpCommonAttributes.LOGIN_OUT, "", SleepDayResponse())
                    userLoginOut(result.code)
                }
            } catch (e: Exception) {
//                val stackTrace = e.stackTrace[0]
                LogUtils.e(TAG, "getPressureListByDateRange e =$e" /*+ "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className*/)
                val result = Response<PressureListResponse>(
                    "", "", HttpCommonAttributes.SERVER_ERROR, "", PressureListResponse()
                )
                getPressureListByDateRange.postValue(result)
            }
        }
    }

    val getSinglePressureListByDateRange = MutableLiveData<Response<SinglePressureListResponse>>()
    fun getSinglePressureListByDateRange(beginDate: String, dateType: Int, endDate: String) {
        launchUI {
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (userId.isNotEmpty()) {
                    val result =
                        MyRetrofitClient.service.getSinglePressureListByDateRange(
                            JsonUtils.getRequestJson(
                                GetListByDataRangeBean(userId, beginDate, endDate, dateType),
                                GetListByDataRangeBean::class.java
                            )
                        )
                    LogUtils.i(TAG, "getSinglePressureListByDateRange result = $result")
                    getSinglePressureListByDateRange.postValue(result)
                    userLoginOut(result.code)
                } else {
                    val result = Response("", "", HttpCommonAttributes.LOGIN_OUT, "", SleepDayResponse())
                    userLoginOut(result.code)
                }
            } catch (e: Exception) {
//                val stackTrace = e.stackTrace[0]
                LogUtils.e(TAG, "getSinglePressureListByDateRange e =$e" /*+ "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className*/)
                val result = Response<SinglePressureListResponse>(
                    "", "", HttpCommonAttributes.SERVER_ERROR, "", SinglePressureListResponse()
                )
                getSinglePressureListByDateRange.postValue(result)
            }
        }
    }


    val upLoadPressureData = MutableLiveData("")

    //上传压力数据
    fun upLoadPressure(list: MutableList<Pressure>) {
        launchUI {
            val trackingLog = TrackingLog.getSerTypeTrack("日常数据上传服务器", "压力批量上传", "infowear/pressure/bulk")
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (userId.isNotEmpty()) {
                    val dataList = UpLoadPressureBean()
                    for (i in 0 until list.size) {
                        val bean = UpLoadPressureBean.Data()
                        bean.userId = userId
                        bean.date = list[i].date
                        bean.deviceType = list[i].deviceType
                        bean.deviceMac = list[i].deviceMac
                        bean.deviceVersion = list[i].deviceVersion
                        bean.deviceSyncTimestamp = list[i].timeStamp.toString()
                        bean.pressureFrequency = list[i].continuousPressureFrequency
                        bean.pressureData = list[i].pressureData
                        bean.maxPressure = list[i].maxPressure
                        bean.minPressure = list[i].minPressure
                        bean.avgPressure = list[i].avgPressure
                        dataList.dataList.add(bean)
                    }
                    trackingLog.serReqJson = AppUtils.toSimpleJsonString(dataList)
                    val result = MyRetrofitClient.service.upLoadPressureData(
                        JsonUtils.getRequestJson(
                            "upLoadPressure",
                            dataList,
                            UpLoadPressureBean::class.java
                        )
                    )
                    LogUtils.i(TAG, "upLoadPressure result = $result")
                    trackingLog.endTime = TrackingLog.getNowString()
                    trackingLog.serResJson = AppUtils.toSimpleJsonString(result)
                    if (result.code == HttpCommonAttributes.REQUEST_SUCCESS) {
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, trackingLog)
                        for (i in 0 until list.size) {
                            var pressure = Pressure()
                            pressure.isUpLoad = true
                            pressure.updateAll("userId = ? and date = ?", userId, list[i].date)
                        }
                    } else {
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, trackingLog.apply {
                            log += "\n日常数据上传服务器失败/超时"
                        }, "1426", true)
                    }
                    upLoadPressureData.postValue(result.code)
                    userLoginOut(result.code)
                }
            } catch (e: Exception) {
//                val stackTrace = e.stackTrace[0]
                LogUtils.e(TAG, "upLoadPressure e =$e" /*+ "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className*/)
                upLoadPressureData.postValue(HttpCommonAttributes.SERVER_ERROR)
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, trackingLog.apply {
                    log += "\nupLoadPressure e =$e\n日常数据上传服务器失败/超时"
                }, "1426", true)
            }
        }
    }

    suspend fun queryUnUploadPressure(isSend: Boolean): MutableList<Pressure> {
        return suspendCancellableCoroutine<MutableList<Pressure>> {
            synchronized(DailyModel::class.java) {
                var list: MutableList<Pressure>? = null
                list = LitePal.where(
                    "isUpLoad = 0 and userId = ?",
                    SpUtils.getValue(SpUtils.USER_ID, "")
                ).limit(10).find(Pressure::class.java)
                if (list == null) list = mutableListOf()
                if (isSend) {
                    if (list.size > 0) {
                        upLoadPressure(list)
                    }
                }
                it.resume(list)
            }
        }
    }
    //endregion

    //region 压力单次测量
    val saveSinglePressureData = MutableLiveData<MutableList<SinglePressure>>()
    fun saveSinglePressureData(data: OfflinePressureDataBean?) {
        if (data == null) return
        launchUI {
            LogUtils.i(TAG, "saveSinglePressureData --> $data")
            val userId = SpUtils.getValue(SpUtils.USER_ID, "")
            val listTemp = mutableListOf<SinglePressure>()
            val list = mutableListOf<SinglePressure>()
            //去重
            data.list = data.list.distinct()
            for (i in data.list.indices) {
                val bean = SinglePressure()
                bean.userId = userId
                val findData = LitePal.where(
                    "userId = ? and measureTimestamp = ?",
                    userId, (data.list[i].measureTimestamp.toLong() * 1000).toString()
                ).find(SinglePressure::class.java)
                if (findData.size > 0) {
                    for (oldItem in findData) {
                        //只要用户id 测试时间一致，就把旧数据移除
                        oldItem.delete()
                    }
                }
                bean.singlePressureData = data.list[i].measureData.toString()
                bean.measureTimestamp = (data.list[i].measureTimestamp.toLong() * 1000).toString()
                bean.timeStamp = System.currentTimeMillis().toString()
                bean.deviceType = Global.deviceType
                bean.deviceMac = Global.deviceMac
                bean.deviceVersion = Global.deviceVersion
                bean.appVersion = AppUtils.getAppVersionName()
                listTemp.add(bean)
            }
            list.addAll(queryUnUploadSinglePressure(false))
            val result = LitePal.saveAll(listTemp)
            if (result) {
                list.addAll(listTemp)
            }
            saveSinglePressureData.postValue(list)
        }
    }

    val getSinglePressureDataByDay = MutableLiveData<Response<SinglePressureResponse>>()
    fun getSinglePressureDataByDay(date: String) {
        launchUI {
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (userId.isNotEmpty()) {
                    val result =
                        MyRetrofitClient.service.getSinglePressureDataByDay(
                            JsonUtils.getRequestJson(
                                GetDailyDataByDayBean(userId, date),
                                GetDailyDataByDayBean::class.java
                            )
                        )
                    LogUtils.i(TAG, "getSinglePressureDataByDay result = $result")
                    getSinglePressureDataByDay.postValue(result)
                    userLoginOut(result.code)
                } else {
                    val result = Response("", "", HttpCommonAttributes.LOGIN_OUT, "", SleepDayResponse())
                    userLoginOut(result.code)
                }
            } catch (e: Exception) {
//                val stackTrace = e.stackTrace[0]
                LogUtils.e(TAG, "getSinglePressureDataByDay e =$e"/* + "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className*/)
                val result = Response<SinglePressureResponse>("", "", HttpCommonAttributes.SERVER_ERROR, "", SinglePressureResponse())
                getSinglePressureDataByDay.postValue(result)
            }
        }
    }

    //上传心率单次测量数据
    val upLoadSinglePressure = MutableLiveData("")
    fun upLoadSinglePressure(list: MutableList<SinglePressure>) {
        launchUI {
            val trackingLog = TrackingLog.getSerTypeTrack("日常数据上传服务器", "压力单次测量 批量上传", "infowear/pressureMeasure/bulk")
            try {
                trackingLog.log = GsonUtils.toJson(list)
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (userId.isNotEmpty()) {
                    val dataList = UpLoadSinglePressureBean()
                    val lenght = if (list.size >= outLineDataCount) {
                        outLineDataCount
                    } else {
                        list.size
                    }
                    LogUtils.i(TAG, "upLoadSinglePressure list = ${list.toTypedArray().contentToString()}")
                    for (i in 0 until lenght) {
                        try {
                            val bean = UpLoadSinglePressureBean.Data()
                            bean.userId = userId
                            bean.deviceType = list[i].deviceType
                            bean.deviceMac = list[i].deviceMac
                            bean.deviceVersion = list[i].deviceVersion
                            bean.deviceSyncTimestamp = list[i].timeStamp.toString()
                            bean.measureData = list[i].singlePressureData
                            //java.lang.NumberFormatException: For input string TODO
                            bean.measureTime = DateUtils.getStringDate(list[i].measureTimestamp.trim().toLong(), DateUtils.TIME_YYYY_MM_DD_HHMMSS)
                            //如果存在测试数据测量时间一致的数据则不上报,并移除
                            var isCanAdd = true
                            val old = dataList.dataList.find { it.measureData == bean.measureData && it.measureTime == bean.measureTime }
                            if(old != null) {
                                isCanAdd = false
                                list[i].delete()
                            }
                            if(isCanAdd) {
                                dataList.dataList.add(bean)
                            }
                        } catch (e: NumberFormatException) {
                            e.printStackTrace()
                        }
                    }
                    trackingLog.serReqJson = AppUtils.toSimpleJsonString(dataList)
                    LogUtils.i(TAG, "upLoadSinglePressure dataList = ${dataList.dataList.toTypedArray().contentToString()}")
                    val result = MyRetrofitClient.service.upLoadSinglePressureData(
                        JsonUtils.getRequestJson(
                            "upLoadSinglePressure",
                            dataList,
                            UpLoadSinglePressureBean::class.java
                        )
                    )
                    LogUtils.i(TAG, "upLoadSinglePressure result = $result")
                    trackingLog.endTime = TrackingLog.getNowString()
                    trackingLog.serResJson = AppUtils.toSimpleJsonString(result)
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, trackingLog)
                    if (result.code == HttpCommonAttributes.REQUEST_SUCCESS) {
                        for (i in 0 until list.size) {
                            var heartrate = SinglePressure()
                            heartrate.isUpLoad = true
                            heartrate.updateAll("userId = ? and measureTimestamp = ?", userId, list[i].measureTimestamp)
                        }
                    } else {
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, trackingLog.apply {
                            log += "\n日常数据上传服务器失败/超时"
                        }, "1426", true)
                    }
                    upLoadSinglePressure.postValue(result.code)
                    userLoginOut(result.code)
                }
            } catch (e: Exception) {
//                val stackTrace = e.stackTrace[0]
                LogUtils.e(TAG, "upLoadSinglePressure e =$e" /*+ "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className*/)
                upLoadSinglePressure.postValue(HttpCommonAttributes.SERVER_ERROR)
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, trackingLog.apply {
                    log += "\nupLoadSinglePressure e =$e\n日常数据上传服务器失败/超时"
                }, "1426", true)
            }
        }
    }

    suspend fun queryUnUploadSinglePressure(isSend: Boolean): MutableList<SinglePressure> {
        return suspendCancellableCoroutine<MutableList<SinglePressure>> {
            synchronized(DailyModel::class.java) {
                var list: MutableList<SinglePressure>? = null
                list = LitePal.where(
                    "isUpLoad = 0 and userId = ?",
                    SpUtils.getValue(SpUtils.USER_ID, "")
                ).limit(30).find(SinglePressure::class.java)
                if (list == null) list = mutableListOf()
                if (isSend) {
                    if (list.size > 0) {
                        upLoadSinglePressure(list)
                    }
                }
                it.resume(list)
            }
        }
    }
    //endregion

    //region 有效站立
    val saveEffectiveStandData = MutableLiveData<EffectiveStand>()
    fun saveEffectiveStandData(data: EffectiveStandingBean) {
        launchUI {
            LogUtils.i(TAG, "saveEffectiveStandData --> ${data.toString()}")
            val userId = SpUtils.getValue(SpUtils.USER_ID, "")
            val date = DateUtils.getStringDate(DateUtils.getLongTime(data.date, DateUtils.TIME_YYYY_MM_DD), DateUtils.TIME_YYYY_MM_DD)
            if (!DateUtils.isEffectiveDate(date)) return@launchUI
            LogUtils.i(TAG, "saveEffectiveStandData --> In the valid period $date")

            val findData = LitePal.where(
                "userId = ? and date = ?",
                userId, date
            ).find(EffectiveStand::class.java)
            var isChange = false
            if (findData.size > 0) {
                val value = data.effectiveStandingData.toTypedArray().contentToString().replace("[", "")
                    .replace("]", "").replace(" ", "")
                if (findData[0].effectiveStandingData != value) {
                    isChange = true
                    deleteAll(EffectiveStand::class.java, "userId = ? and date = ?", userId, date)
                }
            } else {
                isChange = true
            }

            var bean = EffectiveStand()
            if (isChange) {
                val list = mutableListOf<EffectiveStand>()
                bean.userId = userId
                bean.date = date
                bean.effectiveStandFrequency = data.effectiveStandingFrequency.toString()
                bean.effectiveStandingData = data.effectiveStandingData.toTypedArray().contentToString()
                    .replace("[", "").replace("]", "").replace(" ", "")
                bean.timeStamp = System.currentTimeMillis().toString()
                bean.deviceType = Global.deviceType
                bean.deviceMac = Global.deviceMac
                bean.deviceVersion = Global.deviceVersion
                bean.appVersion = AppUtils.getAppVersionName()
                list.addAll(queryUnUploadEffectiveStand(false))

                val success = bean.saveUpdate(
                    EffectiveStand::class.java,
                    "userId = ? and date = ?",
                    bean.userId,
                    bean.date
                )
                if (success) {
                    list.add(bean)
//                    upLoadEffectiveStand(list)
                }

            } else {
                bean = findData[0]
            }
            saveEffectiveStandData.postValue(bean)
        }
    }

    val getEffectiveStandDataByDay = MutableLiveData<Response<EffectiveStandResponse>>()
    fun getEffectiveStandDataByDay(date: String) {
        launchUI {
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (userId.isNotEmpty()) {
                    val result =
                        MyRetrofitClient.service.getEffectiveStandDataByDay(
                            JsonUtils.getRequestJson(
                                GetDailyDataByDayBean(userId, date),
                                GetDailyDataByDayBean::class.java
                            )
                        )
                    LogUtils.i(TAG, "getEffectiveStandDataByDay result = $result")
                    getEffectiveStandDataByDay.postValue(result)
                    userLoginOut(result.code)
                } else {
                    val result = Response("", "", HttpCommonAttributes.LOGIN_OUT, "", SleepDayResponse())
                    userLoginOut(result.code)
                }
            } catch (e: Exception) {
//                val stackTrace = e.stackTrace[0]
                LogUtils.e(TAG, "getEffectiveStandDataByDay e =$e"/* + "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className*/)
                val result = Response("", "", HttpCommonAttributes.SERVER_ERROR, "", EffectiveStandResponse())
                getEffectiveStandDataByDay.postValue(result)
            }
        }
    }

    val getEffectiveStandListByDateRange = MutableLiveData<Response<EffectiveStandListResponse>>()
    fun getEffectiveStandListByDateRange(beginDate: String, endDate: String) {
        launchUI {
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (userId.isNotEmpty()) {
                    val result =
                        MyRetrofitClient.service.getEffectiveStandListByDateRange(
                            JsonUtils.getRequestJson(
                                DataRangeNoTypeBean(userId, beginDate, endDate),
                                DataRangeNoTypeBean::class.java
                            )
                        )
                    LogUtils.i(TAG, "getEffectiveStandListByDateRange result = $result")
                    getEffectiveStandListByDateRange.postValue(result)
                    userLoginOut(result.code)
                } else {
                    val result = Response("", "", HttpCommonAttributes.LOGIN_OUT, "", SleepDayResponse())
                    userLoginOut(result.code)
                }
            } catch (e: Exception) {
//                val stackTrace = e.stackTrace[0]
                LogUtils.e(TAG, "getEffectiveStandListByDateRange e =$e" /*+ "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className*/)
                val result = Response(
                    "", "", HttpCommonAttributes.SERVER_ERROR, "", EffectiveStandListResponse()
                )
                getEffectiveStandListByDateRange.postValue(result)
            }
        }
    }

    val upLoadEffectiveStand = MutableLiveData("")
    fun upLoadEffectiveStand(list: MutableList<EffectiveStand>) {
        launchUI {
            val trackingLog = TrackingLog.getSerTypeTrack("日常数据上传服务器", "有效站立 批量上传", "infowear/effectiveStanding/bulk")
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (userId.isNotEmpty()) {
                    val dataList = UpLoadEffectiveStandBean()
                    for (i in 0 until list.size) {
                        val bean = UpLoadEffectiveStandBean.Data()
                        bean.userId = userId
                        bean.date = list[i].date
                        bean.deviceType = list[i].deviceType
                        bean.deviceMac = list[i].deviceMac
                        bean.deviceVersion = list[i].deviceVersion
                        bean.deviceSyncTimestamp = list[i].timeStamp.toString()
                        bean.effectiveStandingData = list[i].effectiveStandingData
                        bean.effectiveStandingFrequency = list[i].effectiveStandFrequency
                        dataList.dataList.add(bean)
                    }
                    trackingLog.serReqJson = AppUtils.toSimpleJsonString(dataList)
                    val result = MyRetrofitClient.service.upLoadEffectiveStand(
                        JsonUtils.getRequestJson(
                            "upLoadEffectiveStand",
                            dataList,
                            UpLoadEffectiveStandBean::class.java
                        )
                    )
                    LogUtils.i(TAG, "upLoadEffectiveStand result = $result")
                    trackingLog.endTime = TrackingLog.getNowString()
                    trackingLog.serResJson = AppUtils.toSimpleJsonString(result)
                    if (result.code == HttpCommonAttributes.REQUEST_SUCCESS) {
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, trackingLog)
                        for (i in 0 until list.size) {
                            var effectivestand = EffectiveStand()
                            effectivestand.isUpLoad = true
                            effectivestand.updateAll("userId = ? and date = ?", userId, list[i].date)
                        }
                    } else {
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, trackingLog.apply {
                            log += "\n日常数据上传服务器失败/超时"
                        }, "1426", true)
                    }
                    upLoadEffectiveStand.postValue(result.code)
                    userLoginOut(result.code)
                }
            } catch (e: Exception) {
//                val stackTrace = e.stackTrace[0]
                LogUtils.e(TAG, "upLoadEffectiveStand e =$e" /*+ "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className*/)
                upLoadEffectiveStand.postValue(HttpCommonAttributes.SERVER_ERROR)
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, trackingLog.apply {
                    log += "\nupLoadEffectiveStand e =$e\n日常数据上传服务器失败/超时"
                }, "1426", true)
            }
        }
    }

    suspend fun queryUnUploadEffectiveStand(isSend: Boolean): MutableList<EffectiveStand> {
        return suspendCancellableCoroutine<MutableList<EffectiveStand>> {
            synchronized(DailyModel::class.java) {
                var list: MutableList<EffectiveStand>? = null
                list = LitePal.where(
                    "isUpLoad = 0 and userId = ?",
                    SpUtils.getValue(SpUtils.USER_ID, "")
                ).limit(10).find(EffectiveStand::class.java)
                if (list == null) list = mutableListOf()
                if (isSend) {
                    if (list.size > 0) {
                        upLoadEffectiveStand(list)
                    }
                }
                it.resume(list)
            }
        }
    }
    //endregion

    private var mLastClickTime: Long = 0

    /**
     * 上传手表健康数据
     */
    fun upLoadFitnessData() {
        val nowTime = System.currentTimeMillis()
        if (abs(nowTime - mLastClickTime) < 3000) {
            return
        }
        mLastClickTime = nowTime
        launchUI {
            try {
                LogUtils.e(TAG, "start upLoadFitnessData")
                //上传日常数据
                queryUnUploadDailyData(true)
                //上传睡眠数据
                queryUnUploadSleepData(true)
                //上传心率数据
                queryUnUploadHeartRate(true)
                //上传心率单次测量数据
                queryUnUploadSingleHeartRate()
                //上传血氧饱和度
                queryUnUploadBloodOxygen(true)
                //上传离线血氧
                queryUnUploadSingleBloodOxygen(true)
                //上传有效站立
                queryUnUploadEffectiveStand(true)
                //上传压力数据
                queryUnUploadPressure(true)
                //上传离线压力测量数据
                queryUnUploadSinglePressure(true)
            } catch (e: Exception) {
                e.printStackTrace()
                LogUtils.e(TAG, "upLoadFitnessData:$e")
            }
        }
    }

    /***********************************************************************************************
     *      获取最近一次记录数据
     **********************************************************************************************/
//    val getDailyLatelyData = MutableLiveData<Response<DailyLatelyResponse>>()
    var syncCount = 0;
    suspend fun getDailyLatelyData(date: String) {
        try {
            val userId = SpUtils.getValue(SpUtils.USER_ID, "")
            if (userId.isNotEmpty()) {

                val result = MyRetrofitClient.service.getDailyLatelyData(
                    JsonUtils.getRequestJson(
                        LatelyDataBean(userId, date),
                        LatelyDataBean::class.java
                    )
                )
                LogUtils.i(TAG, "getDailyLatelyData result = $result")
                syncCount++
                when (result.code) {
                    HttpCommonAttributes.REQUEST_SUCCESS -> {
                        if (result.data.date == date) {
                            val stepPosition =
                                Global.healthyItemList.indexOfFirst { it.topTitleText == BaseApplication.mContext.getString(R.string.healthy_sports_list_step) }
                            val caloriesPosition =
                                Global.healthyItemList.indexOfFirst { it.topTitleText == BaseApplication.mContext.getString(R.string.healthy_sports_list_calories) }
                            val distancePosition =
                                Global.healthyItemList.indexOfFirst { it.topTitleText == BaseApplication.mContext.getString(R.string.healthy_sports_list_distance) }
                            if (stepPosition != -1) {
                                Global.healthyItemList[stepPosition].context = result.data.totalStep
                            }

                            if (caloriesPosition != -1) {
                                Global.healthyItemList[caloriesPosition].context = result.data.totalCalorie
                            }

                            if (distancePosition != -1) {
                                Global.healthyItemList[distancePosition].context = result.data.totalDistance
                            }
                        }
                    }

                    else -> {
                        val stepPosition =
                            Global.healthyItemList.indexOfFirst { it.topTitleText == BaseApplication.mContext.getString(R.string.healthy_sports_list_step) }
                        val caloriesPosition =
                            Global.healthyItemList.indexOfFirst { it.topTitleText == BaseApplication.mContext.getString(R.string.healthy_sports_list_calories) }
                        val distancePosition =
                            Global.healthyItemList.indexOfFirst { it.topTitleText == BaseApplication.mContext.getString(R.string.healthy_sports_list_distance) }
                        if (stepPosition != -1) {
                            Global.healthyItemList[stepPosition].context = "0"
                        }

                        if (caloriesPosition != -1) {
                            Global.healthyItemList[caloriesPosition].context = "0"
                        }

                        if (distancePosition != -1) {
                            Global.healthyItemList[distancePosition].context = "0.00"
                        }
                    }
                }
//                    getDailyLatelyData.postValue(result)
                userLoginOut(result.code)
            } else {
                val result = Response("", "", HttpCommonAttributes.LOGIN_OUT, "", SleepDayResponse())
                userLoginOut(result.code)
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "getDailyLatelyData e =$e")
            val result = Response("", "", HttpCommonAttributes.SERVER_ERROR, "", DailyLatelyResponse())
//                getDailyLatelyData.postValue(result)
        }
    }

    //    val getSleepLatelyData = MutableLiveData<Response<SleepLatelyResponse>>()
    suspend fun getSleepLatelyData(date: String) {
        try {
            val userId = SpUtils.getValue(SpUtils.USER_ID, "")
            if (userId.isNotEmpty()) {

                val result = MyRetrofitClient.service.getSleepLatelyData(
                    JsonUtils.getRequestJson(
                        LatelyDataBean(userId, date),
                        LatelyDataBean::class.java
                    )
                )
                LogUtils.i(TAG, "getSleepLatelyData result = $result")
                syncCount++
                when (result.code) {
                    HttpCommonAttributes.REQUEST_SUCCESS -> {
                        if (result.data.date == date) {
                            val position =
                                Global.healthyItemList.indexOfFirst { it.topTitleText == BaseApplication.mContext.getString(R.string.healthy_sports_list_sleep) }
                            if (position != -1) {
                                Global.healthyItemList[position].context = result.data.sleepDuration
                            }
                        }
                    }

                    else -> {
                        val position =
                            Global.healthyItemList.indexOfFirst { it.topTitleText == BaseApplication.mContext.getString(R.string.healthy_sports_list_sleep) }
                        if (position != -1) {
                            Global.healthyItemList[position].context = "0"
                        }
                    }
                }

//                    getSleepLatelyData.postValue(result)
                userLoginOut(result.code)
            } else {
                val result = Response("", "", HttpCommonAttributes.LOGIN_OUT, "", SleepLatelyResponse())
                userLoginOut(result.code)
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "getSleepLatelyData e =$e")
            val result = Response("", "", HttpCommonAttributes.SERVER_ERROR, "", SleepLatelyResponse())
//                getSleepLatelyData.postValue(result)
        }
    }

    //    val getHeartRateLatelyData = MutableLiveData<Response<HeartRateLatelyResponse>>()
    suspend fun getHeartRateLatelyData(date: String) {
        try {
            val userId = SpUtils.getValue(SpUtils.USER_ID, "")
            if (userId.isNotEmpty()) {

                val result = MyRetrofitClient.service.getHeartRateData(
                    JsonUtils.getRequestJson(
                        LatelyDataBean(userId, date),
                        LatelyDataBean::class.java
                    )
                )
                LogUtils.i(TAG, "getHeartRateLatelyData result = $result")
                syncCount++
                when (result.code) {
                    HttpCommonAttributes.REQUEST_SUCCESS -> {
                        if (result.data.date == date) {
                            val position =
                                Global.healthyItemList.indexOfFirst { it.topTitleText == BaseApplication.mContext.getString(R.string.healthy_sports_list_heart) }
                            if (position != -1) {
                                Global.healthyItemList[position].context = result.data.lastHeartRate
                            }
                        }
                    }

                    else -> {
                        val position =
                            Global.healthyItemList.indexOfFirst { it.topTitleText == BaseApplication.mContext.getString(R.string.healthy_sports_list_heart) }
                        if (position != -1) {
                            Global.healthyItemList[position].context = "0"
                        }
                    }
                }
//                    getHeartRateLatelyData.postValue(result)
                userLoginOut(result.code)
            } else {
                val result = Response("", "", HttpCommonAttributes.LOGIN_OUT, "", HeartRateLatelyResponse())
                userLoginOut(result.code)
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "getHeartRateLatelyData e =$e")
            val result = Response("", "", HttpCommonAttributes.SERVER_ERROR, "", HeartRateLatelyResponse())
//                getHeartRateLatelyData.postValue(result)
        }
    }

    //    val getBloodOxygenLatelyData = MutableLiveData<Response<BloodOxygenLatelyResponse>>()
    suspend fun getBloodOxygenLatelyData(date: String) {
        try {
            val userId = SpUtils.getValue(SpUtils.USER_ID, "")
            if (userId.isNotEmpty()) {

                val result = MyRetrofitClient.service.getBloodOxygenLatelyData(
                    JsonUtils.getRequestJson(
                        LatelyDataBean(userId, date),
                        LatelyDataBean::class.java
                    )
                )
                LogUtils.i(TAG, "getBloodOxygenLatelyData result = $result")
                syncCount++
                when (result.code) {
                    HttpCommonAttributes.REQUEST_SUCCESS -> {
                        if (result.data.date == date) {
                            val position = Global.healthyItemList.indexOfFirst { it.topTitleText == BaseApplication.mContext.getString(R.string.healthy_sports_list_blood_oxygen) }
                            if (position != -1) {
                                Global.healthyItemList[position].context = result.data.lastbloodOxygen
                            }
                        }
                    }

                    else -> {
                        val position =
                            Global.healthyItemList.indexOfFirst { it.topTitleText == BaseApplication.mContext.getString(R.string.healthy_sports_list_blood_oxygen) }
                        if (position != -1) {
                            Global.healthyItemList[position].context = "0"
                        }
                    }
                }
//                    getBloodOxygenLatelyData.postValue(result)
                userLoginOut(result.code)
            } else {
                val result = Response("", "", HttpCommonAttributes.LOGIN_OUT, "", BloodOxygenLatelyResponse())
                userLoginOut(result.code)
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "getBloodOxygenLatelyData e =$e")
            val result = Response("", "", HttpCommonAttributes.SERVER_ERROR, "", BloodOxygenLatelyResponse())
//                getBloodOxygenLatelyData.postValue(result)
        }
    }

    suspend fun getSingleBloodOxygenLatelyData(date: String) {
        try {
            val userId = SpUtils.getValue(SpUtils.USER_ID, "")
            if (userId.isNotEmpty()) {

                val result = MyRetrofitClient.service.getSingleBloodOxygenLatelyData(
                    JsonUtils.getRequestJson(
                        LatelyDataBean(userId, date),
                        LatelyDataBean::class.java
                    )
                )
                LogUtils.i(TAG, "getSingleBloodOxygenLatelyData result = $result")
                syncCount++
                when (result.code) {
                    HttpCommonAttributes.REQUEST_SUCCESS -> {
                        val position =
                            Global.healthyItemList.indexOfFirst { it.topTitleText == BaseApplication.mContext.getString(R.string.healthy_sports_list_blood_oxygen) }
                        if (position != -1) {
                            Global.healthyItemList[position].context = result.data.measureData
                        }
                    }

                    else -> {
                        val position = Global.healthyItemList.indexOfFirst {
                            it.topTitleText == BaseApplication.mContext.getString(R.string.healthy_sports_list_blood_oxygen)
                        }
                        if (position != -1) {
                            Global.healthyItemList[position].context = "0"
                        }
                    }
                }
//                    getBloodOxygenLatelyData.postValue(result)
                userLoginOut(result.code)
            } else {
                val result = Response("", "", HttpCommonAttributes.LOGIN_OUT, "", BloodOxygenLatelyResponse())
                userLoginOut(result.code)
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "getBloodOxygenLatelyData e =$e")
            val result = Response("", "", HttpCommonAttributes.SERVER_ERROR, "", BloodOxygenLatelyResponse())
//                getBloodOxygenLatelyData.postValue(result)
        }
    }

    //    val getEffectiveStandingLatelyData = MutableLiveData<Response<EffectiveStandLatelyResponse>>()
    suspend fun getEffectiveStandingLatelyData(date: String) {
        try {
            val userId = SpUtils.getValue(SpUtils.USER_ID, "")
            if (userId.isNotEmpty()) {

                val result = MyRetrofitClient.service.getEffectiveStandingLatelyData(
                    JsonUtils.getRequestJson(
                        LatelyDataBean(userId, date),
                        LatelyDataBean::class.java
                    )
                )
                LogUtils.i(TAG, "getEffectiveStandingLatelyData result = $result")
                syncCount++
                when (result.code) {
                    HttpCommonAttributes.REQUEST_SUCCESS -> {
                        if (result.data.date == date) {
                            val time = calcLatelyData(result.data.effectiveStandingData.trim().split(",").toList())
                            val dateTime = DateUtils.getLongTime("${result.data.date}".trim(), DateUtils.TIME_YYYY_MM_DD)
                            val position =
                                Global.healthyItemList.indexOfFirst { it.topTitleText == BaseApplication.mContext.getString(R.string.healthy_sports_list_effective_stand) }
                            if (position != -1) {
                                Global.healthyItemList[position].context = time[0]
                                val isSystem24HourFormat = DateFormat.is24HourFormat(BaseApplication.mContext)
                                if (isSystem24HourFormat) {
                                    Global.healthyItemList[position].subTitleText = DateUtils.getStringDate(dateTime, "MM/dd")
                                } else {
                                    Global.healthyItemList[position].subTitleText = DateUtils.getStringDate(dateTime, DateUtils.MMdd_12)
                                }
                            }
                        }
                    }

                    else -> {
                        val position =
                            Global.healthyItemList.indexOfFirst { it.topTitleText == BaseApplication.mContext.getString(R.string.healthy_sports_list_effective_stand) }
                        if (position != -1) {
                            Global.healthyItemList[position].context = "0"
                        }
                    }
                }
//                    getEffectiveStandingLatelyData.postValue(result)
                userLoginOut(result.code)
            } else {
                val result = Response("", "", HttpCommonAttributes.LOGIN_OUT, "", EffectiveStandLatelyResponse())
                userLoginOut(result.code)
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "getEffectiveStandingLatelyData e =$e")
            val result = Response("", "", HttpCommonAttributes.SERVER_ERROR, "", EffectiveStandLatelyResponse())
//                getEffectiveStandingLatelyData.postValue(result)
        }
    }

    //val getSingleLastPressureData = MutableLiveData<Response<SinglePressureLastResponse>>()
    fun getSingleLastPressureData(date: String) {
        launchUI {
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (userId.isNotEmpty()) {
                    val result =
                        MyRetrofitClient.service.getSingleLastPressureData(
                            JsonUtils.getRequestJson(
                                GetDailyDataByDayBean(userId, date),
                                GetDailyDataByDayBean::class.java
                            )
                        )
                    LogUtils.i(TAG, "getSingleLastPressureData result = $result")
                    //getSingleLastPressureData.postValue(result)
                    syncCount++
                    when (result.code) {
                        HttpCommonAttributes.REQUEST_SUCCESS -> {
                            val position = Global.healthyItemList.indexOfFirst { it.topTitleText == BaseApplication.mContext.getString(R.string.healthy_pressure_title) }
                            if (position != -1) {
                                Global.healthyItemList[position].context = result.data.measureData
                            }
                        }

                        else -> {
                            val position =
                                Global.healthyItemList.indexOfFirst { it.topTitleText == BaseApplication.mContext.getString(R.string.healthy_pressure_title) }
                            if (position != -1) {
                                Global.healthyItemList[position].context = "0"
                            }
                        }
                    }
                    userLoginOut(result.code)
                } else {
                    val result = Response("", "", HttpCommonAttributes.LOGIN_OUT, "", SleepDayResponse())
                    userLoginOut(result.code)
                }
            } catch (e: Exception) {
//                val stackTrace = e.stackTrace[0]
                LogUtils.e(TAG, "getSingleLastPressureData e =$e"/* + "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className*/)
                val result = Response<SinglePressureLastResponse>("", "", HttpCommonAttributes.SERVER_ERROR, "", SinglePressureLastResponse())
                //getSingleLastPressureData.postValue(result)
            }
        }
    }

    fun getPressureLatelyData(date: String) {
        launchUI {
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (userId.isNotEmpty()) {
                    val result =
                        MyRetrofitClient.service.getLastPressureData(
                            JsonUtils.getRequestJson(
                                GetDailyDataByDayBean(userId, date),
                                GetDailyDataByDayBean::class.java
                            )
                        )
                    LogUtils.i(TAG, "getPressureLatelyData result = $result")
                    syncCount++
                    when (result.code) {
                        HttpCommonAttributes.REQUEST_SUCCESS -> {
                            if (result.data.date == date) {
                                val position =
                                    Global.healthyItemList.indexOfFirst { it.topTitleText == BaseApplication.mContext.getString(R.string.healthy_pressure_title) }
                                if (position != -1) {
                                    Global.healthyItemList[position].context = result.data.lastPressure
                                }
                            }
                        }

                        else -> {
                            val position =
                                Global.healthyItemList.indexOfFirst { it.topTitleText == BaseApplication.mContext.getString(R.string.healthy_pressure_title) }
                            if (position != -1) {
                                Global.healthyItemList[position].context = "0"
                            }
                        }
                    }
                    userLoginOut(result.code)
                } else {
                    val result = Response("", "", HttpCommonAttributes.LOGIN_OUT, "", SleepDayResponse())
                    userLoginOut(result.code)
                }
            } catch (e: Exception) {
//                val stackTrace = e.stackTrace[0]
                LogUtils.e(TAG, "getSingleLastPressureData e =$e"/* + "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className*/)
                val result = Response<SinglePressureLastResponse>("", "", HttpCommonAttributes.SERVER_ERROR, "", SinglePressureLastResponse())
            }
        }
    }

    private fun calcLatelyData(list: List<String>): Array<String> {
        var position = -1
        for (i in list.indices) {
            if (list[i].trim() != "" && list[i].trim() != "0") {
                position = i
            }
        }
        var time = ""
        val stringArray = arrayOf<String>("", "")
        if (position != -1) {
            time = if (position < 10) {
                stringArray[1] = "0$position:00"
                if (position + 1 == 10) {
                    "0${position}:00 - 10:00"
                } else {
                    "0${position}:00 - 0${position + 1}:00"
                }
            } else {
                stringArray[1] = "$position:00"
                if ((position + 1) >= 24) {
                    "$position:00 - 00:00"
                } else {
                    "$position:00 - ${position + 1}:00"
                }
            }
            stringArray[0] = time

        }
        return stringArray
    }

    private var lastrefreshTime: Long = 0
    fun refreshHealthyFromOnline() {
        val currentRefTime = System.currentTimeMillis()
        //限制1s内重复刷新
        if (abs(currentRefTime - lastrefreshTime) <= 1000) {
            return
        }
        lastrefreshTime = currentRefTime
        launchUI {
            withTimeoutOrNull(30 * 1000) {
                LogUtils.i(TAG, "refreshHealthyFromOnline")
                syncCount = 0
                val request1 = async { getDailyLatelyData(getCurDate()) }
                val request2 = async { getSleepLatelyData(getCurDate()) }
                val request3 = async { getHeartRateLatelyData(getCurDate()) }
                val request4 = async { getSingleBloodOxygenLatelyData(getCurDate()) }
                val request5 = async { getEffectiveStandingLatelyData(getCurDate()) }
                val request6 = async {
                    if (Global.deviceSettingBean != null && Global.deviceSettingBean!!.dataRelated.offline_pressure) {
                        getSingleLastPressureData(getCurDate())
                    } else if (Global.deviceSettingBean != null && Global.deviceSettingBean!!.dataRelated.pressure) {
                        getPressureLatelyData(getCurDate())
                    }
                }
                com.blankj.utilcode.util.LogUtils.d("refreshHealthyFromOnline start await")
                request1.await()
                request2.await()
                request3.await()
                request4.await()
                request5.await()
                request6.await()
                //刷新完成低于5s太快，延迟一会再完成
                if (abs(System.currentTimeMillis() - lastrefreshTime) <= 5000) {
                    delay(3000)
                }
            }
            com.blankj.utilcode.util.LogUtils.d("refreshHealthyFromOnline await Finish or Timeout syncCount:$syncCount")
            RefreshHealthyMainData.postValue(true)
        }
    }

    private fun getCurDate(): String {
        return DateUtils.getStringDate(System.currentTimeMillis(), "yyyy-MM-dd")
    }

    /**
     * 保存设备产品图
     */
    private var isSaveDeviceIconIng = false
    fun saveDeviceIcon(homeLogo: String) {
        //未弹权限说明弹窗前不缓存
        if (!SpUtils.getSPUtilsInstance().getBoolean(SpUtils.FIRST_MAIN_PERMISSION_EXPLAIN, false)) return

        LogUtils.i("saveDeviceIcon", "isSaveDeviceIconIng $isSaveDeviceIconIng")
        if (isSaveDeviceIconIng) return
        isSaveDeviceIconIng = true

        GlideApp.with(BaseApplication.mContext).load(homeLogo).into(object : CustomTarget<Drawable>() {
            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                val bitmapDrawable: BitmapDrawable = resource as BitmapDrawable
                val bitmap: Bitmap = bitmapDrawable.bitmap
                if (bitmap != null) {
                    ThreadUtils.executeByIo(object : ThreadUtils.Task<Int>() {
                        override fun doInBackground(): Int {
                            if (FileUtils.createFileByDeleteOldFile(Global.DEVICE_ICON_PATH)) {
                                var ostream = FileOutputStream(Global.DEVICE_ICON_PATH)
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, ostream)
                                ostream.close()
                            }
                            return 0
                        }

                        override fun onSuccess(result: Int?) {
                            isSaveDeviceIconIng = false
                            LogUtils.i("saveDeviceIcon", "onSuccess " + Global.DEVICE_ICON_PATH)
                        }

                        override fun onCancel() {}

                        override fun onFail(t: Throwable?) {
                            isSaveDeviceIconIng = false
                            LogUtils.i("saveDeviceIcon", "onFail " + t?.localizedMessage)
                        }

                    })
                }
            }

            override fun onLoadCleared(placeholder: Drawable?) {
                isSaveDeviceIconIng = false
                LogUtils.i("saveDeviceIcon", "onFail")
            }

        })
    }

}
package com.jwei.xzfit.utils.manager

import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.ThreadUtils
import com.blankj.utilcode.util.TimeUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vdurmont.emoji.EmojiParser
import com.zhapp.ble.bean.TrackingLogBean
import com.jwei.xzfit.db.model.track.AppTrackingLog
import com.jwei.xzfit.db.model.track.BehaviorTrackingLog
import com.jwei.xzfit.db.model.track.DevTrackingLog
import com.jwei.xzfit.db.model.track.TrackingLog
import com.jwei.xzfit.https.HttpCommonAttributes
import com.jwei.xzfit.https.params.DevTrackingParam
import com.jwei.xzfit.https.params.TrackingAppParam
import com.jwei.xzfit.https.params.UserBehaviorParam
import com.jwei.xzfit.https.tracking.TrackingRetrofitClient
import com.jwei.xzfit.ui.data.Global
import com.jwei.xzfit.utils.AppUtils
import com.jwei.xzfit.utils.HttpLog
import com.jwei.xzfit.utils.SpUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.litepal.LitePal
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Created by Android on 2023/4/7.
 * app异常日志埋点方案
 * https://qrk9bouqk0.feishu.cn/docx/MfdbdQSmEoi1hkxKglMciRtlned
 * 埋点接口
 * https://qrk9bouqk0.feishu.cn/docx/HiGidsrPioKqROxcoElclDlvnQg
 */
object AppTrackingManager {
    private const val TAG = "AppTrackingManager"
    private var scope = MainScope()
    private val litePalMutex by lazy { Mutex() }
    private val gson by lazy { Gson() }

    //region app异常日志埋点
    // 模块代码 10 = 注册；11 = 登录；12 = 绑定解绑；13 = 蓝牙重连；14 = 同步数据；15 = 运动记录；16 = 天气；17 = 消息通知；18 = 表盘；19 = OTA；20 = AGPS；21 = 辅助运动
    const val MODULE_REGISTER = 10
    const val MODULE_LOGIN = 11
    const val MODULE_BIND = 12
    const val MODULE_RECONNECT = 13
    const val MODULE_SYNC_FITNESS = 14
    const val MODULE_SYNC_SPORT = 15
    const val MODULE_SYNC_WEATHER = 16
    const val MODULE_NOTIFY = 17
    const val MODULE_SYNC_DIAL = 18
    const val MODULE_SYNC_OTA = 19
    const val MODULE_SYNC_AGPS = 20
    const val MODULE_SPORT = 21

    /**
     * 获取异常埋点对象并赋值默认数据
     */
    private fun getNewAppTrackingLog(pageModule: Int): AppTrackingLog {
        val data = AppTrackingLog()
        val nowTime = System.currentTimeMillis().toString()
        data.userId = SpUtils.getValue(SpUtils.USER_ID, "")
        data.pageModule = pageModule.toString()
        data.startTimestamp = (System.currentTimeMillis() / 1000).toString()
        data.createDateTime = nowTime
        data.deviceType = Global.deviceType
        data.deviceMac = Global.deviceMac
        data.deviceVersion = Global.deviceVersion
        data.appVersion = AppUtils.getAppVersionName()
        return data
    }

    /**
     * 追踪事件日志
     * @param moduleId 事件ID
     * @param trackingLog 事件日志
     * @param code 状态码
     * @param isEnd 是否事件结束
     * @param isStart 是否事件开始
     */
    @JvmStatic
    fun trackingModule(moduleId: Int, trackingLog: TrackingLog, code: String = "", isEnd: Boolean = false, isStart: Boolean = false) {
        if(true) return //TODO 可维可测 上架屏蔽 - 开发中
        if (moduleId < 10 || moduleId > 21) {
            com.jwei.xzfit.utils.LogUtils.e(TAG, "日志埋点异常 -- moduleId:$moduleId,trackingLog:$trackingLog")
            return
        }

        scope.launch {
            try {

                withContext(Dispatchers.IO) {
                    litePalMutex.withLock {
                        //查询模块最后一条记录
                        val oldData = LitePal.where(
                            "pageModule = ?", "$moduleId"
                        ).order("startTimestamp desc").limit(1).find(AppTrackingLog::class.java)
                        //累加还是新建存储
                        val appTrackingLog = if (oldData.isEmpty()) {
                            getNewAppTrackingLog(moduleId)
                        } else {
                            val old = oldData.get(0)
                            if (isStart /*|| old.isEndTrack*/) {
                                getNewAppTrackingLog(moduleId)
                            } else {
                                old
                            }
                        }
                        //LogUtils.e(TAG, "原日志埋点：$appTrackingLog")
                        //修改事件埋点
                        appTrackingLog.apply {
                            if (isStart) {
                                //开始xx00
                                errorCode = (moduleId.toString() + "00")
                            } else if (isEnd) {
                                if (code.isNotEmpty()) {
                                    //错误码不为空
                                    errorCode = code
                                } else {
                                    //成功
                                    errorCode = (moduleId.toString() + "01")
                                }
                            }

                            val tracks: ArrayList<TrackingLog> = if (errorLog.isEmpty()) {
                                arrayListOf<TrackingLog>()
                            } else {
                                GsonUtils.fromJson(gson, errorLog, object : TypeToken<List<TrackingLog>>() {}.type)
                            }
                            trackingLog.code = errorCode
                            trackingLog.startTime = TrackingLog.getNowString()
                            //替换emoji
                            if (!trackingLog.log.isNullOrEmpty()) {
                                trackingLog.log = EmojiParser.replaceAllEmojis(trackingLog.log, "[emoji]")
                            }
                            //记录
                            tracks.add(trackingLog)
                            errorLog = GsonUtils.toJson(gson, tracks)
                            if (isEnd) isEndTrack = isEnd
                            trackingLog.startTime?.let {
                                startTimestamp = (TimeUtils.string2Millis(it, com.jwei.xzfit.utils.TimeUtils.getSafeDateFormat("yyyy-MM-dd HH:mm:ss:SSS")) / 1000).toString()
                            }
                            isUpLoad = false
                        }
                        //LogUtils.d(TAG, "日志埋点：$appTrackingLog")
                        //修改数据库 "pageModule = ? and startTimestamp = ?", item.pageModule, item.startTimestamp
                        appTrackingLog.saveUpdate()
                        //开始，过程中，成功类型 启动app检查未上报的再上报，异常类型直接上报
                        if (isEnd && !appTrackingLog.errorCode.endsWith("00")
                            && !appTrackingLog.errorCode.endsWith("01")
                        ) {
                            //上报moduleId类型异常至服务器
                            //延时3秒上报，防止同类型多次重复上报
                            appTrackMap.get(moduleId)?.let {
                                ThreadUtils.cancel(it)
                            }
                            val task = PostAppTrackingTask(moduleId)
                            appTrackMap[moduleId] = task
                            ThreadUtils.executeByIoWithDelay(task, 3, TimeUnit.SECONDS)
                            //LogUtils.d(TAG, "postDelayed PostAppTrackingRunnable : $moduleId")
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                com.jwei.xzfit.utils.LogUtils.e(TAG, "记录异常埋点日志失败：$e")
            }
        }
    }

    //ConcurrentHashMap
    private val appTrackMap by lazy { ConcurrentHashMap<Int, ThreadUtils.SimpleTask<Unit>>() }

    //推送异常埋点任务
    private class PostAppTrackingTask(var moduleId: Int) : ThreadUtils.SimpleTask<Unit>() {
        override fun doInBackground() {
            postTrackingDataToServer(moduleId)
        }

        override fun onSuccess(result: Unit?) {}
    }

    /**
     * app异常埋点最大上传条数
     */
    private const val MAX_APP_TRACKING_LIMIT = 10

    //缓存成功的事件 k: 附着与最近一次失败的pageModule_startTimestamp,v: list
    private val dbSuccessCacheMap: MutableMap<String, MutableList<AppTrackingLog>> = mutableMapOf()

    /**
     * 传服务器埋点异常日志
     * 每次上传不超过10条 list json
     */
    fun postTrackingDataToServer(moduleId: Int = 0) {
        if(true) return //TODO 可维可测 上架屏蔽 - 开发中
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    litePalMutex.withLock {
                        //LogUtils.d(TAG, "PostAppTrackingRunnable run $moduleId")
                        //移除所有未完成事件
                        if (moduleId == 0) {
                            LitePal.where("isEndTrack = ?", "0").find(AppTrackingLog::class.java).forEach {
                                //LogUtils.d(TAG, "移除埋点：$it")
                                it.delete()
                            }
                        } else {
                            LitePal.where("isEndTrack = ? and pageModule = ?", "0", "$moduleId").find(AppTrackingLog::class.java).forEach {
                                //LogUtils.d(TAG, "移除埋点：$it")
                                it.delete()
                            }
                        }
                        //清除一周前已上传过的数据
                        clearWeekAgoAppTrackingLog()

                        //查询
                        val dbData = if (moduleId == 0) {
                            //查询所有未上传至服务器的数据
                            LitePal.where("isUpLoad = ?", "0")
                                .order("startTimestamp")
                                .find(AppTrackingLog::class.java)
                        } else {
                            //查询moduleId类型所有未上传至服务器的数据
                            LitePal.where("isUpLoad = ? and pageModule = ?", "0", "$moduleId")
                                .order("startTimestamp")
                                .find(AppTrackingLog::class.java)
                        }

                        //按pageModule分成map
                        val dbMap: MutableMap<String, MutableList<AppTrackingLog>> = mutableMapOf()
                        for (item in dbData) {
                            val dbMapList = dbMap.get(item.pageModule) ?: mutableListOf()
                            dbMapList.add(item)
                            dbMap[item.pageModule] = dbMapList
                        }
                        if (dbMap.isEmpty()) {
                            com.jwei.xzfit.utils.LogUtils.d(TAG, "异常埋点日志为空")
                            return@withLock
                        }

                        //每10条日志上传一次
                        val data: MutableList<MutableList<TrackingAppParam>> = mutableListOf()
                        //填充待上传数据
                        for (pageModule in dbMap.keys) {
                            val dbModuleList = dbMap.get(pageModule)
                            if (dbModuleList != null) {
                                //成功次数
                                var successNum = 0
                                //成功事件缓存list
                                var successCache = mutableListOf<AppTrackingLog>()
                                for (item in dbModuleList) {
                                    //成功的志记录次数
                                    if (item.errorCode.endsWith("01")) {
                                        successNum += 1
                                        successCache.add(item)
                                        //跳出此次循环
                                        continue
                                    }
                                    //移除未完成事件不上报
                                    if (item.errorCode.isEmpty() || item.errorCode.endsWith("00")) {
                                        //com.jwei.publicone.utils.LogUtils.d(TAG, "过滤异常日志上报：" + GsonUtils.toJson(item))
                                        item.isUpLoad = true
                                        item.saveUpdate()
                                        continue
                                    }
                                    val appParams = if (data.isEmpty() || data.get(data.size - 1).size >= MAX_APP_TRACKING_LIMIT) {
                                        mutableListOf<TrackingAppParam>().apply {
                                            data.add(this)
                                        }
                                    } else {
                                        data.get(data.size - 1)
                                    }
                                    val appParam = TrackingAppParam()
                                    appParam.pageModule = item.pageModule
                                    appParam.errorCode = item.errorCode
                                    appParam.deviceType = item.deviceType
                                    appParam.deviceVersion = item.deviceVersion
                                    appParam.deviceSn = item.deviceMac
                                    appParam.startTimestamp = item.startTimestamp
                                    appParam.errorLog = item.errorLog
                                    appParam.registrationArea = SpUtils.getValue(SpUtils.SERVICE_REGION_COUNTRY_CODE, "").uppercase(Locale.ENGLISH)
                                    appParam.successNum = "$successNum"
                                    appParam.warn = getIsWarnTracking(item.errorCode)
                                    appParams.add(appParam)

                                    //缓存成功事件list
                                    if (successNum != 0) {
                                        dbSuccessCacheMap["${item.pageModule}_${item.startTimestamp}"] = successCache
                                    }
                                    //清空成功次数
                                    successNum = 0
                                }

                                //该模块最后几条为成功，没有失败，上传成功事件
                                /*if (successNum != 0) {
                                    val lastItem = dbModuleList.get(dbModuleList.size - 1)
                                    val appParams = if (data.isEmpty() || data.get(data.size - 1).size >= MAX_APP_TRACKING_LIMIT) {
                                        mutableListOf<TrackingAppParam>().apply {
                                            data.add(this)
                                        }
                                    } else {
                                        data.get(data.size - 1)
                                    }
                                    val appParam = TrackingAppParam()
                                    appParam.pageModule = lastItem.pageModule
                                    appParam.errorCode = lastItem.errorCode
                                    appParam.deviceType = lastItem.deviceType
                                    appParam.deviceVersion = lastItem.deviceVersion
                                    appParam.deviceSn = lastItem.deviceMac
                                    appParam.startTimestamp = lastItem.startTimestamp
                                    appParam.errorLog = lastItem.errorLog
                                    appParam.registrationArea = SpUtils.getValue(SpUtils.SERVICE_REGION_COUNTRY_CODE, "").uppercase(Locale.ENGLISH)
                                    appParam.successNum = "$successNum"
                                    appParams.add(appParam)
                                    //缓存成功事件list
                                    dbSuccessCacheMap["${lastItem.pageModule}_${lastItem.startTimestamp}"] = successCache
                                    //清空成功次数
                                    successNum = 0
                                }*/
                            }
                        }

                        //上报
                        if (data.isEmpty()) {
                            com.jwei.xzfit.utils.LogUtils.d(TAG, "异常埋点日志填充服务器数据为空")
                            return@withLock
                        }

                        for (serItem in data) {
                            val requestBody = GsonUtils.toJson(serItem).toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                            val result = TrackingRetrofitClient.service.appTrackingErrorBulk(requestBody)
                            //val result = TrackingRetrofitClient.service.appTrackingErrorBulk(serItem)
                            com.jwei.xzfit.utils.LogUtils.d(TAG, "postTrackingDataToServer:${GsonUtils.toJson(result)}")
                            if (result.code == HttpCommonAttributes.REQUEST_SUCCESS) {
                                updateSuccess(serItem)
                            } else {
                                com.jwei.xzfit.utils.LogUtils.e(TAG, "上报异常埋点日志失败：${GsonUtils.toJson(result)}")
                                HttpLog.log("上报异常埋点日志失败：${GsonUtils.toJson(result)}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                com.jwei.xzfit.utils.LogUtils.e(TAG, "上报异常埋点日志失败：$e")
                HttpLog.log("上报异常埋点日志失败：$e")
            }
        }
    }

    /**
     * 错误码是否异常
     */
    private fun getIsWarnTracking(errorCode: String): String {
        val isWarn = when (errorCode) {
            "1010","1011","1012","1014",
            "1110","1112","1118","1120",
            "1210","1211","1215","1219",
            "1311","1312","1315","1316",
            "1610","1611",
            "1711","1712","1713","1714","1716","1717","1718",
            "1811" -> "1"
            else -> "0"
        }
        return isWarn
    }

    /**
     * 处理上报成功的数据库 数据
     */
    private fun updateSuccess(serItem: MutableList<TrackingAppParam>) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    litePalMutex.withLock {
                        for (item in serItem) {
                            //失败的置位已上传
                            updateUploadStatus(item)
                            //成功的置位已上传
                            val sucList = dbSuccessCacheMap.get("${item.pageModule}_${item.startTimestamp}")
                            if (!sucList.isNullOrEmpty()) {
                                for (sucItem in sucList) {
                                    updateUploadStatus(sucItem)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                com.jwei.xzfit.utils.LogUtils.e(TAG, "处理上报成功的数据库失败：$e")
            }
        }
    }

    /**
     * 更新已上传状态
     */
    private fun updateUploadStatus(item: TrackingAppParam) {
        val dbData = LitePal.where("pageModule = ? and startTimestamp = ?", item.pageModule, item.startTimestamp)
            .find(AppTrackingLog::class.java)
        if (!dbData.isNullOrEmpty()) {
            for (dbItem in dbData) {
                dbItem.isUpLoad = true
                dbItem.saveUpdate()
            }
        }
    }

    /**
     * 更新已上传状态
     */
    private fun updateUploadStatus(item: AppTrackingLog) {
        val dbData = LitePal.where("pageModule = ? and startTimestamp = ?", item.pageModule, item.startTimestamp)
            .find(AppTrackingLog::class.java)
        if (!dbData.isNullOrEmpty()) {
            for (dbItem in dbData) {
                dbItem.isUpLoad = true
                dbItem.saveUpdate()
            }
        }
    }

    /**
     * 清除一周前已上传过的数据
     */
    private fun clearWeekAgoAppTrackingLog() {
        //查询七天前的已上传的数据
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -7)
        val data = LitePal.where("isUpLoad = ? and startTimestamp < ?", "1", (calendar.time.time / 1000).toString())
            .order("startTimestamp")
            .find(AppTrackingLog::class.java)
        //LogUtils.d("七天前已上传的数据：" + GsonUtils.toJson(data))
        if (data.isNotEmpty()) {
            for (item in data) {
                item.delete()
            }
        }
    }

    //endregion

    //region app行为日志埋点
    // 6=Infowear_Function usage
    // 1 = 常用联系人
    //	2 = 事件提醒
    //	3 = 久坐提醒
    //	4 =吃药提醒
    //	5 = 喝水提醒
    //	6 = 闹钟提醒
    //	7 = 摇摇拍照
    //
    // 7=Infowear_Function open
    // 9 = 消息通知设置
    //	10 = 睡眠设置
    //	11 =连续心率检测
    //	12 = 天气设置
    //	13 = 抬腕亮屏
    //	14 = 省电模式
    //	15 = 勿扰设置
    //	16 = 息屏显示
    //	17 =查找手表
    //
    //  8=infowear_watch faces
    //  18=同步相册表盘
    //	19=同步其他云表盘
    //
    //  9=infowear_exercise
    //  20=户外跑步
    //	21=户外骑行
    //	22=户外健走
    //
    //  10=infowear_Health data
    //  24=步数
    //	25=距离
    //	26=卡路里
    //	27=睡眠
    //	28=心率
    //	29=血氧
    //	30=有效站立
    //	31=女性健康
    //	32=运动记录
    //
    //  11=infowearLogin Event
    //  33=账号登录成功
    //	34=注册成功
    //	35=无账号登录
    //	36=个人信息填写成功
    //	37=个人信息填写跳过
    //
    //  12=infowear_Backstage activation
    //  39=开启后台保活
    /**
     * 获取用户行为对象
     * https://qrk9bouqk0.feishu.cn/sheets/shtcnBwu2rueK8weZszsKRbyYVb?sheet=2jHgUx
     */
    @JvmStatic
    fun getNewBehaviorTracking(pageModule: String, moduleFunction: String): BehaviorTrackingLog {
        val behaviorTrackingLog = BehaviorTrackingLog().apply {
            userId = SpUtils.getValue(SpUtils.USER_ID, "")
            startTimestamp = (System.currentTimeMillis() / 1000).toString()
            this.pageModule = pageModule
            this.moduleFunction = moduleFunction
            functionSwitchStatus
            functionStatus
            registrationArea = SpUtils.getValue(SpUtils.SERVICE_REGION_COUNTRY_CODE, "").uppercase(Locale.ENGLISH)
        }
        return behaviorTrackingLog
    }

    /**
     * 记录用户行为埋点
     */
    @JvmStatic
    fun saveBehaviorTracking(behaviod: BehaviorTrackingLog) {
        if(true) return //TODO 可维可测 上架屏蔽 - 开发中
        behaviod.saveUpdate()
    }

    /**
     * 保存只记录一次行为的事件
     * @param pageModule 6-7
     * @param moduleFunction 1-17
     * https://qrk9bouqk0.feishu.cn/sheets/shtcnBwu2rueK8weZszsKRbyYVb?sheet=2jHgUx
     */
    fun saveOnlyBehaviorTracking(pageModule: String, moduleFunction: String, functionSwitchStatus: String = "1", functionStatus: String = "1") {
        if(true) return //TODO 可维可测 上架屏蔽 - 开发中
        val moduleFunctionId = "$pageModule$moduleFunction"
        var only: ArrayList<String>? = GsonUtils.fromJson(SpUtils.getValue(SpUtils.BEHAVIOR_ONLY_KEY, ""), object : TypeToken<ArrayList<String>?>() {}.type)
        if (only == null) only = arrayListOf()
        if (!only.contains(moduleFunctionId)) {
            saveBehaviorTracking(getNewBehaviorTracking(pageModule, moduleFunction).apply {
                this.functionSwitchStatus = functionSwitchStatus
                this.functionStatus = functionStatus
            })
            only.add(moduleFunctionId)
            SpUtils.setValue(SpUtils.BEHAVIOR_ONLY_KEY, GsonUtils.toJson(only))
        }
    }

    /**
     * 用户行为最大上传条数
     */
    private const val MAX_BEHAVIOR_LIMIT = 20

    /**
     * 上传用户行为埋点
     */
    fun postBehaviorTracking() {
        if(true) return //TODO 可维可测 上架屏蔽 - 开发中
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    litePalMutex.withLock {
                        //查数据库
                        val dbData = LitePal.where("isUpLoad = ?", "0")
                            .order("createDateTime")
                            .find(BehaviorTrackingLog::class.java)
                        if (dbData.isEmpty()) {
                            com.jwei.xzfit.utils.LogUtils.d(TAG, "用户行为日志埋点为空")
                            return@withLock
                        }
                        //清除一周前已上传过的数据
                        clearWeekAgoBehavior()
                        //分段
                        val segmentations: MutableList<List<BehaviorTrackingLog>> = ArrayList()
                        var begin = 0
                        while (begin < dbData.size) {
                            val end = if (begin + MAX_BEHAVIOR_LIMIT > dbData.size) dbData.size else begin + MAX_BEHAVIOR_LIMIT
                            segmentations.add(dbData.subList(begin, end))
                            begin += MAX_BEHAVIOR_LIMIT
                        }
                        //上传
                        for (section in segmentations) {
                            val serItems = mutableListOf<UserBehaviorParam>()
                            for (item in section) {
                                serItems.add(UserBehaviorParam().apply {
                                    pageModule = item.pageModule
                                    moduleFunction = item.moduleFunction
                                    startTimestamp = item.startTimestamp
                                    if (item.registrationArea.isNotEmpty()) registrationArea = item.registrationArea
                                    if (item.durationSec.isNotEmpty()) durationSec = item.durationSec
                                    if (item.functionSwitchStatus.isNotEmpty()) functionUseStatus = item.functionSwitchStatus
                                    if (item.functionStatus.isNotEmpty()) functionStatus = item.functionStatus
                                })
                            }
                            //com.jwei.publicone.utils.LogUtils.d(TAG, "postBehaviorTracking:${GsonUtils.toJson(serItems)}")
                            val requestBody = GsonUtils.toJson(gson, serItems).toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                            val result = TrackingRetrofitClient.service.appUserBehaviorTrackingBulk(requestBody)
                            com.jwei.xzfit.utils.LogUtils.d(TAG, "postBehaviorTracking:${GsonUtils.toJson(result)}")
                            if (result.code == HttpCommonAttributes.REQUEST_SUCCESS) {
                                updateBehaviorSuccess(section)
                            } else {
                                com.jwei.xzfit.utils.LogUtils.e(TAG, "上报用户行为日志失败：${GsonUtils.toJson(result)}")
                                HttpLog.log("上报用户行为日志失败：${GsonUtils.toJson(result)}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                com.jwei.xzfit.utils.LogUtils.e(TAG, "用户行为日志上传失败：$e")
                HttpLog.log("用户行为日志上传失败：$e")
            }
        }
    }

    /**
     * 更新用户行为数据库上传字段
     */
    private fun updateBehaviorSuccess(items: List<BehaviorTrackingLog>) {
        for (item in items) {
            item.isUpLoad = true
            item.saveUpdate()
        }
    }

    /**
     * 清除一周前已上传过的数据
     */
    private fun clearWeekAgoBehavior() {
        //查询七天前的已上传的数据
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -7)
        val data = LitePal.where("isUpLoad = ? and startTimestamp < ?", "1", (calendar.time.time / 1000).toString())
            .order("startTimestamp")
            .find(BehaviorTrackingLog::class.java)
        //LogUtils.d("七天前已上传的数据：" + GsonUtils.toJson(data))
        if (data.isNotEmpty()) {
            for (item in data) {
                item.delete()
            }
        }
    }
    //endregion

    //region 设备日志埋点
    fun postDeviceTrackingLog(trackingLogBean: TrackingLogBean) {
        if(true) return //TODO 可维可测 上架屏蔽 - 开发中
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    litePalMutex.withLock {
                        //存数据库
                        if (!trackingLogBean.fileName.isNullOrEmpty() &&
                            trackingLogBean.trackingLog != null &&
                            trackingLogBean.trackingLog.isNotEmpty()
                        ) {
                            val devTrackingLog = DevTrackingLog()
                            devTrackingLog.userId = SpUtils.getValue(SpUtils.USER_ID, "")
                            devTrackingLog.createDateTime = System.currentTimeMillis().toString()
                            devTrackingLog.deviceType = Global.deviceType
                            devTrackingLog.deviceSn = Global.deviceMac
                            devTrackingLog.deviceVersion = Global.deviceVersion
                            devTrackingLog.registrationArea = SpUtils.getValue(SpUtils.SERVICE_REGION_COUNTRY_CODE, "").uppercase(Locale.ENGLISH)
                            devTrackingLog.data = ConvertUtils.bytes2String(trackingLogBean.trackingLog, "UTF-8")
                            devTrackingLog.logFileName = trackingLogBean.fileName
                            devTrackingLog.saveUpdate()
                        }
                        //清除一周前已上传过的数据
                        clearWeekAgoDevTrackingLog()
                        //查询未上传日志上传
                        val dbData = LitePal.where("isUpLoad = ?", "0")
                            .order("createDateTime desc")
                            .find(DevTrackingLog::class.java)

                        if (dbData.isNullOrEmpty()) {
                            com.jwei.xzfit.utils.LogUtils.d(TAG, "设备埋点日志数据为空")
                            return@withLock
                        }
                        //上传至服务器
                        for (item in dbData) {
                            val serItem = DevTrackingParam()
                            serItem.deviceType = item.deviceType
                            serItem.deviceVersion = item.deviceVersion
                            serItem.deviceSn = item.deviceSn
                            serItem.registrationArea = item.registrationArea
                            val logDatas = mutableListOf<String>()
                            if (item.data.isNotEmpty()) {
                                for (str in item.data.split("\r\n")) {
                                    if (str.isNotEmpty()) {
                                        logDatas.add(str)
                                    }
                                }
                            }
                            serItem.dataList = logDatas
                            serItem.logFileName = item.logFileName

                            val requestBody = GsonUtils.toJson(serItem).toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                            val result = TrackingRetrofitClient.service.devTrackingErrorBulk(requestBody)
                            com.jwei.xzfit.utils.LogUtils.d(TAG, "postDeviceTrackingLog:${GsonUtils.toJson(result)}")
                            if (result.code == HttpCommonAttributes.REQUEST_SUCCESS) {
                                updateDevSuccess(item)
                            } else {
                                com.jwei.xzfit.utils.LogUtils.e(TAG, "上报设备埋点日志失败：${GsonUtils.toJson(result)}")
                                HttpLog.log("上报设备埋点日志失败：${GsonUtils.toJson(result)}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                com.jwei.xzfit.utils.LogUtils.e(TAG, "设备异常日志埋点异常：$e")
                HttpLog.log("设备异常日志埋点异常：$e")
            }
        }
    }

    /**
     * 更新设备日志埋点上传状态
     */
    private fun updateDevSuccess(item: DevTrackingLog) {
        item.isUpLoad = true
        item.saveUpdate()
    }

    /**
     * 清除一周前已上传过的数据
     */
    private fun clearWeekAgoDevTrackingLog() {
        //查询七天前的已上传的数据
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -7)
        val data = LitePal.where("isUpLoad = ? and createDateTime < ?", "1", calendar.time.time.toString())
            .order("createDateTime")
            .find(DevTrackingLog::class.java)
        //LogUtils.d("七天前已上传的数据：" + GsonUtils.toJson(data))
        if (data.isNotEmpty()) {
            for (item in data) {
                item.delete()
            }
        }
    }
    //endregion
}
package com.smartwear.xzfit.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.TimeUtils
import com.smartwear.xzfit.base.BaseViewModel
import com.smartwear.xzfit.db.model.sport.SportModleInfo
import com.smartwear.xzfit.https.HttpCommonAttributes
import com.smartwear.xzfit.https.MyRetrofitClient
import com.smartwear.xzfit.https.params.QueryExerciseDetailsParam
import com.smartwear.xzfit.https.params.QueryExerciseListParam
import com.smartwear.xzfit.https.response.SportExerciseResponse
import com.smartwear.xzfit.ui.data.Global
import com.smartwear.xzfit.ui.sport.bean.SportDataBean
import com.smartwear.xzfit.ui.sport.livedata.SportLiveData
import com.zhapp.ble.utils.UnitConversionUtils
import com.smartwear.xzfit.ui.user.bean.UserBean
import com.smartwear.xzfit.utils.*
import com.smartwear.xzfit.utils.manager.DevSportManager
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.litepal.LitePal
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.collections.ArrayList
import kotlin.coroutines.resume

/**
 * Created by Android on 2021/9/28.
 */
class SportModel : BaseViewModel() {

    val sportLiveData = SportLiveData.instance

//    //累计距离
//    val odometer = MutableLiveData(0f)

    //开始运动倒计时
    val countDown = MutableLiveData(3)

    //GPS信号强度
    val GPSRssl = MutableLiveData(0)

    //是否运动上锁
    val isLock = MutableLiveData(false)

    //是否暂停
    val isPause = MutableLiveData(false)
    private lateinit var seekBarJob: Job

    //按压进度
    val seekBARdown = MutableLiveData(0)

    //运动耗时 ms
    //var sportTime = MutableLiveData(0L)
    private lateinit var sportTimeJob: Job

    //选中的运动数据类型
    val sportDataType = MutableLiveData(Global.SPORT_SINGLE_DATA_TYPE_TIME)

    //刷新间隔
    private var refInterval = 0L

    //十秒内距离和
    private var dotDistance = 0f

    //region 界面逻辑
    /**
     * 倒计时开始运动
     * */
    fun startCountDown() {
        launchUI {
            countDown.value?.let {
                while (countDown.value!! > 1) {
                    delay(1000)
                    countDown.value = countDown.value!! - 1
                }
            }
        }
    }

    /**
     * 更新gps强度
     * */
    fun setGPSRssl(level: Int) {
        GPSRssl.postValue(level)
    }

    //上锁
    fun sportLock() {
        isLock.postValue(true)
    }

    /**
     * 开始运动
     * */
    fun sportStart() {
        sportDataRecord(0f, true)
        if (::sportTimeJob.isInitialized) {
            sportTimeJob.cancel()
        }
        sportTimeJob = launchUI {
            while (true) {
                delay(500L)
                sportLiveData.getSportTime().value =
                    sportLiveData.getSportTime().value!!.toLong() + 500L
                refInterval += 500L
                if (refInterval == Global.SPORT_REF_DATA_INTERVAL) {
                    refInterval = 0L
                    calculateSportData(0f)
                    sportDataRecord(dotDistance)
                }
            }
        }
    }

    //暂停
    fun sportPause() {
        isPause.postValue(true)
        if (::sportTimeJob.isInitialized) {
            sportTimeJob.cancel()
        }
        calculateSportData(0f)
        sportDataRecord(dotDistance)
    }

    //停止运动
    fun sportSport() {
        calculateSportPaceSpeedData()
    }

    //重新开始
    fun sportRestart() {
        isPause.postValue(false)
        sportStart()
    }

    /**
     * 开始按压
     * */
    fun seekBarStart() {
        seekBARdown.postValue(0)
        seekBarJob = launchUI {
            while (seekBARdown.value!!.toInt() < Global.PRESS_SEEKBAR_MAX) {
                delay(Global.PRESS_SEEKBAR_INTERVAL)
                seekBARdown.value = seekBARdown.value!!.toInt() + 1
                if (seekBARdown.value!!.toInt() == Global.PRESS_SEEKBAR_MAX) {
                    if (isLock.value!!) isLock.value = false
                }
            }
        }
    }

    /**
     * 停止按压
     * */
    fun seekBarStop() {
        seekBarJob.cancel()
        seekBARdown.postValue(0)
    }

    /**
     * 改变选择的运动单个数据类型
     * */
    fun changeSingleDataType(type: Int) {
        sportDataType.value = type
    }
    //endregion

    //region 根据移动距离测量数据

    /**
     * 根据移动距离测量数据
     * */
    fun calculateSportData(distance: Float) {
//        val distance = AMapUtils.calculateLineDistance(oldLatLng, newLatLng)
        LogUtils.d("运动距离：$distance，运动时间:" + sportLiveData.getSportTime().value!!.toLong())
        AppUtils.tryBlock {

            //总距离
            sportLiveData.getSportDistance().value =
                sportLiveData.getSportDistance().value!!.toFloat() + distance
            //十秒内距离
            dotDistance += distance
            //运动距离 > 0 && 运动时间 > 10 S
            if (sportLiveData.getSportDistance().value!!.toLong() > 0 &&
                sportLiveData.getSportTime().value!!.toLong() >= 10 * 1000
            ) {
                //配速
                sportLiveData.getSportMinkm().value = DevSportManager.calculateMinkm(
                    sportLiveData.getSportTime().value!!.toLong(),
                    sportLiveData.getSportDistance().value!!.toFloat()
                )
                //速度
                sportLiveData.getSportSpeed().value = calculateSpeed(
                    sportLiveData.getSportTime().value!!.toLong(),
                    sportLiveData.getSportDistance().value!!.toFloat()
                )
                //卡路里
                sportLiveData.getCalories().value = calculateCalories(
                    sportLiveData.getSportTime().value!!.toLong(),
                    sportLiveData.getSportDistance().value!!.toFloat()
                )
            }
        }

    }

    /**
     * 每十秒打点记录
     * @param distance 10内运动距离
     * @param isStart 是否开始运动数据
     * */
    private fun sportDataRecord(distance: Float, isStart: Boolean = false) {
        if (isStart) {
            sportLiveData.getSportData().value!!.add(mutableListOf())
        }
        //往最后一个集合塞数据
        if (sportLiveData.getSportData().value!!.size > 0) {
            val list =
                sportLiveData.getSportData().value!!.get(sportLiveData.getSportData().value!!.size - 1)
            list.add(SportDataBean(System.currentTimeMillis(), distance))
            dotDistance = 0f
        }
    }

    /**
     * 速度
     * */
    private fun calculateSpeed(millis: Long, distance: Float): Float {
        val h = millis / 1000f / 60 / 60
        val km = distance / 1000f / (if (AppUtils.getDeviceUnit() == 0) 1f else 1.61f)
        return km / h
    }

    /**
     * 配速 ?分?秒/公里
     * */
    /*private fun calculateMinkm(millis: Long, distance: Float): String {
        if (distance < 1f) {
            return "0'0\""
        }
        val km = distance / 1000f / (if (AppUtils.getDeviceUnit() == 0) 1f else 1.61f)
        val resultF = (millis / 1000f / km).toInt() / 60
        val resultM = (millis / 1000f / km).toInt() % 60
        //峰值限制
        if (resultF > 59) {
            return /*"59'59\""*/"0'0\""
        }
        return "$resultF'$resultM\""
    }*/

    /**
     * 卡路里
     * 跑步热量（ kcal)=体重(kg）×运动时间(小时）×指数K   指数K=30/速度
     * */
    private fun calculateCalories(millis: Long, distance: Float): Float {
        var weight = 65f
        var height = 170f

        val bean = UserBean().getData()
        if (bean != null) {
            weight = bean.weight.toFloat()
            height = bean.height.toFloat()
        }
        val stepNum = (distance * 100000 / 10 / 32 / height)
        return (weight * 1.036 * height * 0.32 * stepNum * 0.00001f).toFloat()
    }

    //region 运动速度，配速数据

    var maxPace = ""
    var minPace = ""
    var maxSpeed = ""
    var minSpeed = ""
    var paceDataBuilder = StringBuilder()
    var speedDataBuilder = StringBuilder()

    fun calculateSportPaceSpeedData() {
        val lists = sportLiveData.getSportData().value
        LogUtils.e("SportData list --------->")
        LogUtils.json(lists)
        //配速集合
        val paceList = mutableListOf<Float>()
        //速度集合
        val speedList = mutableListOf<Float>()

        //region  获取两点直接速度，配速集合
        var tempData: SportDataBean? = null
        if (lists != null && lists.size > 0) {
            for (list in lists) {
                tempData = null
                for (sportData in list) {
                    if (tempData == null) {
                        tempData = sportData
                        continue
                    }
                    //两点耗时
                    val time = sportData.timeMillis - tempData.timeMillis
                    val dis = sportData.distance
                    tempData = sportData
                    //速度 km/h
                    if (dis != 0f) {
                        val s =
                            (dis / 1000f /*/ (if (AppUtils.getDeviceUnit() == 0) 1f else 1.61f)*/) / (time / 1000f / 60 / 60)
                        speedList.add(s)
                    } else {
                        speedList.add(0f)
                    }
                    //配速 走 1 km|mi所需要时间秒s
                    if (dis != 0f) {
                        val p =  //秒 * 1000(公英) / 已走距离
                            (time / 1000f) * (1000/* * if (AppUtils.getDeviceUnit() == 0) 1f else 1.61f*/) / dis
                        paceList.add(p)
                    } else {
                        paceList.add(0f)
                    }
                }
            }
        }
        //endregion
        LogUtils.e("speedList list --------->")
        LogUtils.json(speedList)
        LogUtils.e("paceList list --------->")
        LogUtils.json(paceList)
        //region 速度分16或17段取平均值记录数据
        var speedSize = 0
        if (speedList.size > 16) {
            val slists = getSubLists(speedList, speedList.size / 16)
            var tempMax = 0f
            var tempMin = 0f
            slists.forEach {
                val size = it.size
                var all = 0f
                it.forEach { v ->
                    all += v
                }
                if (tempMax < it.maxOrNull() ?: 0f) {
                    tempMax = it.maxOrNull() ?: 0f
                }
                it.forEach { f ->
                    if (tempMin == 0f) {
                        if (f != 0f) {
                            tempMin = f
                        }
                    } else {
                        if (f != 0f && f < tempMin) {
                            tempMin = f
                        }
                    }
                }
                if (speedSize != 16) {
                    if (speedDataBuilder.isEmpty()) {
                        speedDataBuilder.append(UnitConversionUtils.bigDecimalFormat(all / size))
                    } else {
                        speedDataBuilder.append(",")
                            .append(UnitConversionUtils.bigDecimalFormat(all / size))
                    }
                    speedSize++
                }
            }
            if ((speedList.size / 16) * 16 < speedList.size - 1) {
                val endlist = speedList.subList((speedList.size / 16) * 16, speedList.size - 1)
                if (endlist.size > 0) {
                    val size = endlist.size
                    var all = 0f
                    endlist.forEach { v ->
                        all += v
                    }
                    val value = all / size
//                if (value > 0) {
                    speedDataBuilder.append(",").append(UnitConversionUtils.bigDecimalFormat(value))
//                }
                }
            }
            maxSpeed = UnitConversionUtils.bigDecimalFormat(tempMax)
            minSpeed = UnitConversionUtils.bigDecimalFormat(tempMin)
        }
        //endregion
        //region 配速分16或17段取平均值记录数据
        var paceSize = 0
        if (paceList.size > 16) {
            val slists = getSubLists(paceList, paceList.size / 16)
            var tempMax = 0f
            var tempMin = 0f
            slists.forEach {
                val size = it.size
                var all = 0f
                it.forEach { v ->
                    all += v
                }
                if (tempMax < it.maxOrNull() ?: 0f) {
                    tempMax = it.maxOrNull() ?: 0f
                }
                it.forEach { f ->
                    if (tempMin == 0f) {
                        if (f != 0f) {
                            tempMin = f
                        }
                    } else {
                        if (f != 0f && f < tempMin) {
                            tempMin = f
                        }
                    }
                }
                if (paceSize != 16) {
                    if (paceDataBuilder.isEmpty()) {
                        paceDataBuilder.append((all / size).toInt())
                    } else {
                        paceDataBuilder.append(",").append((all / size).toInt())
                    }
                    paceSize++
                }
            }
            if ((speedList.size / 16) * 16 < speedList.size - 1) {
                val endlist = paceList.subList((paceList.size / 16) * 16, paceList.size - 1)
                if (endlist.size > 0) {
                    val size = endlist.size
                    var all = 0f
                    endlist.forEach { v ->
                        all += v
                    }
                    val value = all / size
//                if (value > 0) { //TODO 最后一段时间,大于16段时间平均时间 10% 就要记录
                    paceDataBuilder.append(",").append(value.toInt())
//                }
                }
            }
            maxPace = tempMin.toInt().toString()
            minPace = tempMax.toInt().toString()
        }
        //endregion
    }

    /**
     * list 分段处理
     * */
    fun getSubLists(allData: List<Float>, size: Int): List<List<Float>> {
        val result: MutableList<List<Float>> = ArrayList()
        var begin = 0
        while (begin < allData.size) {
            val end = if (begin + size > allData.size) allData.size else begin + size
            result.add(allData.subList(begin, end))
            begin += size
        }
        return result
    }
    //endregion
    //endregion

    //region 上传多运动信息
    /**
     * 上传多运动信息
     * */
    fun uploadExerciseData(infos: MutableList<SportModleInfo>) {
        if (infos.size == 0) return
        launchUI {
            AppUtils.trySuspendBlock {
                val uploadSportBean = SportExerciseResponse()
                uploadSportBean.dataList = mutableListOf()
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (userId.isEmpty()) return@trySuspendBlock
                for (info in infos) {
                    if (info.isUpLoad) continue
                    uploadSportBean.dataList!!.add(compressInfo(info.clone()))
                }
                val result = MyRetrofitClient.service.uploadExerciseData(
                    JsonUtils.getRequestJson(uploadSportBean, SportExerciseResponse::class.java)
                )
                LogUtils.e("上传多运动数据", "上报运动数据成功 infos = ${GsonUtils.toJson(infos)}")
                //LogUtils.e("上报运动数据成功")
                //LogUtils.json(infos)
                if (result.code == HttpCommonAttributes.REQUEST_SUCCESS) {
                    for (i in 0 until infos.size) {
                        var sportModleInfo = SportModleInfo()
                        sportModleInfo.isUpLoad = true
                        sportModleInfo.updateAll("userId = ? and date = ?", userId, infos[i].date)
                    }
                }
                userLoginOut(result.code)
            }
        }
    }

    /**
     * 压缩加密Info大数据
     */
    fun compressInfo(info: SportModleInfo): SportModleInfo {
        info.exerciseApp?.mapData?.let {
            info.exerciseApp!!.mapData = AESUtils.encrypt(compress(it), JsonUtils.serviceKey)
        }

        info.exerciseIndoor?.recordPointSportData?.let {
            info.exerciseIndoor!!.recordPointSportData =
                AESUtils.encrypt(compress(it), JsonUtils.serviceKey)
        }

        info.exerciseOutdoor?.recordPointSportData?.let {
            info.exerciseOutdoor!!.recordPointSportData =
                AESUtils.encrypt(compress(it), JsonUtils.serviceKey)
        }
        info.exerciseOutdoor?.gpsMapDatas?.let {
            info.exerciseOutdoor!!.gpsMapDatas =
                AESUtils.encrypt(compress(it), JsonUtils.serviceKey)
        }

        info.exerciseSwimming?.recordPointSportData?.let {
            info.exerciseSwimming!!.recordPointSportData =
                AESUtils.encrypt(compress(it), JsonUtils.serviceKey)
        }
        info.exerciseSwimming?.gpsMapDatas?.let {
            info.exerciseSwimming!!.gpsMapDatas =
                AESUtils.encrypt(compress(it), JsonUtils.serviceKey)
        }
        return info
    }

    /**
     * 压缩
     * */
    private fun compress(str: String?): String {
        if (str.isNullOrEmpty()) {
            return ""
        }
        try {
            // 创建一个新的输出流
            val out = ByteArrayOutputStream()
            // 使用默认缓冲区大小创建新的输出流
            val gzip = GZIPOutputStream(out)
            // 将字节写入此输出流
            gzip.write(str.toByteArray(charset("utf-8"))) // 因为后台默认字符集有可能是GBK字符集，所以此处需指定一个字符集
            gzip.close()
            // 使用指定的 charsetName，通过解码字节将缓冲区内容转换为字符串
            return out.toString("ISO-8859-1")
        } catch (e: IOException) {
            e.printStackTrace()
            return ""
        }
    }
    //endregion

    //region 获取多运动数据
    val sportRecordData = MutableLiveData(mutableListOf<SportModleInfo>())
    val querySportMutex = Mutex()

    /**
     * 查询本地/云 当前日期的运动数据
     * @param mSelectionDate yyyy-MM-dd
     * @param isSync 是否请求云数据
     * @param pageIndex 云-页下标 1..
     * @param pageSize 数量
     */
    fun querySportRecordData(
        mSelectionDate: String,
        isSync: Boolean = true,
        pageIndex: Int = 1,
        pageSize: String = "500",
    ) {
        launchUI {
            querySportMutex.withLock { //同步锁
                //获取数据库
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                val sqlList = getDbSportRecordData(mSelectionDate)
                LogUtils.d("获取数据库list -- > ${sqlList.size}")
                LogUtils.json(sqlList)
                sportRecordData.postValue(sqlList)

                if (userId.isNotEmpty() && isSync) {
                    AppUtils.trySuspendBlock {
                        LogUtils.d("mSelectionDate == $mSelectionDate")
                        val cal: Calendar = Calendar.getInstance()
                        val zoneOffset: Int = cal.get(Calendar.ZONE_OFFSET)
                        LogUtils.d("zoneOffset == $zoneOffset")
                        val bean = QueryExerciseListParam(
                            userId,
                            mSelectionDate,
                            "$pageIndex",
                            pageSize,
                            "${zoneOffset / 1000 / 60}"
                        )
                        val result = MyRetrofitClient.service.queryExerciseList(
                            JsonUtils.getRequestJson(
                                bean,
                                QueryExerciseListParam::class.java
                            )
                        )
                        LogUtils.d("queryExerciseList result = $result")
                        userLoginOut(result.code)
                        val serData = result.data
                        if (serData != null && !serData.list.isNullOrEmpty()) {
                            LogUtils.d("获取服务器list -- > ${serData.list!!.size}")
                            LogUtils.json(serData.list)
                            var isRef = false
                            //LogUtils.json(serData)
                            serData.list!!.forEach { it.isUpLoad = true }
                            //云 同步 本地list ----》 更新 sportId isUpLoad
                            serData.list!!.forEach { ser ->
                                //云 更新/存入 本地
                                val sqlBean = sqlList.firstOrNull {
                                    ser.userId == it.userId && ser.dataSources == it.dataSources &&
                                            ser.exerciseType == it.exerciseType && ser.sportTime == it.sportTime &&
                                            ser.sportEndTime == it.sportEndTime
                                }
                                if (sqlBean != null) {
                                    sqlBean.sportId = ser.sportId
                                    sqlBean.isUpLoad = ser.isUpLoad
                                    sqlBean.saveUpdate(
                                        SportModleInfo::class.java,
                                        "userId = ? and dataSources = ? and exerciseType = ? and sportTime = ? and sportEndTime = ?",
                                        userId,
                                        "${sqlBean.dataSources}",
                                        sqlBean.exerciseType,
                                        "${sqlBean.sportTime}",
                                        "${sqlBean.sportEndTime}"
                                    )
                                } else {
                                    //本地没有直接存入
                                    ser.date = TimeUtils.millis2String(
                                        ser.sportTime * 1000,
                                        com.smartwear.xzfit.utils.TimeUtils.getSafeDateFormat(com.smartwear.xzfit.utils.TimeUtils.DATEFORMAT_COMM)
                                    )
                                    val isSucceed = ser.saveUpdate(
                                        SportModleInfo::class.java,
                                        "userId = ? and dataSources = ? and exerciseType = ? and sportTime = ? and sportEndTime = ?",
                                        userId,
                                        "${ser.dataSources}",
                                        ser.exerciseType,
                                        "${ser.sportTime}",
                                        "${ser.sportEndTime}"
                                    )
                                    LogUtils.d("本地没有直接存入 --> $isSucceed")
                                    isRef = true
                                }
                            }

                            //刷新
                            if (isRef) {
                                querySportRecordData(mSelectionDate, false)
                                //同步云数据后刷新首页运动记录
                                SendCmdUtils.getSportData()
                            }
                        }

                        //本地 同步 云 list ----》 提交未提交的本地数据
                        val unUploadList =
                            sqlList.filter { !it.isUpLoad } as MutableList<SportModleInfo>
                        LogUtils.d("未提交运动数据list -- > ${unUploadList.size}")
                        if (unUploadList.size > 0) {
                            //LogUtils.json(unUploadList)
                            uploadExerciseData(unUploadList)
                        }
                    }
                }
            }
        }
    }

    /**
     * 查询本地db当前日期的运动数据
     * */
    private suspend fun getDbSportRecordData(date: String): MutableList<SportModleInfo> {
        return suspendCancellableCoroutine<MutableList<SportModleInfo>> { result ->
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    //获取数据库 多表需要激进查询
                    val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                    var sqlList = LitePal.where(
                        "userId = ? and date like ?", "$userId", "%$date%"
                    ).limit(20).order("sportTime desc").find(SportModleInfo::class.java, true)
                    if (sqlList == null) {
                        sqlList = mutableListOf()
                    }
                    result.resume(sqlList)
                }
            }
        }
    }
    //endregion

    //region 获取运动数据详情
    suspend fun querySportDetailsData(t: SportModleInfo): SportModleInfo? {
        return suspendCancellableCoroutine<SportModleInfo?> { result ->
            launchUI {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (userId.isEmpty() || t.sportId == 0L) {
                    result.resume(null)
                } else {
                    var serData: SportModleInfo? = null
                    AppUtils.trySuspendBlock {
                        val bean = QueryExerciseDetailsParam(userId, t.sportId)
                        val serResult = MyRetrofitClient.service.queryExerciseInfo(
                            JsonUtils.getRequestJson(
                                bean,
                                QueryExerciseDetailsParam::class.java
                            )
                        )
                        LogUtils.d("QueryExerciseDetails result = $serResult")
                        serData = serResult.data
                        LogUtils.d("获取运动详情---->")
                        LogUtils.json(serData)
                        userLoginOut(serResult.code)
                    }

                    if (serData != null) {

                        t.exerciseApp = serData?.exerciseApp
                        t.exerciseAuxiliary = serData?.exerciseAuxiliary
                        t.exerciseIndoor = serData?.exerciseIndoor
                        t.exerciseOutdoor = serData?.exerciseOutdoor
                        t.exerciseSwimming = serData?.exerciseSwimming
                        //解压解密
                        val saveInfo = unCompressInfo(t)
                        //存子表
                        saveInfo.exerciseApp?.save()
                        saveInfo.exerciseAuxiliary?.save()
                        saveInfo.exerciseIndoor?.save()
                        saveInfo.exerciseOutdoor?.save()
                        saveInfo.exerciseSwimming?.save()
                        //存总表
                        val isSuccess = saveInfo.save()
                        LogUtils.w("储存运动详情数据成功? =  $isSuccess")

                        result.resume(serData)
                    } else {
                        result.resume(null)
                    }
                }
            }
        }
    }

    /**
     * 解密 解压缩
     */
    fun unCompressInfo(info: SportModleInfo): SportModleInfo {
        AppUtils.tryBlock {
            info.exerciseApp?.mapData?.let {
                info.exerciseApp!!.mapData = unCompress(AESUtils.decrypt(it, JsonUtils.serviceKey))
            }
            info.exerciseIndoor?.recordPointSportData?.let {
                info.exerciseIndoor!!.recordPointSportData =
                    unCompress(AESUtils.decrypt(it, JsonUtils.serviceKey))
            }
            info.exerciseOutdoor?.recordPointSportData?.let {
                info.exerciseOutdoor!!.recordPointSportData =
                    unCompress(AESUtils.decrypt(it, JsonUtils.serviceKey))
            }
            info.exerciseOutdoor?.gpsMapDatas?.let {
                info.exerciseOutdoor!!.gpsMapDatas =
                    unCompress(AESUtils.decrypt(it, JsonUtils.serviceKey))
            }
            info.exerciseSwimming?.recordPointSportData?.let {
                info.exerciseSwimming!!.recordPointSportData =
                    unCompress(AESUtils.decrypt(it, JsonUtils.serviceKey))
            }
            info.exerciseSwimming?.gpsMapDatas?.let {
                info.exerciseSwimming!!.gpsMapDatas =
                    unCompress(AESUtils.decrypt(it, JsonUtils.serviceKey))
            }
        }
        return info
    }

    private fun unCompress(str: String?): String {
        if (str.isNullOrEmpty()) {
            return ""
        }
        try {
            // 创建一个新的输出流
            val out = ByteArrayOutputStream()
            // 创建一个 ByteArrayInputStream，使用 buf 作为其缓冲区数组
            val `in` = ByteArrayInputStream(str.toByteArray(charset("ISO-8859-1")))
            // 使用默认缓冲区大小创建新的输入流
            val gzip = GZIPInputStream(`in`)
            val buffer = ByteArray(256)
            var n = 0
            // 将未压缩数据读入字节数组
            while (gzip.read(buffer).also { n = it } >= 0) {
                out.write(buffer, 0, n)
            }
            // 使用指定的 charsetName，通过解码字节将缓冲区内容转换为字符串
            return out.toString("utf-8")
        } catch (e: IOException) {
            e.printStackTrace()
            return ""
        }
    }
    //endregion

    //region 获取不同类型的累计距离
//    /**
//     * 获取APP运动不同类型的累计距离
//     * */
//    fun getOdometer(sportType: Int) {
//        viewModelScope.launch(Dispatchers.IO){
//            var allMileage = 0f
//            val userId = SpUtils.getValue(SpUtils.USER_ID, "")
//            if(userId.isNotEmpty()) {
//                val list = LitePal.where(
//                    "userId = ? and exerciseType = ?", userId, "$sportType"
//                ).find(SportModleInfo::class.java, true)
//                if(list.isNullOrEmpty()){
//                    odometer.postValue(0f)
//                    return@launch
//                }
//                list.forEach { info->
//                    when(info.dataSources) {
//                        0 -> {
//                            if(info.exerciseType.toInt() == sportType) {
//                                info.exerciseApp?.apply {
//                                    allMileage += sportsMileage.toFloat()
//                                }
//                            }
//                        }
//                        1 -> {
//                            if(info.exerciseType.toInt() == sportType) {
//                                info.exerciseAuxiliary?.apply {
//                                    allMileage += sportsMileage.toFloat()
//                                }
//                            }
//                        }
//                        2 -> {
//                            when(sportType){
//                                0 -> {
//                                    if(info.exerciseType == "1"){
//                                        info.exerciseOutdoor?.apply { allMileage += reportDistance.toFloat() }
//                                    }
//                                }
//                                1 -> {
//                                    if(info.exerciseType == "6"){
//                                        info.exerciseOutdoor?.apply { allMileage += reportDistance.toFloat() }
//                                    }
//                                }
//                                2 ->{
//                                    if(info.exerciseType == "2"){
//                                        info.exerciseOutdoor?.apply { allMileage += reportDistance.toFloat() }
//                                    }
//                                }
//                            }
////                            info.exerciseIndoor?.apply { allMileage += reportDistance.toFloat() }
////                            info.exerciseSwimming?.apply { allMileage += reportDistance.toFloat() }
//                        }
//                    }
//                }
//                odometer.postValue(allMileage)
//            }
//        }
//    }
    //endregion

    //region 计算GPS信号强度
    fun calculateGPS(max: Int, valid: Int): Int {
        if (max != 0 && valid != 0) {
            val value = valid * 1.0f / max
            return when (value) {
                in 0f..0.33f -> {
                    1
                }
                in 0.33f..0.66f -> {
                    2
                }
                else -> 3
            }
        }
        return 0
    }
    //endregion

}
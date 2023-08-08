package com.smartwear.xzfit.utils.manager

import android.app.Activity
import android.content.Intent
import android.text.TextUtils
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.TimeUtils
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataSet
import com.google.android.gms.fitness.data.DataSource
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataUpdateRequest
import com.zhapp.ble.bean.DailyBean
import com.smartwear.xzfit.base.BaseApplication
import com.smartwear.xzfit.ui.user.bean.UserBean
import com.smartwear.xzfit.utils.AppUtils
import com.smartwear.xzfit.utils.SpUtils
import java.lang.ref.WeakReference
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * Created by Android on 2022/5/14.
 * google fit 管理类
 * https://developers.google.com/fit/android
 */
object GoogleFitManager {
    const val TAG = "GoogleFitManager"

    //请求授权的页面
    private var mActivity: WeakReference<Activity>? = null

    /**
     * google Fit授权登录请求码 //TODO 注意尽量不要与其它启动页面的code相同
     * @see com.smartwear.xzfit.ui.HomeActivity.ACTIVITY_REQUEST_CODE
     * @see com.smartwear.xzfit.utils.manager.StravaManager.ACTIVITY_WEB_RESULE_REQUEST_CODE
     * */
    private const val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 1000

    //授权回调接口
    private var mListener: GoogleFitAuthListener? = null

    //google授权的账户
    private var account: GoogleSignInAccount? = null

    //需要订阅的google fit数据类型
    private lateinit var fitnessOptions: FitnessOptions

    /**
     * google fit 授权
     * https://developers.google.cn/fit/android/get-started
     * */
    fun authorizationGoogleFit(activity: Activity?, listener: GoogleFitAuthListener?) {
        mListener = listener
        if (!AppUtils.checkGooglePlayServices(BaseApplication.mContext)) {
            if (mListener != null) mListener?.onFailure("does not support google services!")
            return
        }
        if (activity == null || activity.isDestroyed || activity.isFinishing) {
            if (mListener != null) mListener?.onFailure("a invalid Context object!")
            return
        }
        mActivity = WeakReference(activity)
        mActivity?.get()?.let { context ->
            //初始化google fit数据类型
            initFitnessOptions()
            //获取google账户授权
            account = GoogleSignIn.getAccountForExtension(context, fitnessOptions)
            //检测是否有授权
            if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
                GoogleSignIn.requestPermissions(context, GOOGLE_FIT_PERMISSIONS_REQUEST_CODE, account, fitnessOptions)
            } else {
                LogUtils.d(TAG, "GoogleSignIn requestPermissions Succeeded")
                if (mListener != null) {
                    mListener?.onAccessSucceeded()
                }
            }
        }
    }

    /**
     * 注册Google Fit 需要访问/写入的数据类型
     * */
    private fun initFitnessOptions() {
        if (!::fitnessOptions.isInitialized) {
            fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_WRITE) // 步数
                .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_WRITE) // 总步数
                .addDataType(DataType.AGGREGATE_DISTANCE_DELTA, FitnessOptions.ACCESS_WRITE) // 总距离
                .addDataType(DataType.AGGREGATE_CALORIES_EXPENDED, FitnessOptions.ACCESS_WRITE) // 总卡路里
                .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_WRITE)   //心率BPM
                .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_WRITE)   // 卡路里
                .build()
        }
    }

    /**
     * 处理google fit 授权页面回调
     * */
    fun resultRequestPermissions(requestCode: Int, resultCode: Int, data: Intent?) {
        mActivity?.get()?.let { context ->
            when (requestCode) {
                GOOGLE_FIT_PERMISSIONS_REQUEST_CODE -> {
                    if (resultCode == Activity.RESULT_OK) {
                        LogUtils.d(TAG, "GoogleSignIn requestPermissions Succeeded")
                        if (mListener != null) {
                            mListener?.onAccessSucceeded()
                        }
                    } else {
                        if (mListener != null) {
                            mListener?.onFailure("requestPermissions not successful!")
                        }
                    }
                }
            }
        }
    }

    /**
     * 退出登录
     * https://developers.google.cn/fit/android/disconnect
     */
    fun deauthorizeGoogleFit(activity: Activity?, listener: GoogleFitAuthListener?) {
        if (!AppUtils.checkGooglePlayServices(BaseApplication.mContext)) {
            listener?.onFailure("does not support google services!")
            return
        }
        if (activity == null || activity.isDestroyed || activity.isFinishing) {
            listener?.onFailure("a invalid Context object!")
            return
        }
        mActivity = WeakReference(activity)
        mActivity?.get()?.let { context ->
            initFitnessOptions()
            Fitness.getConfigClient(context, GoogleSignIn.getAccountForExtension(context, fitnessOptions))
                .disableFit()
                .addOnSuccessListener {
                    listener?.onAccessSucceeded()
                }
                .addOnFailureListener { e ->
                    listener?.onFailure("deauthorizeGoogleFit Failure : $e")
                }
        }
    }

    //region 上报步数
    fun postGooglefitData(dailyBean: DailyBean?) {
        //    DailyData
        //    {date='2022-05-14 00:00:00',
        //    stepsFrequency=60,
        //    stepsData=[0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 52, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
        //    distanceFrequency=60,
        //    distanceFata=[0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 38, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
        //    calorieFrequency=60,
        //    calorieData=[0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]}
        if (!SpUtils.getGooglefitSwitch()) {
            LogUtils.e(TAG, "google fit is close")
            return
        }
        if (account == null) {
            authorizationGoogleFit(ActivityUtils.getTopActivity(), object : GoogleFitAuthListener {
                override fun onAccessSucceeded() {
                    postGooglefitData(dailyBean)
                }

                override fun onFailure(msg: String) {
                    LogUtils.e(TAG, "authorizationGoogle google fit onFailure : $msg")
                }

            })
            return
        }
        if (dailyBean != null) {
            val dataTime = TimeUtils.string2Millis(dailyBean.date)
            val oneDayTime = 24 * 3600 * 1000L
            val oneHourTime = 3600 * 1000L
            //步数
            if (dailyBean.stepsData != null) {
                val steps = dailyBean.stepsData
                if (ActivityUtils.getTopActivity() != null && !ActivityUtils.getTopActivity().isDestroyed) {
                    for (i in 0 until steps.size) {
                        val step = steps[i]
                        val startTime = dataTime + oneHourTime * i
                        val endTime = startTime + oneHourTime - 1
                        //LogUtils.d(TAG, "upload google fit start num dailyBean.stepsData " + GsonUtils.toJson(dailyBean.stepsData) + " " + i + " step=" + step);
                        UpLoadGooglefitStep(step, startTime, 3600 * 1000 - 1, ActivityUtils.getTopActivity())
                    }
                    //TODO 0:15 0:45 1:15
                    LogUtils.e(TAG, "upload google fit start num dailyBean.stepsData " + GsonUtils.toJson(dailyBean.stepsData))
                }
            }
        }
    }

    private fun UpLoadGooglefitStep(steps: Int, timemillis: Long, timeMode: Int, activity: Activity) {
        try {
            if (account == null) return
            mActivity?.get()?.let { context ->
                val stepSource = DataSource.Builder()
                    .setAppPackageName(activity.applicationContext.packageName)
                    .setStreamName("Googlefit" + " - step count")
                    .setDataType(DataType.AGGREGATE_STEP_COUNT_DELTA)
                    .setType(DataSource.TYPE_RAW)
                    .build()
                val dataSet = DataSet.create(stepSource)
                val stepDataPoint = dataSet.createDataPoint()
                stepDataPoint.setTimeInterval(timemillis, timemillis + timeMode, TimeUnit.MILLISECONDS)
                stepDataPoint.getValue(Field.FIELD_STEPS).setInt(steps)
                dataSet.add(stepDataPoint)
                Fitness.getHistoryClient(context, account!!)
                    .insertData(dataSet)
                    .addOnCompleteListener {
                        //LogUtils.d(TAG, "AGGREGATE_STEP_COUNT_DELTA OnComplete")
                    }.addOnFailureListener { e ->
                        LogUtils.d(TAG, "AGGREGATE_STEP_COUNT_DELTA Failure : $e")
                    }
                val request = DataUpdateRequest.Builder()
                    .setDataSet(dataSet)
                    .setTimeInterval(timemillis, timemillis + timeMode, TimeUnit.MILLISECONDS)
                    .build()
                Fitness.getHistoryClient(context, account!!).updateData(request)
                    .addOnCompleteListener {
                        //LogUtils.d(TAG, "AGGREGATE_STEP_COUNT_DELTA Complete")
                    }.addOnFailureListener { e ->
                        LogUtils.d(TAG, "AGGREGATE_STEP_COUNT_DELTA Failure : $e")
                    }
                val distanceSource = DataSource.Builder()
                    .setAppPackageName(activity.applicationContext.packageName)
                    .setStreamName("Googlefit" + " - distance count")
                    .setDataType(DataType.AGGREGATE_DISTANCE_DELTA)
                    .setType(DataSource.TYPE_RAW)
                    .build()
                val distanceSet = DataSet.create(distanceSource)
                val distanceDataPoint = distanceSet.createDataPoint()
                distanceDataPoint.setTimeInterval(timemillis, timemillis + timeMode, TimeUnit.MILLISECONDS)
                val distance: Float = getDistance(steps).toFloat()
                distanceDataPoint.getValue(Field.FIELD_DISTANCE).setFloat(distance)
                distanceSet.add(distanceDataPoint)
                Fitness.getHistoryClient(context, account!!)
                    .insertData(distanceSet)
                    .addOnCompleteListener {
                        //LogUtils.d(TAG, "AGGREGATE_DISTANCE_DELTA Complete")
                    }.addOnFailureListener { e ->
                        LogUtils.d(TAG, "AGGREGATE_DISTANCE_DELTA Failure : $e")
                    }
                val calDataSource = DataSource.Builder()
                    .setAppPackageName(activity.applicationContext.packageName)
                    .setStreamName("Googlefit" + " - cal count")
                    .setDataType(DataType.AGGREGATE_CALORIES_EXPENDED)
                    .setType(DataSource.TYPE_RAW)
                    .build()
                val calSet = DataSet.create(calDataSource)
                val calDataPoint = calSet.createDataPoint()
                calDataPoint.setTimeInterval(timemillis, timemillis + timeMode, TimeUnit.MILLISECONDS)
                val calorie: Float = getCalory(steps).toFloat()
                calDataPoint.getValue(Field.FIELD_CALORIES).setFloat(calorie)
                calSet.add(calDataPoint)
                Fitness.getHistoryClient(context, account!!)
                    .insertData(calSet)
                    .addOnCompleteListener {
                        //LogUtils.d(TAG, "AGGREGATE_CALORIES_EXPENDED Complete")
                    }.addOnFailureListener { e ->
                        LogUtils.d(TAG, "AGGREGATE_CALORIES_EXPENDED Failure : $e")

                    }
            }
        } catch (e: Exception) {
            LogUtils.i(TAG, "Data insert Exception!$e")
        }
    }

    fun getDistance(sportStep: Int): String {
        val (_, height1, _, weight1) = UserBean().getData()
        var height = 170
        if (TextUtils.isEmpty(height1)) {
            height = Math.ceil(height1.toFloat().toDouble()).toInt()
        }
        var weight = 65
        if (TextUtils.isEmpty(weight1)) {
            weight = Math.ceil(weight1.toFloat().toDouble()).toInt()
        }
        return getTwoFormat(getTwoDistance(height.toFloat(), sportStep))
    }

    fun getTwoFormat(value: Float): String {
        //LogUtils.i(TAG, "GetTwoFormat 算法验证 = value = $value")
        return if (!checkValueIsTwo(value)) {
            val result = (value * 100).toInt().toFloat() / 100
            val decimalFormat = DecimalFormat("0.00", DecimalFormatSymbols(Locale.ENGLISH)) //构造方法的字符格式这里如果小数不足2位,会以0补足.
            var distanceString = decimalFormat.format(result.toDouble()) //format 返回的是字符串
            distanceString = distanceString.replace(",", ".")
            //LogUtils.i(TAG, "GetTwoFormat 算法验证 distanceString = $distanceString")
            distanceString
        } else {
            val distanceString = value.toString()
            //LogUtils.i(TAG, "GetTwoFormat 算法验证 distanceString = $distanceString")
            distanceString
        }
    }

    /**
     * 距离算法2
     *
     * @param height
     * @param sportStep
     * @return
     */
    fun getTwoDistance(height: Float, sportStep: Int): Float {
        var bleDistance = 0f
        bleDistance = (height * 41 * sportStep * 0.00001 * 10 * 0.001).toFloat()
        return bleDistance
    }

    /**
     * 检查输入的 float类型，是否是两位小数。
     *
     * @return
     */
    fun checkValueIsTwo(value: Float): Boolean {
        var value_str = value.toString()
        value_str = value_str.replace(".", ",")
        val data = value_str.split(",".toRegex()).toTypedArray()
        return if (data.size == 2) {
            if (data[1].length == 2) {
                true
            } else {
                false
            }
        } else {
            false
        }
    }

    fun getCalory(sportStep: Int): String {
        val (_, height1, _, weight1) = UserBean().getData()
        var height = 170
        if (TextUtils.isEmpty(height1)) {
            height = Math.ceil(height1.toFloat().toDouble()).toInt()
        }
        var weight = 65
        if (TextUtils.isEmpty(weight1)) {
            weight = Math.ceil(weight1.toFloat().toDouble()).toInt()
        }
        return getTwoFormat(getTwoCalory(height.toFloat(), weight.toFloat(), sportStep))
    }

    /**
     * 卡路里算法2
     *
     * @param height
     * @param weight
     * @param sportStep
     * @return
     */
    fun getTwoCalory(height: Float, weight: Float, sportStep: Int): Float {
        var bleCalory = 0f
        bleCalory = (weight * 1.036 * height * 0.41 * sportStep * 0.00001).toFloat()
        return bleCalory
    }
    //endregion

    //region 总步数
//    private fun upStep(step: Int, startTime: Long, endTime: Long) {
//        val dataSet = createDataForRequest(
//            DataType.TYPE_STEP_COUNT_DELTA, Field.FIELD_STEPS,
//            step, startTime, endTime, ActivityUtils.getTopActivity()
//        ) // 步数;
//        upLoadGoogleFitData(dataSet)
//    }
//
//    fun createDataForRequest(dataType: DataType, field: Field?, values: Any?, startTime: Long, endTime: Long, activity: Activity?): DataSet {
//        val dataSource = DataSource.Builder()
//            .setAppPackageName(activity)
//            .setDataType(dataType)
//            .setStreamName("streamName")
//            .setType(DataSource.TYPE_RAW)
//            .build()
//        val dataSet = DataSet.create(dataSource)
//        var dataPoint = dataSet.createDataPoint().setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
//        if (dataType === DataType.TYPE_CALORIES_EXPENDED || dataType === DataType.TYPE_HEART_RATE_BPM) {
//            dataPoint.getValue(field).setFloat((values as Float?)!!)
//        } else {
//            //如果是float类型则要调用setFloagValues
//            if (values is Int) {
//                dataPoint.setIntValues((values as Int?)!!)
//            } else {
//                dataPoint = dataPoint.setFloatValues((values as Float?)!!)
//            }
//        }
//        dataSet.add(dataPoint)
//        return dataSet
//    }
//
//    fun upLoadGoogleFitData(stepDataSet: DataSet?) {
//        if (account == null) return
//        mActivity!!.get()?.let { context ->
//            val responseStep = Fitness
//                .getHistoryClient(context, account!!)
//                .insertData(stepDataSet)
//                .addOnCompleteListener {
//                    LogUtils.d(TAG, "TYPE_STEP_COUNT_DELTA Complete")
//                }.addOnFailureListener { e ->
//                    LogUtils.d(TAG, "TYPE_STEP_COUNT_DELTA Failure : $e")
//                }
//            LogUtils.e(TAG, "upload google fit start")
//        }
//    }
    //endregion

    //region 心率
//    fun postHeart(mHeartInfo: HeartInfo?, context: Context?) {
//        if (!SpUtils.getGooglefitSwitch()) {
//            LogUtils.e(TAG, "google fit is close")
//            return
//        }
//        if (account == null) {
//            authorizationGoogle(ActivityUtils.getTopActivity(), object : AuthListener {
//                override fun onAccessSucceeded() {
//                    postHeart(mHeartInfo)
//                }
//
//                override fun onFailure(msg: String) {
//                    LogUtils.e(TAG, "authorizationGoogle google fit onFailure : $msg")
//                }
//
//            })
//            return
//        }
//        try {
//            val mHeartModel = HeartModel(mHeartInfo)
//            val heartData: Array<String> = mHeartModel.getHeartData().split(",")
//            val time: Long = NewTimeUtils.getLongTime(mHeartModel.getHeartDate(), NewTimeUtils.TIME_YYYY_MM_DD)
//            val mCalendar = Calendar.getInstance()
//            mCalendar.timeInMillis = time
//            mCalendar[Calendar.HOUR_OF_DAY] = 0
//            mCalendar[Calendar.MINUTE] = 0
//            mCalendar[Calendar.SECOND] = 0
//            mCalendar[Calendar.MILLISECOND] = 0
//            for (i in heartData.indices) {
//                val bmp = heartData[i].toInt()
//                if (bmp == 0) {
//                    continue
//                }
//                updateHeart(context, bmp.toFloat(), mCalendar.timeInMillis + 5 * 60 * 1000 * i)
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
//
//    private fun updateHeart(activity: Context, bmp: Float, timemillis: Long) {
//        if (account == null) {
//            return
//        }
//        //"yyyy-MM-dd HH:mm:ss"
//        val currentTime = TimeUtils.date2String(Date(timemillis), "yyyy-MM-dd HH:mm:ss")
//        LogUtils.i(TAG, "updateHeart heart = $bmp timemillis currentTime = $currentTime")
//
//        val heartSource = DataSource.Builder()
//            .setAppPackageName(activity.applicationContext.packageName)
//            .setStreamName("Googlefit" + " - heartrate ")
//            .setDataType(DataType.TYPE_HEART_RATE_BPM)
//            .setType(DataSource.TYPE_RAW)
//            .build()
//        val heartdataSet = DataSet.create(heartSource)
//        val heartDataPoint = heartdataSet.createDataPoint()
//        heartDataPoint.setTimestamp(timemillis / 60000, TimeUnit.MINUTES)
//        heartDataPoint.getValue(Field.FIELD_BPM).setFloat(bmp)
//        heartdataSet.add(heartDataPoint)
//        Fitness.getHistoryClient(context, account).insertData(heartdataSet).addOnCompleteListener {
//
//        }.addOnFailureListener { e ->
//              LogUtils.d(TAG,"TYPE_HEART_RATE_BPM Failure : $e")
//        }
//        LogUtils.i(TAG, "heartdataSet insert was successful!")
//    }
    //endregion


    interface GoogleFitAuthListener {
        fun onAccessSucceeded()
        fun onFailure(msg: String)
    }
}